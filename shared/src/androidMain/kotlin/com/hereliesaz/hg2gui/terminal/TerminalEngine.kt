package com.hereliesaz.hg2gui.terminal

import android.content.Context
import com.hereliesaz.hg2gui.managers.StyledSpan
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

/** Decides whether a command belongs to HG2Gui itself or an explicitly-selected execution backend. */
class TerminalEngine(
    private val context: Context,
    private val home: File? = null
) {
    private var shell = ShellSession.forAndroid(home, context)
    private var pendingBackendNotice: String? = shell.fallbackNoticeOrNull()
    private val client = sharedHttpClient(context)
    private val downloader by lazy { Hg2Downloader(context.applicationContext, client) }
    private val packages by lazy { Hg2PackageManager(context.applicationContext, client, downloader) }

    private fun ShellSession.fallbackNoticeOrNull(): String? =
        backendDescription.takeIf { it.startsWith("the bare system shell") }
            ?.let { "[using $it]" }

    val workingDirectory: String get() = shell.workingDirectory

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

        val verb = trimmed.substringBefore(' ')
        val aptPackageCommand = translateAptPackageCommand(trimmed)
        val packageOwner = if (verb in setOf("hg2package", "hg2auth")) null else PackageLifecycleStore.ownerOfBinary(context, verb)

        when {
            verb == "bootstrap" -> launch(Dispatchers.IO) {
                DistroManager.bootstrap(context, client).collect { trySend(it) }
                val old = shell
                shell = ShellSession.forAndroid(home, context)
                pendingBackendNotice = shell.fallbackNoticeOrNull()
                old.close()
                onExit(null)
                close()
            }

            verb == "download" || verb == "hg2download" -> launch(Dispatchers.IO) {
                try {
                    downloader.run(trimmed).collect { trySend(it) }
                    onExit(0)
                } catch (e: Exception) {
                    trySend("HG2Gui download error: ${e.message ?: e.javaClass.simpleName}")
                    onExit(-1)
                } finally {
                    close()
                }
            }

            verb == "hg2auth" -> launch(Dispatchers.IO) {
                val code = try {
                    runAuthority(
                        line = trimmed,
                        emit = { trySend(it) },
                        onNeedInput = onNeedInput,
                        onStderr = onStderr,
                        onStyledOutput = onStyledOutput
                    )
                } catch (e: Exception) {
                    trySend("HG2Gui authority error: ${e.message ?: e.javaClass.simpleName}")
                    -1
                }
                onExit(code)
                close()
            }

            verb == "hg2package" -> launch(Dispatchers.IO) {
                val code = try {
                    runPackageLifecycle(
                        line = trimmed,
                        emit = { trySend(it) },
                        onNeedInput = onNeedInput,
                        onStderr = onStderr,
                        onStyledOutput = onStyledOutput
                    )
                } catch (e: Exception) {
                    trySend("HG2Gui package lifecycle error: ${e.message ?: e.javaClass.simpleName}")
                    -1
                }
                onExit(code)
                close()
            }

            packageOwner?.disabled == true -> launch(Dispatchers.IO) {
                trySend(
                    "${packageOwner.name} is installed but disabled in HG2Gui. " +
                        "Enable it under Packages → ${packageOwner.managerLabel} → ${packageOwner.name}."
                )
                onExit(126)
                close()
            }

            verb == "pkg" || verb == "hg2pkg" || aptPackageCommand != null -> launch(Dispatchers.IO) {
                val packageLine = aptPackageCommand ?: trimmed
                try {
                    packages.run(packageLine).collect { trySend(it) }
                    onExit(0)
                } catch (e: Exception) {
                    trySend("HG2Gui package error: ${e.message ?: e.javaClass.simpleName}")
                    onExit(-1)
                } finally {
                    close()
                }
            }

            verb in Builtins.NAMES -> launch(Dispatchers.IO) {
                send(Builtins.run(context, trimmed))
                onExit(null)
                close()
            }

            else -> launch(Dispatchers.IO) {
                val notice = pendingBackendNotice
                pendingBackendNotice = null
                val exitCode = if (packageOwner?.isolated == true) {
                    runIsolatedPackage(
                        packageOwner,
                        trimmed,
                        emit = { output -> trySend(if (notice != null) "$notice\n$output" else output) },
                        onNeedInput = onNeedInput,
                        onStderr = onStderr,
                        onStyledOutput = onStyledOutput
                    )
                } else {
                    val snapshot = packageOwner?.let { PackageLifecycleStore.beginRun(context, it, shell.workingDirectory) }
                    try {
                        shell.stream(
                            trimmed,
                            onLine = { output -> trySend(if (notice != null) "$notice\n$output" else output) },
                            onNeedInput = { prompt -> runBlocking { onNeedInput(prompt) } },
                            onStderrLine = { output -> onStderr(output) },
                            onStyledLine = { lines -> onStyledOutput(lines) }
                        )
                    } finally {
                        PackageLifecycleStore.finishRun(context, snapshot)
                    }
                }
                onExit(exitCode)
                close()
            }
        }
        awaitClose { }
    }

    /** Headless one-shot execution used by installers and MCP callers. Elevated authority is denied. */
    suspend fun runToCompletion(line: String): Pair<String, Int> = withContext(Dispatchers.IO) {
        val trimmed = line.trim()
        val verb = trimmed.substringBefore(' ')
        val aptPackageCommand = translateAptPackageCommand(trimmed)

        if (verb == "hg2auth") {
            return@withContext if (shellWords(trimmed).getOrNull(1) == "status") {
                ExecutionAuthority.summary(context) to 0
            } else {
                "ADB/root authority operations require an interactive HG2Gui confirmation." to 2
            }
        }

        if (verb == "hg2package") {
            val action = shellWords(trimmed).getOrNull(1)
            if (action in setOf("reset", "remove", "purge", "release")) {
                return@withContext "HG2Gui package $action requires interactive confirmation." to 2
            }
            val transcript = StringBuilder()
            val code = runPackageLifecycle(
                line = trimmed,
                emit = { output ->
                    if (transcript.isNotEmpty()) transcript.append('\n')
                    transcript.append(output)
                },
                onNeedInput = { "n" },
                onStderr = {},
                onStyledOutput = {}
            )
            return@withContext transcript.toString() to code
        }

        val owner = PackageLifecycleStore.ownerOfBinary(context, verb)
        if (owner?.disabled == true) {
            return@withContext "${owner.name} is installed but disabled in HG2Gui." to 126
        }

        val hg2Flow = when {
            verb == "download" || verb == "hg2download" -> downloader.run(trimmed)
            verb == "pkg" || verb == "hg2pkg" -> packages.run(trimmed)
            aptPackageCommand != null -> packages.run(aptPackageCommand)
            else -> null
        }
        if (hg2Flow != null) {
            val transcript = StringBuilder()
            return@withContext try {
                hg2Flow.collect { output ->
                    if (transcript.isNotEmpty()) transcript.append('\n')
                    transcript.append(output)
                }
                transcript.toString() to 0
            } catch (e: Exception) {
                if (transcript.isNotEmpty()) transcript.append('\n')
                transcript.append("HG2Gui error: ${e.message ?: e.javaClass.simpleName}")
                transcript.toString() to -1
            }
        }

        if (owner?.isolated == true) {
            val transcript = StringBuilder()
            val code = runIsolatedPackage(
                owner,
                trimmed,
                emit = { output ->
                    if (transcript.isNotEmpty()) transcript.append('\n')
                    transcript.append(output)
                },
                onNeedInput = { null },
                onStderr = {},
                onStyledOutput = {}
            )
            return@withContext transcript.toString() to code
        }

        val snapshot = owner?.let { PackageLifecycleStore.beginRun(context, it, shell.workingDirectory) }
        var transcript = ""
        val exitCode = try {
            shell.stream(
                trimmed,
                onLine = { transcript = it },
                onNeedInput = { null },
                onStderrLine = {},
                onStyledLine = {}
            )
        } finally {
            PackageLifecycleStore.finishRun(context, snapshot)
        }
        transcript to exitCode
    }

    fun interrupt() {
        shell.interrupt()
    }

    fun destroy() {
        shell.close()
    }

    private suspend fun runIsolatedPackage(
        pkg: PackageLifecycleStore.InstalledPackage,
        command: String,
        emit: suspend (String) -> Unit,
        onNeedInput: suspend (String) -> String?,
        onStderr: (String) -> Unit,
        onStyledOutput: (List<List<StyledSpan>>) -> Unit
    ): Int {
        if (PackageIsolation.engine(context) == null) {
            error("${pkg.name} is marked isolated, but PRoot is unavailable. Use Packages → ${pkg.name} → Release isolation or install proot.")
        }
        if (!PackageIsolation.isSeeded(context, pkg)) {
            emit("Preparing private runtime for ${pkg.name}…")
            val seedCode = shell.stream(
                PackageIsolation.seedCommand(context, pkg),
                onLine = { output -> runBlocking { emit(output) } },
                onNeedInput = { null },
                onStderrLine = onStderr,
                onStyledLine = onStyledOutput
            )
            if (seedCode != 0) error("Could not seed isolated runtime for ${pkg.name} (exit $seedCode)")
        }

        val before = PackageIsolation.snapshot(context, pkg)
        val exitCode = shell.stream(
            PackageIsolation.command(context, pkg, command),
            onLine = { output -> runBlocking { emit(output) } },
            onNeedInput = { prompt -> runBlocking { onNeedInput(prompt) } },
            onStderrLine = onStderr,
            onStyledLine = onStyledOutput
        )
        val after = PackageIsolation.snapshot(context, pkg)
        emit(PackageIsolation.formatAudit(PackageIsolation.audit(before, after)))
        return exitCode
    }

    private suspend fun runAuthority(
        line: String,
        emit: suspend (String) -> Unit,
        onNeedInput: suspend (String) -> String,
        onStderr: (String) -> Unit,
        onStyledOutput: (List<List<StyledSpan>>) -> Unit
    ): Int {
        val words = shellWords(line)
        val backend = words.getOrNull(1) ?: "status"
        if (backend == "status") {
            emit(ExecutionAuthority.summary(context))
            return 0
        }

        return when (backend) {
            "adb" -> {
                val action = words.getOrNull(2) ?: error("usage: hg2auth adb <devices|pair|connect|disconnect|shell> …")
                when (action) {
                    "devices" -> runRawAuthorityCommand(
                        ExecutionAuthority.adbCommand(context, listOf("devices", "-l")),
                        emit,
                        onNeedInput,
                        onStderr,
                        onStyledOutput
                    )
                    "pair" -> {
                        val endpoint = words.getOrNull(3) ?: error("ADB pairing endpoint is required, e.g. 192.168.1.5:37123")
                        val code = words.getOrNull(4) ?: error("ADB pairing code is required")
                        runRawAuthorityCommand(
                            ExecutionAuthority.adbCommand(context, listOf("pair", endpoint, code)),
                            emit,
                            onNeedInput,
                            onStderr,
                            onStyledOutput
                        )
                    }
                    "connect" -> {
                        val endpoint = words.getOrNull(3) ?: error("ADB connection endpoint is required, e.g. 192.168.1.5:41453")
                        runRawAuthorityCommand(
                            ExecutionAuthority.adbCommand(context, listOf("connect", endpoint)),
                            emit,
                            onNeedInput,
                            onStderr,
                            onStyledOutput
                        )
                    }
                    "disconnect" -> runRawAuthorityCommand(
                        ExecutionAuthority.adbCommand(context, listOf("disconnect") + words.drop(3)),
                        emit,
                        onNeedInput,
                        onStderr,
                        onStyledOutput
                    )
                    "shell" -> {
                        val command = words.drop(3).joinToString(" ").trim()
                        if (command.isBlank()) error("ADB shell command is required")
                        val answer = onNeedInput("Run through ADB shell? $command [y/N]")
                        if (!answer.isYes()) {
                            emit("ADB shell cancelled.")
                            0
                        } else {
                            runRawAuthorityCommand(
                                ExecutionAuthority.adbShellCommand(context, command),
                                emit,
                                onNeedInput,
                                onStderr,
                                onStyledOutput
                            )
                        }
                    }
                    else -> error("Unknown ADB authority action '$action'")
                }
            }
            "root" -> {
                val action = words.getOrNull(2) ?: error("usage: hg2auth root <test|shell> …")
                when (action) {
                    "test" -> {
                        val answer = onNeedInput("Request root access to test the su provider? [y/N]")
                        if (!answer.isYes()) {
                            emit("Root test cancelled.")
                            0
                        } else {
                            runRawAuthorityCommand(
                                ExecutionAuthority.rootCommand(context, "id"),
                                emit,
                                onNeedInput,
                                onStderr,
                                onStyledOutput
                            )
                        }
                    }
                    "shell" -> {
                        val command = words.drop(3).joinToString(" ").trim()
                        if (command.isBlank()) error("Root command is required")
                        val answer = onNeedInput("Run as root? $command [y/N]")
                        if (!answer.isYes()) {
                            emit("Root command cancelled.")
                            0
                        } else {
                            runRawAuthorityCommand(
                                ExecutionAuthority.rootCommand(context, command),
                                emit,
                                onNeedInput,
                                onStderr,
                                onStyledOutput
                            )
                        }
                    }
                    else -> error("Unknown root authority action '$action'")
                }
            }
            else -> error("Unknown execution authority '$backend'")
        }
    }

    private suspend fun runPackageLifecycle(
        line: String,
        emit: suspend (String) -> Unit,
        onNeedInput: suspend (String) -> String,
        onStderr: (String) -> Unit,
        onStyledOutput: (List<List<StyledSpan>>) -> Unit
    ): Int {
        val words = shellWords(line)
        val action = words.getOrNull(1)
            ?: error("usage: hg2package <info|update|disable|enable|isolate|release|reset|remove|purge> <manager> <package>")
        val manager = words.getOrNull(2) ?: error("Package manager is required")
        val name = words.getOrNull(3) ?: error("Package name is required")
        val pkg = PackageLifecycleStore.find(context, manager, name)
            ?: error("Package '$name' is not installed under manager '$manager'")

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
                if (PackageIsolation.engine(context) == null) {
                    emit("Installing PRoot isolation engine…")
                    packages.run("pkg install proot").collect { emit(it) }
                }
                if (PackageIsolation.engine(context) == null) {
                    error("PRoot was installed but is not executable in this Android runtime.")
                }
                PackageIsolation.wipe(context, pkg)
                PackageLifecycleStore.setIsolated(context, manager, name, true)
                emit("Isolated: ${pkg.name}. Future runs use a private runtime and cannot inherit HG2Gui ADB/root authority.")
                0
            }
            "release" -> {
                val answer = onNeedInput("Release isolation for ${pkg.name}? Its private sandbox state will be deleted. [y/N]")
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
            "reset" -> {
                val detail = if (pkg.isolated) {
                    "its entire private sandbox; it will be freshly reseeded on next run"
                } else {
                    "its tracked cache, config, state, logs, and generated files"
                }
                val answer = onNeedInput("Reset ${pkg.name}? This deletes $detail while keeping it installed. [y/N]")
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
            "info" -> runManagerCommand(PackageLifecycleStore.infoCommand(pkg), emit, onNeedInput, onStderr, onStyledOutput)
            "update" -> {
                val code = runManagerCommand(PackageLifecycleStore.updateCommand(pkg), emit, onNeedInput, onStderr, onStyledOutput)
                if (code == 0 && pkg.isolated) {
                    PackageIsolation.wipe(context, pkg)
                    emit("Isolation runtime cleared; ${pkg.name} will reseed from the updated package on next run.")
                }
                code
            }
            "remove", "purge" -> {
                val answer = onNeedInput("${action.replaceFirstChar { it.uppercase() }} ${pkg.name}? [y/N]")
                if (!answer.isYes()) {
                    emit("${action.replaceFirstChar { it.uppercase() }} cancelled: ${pkg.name}")
                    0
                } else {
                    val code = runManagerCommand(
                        PackageLifecycleStore.removeCommand(pkg, purge = action == "purge"),
                        emit,
                        onNeedInput,
                        onStderr,
                        onStyledOutput
                    )
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

    private suspend fun runManagerCommand(
        command: String,
        emit: suspend (String) -> Unit,
        onNeedInput: suspend (String) -> String,
        onStderr: (String) -> Unit,
        onStyledOutput: (List<List<StyledSpan>>) -> Unit
    ): Int {
        val translatedApt = translateAptPackageCommand(command)
        val verb = command.substringBefore(' ')
        if (verb == "pkg" || verb == "hg2pkg" || translatedApt != null) {
            return try {
                packages.run(translatedApt ?: command).collect { emit(it) }
                0
            } catch (e: Exception) {
                emit("HG2Gui package error: ${e.message ?: e.javaClass.simpleName}")
                -1
            }
        }
        return runRawAuthorityCommand(command, emit, onNeedInput, onStderr, onStyledOutput)
    }

    private suspend fun runRawAuthorityCommand(
        command: String,
        emit: suspend (String) -> Unit,
        onNeedInput: suspend (String) -> String,
        onStderr: (String) -> Unit,
        onStyledOutput: (List<List<StyledSpan>>) -> Unit
    ): Int = shell.stream(
        command,
        onLine = { output -> runBlocking { emit(output) } },
        onNeedInput = { prompt -> runBlocking { onNeedInput(prompt) } },
        onStderrLine = onStderr,
        onStyledLine = onStyledOutput
    )

    private fun translateAptPackageCommand(line: String): String? {
        val words = shellWords(line)
        if (words.isEmpty() || words.first() !in setOf("apt", "apt-get")) return null

        val operationIndex = words.indexOfFirst { it in APT_MUTATING_OPERATIONS }
        if (operationIndex < 0) return null

        val operation = words[operationIndex]
        val translatedOperation = when (operation) {
            "install" -> "install"
            "remove" -> "remove"
            "purge" -> "purge"
            "update" -> "update"
            "upgrade", "dist-upgrade", "full-upgrade" -> "upgrade"
            else -> return null
        }

        val packageNames = words.drop(operationIndex + 1).filterNot { it.startsWith("-") }
        return when (translatedOperation) {
            "update", "upgrade" -> "pkg $translatedOperation"
            else -> if (packageNames.isEmpty()) null else "pkg $translatedOperation ${packageNames.joinToString(" ")}"
        }
    }

    private fun shellWords(line: String): List<String> = Regex("""(?:[^\s\"']+|\"[^\"]*\"|'[^']*')+""")
        .findAll(line)
        .map { it.value.trim().removeSurrounding("\"").removeSurrounding("'") }
        .toList()

    private fun String?.isYes(): Boolean = this?.trim()?.lowercase() in setOf("y", "yes")

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "%.1f KiB".format(bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L -> "%.1f MiB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GiB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }

    companion object {
        private val APT_MUTATING_OPERATIONS = setOf(
            "install",
            "remove",
            "purge",
            "update",
            "upgrade",
            "dist-upgrade",
            "full-upgrade"
        )

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
    }
}
