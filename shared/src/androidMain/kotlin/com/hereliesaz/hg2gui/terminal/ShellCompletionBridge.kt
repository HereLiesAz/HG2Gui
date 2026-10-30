package com.hereliesaz.hg2gui.terminal

import android.content.Context
import com.hereliesaz.hg2gui.util.collectOutput
import java.io.File

/**
 * Android runtime adapters for shell completion systems. Completion probes never execute the
 * partially composed command. The user's text is passed as data to the shell's completion
 * primitive (or used only as a prefix for command/file discovery).
 *
 * Probes run in the Termux execution environment (the same prefix/linker/exec-interceptor setup the
 * session shell gets), never as a login shell: a login shell would source the user's dotfiles on
 * every keystroke. A probe that exits with an unexpected code is treated as failed, not as output,
 * so a launch error is never shown to the user as a list of completions. Candidates are returned
 * with raw values; shell quoting of what gets inserted happens in
 * [CompletionNormalizer.merge] so every source is covered the same way.
 */
object ShellCompletionBridge {
    private const val TIMEOUT_MS = 1_500L
    private const val MAX_OUTPUT_CHARS = 64_000
    private const val MAX_DEFINITION_CHARS = 256_000
    private const val MAX_CANDIDATES = 200

    /**
     * Exit codes that are a normal answer: 0, and 1 for "no matches" (`compgen`, fish `complete`).
     * Anything else (126/127 not executable or not found, 128+n killed by a signal such as the
     * linker's abort) means the probe did not run and its output must not be parsed.
     */
    internal val PROBE_OK_EXIT_CODES: Set<Int> = setOf(0, 1)

    private const val BASH_SCRIPT =
        "prefix=\$1; if [ \"\$2\" = command ]; then compgen -A command -- \"\$prefix\"; else compgen -f -- \"\$prefix\"; fi"

    /**
     * Arguments for the bash completion probe. `--noprofile --norc` (not `-l`) so no login
     * dotfile is sourced per keystroke; the typed word travels as `$1`, data and never code.
     */
    internal fun bashProbeArgs(tokenValue: String, mode: String): List<String> =
        listOf("--noprofile", "--norc", "-c", BASH_SCRIPT, "hg2gui-complete", tokenValue, mode)

    fun complete(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val shellCandidates = when (request.provider) {
            ShellCompletionProvider.FISH_COMPLETION -> completeFish(context, request)
            ShellCompletionProvider.ZSH_COMPLETION -> completeZsh(context, request)
            ShellCompletionProvider.BASH_COMPLETION -> completeBash(context, request)
            ShellCompletionProvider.NONE -> completeFilesystem(request)
        }
        val staticDefinitionCandidates = completeStaticDefinitions(context, request)
        val semanticCandidates = SemanticCompletionProviders.complete(context, request)
        return CompletionNormalizer.filterFor(
            request,
            CompletionNormalizer.merge(semanticCandidates + staticDefinitionCandidates + shellCandidates)
        ).take(MAX_CANDIDATES)
    }

    private fun completeFish(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val fish = executable(context, "fish") ?: return completeFilesystem(request)
        val output = runProbe(
            context = context,
            executable = fish,
            args = listOf("-c", "complete -C \"\$argv[1]\"", "hg2gui-complete", request.beforeCursor),
            cwd = request.cwd
        ) ?: return completeFilesystem(request)
        return CompletionNormalizer.parseFish(output)
    }

    private fun completeBash(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val bash = nativeBash(context) ?: executable(context, "bash") ?: return completeFilesystem(request)
        val commandPosition = request.isCommandPosition
        val mode = if (commandPosition) "command" else "file"
        val typed = request.tokenValue
        val output = runProbe(
            context = context,
            executable = bash,
            args = bashProbeArgs(typed, mode),
            cwd = request.cwd
        ) ?: return completeFilesystem(request)
        return CompletionNormalizer.parseBash(output).map { candidate ->
            if (commandPosition) {
                candidate.copy(kind = CompletionKind.COMMAND)
            } else {
                // The value is rebuilt whole: copy() would keep the old insertText, which the
                // quoting step then mistakes for a provider-supplied (already quoted) one.
                val guarded = CompletionNormalizer.guardOptionLookalike(candidate.value, typed)
                candidate.copy(value = guarded, label = guarded, insertText = guarded)
            }
        }
    }

    private fun completeZsh(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val zsh = executable(context, "zsh") ?: return completeFilesystem(request)
        if (!request.isCommandPosition) return completeFilesystem(request)
        val script = "prefix=\$1; print -rl -- \${(k)commands[(I)\${prefix}*]}"
        val output = runProbe(
            context = context,
            executable = zsh,
            args = listOf("-fc", script, "hg2gui-complete", request.tokenValue),
            cwd = request.cwd
        ) ?: return emptyList()
        return CompletionNormalizer.parseZsh(output).map { it.copy(kind = CompletionKind.COMMAND) }
    }

