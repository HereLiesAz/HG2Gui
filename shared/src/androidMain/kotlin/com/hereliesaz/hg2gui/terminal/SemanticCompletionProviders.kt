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
        addAll(gitRemoteCandidates(context, request))
        addAll(sshHostCandidates(context, request))
        addAll(serviceCandidates(context, request))
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
                    priority = 30,
                    enumerationComplete = true
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
                    priority = 40,
                    enumerationComplete = true
                )
            }
            .toList()
    }

    private fun gitRemoteCandidates(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val words = request.beforeCursor.trimStart().split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.firstOrNull()?.substringAfterLast('/') != "git") return emptyList()

        val operation = words.getOrNull(1) ?: return emptyList()
        val remoteOperation = operation in setOf("fetch", "pull", "push") ||
            (operation == "remote" && words.getOrNull(2) in setOf("get-url", "remove", "rename", "set-head", "set-url", "show", "prune"))
        if (!remoteOperation) return emptyList()

        val git = executable(context, "git") ?: return emptyList()
        val output = runProbe(git, listOf("remote"), request.cwd) ?: return emptyList()
        return output.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .filter { it.startsWith(request.tokenPrefix, ignoreCase = true) }
            .distinct()
            .sorted()
            .take(MAX_CANDIDATES)
            .map { remote ->
                CompletionCandidate(
                    value = remote,
                    description = "git remote",
                    kind = CompletionKind.VALUE,
                    source = CompletionSource.COMMAND_PROTOCOL,
                    priority = 38,
                    enumerationComplete = false
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
                    priority = 35,
                    enumerationComplete = false
                )
            }
            .toList()
    }

    private fun serviceCandidates(context: Context, request: CompletionRequest): List<CompletionCandidate> {
        val words = request.beforeCursor.trimStart().split(Regex("\\s+")).filter(String::isNotBlank)
        val verb = words.firstOrNull()?.substringAfterLast('/') ?: return emptyList()
        val acceptsService = when (verb) {
            "sv-enable", "sv-disable" -> true
            "sv" -> words.getOrNull(1) in setOf(
                "up", "down", "status", "once", "pause", "cont", "hup", "alarm",
                "interrupt", "1", "2", "term", "kill", "exit", "start", "stop", "restart"
            )
            else -> false
        }
        if (!acceptsService) return emptyList()

        val serviceDir = File(DistroManager.prefixDir(context), "var/service")
        val services = serviceDir.listFiles { file -> file.isDirectory && File(file, "run").exists() }
            ?: return emptyList()
        return services.asSequence()
            .map(File::getName)
            .filter { it.startsWith(request.tokenPrefix, ignoreCase = true) }
            .distinct()
            .sorted()
            .take(MAX_CANDIDATES)
            .map { service ->
                CompletionCandidate(
                    value = service,
                    description = "Termux runit service",
                    kind = CompletionKind.SERVICE,
                    source = CompletionSource.COMMAND_PROTOCOL,
                    priority = 40,
                    // sv-enable/sv-disable specifically operate on the configured service names.
                    // Plain sv also accepts explicit service paths, so its inventory is advisory.
                    enumerationComplete = verb == "sv-enable" || verb == "sv-disable"
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
