package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.os.Build
import com.hereliesaz.hg2gui.ui.ShellIdentitySource
import com.hereliesaz.hg2gui.ui.ShellStatusProjector
import com.hereliesaz.hg2gui.util.collectOutput
import java.io.File

/**
 * Completion sources whose values come from HG2Gui/runtime state rather than a shell's generic
 * completion engine. They are intentionally narrow and data-driven: if a value cannot be
 * enumerated safely, this provider returns nothing and leaves free-form input available.
 *
 * Candidates carry raw values (branch names, hosts, package names); the typed word is matched in
 * its unquoted form ([CompletionRequest.tokenValue]) and quoting for insertion is applied once, by
 * [CompletionNormalizer.merge].
 */
object SemanticCompletionProviders {
    private const val TIMEOUT_MS = 1_500L
    private const val MAX_OUTPUT_BYTES = 256 * 1024
    private val REMOTE_HEAD = Regex("refs/remotes/[^/]+/HEAD")
    private const val MAX_CANDIDATES = 200

    fun complete(context: Context, request: CompletionRequest): List<CompletionCandidate> = buildList {
        addAll(packageCandidates(context, request))
        addAll(gitBranchCandidates(context, request))
        addAll(gitRemoteCandidates(context, request))
        addAll(sshHostCandidates(context, request))
        addAll(serviceCandidates(context, request))
    }.take(MAX_CANDIDATES)

    /** Safe read-only branch inventory reused by the native structured-prompt branch chooser. */
    fun gitBranches(context: Context, cwd: String?): List<String> {
        val git = executable(context, "git") ?: return emptyList()
        val output = runProbe(
            context = context,
            executable = git,
            args = listOf("for-each-ref", "--format=%(refname)", "refs/heads", "refs/remotes"),
            cwd = cwd
        ) ?: return emptyList()
        return output.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            // Only a remote's symbolic HEAD (refs/remotes/<remote>/HEAD) is noise; a local branch
            // that merely ends in "/HEAD" is a real branch, so filter on the full refname.
            .filterNot { REMOTE_HEAD.matches(it) }
            .map { it.removePrefix("refs/heads/").removePrefix("refs/remotes/") }
            .distinct()
            .sorted()
            .take(MAX_CANDIDATES)
            .toList()
    }

    /**
     * `git status --porcelain=v1 --branch` for the terminal's branch/dirty status chip: the raw
     * output, or null when git is not installed, [cwd] is not inside a repository, or the probe
     * timed out. Read-only (`--no-optional-locks` keeps it from touching the index).
     */
    fun gitStatusBranch(context: Context, cwd: String?): String? {
        val git = executable(context, "git") ?: return null
        return runProbe(
            context = context,
            executable = git,
            args = listOf("--no-optional-locks", "status", "--porcelain=v1", "--branch"),
            cwd = cwd
        )
    }

    @Volatile
    private var cachedIdentity: String? = null

    /**
     * The answer to [ShellStatusProjector.IDENTITY_COMMAND] for the terminal's `user@host` tag:
     * `user` on the first line, `host` on the second, or null when neither could be determined. Blocks
     * for up to the probe timeout (the bundled `hostname` runs as a process), so call it off the main
     * thread. The user is Android's own name for this app's uid (what `id -un` prints in the runtime),
     * then `$USER`, then `user.name`; the host is the bundled `hostname`, then `$HOSTNAME`, then the
     * device model. It cannot change while the app runs, so the first complete answer is kept.
     */
    fun identity(context: Context): String? {
        cachedIdentity?.let { return it }
        val hostnameOutput = executable(context, "hostname")?.let { runProbe(context, it, emptyList(), null) }
        val answer = ShellStatusProjector.formatIdentity(
            user = ShellIdentitySource.pickUser(android.os.Process.myUid(), System.getenv("USER"), System.getProperty("user.name")),
            host = ShellIdentitySource.pickHost(hostnameOutput, System.getenv("HOSTNAME"), Build.MODEL)
        )
        if (answer != null) cachedIdentity = answer
        return answer
    }

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
            .filter { (name, _) -> name.startsWith(request.tokenValue, ignoreCase = true) }
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

        return gitBranches(context, request.cwd)
            .asSequence()
            .filter { it.startsWith(request.tokenValue, ignoreCase = true) }
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
        val output = runProbe(context, git, listOf("remote"), request.cwd) ?: return emptyList()
        return output.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .filter { it.startsWith(request.tokenValue, ignoreCase = true) }
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
            // A "host" starting with '-' would be read by ssh/scp as an option (-oProxyCommand=...).
            .filter { !it.startsWith("-") }
            .filter { it.startsWith(request.tokenValue, ignoreCase = true) }
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
            .filter { it.startsWith(request.tokenValue, ignoreCase = true) }
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

    private fun runProbe(context: Context, executable: File, args: List<String>, cwd: String?): String? = try {
        val builder = ProcessBuilder(listOf(executable.absolutePath) + args)
        cwd?.let(::File)?.takeIf(File::isDirectory)?.let(builder::directory)
        // A Termux-built git needs the same prefix/linker/exec-interceptor environment as any
        // other Termux binary HG2Gui launches, or it fails to resolve its libraries and helpers.
        runCatching { Hg2ExecEnvironment.apply(context, builder.environment()) }
        // stderr stays separate (collectOutput drains and discards it) and a non-zero exit is a
        // failed probe: otherwise "fatal: not a git repository" is parsed as a list of branches.
        val process = builder.start()
        // Drained while waiting, so a repository with many refs can't fill the pipe and time out.
        process.collectOutput(TIMEOUT_MS, MAX_OUTPUT_BYTES)?.textIfExit()
    } catch (_: Exception) {
        null
    }
}