    /**
     * Reads only declarative completion files as text. It never sources them. Bash/Zsh completion
     * functions can execute arbitrary shell code, so dynamic definitions are intentionally left to
     * the conservative fallback rather than being run as a side effect of opening HG2Gui UI.
     */
    private fun completeStaticDefinitions(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val command = CompletionWords
            .unquote(request.beforeCursor.trimStart().takeWhile { !it.isWhitespace() })
            .substringAfterLast('/')
        if (command.isBlank()) return emptyList()
        val prefix = DistroManager.prefixDir(context)
        val home = DistroManager.homeDir(context)

        val files = when (request.provider) {
            ShellCompletionProvider.BASH_COMPLETION -> listOf(
                File(prefix, "share/bash-completion/completions/$command"),
                File(prefix, "etc/bash_completion.d/$command"),
                File(home, ".local/share/bash-completion/completions/$command")
            )
            ShellCompletionProvider.ZSH_COMPLETION -> listOf(
                File(prefix, "share/zsh/site-functions/_$command"),
                File(home, ".zfunc/_$command"),
                File(home, ".local/share/zsh/site-functions/_$command")
            )
            else -> emptyList()
        }

        return files.asSequence()
            .filter(File::isFile)
            .mapNotNull(::readDefinition)
            .flatMap { text ->
                when (request.provider) {
                    ShellCompletionProvider.BASH_COMPLETION -> StaticCompletionDefinitions.parseBash(text, request.tokenValue)
                    ShellCompletionProvider.ZSH_COMPLETION -> StaticCompletionDefinitions.parseZsh(text, request.tokenValue)
                    else -> emptyList()
                }.asSequence()
            }
            .take(MAX_CANDIDATES)
            .toList()
    }

    private fun readDefinition(file: File): String? = try {
        if (file.length() > MAX_DEFINITION_CHARS) return null
        file.readText().take(MAX_DEFINITION_CHARS)
    } catch (_: Exception) {
        null
    }

    private fun completeFilesystem(request: CompletionRequest): List<CompletionCandidate> {
        val cwd = request.cwd?.let(::File)?.takeIf(File::isDirectory) ?: return emptyList()
        // The word as the shell would see it: `My\ Fi` must list "My File", not look for "My\ Fi".
        val prefix = request.tokenValue
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
                val value = CompletionNormalizer.guardOptionLookalike(parentPart + file.name + suffix, prefix)
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

    /**
     * Runs one completion probe and returns its stdout, or null when it could not run, timed out,
     * or exited with a code outside [acceptedExitCodes]. stderr is deliberately not merged into
     * stdout: warnings and linker errors must never become completion rows.
     */
    private fun runProbe(
        context: Context,
        executable: File,
        args: List<String>,
        cwd: String?,
        acceptedExitCodes: Set<Int> = PROBE_OK_EXIT_CODES
    ): String? {
        return try {
            val builder = ProcessBuilder(listOf(executable.absolutePath) + args)
            cwd?.let(::File)?.takeIf(File::isDirectory)?.let(builder::directory)
            applyProbeEnvironment(context, builder.environment())
            val process = builder.start()
            // Drained while waiting: a large completion list would otherwise fill the pipe and
            // stall the shell until the timeout discarded a healthy result.
            val result = process.collectOutput(TIMEOUT_MS, MAX_OUTPUT_CHARS * 4) ?: return null
            val text = result.textIfExit(acceptedExitCodes) ?: return null
            if (text.length <= MAX_OUTPUT_CHARS) text else text.take(MAX_OUTPUT_CHARS)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * The environment a Termux-built shell needs to start at all (prefix, linker, exec
     * interceptor), as SemanticCompletionProviders gives git, plus the shell-session basics (PATH,
     * LD_LIBRARY_PATH) so `compgen -A command` sees the prefix's commands. BASH_ENV/ENV are removed:
     * a non-interactive bash would otherwise source whatever file they name on every probe.
     */
    private fun applyProbeEnvironment(context: Context, env: MutableMap<String, String>) {
        runCatching { Hg2ExecEnvironment.apply(context, env) }
        val prefix = DistroManager.prefixDir(context).absolutePath
        env["PATH"] = listOfNotNull("$prefix/bin", env["PATH"]?.takeIf(String::isNotBlank)).joinToString(":")
        env["LD_LIBRARY_PATH"] = "$prefix/lib"
        env.remove("BASH_ENV")
        env.remove("ENV")
    }
}
