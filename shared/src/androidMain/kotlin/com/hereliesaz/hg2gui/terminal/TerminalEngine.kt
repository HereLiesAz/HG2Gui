package com.hereliesaz.hg2gui.terminal

import android.content.Context
import com.hereliesaz.hg2gui.managers.StyledSpan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

/**
 * Decides whether a command belongs to HG2Gui itself or an explicitly-selected execution backend.
 *
 * [policy] says who is asking. The default is a person typing in a terminal tab; background and
 * headless callers (the signature API, the MCP server, installers) construct the engine with their
 * own [ExecutionPolicy] and the engine itself refuses what they may not do - enable/disable/isolate/
 * update/reset/remove of packages, `bootstrap`, `vfs mount`, `hg2auth`, and device-affecting
 * builtins - so no entry point has to string-filter commands first.
 *
 * Every line is analysed with [CommandLine] and planned by [LinePlanner] before anything runs: the
 * Disable, isolation and package-transaction rules apply to the command that will really execute,
 * wherever it sits in `a && b | c`, behind `env`/`time`/`sudo`, or inside `bash -c`/`$( )`.
 */
class TerminalEngine(
    private val context: Context,
    private val home: File? = null,
    val policy: ExecutionPolicy = ExecutionPolicy.INTERACTIVE
) {
    private var shell = ShellSession.forAndroid(home, context)
    private var pendingBackendNotice: String? = shell.fallbackNoticeOrNull()

    // The shim directory's change counter as this tab's shell last saw it. Creating the shell
    // (above) already synced the directory, so it starts current.
    private var shimGenerationSeen: Long = DisableShims.generation()
    private val client = sharedHttpClient(context)
    private val downloader by lazy { Hg2Downloader(context.applicationContext, client) }
    private val packages by lazy { Hg2PackageManager(context.applicationContext, client, downloader) }

    init {
        // An alias must not shadow a real command of the same name (`gs` is Ghostscript, `l`/`c`/`h`
        // may be tools too): ShellAliases asks the installed prefix before expanding.
        val appContext = context.applicationContext
        ShellAliases.realCommandProbe = { name -> realCommandExists(appContext, name) }
    }

    private fun ShellSession.fallbackNoticeOrNull(): String? =
        backendDescription.takeIf { it.startsWith("the bare system shell") }
            ?.let { "[using $it]" }

    val workingDirectory: String get() = shell.workingDirectory

    /** What the engine hands each executor: where output goes and how a question is answered. */
    private class Io(
        /** A discrete message (a notice, a package-manager line). */
        val emit: suspend (String) -> Unit,
        /** The shell's cumulative transcript; each call supersedes the previous snapshot of that command. */
        val snapshot: suspend (String) -> Unit,
        /** Null means "nobody can answer" (headless). */
        val onNeedInput: suspend (String) -> String?,
        val onStderr: (String) -> Unit,
        val onStyled: (List<List<StyledSpan>>) -> Unit
    ) {
        fun withNotice(notice: String?): Io =
            if (notice == null) this else Io(
                emit = { emit("$notice\n$it") },
                snapshot = { snapshot("$notice\n$it") },
                onNeedInput = onNeedInput,
                onStderr = onStderr,
                onStyled = onStyled
            )

        /** Prefixes everything with [prior] (earlier steps of a `;`/`&&`/`||` list). */
        fun after(prior: String, record: (String) -> Unit): Io {
            val join: (String) -> String = { if (prior.isEmpty()) it else "$prior\n$it" }
            return Io(
                emit = { record(it); emit(join(it)) },
                snapshot = { record(it); snapshot(join(it)) },
                onNeedInput = onNeedInput,
                onStderr = onStderr,
                onStyled = onStyled
            )
        }
    }

    fun run(
        line: String,
        onNeedInput: suspend (prompt: String) -> String,
        onExit: (Int?) -> Unit = {},
        onStderr: (String) -> Unit = {},
        onStyledOutput: (lines: List<List<StyledSpan>>) -> Unit = {}
    ): Flow<String> = callbackFlow {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            close()
            return@callbackFlow
        }

        // Everything - including the package-owner lookup, which scans dpkg state on first use -
        // runs on IO, never on the collector's (UI) dispatcher.
        launch(Dispatchers.IO) {
            val io = Io(
                emit = { send(it) },
                snapshot = { send(it) },
                onNeedInput = { prompt -> onNeedInput(prompt) },
                onStderr = onStderr,
                onStyled = onStyledOutput
            )
            // Same contract for every route: an exception never skips onExit/close, or the
            // caller's prompt would wait on a command that already died.
            val exitCode = try {
                execute(trimmed, io, policy)
            } catch (e: CancellationException) {
                onExit(null)
                throw e
            } catch (e: Exception) {
                runCatching { send("HG2Gui error: ${e.message ?: e.javaClass.simpleName}") }
                -1
            }
            onExit(exitCode)
            close()
        }
        awaitClose { }
    }

    /**
     * Headless one-shot execution used by installers and MCP callers. Elevated authority is denied
     * and nobody can answer a prompt; a headless engine keeps its own [policy], an interactive one
     * runs under [ExecutionPolicy.INSTALLER] here.
     */
    suspend fun runToCompletion(line: String): Pair<String, Int> = withContext(Dispatchers.IO) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@withContext "" to 0
        val transcript = StringBuilder()
        var snapshotFrom = -1
        val io = Io(
            emit = { output ->
                if (transcript.isNotEmpty()) transcript.append('\n')
                snapshotFrom = -1
                transcript.append(output)
            },
            snapshot = { output ->
                if (snapshotFrom < 0) {
                    if (transcript.isNotEmpty()) transcript.append('\n')
                    snapshotFrom = transcript.length
                }
                transcript.setLength(snapshotFrom)
                transcript.append(output)
            },
            onNeedInput = { null },
            onStderr = {},
            onStyled = {}
        )
        val effective = if (policy.headless) policy else ExecutionPolicy.INSTALLER
        val code = try {
            execute(trimmed, io, effective)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (transcript.isNotEmpty()) transcript.append('\n')
            transcript.append("HG2Gui error: ${e.message ?: e.javaClass.simpleName}")
            -1
        }
        transcript.toString() to (code ?: 0)
    }

    /**
     * Runs [command] as root or through ADB shell after a person approved *exactly this string*
     * in a dialog (Hg2ApiAuthorityActivity). [command] reaches `su -c` / `adb shell` as one
     * argument: it is never split into words and re-joined, so quotes keep the meaning the
     * approver saw. No `hg2auth` line is built and no prompt is asked; the caller owns the consent.
     */
    fun runApprovedAuthority(
        kind: AuthorityKind,
        command: String,
        onExit: (Int?) -> Unit = {},
        onStderr: (String) -> Unit = {},
        onStyledOutput: (lines: List<List<StyledSpan>>) -> Unit = {}
    ): Flow<String> = callbackFlow {
        launch(Dispatchers.IO) {
            val io = Io(
                emit = { send(it) },
                snapshot = { send(it) },
                onNeedInput = { "" },
                onStderr = onStderr,
                onStyled = onStyledOutput
            )
            val exitCode = try {
                if (command.isBlank()) {
                    send("A command is required.")
                    2
                } else {
                    val built = when (kind) {
                        AuthorityKind.ROOT -> ExecutionAuthority.rootCommand(context, command)
                        AuthorityKind.ADB -> ExecutionAuthority.adbShellCommand(context, command)
                    }
                    runRawAuthorityCommand(built, io)
                }
            } catch (e: CancellationException) {
                onExit(null)
                throw e
            } catch (e: Exception) {
                runCatching { send("HG2Gui authority error: ${e.message ?: e.javaClass.simpleName}") }
                -1
            }
            onExit(exitCode)
            close()
        }
        awaitClose { }
    }

    fun interrupt() {
        shell.interrupt()
    }

    fun destroy() {
        shell.close()
    }

    // ---------------------------------------------------------------------------------------
    // Planning and dispatch
    // ---------------------------------------------------------------------------------------

    private suspend fun execute(line: String, io: Io, effective: ExecutionPolicy): Int? {
        // One scan-state check per line (it stats several install roots), however many commands
        // the line contains.
        val installedNow by lazy { PackageLifecycleStore.installed(context) }
        val plan = LinePlanner.plan(line, Builtins.NAMES) { name ->
            val bare = name.substringAfterLast('/')
            installedNow.firstOrNull { bare in it.binaries }
        }
        return when (plan) {
            is LinePlanner.Plan.Refuse -> {
                io.emit(plan.message)
                plan.exitCode
            }
            is LinePlanner.Plan.Shell -> runShellLine(plan.line, io)
            is LinePlanner.Plan.Steps -> runSteps(plan.steps, io, effective)
        }
    }

    /**
     * Runs a `;`/`&&`/`||` list one step at a time with the shell's short-circuit rules (a skipped
     * step leaves the status unchanged), so each step is routed - pkg through the transaction layer,
     * an isolated package through PRoot - instead of the whole line going to the real apt/dpkg.
     * Output of finished steps stays above the current step's.
     */
    private suspend fun runSteps(steps: List<LinePlanner.Step>, io: Io, effective: ExecutionPolicy): Int? {
        var last: Int? = 0
        var prior = ""
        for (step in steps) {
            val status = last ?: 0
            val runs = when (step.connector) {
                CommandLine.Connector.NONE, CommandLine.Connector.SEQ -> true
                CommandLine.Connector.AND -> status == 0
                CommandLine.Connector.OR -> status != 0
            }
            if (!runs) continue
            var current = ""
            last = dispatchStep(step, io.after(prior) { current = it }, effective)
            if (current.isNotEmpty()) prior = if (prior.isEmpty()) current else "$prior\n$current"
        }
        return last
    }

    private suspend fun dispatchStep(step: LinePlanner.Step, io: Io, effective: ExecutionPolicy): Int? = when (step.kind) {
        LinePlanner.StepKind.ENGINE -> dispatchEngine(step, io, effective)
        LinePlanner.StepKind.BUILTIN -> dispatchBuiltin(step, io, effective)
        LinePlanner.StepKind.OWNED -> runOwnedStep(step, io)
        LinePlanner.StepKind.SHELL -> runShellLine(step.text, io)
    }

    /**
     * Run before anything is handed to the shell. Brings the PATH shims (Disable, dpkg guard) in
     * line with what is installed now, and, if any shell changed them since this tab's shell last
     * looked, drops the shell's cached command locations: bash remembers where `tool` was found, so
     * without `hash -r` a freshly disabled tool would keep running from its remembered real path
     * (and a re-enabled one would keep pointing at the deleted shim).
     */
    private fun prepareShell() {
        DisableShims.sync(context)
        val current = DisableShims.generation()
        if (current == shimGenerationSeen) return
        shimGenerationSeen = current
        // A dead shell restarts with an empty cache, and its restart notice must not be swallowed here.
        if (!shell.isAlive) return
        try {
            shell.stream(DisableShims.REHASH_COMMAND, onLine = {})
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best effort: a failed rehash only delays the shim taking effect in this shell.
        }
    }

    private suspend fun runShellLine(line: String, io: Io): Int {
        val notice = pendingBackendNotice
        pendingBackendNotice = null
        val noticed = io.withNotice(notice)
        return try {
            prepareShell()
            shell.stream(
                line,
                onLine = { output -> runBlocking { noticed.snapshot(output) } },
                onNeedInput = { prompt -> runBlocking { noticed.onNeedInput(prompt) } },
                onStderrLine = noticed.onStderr,
                onStyledLine = noticed.onStyled
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            io.emit("HG2Gui shell error: ${e.message ?: e.javaClass.simpleName}")
            -1
        }
    }

    private suspend fun runOwnedStep(step: LinePlanner.Step, io: Io): Int {
        val owner = step.owner ?: return runShellLine(step.text, io)
        val notice = pendingBackendNotice
        pendingBackendNotice = null
        val noticed = io.withNotice(notice)
        return try {
            runOwnedPackage(owner, step.verb, step.text, noticed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            io.emit("HG2Gui shell error: ${e.message ?: e.javaClass.simpleName}")
            -1
        }
    }

    private suspend fun runOwnedOrShell(step: LinePlanner.Step, io: Io): Int =
        if (step.owner != null) runOwnedStep(step, io) else runShellLine(step.text, io)

    private suspend inline fun guarded(label: String, io: Io, block: () -> Int?): Int? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        io.emit("HG2Gui $label error: ${e.message ?: e.javaClass.simpleName}")
        -1
    }

    private suspend fun dispatchEngine(step: LinePlanner.Step, io: Io, effective: ExecutionPolicy): Int? {
        val text = step.text
        val words = step.words
        when (step.verb) {
            "bootstrap" -> {
                effective.bootstrapDenial()?.let { io.emit(it); return ExecutionPolicy.DENIED_EXIT_CODE }
                return guarded("bootstrap", io) {
                    DistroManager.bootstrap(context, client).collect { io.emit(it) }
                    val old = shell
                    shell = ShellSession.forAndroid(home, context)
                    pendingBackendNotice = shell.fallbackNoticeOrNull()
                    old.close()
                    null
                }
            }

            "download", "hg2download" -> return guarded("download", io) {
                downloader.run(text).collect { io.emit(it) }
                0
            }

            "remote" -> return guarded("remote", io) {
                val output = RemoteDiscovery.run(context, words.drop(1))
                io.emit(output)
                if (output.startsWith("Remote discovery failed") || output.startsWith("No SSH preset") || output.startsWith("SSH client is unavailable")) 1 else 0
            }

            "hg2auth" -> {
                effective.authorityDenial(words.getOrNull(1))?.let { io.emit(it); return ExecutionPolicy.DENIED_EXIT_CODE }
                return guarded("authority", io) { runAuthority(text, io) }
            }

            "hg2package" -> return guarded("package lifecycle", io) { runPackageLifecycle(text, io, effective) }

            "pkg", "hg2pkg", "apt", "apt-get" -> {
                val packageLine: String? = if (step.verb == "apt" || step.verb == "apt-get") {
                    when (val plan = AptTranslation.plan(words)) {
                        is AptTranslation.Plan.Refuse -> {
                            io.emit(plan.message)
                            return 2
                        }
                        is AptTranslation.Plan.Translate -> plan.pkgLine
                        // Read-only apt commands (search, show, list…) run as typed.
                        else -> null
                    }
                } else {
                    AptTranslation.pkgFlagRefusal(words)?.let {
                        io.emit(it)
                        return 2
                    }
                    text
                }
                if (packageLine == null) return runOwnedOrShell(step, io)
                return guarded("package", io) {
                    packages.run(packageLine).collect { io.emit(it) }
                    0
                }
            }

            // `vfs mount` needs root: route it through the same explicit confirmation and
            // ExecutionAuthority su backend as `hg2auth root shell`, never a silent su call.
            "vfs" -> {
                effective.vfsMountDenial()?.let { io.emit(it); return ExecutionPolicy.DENIED_EXIT_CODE }
                return guarded("authority", io) {
                    val target = words.getOrNull(2)
                    if (target == null) {
                        io.emit("Usage: vfs mount <real-path>")
                        1
                    } else {
                        val command = Builtins.vfsMountCommand(context, target)
                        val answer = io.onNeedInput(AuthorityPrompts.rootShell(command))
                        if (!answer.isYes()) {
                            io.emit("vfs mount cancelled.")
                            0
                        } else {
                            runRawAuthorityCommand(ExecutionAuthority.rootCommand(context, command), io)
                        }
                    }
                }
            }
        }
        return runOwnedOrShell(step, io)
    }

    private suspend fun dispatchBuiltin(step: LinePlanner.Step, io: Io, effective: ExecutionPolicy): Int? {
        // Calls, contact edits, brightness and the other device-affecting builtins are refused for a
        // background caller unless a person answers yes to this exact line.
        if (effective.builtinNeedsApproval(step.text)) {
            val answer = io.onNeedInput("Allow ${effective.label} to run '${step.text}'? [y/N]")
            if (!answer.isYes()) {
                io.emit("${step.verb} is not available from ${effective.label} without approval; nothing was run.")
                return ExecutionPolicy.DENIED_EXIT_CODE
            }
        }
        return guarded("builtin", io) {
            io.emit(Builtins.run(context, step.text))
            null
        }
    }

    // ---------------------------------------------------------------------------------------
    // Package-owned commands
    // ---------------------------------------------------------------------------------------

    private suspend fun runOwnedPackage(
        pkg: PackageLifecycleStore.InstalledPackage,
        verb: String,
        command: String,
        io: Io
    ): Int {
        val decision = PackageExecutionBackend.select(context, pkg, verb)
        return when (decision.backend) {
            PackageExecutionBackend.Backend.PROOT_ISOLATED -> runIsolatedPackage(pkg, command, io)
            PackageExecutionBackend.Backend.PROOT_COMPAT -> {
                ensureProotEngine(io.emit, "compatibility")
                io.emit("Execution backend: PRoot compatibility (${decision.reason}).")
                val snapshot = PackageLifecycleStore.beginRun(context, pkg, shell.workingDirectory)
                try {
                    streamThroughShell(PackageExecutionBackend.compatibilityCommand(context, command, shell.workingDirectory), io)
                } finally {
                    PackageLifecycleStore.finishRun(context, snapshot)
                }
            }
            PackageExecutionBackend.Backend.DIRECT_LINKER -> {
                val snapshot = PackageLifecycleStore.beginRun(context, pkg, shell.workingDirectory)
                try {
                    streamThroughShell(command, io)
                } finally {
                    PackageLifecycleStore.finishRun(context, snapshot)
                }
            }
        }
    }

    private fun streamThroughShell(command: String, io: Io): Int {
        prepareShell()
        return shell.stream(
        command,
        onLine = { output -> runBlocking { io.snapshot(output) } },
        onNeedInput = { prompt -> runBlocking { io.onNeedInput(prompt) } },
        onStderrLine = io.onStderr,
        onStyledLine = io.onStyled
        )
    }

    private suspend fun ensureProotEngine(emit: suspend (String) -> Unit, purpose: String) {
        if (PackageIsolation.engine(context) != null) return
        emit("Installing PRoot $purpose engine…")
        packages.run("pkg install proot").collect { emit(it) }
        if (PackageIsolation.engine(context) == null) {
            error("PRoot was installed but is not executable in this Android runtime.")
        }
    }

    private suspend fun runIsolatedPackage(
        pkg: PackageLifecycleStore.InstalledPackage,
        command: String,
        io: Io
    ): Int {
        if (PackageIsolation.engine(context) == null) {
            error("${pkg.name} is marked isolated, but PRoot is unavailable. Use Packages → ${pkg.name} → Release isolation or install proot.")
        }
        // Check the network grant BEFORE the (possibly huge) seed copy, without spending it:
        // PackageIsolation.command() consumes the one-shot grant at launch. Failing here means a
        // refused launch costs nothing instead of copying a whole prefix first.
        val network = PackageCapabilityPolicy.peek(context, pkg.key, PackageCapabilityPolicy.Capability.NETWORK)
        if (!network.allowed) error(PackageCapabilityPolicy.networkRefusal(pkg.name, network.reason))

        if (!PackageIsolation.isSeeded(context, pkg)) {
            io.emit("Preparing private runtime for ${pkg.name}…")
            val seedCode = shell.stream(
                PackageIsolation.seedCommand(context, pkg),
                onLine = { output -> runBlocking { io.snapshot(output) } },
                onNeedInput = { null },
                onStderrLine = io.onStderr,
                onStyledLine = io.onStyled
            )
            if (seedCode != 0) error("Could not seed isolated runtime for ${pkg.name} (exit $seedCode)")
        }

        val before = PackageIsolation.snapshot(context, pkg)
        val exitCode = streamThroughShell(PackageIsolation.command(context, pkg, command), io)
        val after = PackageIsolation.snapshot(context, pkg)
        io.emit(PackageIsolation.formatAudit(PackageIsolation.audit(before, after)))
        return exitCode
    }

    // ---------------------------------------------------------------------------------------
    // hg2auth
    // ---------------------------------------------------------------------------------------

    private suspend fun runAuthority(line: String, io: Io): Int {
        val words = CommandLine.words(line) ?: error("Unterminated quote in the hg2auth command - nothing was run.")
        val backend = words.getOrNull(1) ?: "status"
        if (backend == "status") {
            io.emit(ExecutionAuthority.summary(context))
            return 0
        }

        return when (backend) {
            "adb" -> {
                val action = words.getOrNull(2) ?: error("usage: hg2auth adb <devices|pair|connect|disconnect|shell> …")
                when (action) {
                    "devices" -> runRawAuthorityCommand(ExecutionAuthority.adbCommand(context, listOf("devices", "-l")), io)
                    "pair" -> {
                        val endpoint = words.getOrNull(3) ?: error("ADB pairing endpoint is required, e.g. 192.168.1.5:37123")
                        val code = words.getOrNull(4) ?: error("ADB pairing code is required")
                        runRawAuthorityCommand(ExecutionAuthority.adbCommand(context, listOf("pair", endpoint, code)), io)
                    }
                    "connect" -> {
                        val endpoint = words.getOrNull(3) ?: error("ADB connection endpoint is required, e.g. 192.168.1.5:41453")
                        runRawAuthorityCommand(ExecutionAuthority.adbCommand(context, listOf("connect", endpoint)), io)
                    }
                    "disconnect" -> runRawAuthorityCommand(
                        ExecutionAuthority.adbCommand(context, listOf("disconnect") + words.drop(3)),
                        io
                    )
                    "shell" -> {
                        // The text after `hg2auth adb shell`, exactly as typed: re-joining parsed
                        // words used to drop the quotes that gave the command its meaning.
                        val command = CommandLine.tail(line, 3).orEmpty()
                        if (command.isBlank()) error("ADB shell command is required")
                        val answer = io.onNeedInput(AuthorityPrompts.adbShell(command))
                        if (!answer.isYes()) {
                            io.emit("ADB shell cancelled.")
                            0
                        } else {
                            runRawAuthorityCommand(ExecutionAuthority.adbShellCommand(context, command), io)
                        }
                    }
                    else -> error("Unknown ADB authority action '$action'")
                }
            }
            "root" -> {
                val action = words.getOrNull(2) ?: error("usage: hg2auth root <test|shell> …")
                when (action) {
                    "test" -> {
                        val answer = io.onNeedInput(AuthorityPrompts.ROOT_TEST)
                        if (!answer.isYes()) {
                            io.emit("Root test cancelled.")
                            0
                        } else {
                            runRawAuthorityCommand(ExecutionAuthority.rootCommand(context, "id"), io)
                        }
                    }
                    "shell" -> {
                        val command = CommandLine.tail(line, 3).orEmpty()
                        if (command.isBlank()) error("Root command is required")
                        // The prompt shows precisely the string `su -c` will receive.
                        val answer = io.onNeedInput(AuthorityPrompts.rootShell(command))
                        if (!answer.isYes()) {
                            io.emit("Root command cancelled.")
                            0
                        } else {
                            runRawAuthorityCommand(ExecutionAuthority.rootCommand(context, command), io)
                        }
                    }
                    else -> error("Unknown root authority action '$action'")
                }
            }
            else -> error("Unknown execution authority '$backend'")
        }
    }

    // ---------------------------------------------------------------------------------------
    // hg2package
    // ---------------------------------------------------------------------------------------

    private suspend fun runPackageLifecycle(line: String, io: Io, effective: ExecutionPolicy): Int {
        val words = CommandLine.words(line) ?: error("Unterminated quote in the hg2package command - nothing was run.")
        val action = words.getOrNull(1)
            ?: error("usage: hg2package <info|update|disable|enable|isolate|release|reset|snapshot|restore|snapshot-delete|capability|capability-request|remove|purge> <manager> <package> [args]")
        effective.lifecycleDenial(action)?.let {
            io.emit(it)
            return ExecutionPolicy.DENIED_EXIT_CODE
        }
        val manager = words.getOrNull(2) ?: error("Package manager is required")
        val name = words.getOrNull(3) ?: error("Package name is required")
        val pkg = PackageLifecycleStore.find(context, manager, name)
            ?: error("Package '$name' is not installed under manager '$manager'")
        val emit = io.emit

        return when (action) {
            "disable" -> {
                PackageLifecycleStore.setDisabled(context, manager, name, true)
                emit("Disabled: ${pkg.name}. It remains installed and visible, but HG2Gui will block its commands.")
                0
            }
            "enable" -> {
                PackageLifecycleStore.setDisabled(context, manager, name, false)
                emit("Enabled: ${pkg.name}")
                0
            }
            "isolate" -> {
                ensureProotEngine(emit, "isolation")
                PackageIsolation.wipe(context, pkg)
                PackageLifecycleStore.setIsolated(context, manager, name, true)
                emit("Isolated: ${pkg.name}. Future runs use a private runtime and cannot inherit HG2Gui ADB/root authority.")
                0
            }
            "release" -> {
                val answer = io.onNeedInput("Release isolation for ${pkg.name}? Its private sandbox state will be deleted. [y/N]")
                if (!answer.isYes()) {
                    emit("Release cancelled: ${pkg.name}")
                    0
                } else {
                    val deleted = PackageIsolation.wipe(context, pkg)
                    if (!deleted) {
                        emit("Could not delete ${pkg.name}'s isolated runtime.")
                        1
                    } else {
                        PackageLifecycleStore.setIsolated(context, manager, name, false)
                        emit("Isolation released: ${pkg.name}")
                        0
                    }
                }
            }
            "capability" -> configureCapability(pkg, words.drop(4), io)
            "capability-request" -> brokerCapability(pkg, words.drop(4), io)
            "snapshot" -> {
                val point = PackageRestorePoints.create(context, pkg)
                emit("Restore point ${point.id}: ${point.pathCount} path${if (point.pathCount == 1) "" else "s"}, ${formatBytes(point.bytes)}.")
                0
            }
            "restore" -> {
                val id = words.getOrNull(4) ?: error("Restore-point id is required")
                val answer = io.onNeedInput("Restore ${pkg.name} from $id? Current managed runtime state will be replaced. [y/N]")
                if (!answer.isYes()) {
                    emit("Restore cancelled: ${pkg.name}")
                    0
                } else {
                    val result = PackageRestorePoints.restore(context, pkg, id)
                    emit("Restored ${pkg.name}: ${result.restoredPaths} path${if (result.restoredPaths == 1) "" else "s"} (${formatBytes(result.restoredBytes)}).")
                    if (result.failed.isNotEmpty()) emit("Could not restore:\n${result.failed.joinToString("\n")}")
                    if (result.failed.isEmpty()) 0 else 1
                }
            }
            "snapshot-delete" -> {
                val id = words.getOrNull(4) ?: error("Restore-point id is required")
                val answer = io.onNeedInput("Delete restore point $id for ${pkg.name}? [y/N]")
                if (!answer.isYes()) {
                    emit("Restore-point deletion cancelled: ${pkg.name}")
                    0
                } else if (PackageRestorePoints.delete(context, pkg, id)) {
                    emit("Deleted restore point $id for ${pkg.name}.")
                    0
                } else {
                    emit("Could not delete restore point $id for ${pkg.name}.")
                    1
                }
            }
            "reset" -> {
                val detail = if (pkg.isolated) {
                    "its entire private sandbox; it will be freshly reseeded on next run"
                } else {
                    "its tracked cache, config, state, logs, and generated files"
                }
                val answer = io.onNeedInput("Reset ${pkg.name}? This deletes $detail while keeping it installed. [y/N]")
                if (!answer.isYes()) {
                    emit("Reset cancelled: ${pkg.name}")
                    0
                } else {
                    val result = PackageLifecycleStore.reset(context, pkg)
                    emit(
                        "Reset ${pkg.name}: deleted ${result.deletedPaths} path${if (result.deletedPaths == 1) "" else "s"} " +
                            "(${formatBytes(result.deletedBytes)})."
                    )
                    if (result.failed.isNotEmpty()) emit("Could not delete:\n${result.failed.joinToString("\n")}")
                    if (result.failed.isEmpty()) 0 else 1
                }
            }
            "info" -> runManagerCommand(PackageLifecycleStore.infoCommand(pkg), io)
            "update" -> {
                val code = runManagerCommand(PackageLifecycleStore.updateCommand(pkg), io)
                if (code == 0) {
                    // Compare versions instead of trusting exit 0: for Termux packages the update is
                    // an upgrade that may have found nothing newer, and a sandbox should only be
                    // thrown away when the package really changed. Other managers do not expose a
                    // reliable post-update version here, so they keep the conservative wipe.
                    val after = PackageLifecycleStore.find(context, manager, name)?.version
                    val outcome = PackageUpdateOutcome.describe(pkg.name, pkg.version, after)
                    if (manager == "pkg") emit(outcome.message)
                    if (pkg.isolated && (manager != "pkg" || outcome.changed)) {
                        PackageIsolation.wipe(context, pkg)
                        emit("Isolation runtime cleared; ${pkg.name} will reseed from the updated package on next run.")
                    }
                }
                code
            }
            "remove", "purge" -> {
                val answer = io.onNeedInput("${action.replaceFirstChar { it.uppercase() }} ${pkg.name}? [y/N]")
                if (!answer.isYes()) {
                    emit("${action.replaceFirstChar { it.uppercase() }} cancelled: ${pkg.name}")
                    0
                } else {
                    val code = runManagerCommand(PackageLifecycleStore.removeCommand(pkg, purge = action == "purge"), io)
                    if (code == 0) {
                        PackageIsolation.wipe(context, pkg)
                        PackageLifecycleStore.setDisabled(context, manager, name, false)
                        PackageLifecycleStore.setIsolated(context, manager, name, false)
                    }
                    code
                }
            }
            else -> error("Unknown package lifecycle action '$action'")
        }
    }

    private suspend fun configureCapability(
        pkg: PackageLifecycleStore.InstalledPackage,
        args: List<String>,
        io: Io
    ): Int {
        val emit = io.emit
        if (args.isEmpty()) {
            PackageCapabilityPolicy.summary(context, pkg.key).forEach { (capability, mode) ->
                emit("${capability.wireName}: ${mode.name.lowercase().replace('_', '-')}")
            }
            return 0
        }
        val capability = capability(args[0])
        if (args.size == 1) {
            emit("${capability.wireName}: ${PackageCapabilityPolicy.mode(context, pkg.key, capability).name.lowercase().replace('_', '-')}")
            return 0
        }
        val mode = when (args[1].lowercase()) {
            "ask", "ask-every-time" -> PackageCapabilityPolicy.Mode.ASK
            "deny" -> PackageCapabilityPolicy.Mode.DENY
            "allow-once", "once" -> PackageCapabilityPolicy.Mode.ALLOW_ONCE
            else -> error("Capability mode must be ask, deny, or allow-once")
        }
        if (mode == PackageCapabilityPolicy.Mode.ALLOW_ONCE && !PackageCapabilityPolicy.isEnforceable(capability)) {
            emit(PackageCapabilityPolicy.unenforceableMessage(pkg.name, capability))
            return 1
        }
        val answer = io.onNeedInput("Set ${pkg.name} ${capability.wireName} policy to ${mode.name.lowercase().replace('_', '-')}? [y/N]")
        if (!answer.isYes()) {
            emit("Capability policy unchanged: ${pkg.name} ${capability.wireName}.")
            return 0
        }
        PackageCapabilityPolicy.set(context, pkg.key, capability, mode)
        emit("${pkg.name} ${capability.wireName}: ${mode.name.lowercase().replace('_', '-')}.")
        return 0
    }

    /**
     * A package asks for a capability. Only capabilities HG2Gui can really withhold are ever
     * granted (network: the isolated launch gate). The grant is armed for the next isolated launch
     * and spent there - it is not consumed by this request. ADB, root and host read/write have no
     * enforcement point, so they are refused instead of answered with a grant that changes nothing.
     */
    private suspend fun brokerCapability(
        pkg: PackageLifecycleStore.InstalledPackage,
        args: List<String>,
        io: Io
    ): Int {
        val capability = capability(args.firstOrNull() ?: error("Capability name is required"))
        if (!PackageCapabilityPolicy.isEnforceable(capability)) {
            io.emit(PackageCapabilityPolicy.unenforceableMessage(pkg.name, capability))
            return 1
        }
        var decision = PackageCapabilityPolicy.peek(context, pkg.key, capability)
        if (!decision.allowed && decision.mode == PackageCapabilityPolicy.Mode.ASK) {
            val answer = io.onNeedInput("${pkg.name} requests ${capability.wireName}. Allow this request once? [y/N]")
            if (answer.isYes()) {
                PackageCapabilityPolicy.set(context, pkg.key, capability, PackageCapabilityPolicy.Mode.ALLOW_ONCE)
                decision = PackageCapabilityPolicy.peek(context, pkg.key, capability)
            }
        }
        io.emit(
            if (decision.allowed) {
                "Capability armed once: ${pkg.name} → ${capability.wireName}. The next isolated launch of ${pkg.name} may use it; it is spent there."
            } else {
                "Capability denied: ${pkg.name} → ${capability.wireName} (${decision.reason})."
            }
        )
        return if (decision.allowed) 0 else 1
    }

    private fun capability(raw: String): PackageCapabilityPolicy.Capability =
        PackageCapabilityPolicy.Capability.entries.firstOrNull { it.wireName == raw.lowercase() }
            ?: error("Unknown capability '$raw'. Use: ${PackageCapabilityPolicy.Capability.entries.joinToString { it.wireName }}")

    private suspend fun runManagerCommand(command: String, io: Io): Int {
        val verb = CommandLine.words(command)?.firstOrNull()
        if (verb == "pkg" || verb == "hg2pkg") {
            return try {
                packages.run(command).collect { io.emit(it) }
                0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                io.emit("HG2Gui package error: ${e.message ?: e.javaClass.simpleName}")
                -1
            }
        }
        return runRawAuthorityCommand(command, io)
    }

    private fun runRawAuthorityCommand(command: String, io: Io): Int = streamThroughShell(command, io)

    private fun String?.isYes(): Boolean = this?.trim()?.lowercase() in setOf("y", "yes")

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "%.1f KiB".format(bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L -> "%.1f MiB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GiB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }

    companion object {
        @Volatile
        private var instance: OkHttpClient? = null

        private fun sharedHttpClient(context: Context): OkHttpClient {
            return instance ?: synchronized(this) {
                instance ?: OkHttpClient.Builder()
                    .cache(Cache(File(context.applicationContext.cacheDir, "http"), (10 * 1024 * 1024).toLong()))
                    .build()
                    .also { instance = it }
            }
        }

        /** True when the installed prefix already has a command called [name] (one stat, no scan). */
        private fun realCommandExists(context: Context, name: String): Boolean {
            if (name.isEmpty() || name.contains('/')) return false
            return try {
                File(DistroManager.prefixDir(context), "bin/$name").exists()
            } catch (_: SecurityException) {
                false
            }
        }
    }
}
