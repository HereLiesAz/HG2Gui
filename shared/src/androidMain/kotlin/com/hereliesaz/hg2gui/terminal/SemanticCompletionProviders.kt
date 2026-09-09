package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Completion sources whose values come from HG2Gui/runtime state rather than a shell's generic
 * completion engine. They are intentionally narrow and data-driven: if a value cannot be
 * enumerated safely, this provider returns nothing and leaves free-form input available.
 */
object SemanticCompletionProviders {
    private const val TIMEOUT_MS = 1_500L
    private const val MAX_CANDIDATES = 200

    fun complete(context: Context, request: CompletionRequest): List<CompletionCandidate> = buildList {
        addAll(packageCandidates(context, request))
        addAll(gitBranchCandidates(context, request))
        addAll(sshHostCandidates(context, request))
    }.take(MAX_CANDIDATES)

    private fun packageCandidates(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val words = request.beforeCursor.trimStart().split(Regex("\\s+")).filter(String::isNotBlank)
        val verb = words.firstOrNull()?.substringAfterLast('/')?.lowercase() ?: return emptyList()
        if (verb !in setOf("pkg", "hg2pkg", "apt", "apt-get", "dpkg")) return emptyList()

        val operation = words.getOrNull(1)?.lowercase()
        val installedOps = setOf("remove", "rm", "uninstall", "purge", "show", "info")
        if (operation !in installedOps) return emptyList()

        val prefix = DistroManager.prefixDir(context)
        return DpkgCatalog.installedVersions(prefix).entries
            .asSequence()
            .filter { (name, _) -> name.startsWith(request.tokenPrefix, ignoreCase = true) }
            .sortedBy { it.key.lowercase() }
            .take(MAX_CANDIDATES)
            .map { (name, version) ->
                CompletionCandidate(
                    value = name,
                    description = version.takeIf(String::isNotBlank)?.let { "installed $it" } ?: "installed package",
                    kind = CompletionKind.PACKAGE,
                    source = CompletionSource.PACKAGE_MANAGER,
                    priority = 30
                )
            }
            .toList()
    }

    private fun gitBranchCandidates(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val words = request.beforeCursor.trimStart().split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.firstOrNull()?.substringAfterLast('/') != "git") return emptyList()
        if (words.getOrNull(1) !in setOf("checkout", "switch", "branch", "merge", "rebase")) return emptyList()

        val git = executable(context, "git") ?: return emptyList()
        val output = runProbe(
            executable = git,
            args = listOf("for-each-ref", "--format=%(refname:short)", "refs/heads", "refs/remotes"),
            cwd = request.cwd
        ) ?: return emptyList()

        return output.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .filterNot { it.endsWith("/HEAD") }
            .filter { it.startsWith(request.tokenPrefix, ignoreCase = true) }
            .distinct()
            .sorted()
            .take(MAX_CANDIDATES)
            .map { branch ->
                CompletionCandidate(
                    value = branch,
                    description = "git branch",
                    kind = CompletionKind.BRANCH,
                    source = CompletionSource.COMMAND_PROTOCOL,
                    priority = 40
                )
            }
            .toList()
    }

    private fun sshHostCandidates(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val words = request.beforeCursor.trimStart().split(Regex("\\s+")).filter(String::isNotBlank)
        val verb = words.firstOrNull()?.substringAfterLast('/') ?: return emptyList()
        if (verb !in setOf("ssh", "scp", "sftp")) return emptyList()

        val home = DistroManager.homeDir(context)
        val hosts = linkedSetOf<String>()
        collectSshConfigHosts(File(home, ".ssh/config"), hosts)
        collectKnownHosts(File(home, ".ssh/known_hosts"), hosts)

        return hosts.asSequence()
            .filter { it.startsWith(request.tokenPrefix, ignoreCase = true) }
            .sorted()
            .take(MAX_CANDIDATES)
            .map { host ->
                CompletionCandidate(
                    value = host,
                    description = "known SSH host",
                    kind = CompletionKind.HOST,
                    source = CompletionSource.COMMAND_PROTOCOL,
                    priority = 35
                )
            }
            .toList()
    }

    private fun collectSshConfigHosts(file: File, sink: MutableSet<String>) {
        if (!file.isFile) return
        try {
            file.forEachLine { line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("Host ", ignoreCase = true)) return@forEachLine
                trimmed.substringAfter(' ')
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() && '*' !in it && '?' !in it && '!' !in it }
                    .forEach(sink::add)
            }
        } catch (_: Exception) {
            // Completion is advisory. An unreadable config contributes no candidates.
        }
    }

    private fun collectKnownHosts(file: File, sink: MutableSet<String>) {
        if (!file.isFile) return
        try {
            file.forEachLine { line ->
                val field = line.trim().substringBefore(' ')
                if (field.isBlank() || field.startsWith("|")) return@forEachLine
                field.split(',').forEach { raw ->
                    val host = raw.removePrefix("[").substringBefore("]:").substringBefore(':')
                    if (host.isNotBlank()) sink += host
                }
            }
        } catch (_: Exception) {
            // Completion is advisory. An unreadable known_hosts contributes no candidates.
        }
    }

    private fun executable(context: Context, name: String): File? {
        val prefix = DistroManager.prefixDir(context)
        return sequenceOf(
            File(prefix, "bin/$name"),
            File(context.applicationInfo.nativeLibraryDir, "libbin_$name.so")
        ).firstOrNull(File::canExecute)
    }

    private fun runProbe(executable: File, args: List<String>, cwd: String?): String? = try {
        val builder = ProcessBuilder(listOf(executable.absolutePath) + args)
        cwd?.let(::File)?.takeIf(File::isDirectory)?.let(builder::directory)
        builder.redirectErrorStream(true)
        val process = builder.start()
        if (!process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            null
        } else {
            process.inputStream.bufferedReader().use { it.readText() }
        }
    } catch (_: Exception) {
        null
    }
}
