package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Android runtime adapters for shell completion systems. Completion probes never execute the
 * partially composed command. The user's text is passed as data to the shell's completion
 * primitive (or used only as a prefix for command/file discovery).
 */
object ShellCompletionBridge {
    private const val TIMEOUT_MS = 1_500L
    private const val MAX_OUTPUT_CHARS = 64_000
    private const val MAX_CANDIDATES = 200

    fun complete(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val shellCandidates = when (request.provider) {
            ShellCompletionProvider.FISH_COMPLETION -> completeFish(context, request)
            ShellCompletionProvider.ZSH_COMPLETION -> completeZsh(context, request)
            ShellCompletionProvider.BASH_COMPLETION -> completeBash(context, request)
            ShellCompletionProvider.NONE -> completeFilesystem(request)
        }
        val semanticCandidates = SemanticCompletionProviders.complete(context, request)
        return CompletionNormalizer.filterFor(
            request,
            CompletionNormalizer.merge(semanticCandidates + shellCandidates)
        ).take(MAX_CANDIDATES)
    }

    private fun completeFish(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val fish = executable(context, "fish") ?: return completeFilesystem(request)
        val output = runProbe(
            executable = fish,
            args = listOf("-c", "complete -C \"\$argv[1]\"", "hg2gui-complete", request.beforeCursor),
            cwd = request.cwd
        ) ?: return completeFilesystem(request)
        return CompletionNormalizer.parseFish(output)
    }

    private fun completeBash(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val bash = nativeBash(context) ?: executable(context, "bash") ?: return completeFilesystem(request)
        val commandPosition = request.beforeCursor.trimStart().contains(' ').not()
        val mode = if (commandPosition) "command" else "file"
        val script = "prefix=\$1; if [ \"\$2\" = command ]; then compgen -A command -- \"\$prefix\"; else compgen -f -- \"\$prefix\"; fi"
        val output = runProbe(
            executable = bash,
            args = listOf("-lc", script, "hg2gui-complete", request.tokenPrefix, mode),
            cwd = request.cwd
        ) ?: return completeFilesystem(request)
        return CompletionNormalizer.parseBash(output).map { candidate ->
            if (commandPosition) candidate.copy(kind = CompletionKind.COMMAND) else candidate
        }
    }

    private fun completeZsh(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val zsh = executable(context, "zsh") ?: return completeFilesystem(request)
        val commandPosition = request.beforeCursor.trimStart().contains(' ').not()
        if (!commandPosition) return completeFilesystem(request)
        val script = "prefix=\$1; print -rl -- \${(k)commands[(I)\${prefix}*]}"
        val output = runProbe(
            executable = zsh,
            args = listOf("-fc", script, "hg2gui-complete", request.tokenPrefix),
            cwd = request.cwd
        ) ?: return emptyList()
        return CompletionNormalizer.parseZsh(output).map { it.copy(kind = CompletionKind.COMMAND) }
    }

    private fun completeFilesystem(request: CompletionRequest): List<CompletionCandidate> {
        val cwd = request.cwd?.let(::File)?.takeIf(File::isDirectory) ?: return emptyList()
        val prefix = request.tokenPrefix
        val slash = prefix.lastIndexOf('/')
        val parentPart = if (slash >= 0) prefix.substring(0, slash + 1) else ""
        val namePrefix = if (slash >= 0) prefix.substring(slash + 1) else prefix
        val parent = when {
            parentPart.isEmpty() -> cwd
            parentPart.startsWith('/') -> File(parentPart)
            else -> File(cwd, parentPart)
        }
        val entries = parent.listFiles() ?: return emptyList()
        return entries.asSequence()
            .filter { it.name.startsWith(namePrefix, ignoreCase = true) }
            .sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
            .take(MAX_CANDIDATES)
            .map { file ->
                val suffix = if (file.isDirectory) "/" else ""
                val value = parentPart + file.name + suffix
                CompletionCandidate(
                    value = value,
                    kind = if (file.isDirectory) CompletionKind.DIRECTORY else CompletionKind.FILE,
                    source = CompletionSource.FILESYSTEM,
                    appendSpace = !file.isDirectory
                )
            }
            .toList()
    }

    private fun nativeBash(context: Context): File? =
        File(context.applicationInfo.nativeLibraryDir, "libbin_bash.so").takeIf(File::canExecute)

    private fun executable(context: Context, name: String): File? {
        val prefix = DistroManager.prefixDir(context)
        return sequenceOf(
            File(prefix, "bin/$name"),
            File(prefix, "bin/${name}3"),
            File(context.applicationInfo.nativeLibraryDir, "libbin_$name.so")
        ).firstOrNull(File::canExecute)
    }

    private fun runProbe(executable: File, args: List<String>, cwd: String?): String? {
        return try {
            val builder = ProcessBuilder(listOf(executable.absolutePath) + args)
            cwd?.let(::File)?.takeIf(File::isDirectory)?.let(builder::directory)
            builder.redirectErrorStream(true)
            val process = builder.start()
            val finished = process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return null
            }
            process.inputStream.bufferedReader().use { reader ->
                val text = reader.readText()
                if (text.length <= MAX_OUTPUT_CHARS) text else text.take(MAX_OUTPUT_CHARS)
            }
        } catch (_: Exception) {
            null
        }
    }
}
