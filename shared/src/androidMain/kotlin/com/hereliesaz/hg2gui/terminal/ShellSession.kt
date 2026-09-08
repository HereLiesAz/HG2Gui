package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import com.hereliesaz.hg2gui.managers.PtyPreference
import com.hereliesaz.hg2gui.managers.StyledSpan
import com.termux.terminal.JNI
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalOutput
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class DummyTerminalOutput : TerminalOutput() {
    override fun write(data: ByteArray, offset: Int, count: Int) {}
    override fun titleChanged(oldTitle: String?, newTitle: String?) {}
    override fun onCopyTextToClipboard(text: String) {}
    override fun onPasteTextFromClipboard() {}
    override fun onBell() {}
    override fun onColorsChanged() {}
}

actual class ShellSession private constructor(
    private val home: File?,
    private val command: Array<String>,
    private val extraEnv: Map<String, String>,
    val backendDescription: String = "the bare system shell ($DEFAULT_SHELL) - no fallback reached",
    private val usePty: Boolean = false
) {
    companion object {
        private const val SENTINEL = "__HG2GUI_EOC_a7f3__"
        private const val SENTINEL_HEAD = "__HG2GUI_EOC_"
        private const val SENTINEL_TAIL = "a7f3__"
        private const val NATIVE_BASH = "libbin_bash.so"
        private const val DEFAULT_SHELL = "/system/bin/sh"
        private const val TIMEOUT_MS = 15_000L
        private const val STARTUP_PROBE_MS = 300L
        private const val PROMPT_IDLE_MS = 400L
        private const val INTR_BYTE = 0x03
        private const val PENDING_TRIM_THRESHOLD = 65_536
        private const val PTY_ROWS = 24
        private const val PTY_COLUMNS = 120
        private const val PTY_CELL_WIDTH = 10
        private const val PTY_CELL_HEIGHT = 10

        fun bootstrapBashEnv(context: Context, home: File?): Pair<File, Map<String, String>>? {
            val prefix = DistroManager.prefixDir(context)
            val bin = File(prefix, "bin")
            val nativeBash = File(context.applicationInfo.nativeLibraryDir, NATIVE_BASH)
            if (!prefix.isDirectory || !bin.isDirectory || !nativeBash.canExecute()) return null

            val bootstrapHome = home ?: DistroManager.homeDir(context)
            if (!bootstrapHome.exists()) bootstrapHome.mkdirs()
            val env = mapOf(
                "HOME" to bootstrapHome.absolutePath,
                "PREFIX" to prefix.absolutePath,
                "PATH" to "${prefix.absolutePath}/bin",
                "LD_LIBRARY_PATH" to "${prefix.absolutePath}/lib",
                "TMPDIR" to "${prefix.absolutePath}/tmp",
                "LANG" to "en_US.UTF-8",
                "APT_CONFIG" to "${prefix.absolutePath}/etc/apt/apt.conf",
                "SSL_CERT_FILE" to "${prefix.absolutePath}/etc/tls/cert.pem",
                "CURL_CA_BUNDLE" to "${prefix.absolutePath}/etc/tls/cert.pem",
                "TERM" to "xterm-256color",
                "TERMINFO" to "${prefix.absolutePath}/share/terminfo",
                "HG2GUI_BASH" to nativeBash.absolutePath
            )
            return bootstrapHome to env
        }

        fun forAndroid(home: File?, context: Context): ShellSession {
            val reasons = mutableListOf<String>()
            val usePty = PtyPreference.isEnabled(context)
            val bootstrap = bootstrapBashEnv(context, home)
            if (bootstrap != null) {
                val (bootstrapHome, env) = bootstrap
                val bash = File(env.getValue("HG2GUI_BASH"))
                val session = ShellSession(
                    bootstrapHome,
                    arrayOf(bash.absolutePath, "-l"),
                    env,
                    "bash (Termux bootstrap via nativeLibraryDir)",
                    usePty
                )
                if (session.survivedStartup()) return session
                session.close()
                reasons += "the exec-exempt Termux bash started but exited immediately"
            } else if (DistroManager.isInstalled(context)) {
                reasons += "the APK-installed exec-exempt Termux bash is unavailable"
            } else {
                reasons += "no Termux bootstrap is installed"
            }
            val fallbackEnv = if (usePty) {
                mapOf("PATH" to "/system/bin:/system/xbin", "HOME" to (home?.absolutePath ?: "/"))
            } else emptyMap()
            return ShellSession(
                home,
                arrayOf(DEFAULT_SHELL),
                fallbackEnv,
                "the bare system shell ($DEFAULT_SHELL), without apt/pkg/coreutils - ${reasons.joinToString("; ")}",
                usePty
            )
        }

        private fun wrapFileDescriptor(fileDescriptor: Int): FileDescriptor {
            val result = FileDescriptor()
            try {
                val descriptorField: Field = try {
                    FileDescriptor::class.java.getDeclaredField("descriptor")
                } catch (ignored: NoSuchFieldException) {
                    FileDescriptor::class.java.getDeclaredField("fd")
                }
                descriptorField.isAccessible = true
                descriptorField.set(result, fileDescriptor)
            } catch (e: ReflectiveOperationException) {
                throw IOException("Cannot wrap native pty fd via reflection", e)
            }
            return result
        }
    }

    private var process: Process? = null
    private var ptyFd: Int = -1
    private var ptyPid: Int = -1
    private var stdin: BufferedWriter? = null
    private var stdout: BufferedReader? = null
    private var stderr: BufferedReader? = null
    private val alive = AtomicBoolean(false)
    private val generation = AtomicInteger(0)

    @Volatile
    private var _workingDirectory: String = home?.absolutePath ?: "/"

    actual val workingDirectory: String get() = _workingDirectory
    actual val isAlive: Boolean get() = alive.get() && processStillRunning()

    constructor(home: File?) : this(home, arrayOf(DEFAULT_SHELL), emptyMap())
    constructor(home: File?, shellPath: String) : this(home, arrayOf(shellPath), emptyMap())
    actual constructor(homePath: String?) : this(homePath?.let { File(it) })
    actual constructor(homePath: String?, shellPath: String) : this(homePath?.let { File(it) }, shellPath)

    init {
        startChild(home, command, extraEnv)
    }

    private fun markDeadIfCurrent(myGeneration: Int) {
        if (generation.get() == myGeneration) alive.set(false)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun startChild(home: File?, command: Array<String>, extraEnv: Map<String, String>) {
        try {
            if (usePty) startPty(home, command, extraEnv) else startPipe(home, command, extraEnv)
            alive.set(true)
        } catch (ignored: IOException) {
            alive.set(false)
        } catch (ignored: RuntimeException) {
            alive.set(false)
        }
    }

    private fun startPipe(home: File?, command: Array<String>, extraEnv: Map<String, String>) {
        val builder = ProcessBuilder(*command)
        if (home != null && home.isDirectory) builder.directory(home)
        if (extraEnv.isNotEmpty()) builder.environment().putAll(extraEnv)
        val p = builder.start()
        process = p
        stdin = BufferedWriter(OutputStreamWriter(p.outputStream))
        stdout = BufferedReader(InputStreamReader(p.inputStream))
        stderr = BufferedReader(InputStreamReader(p.errorStream))
    }

    private fun startPty(home: File?, command: Array<String>, extraEnv: Map<String, String>) {
        val cwd = home?.absolutePath ?: "/"
        val envArray = extraEnv.map { (k, v) -> "$k=$v" }.toTypedArray()
        val pid = IntArray(1)
        val fd = JNI.createSubprocess(
            command[0], cwd, command, envArray, pid,
            PTY_ROWS, PTY_COLUMNS, PTY_CELL_WIDTH, PTY_CELL_HEIGHT
        )
        if (fd < 0) throw IOException("createSubprocess() failed to open a pty")
        ptyFd = fd
        ptyPid = pid[0]
        val wrapped = wrapFileDescriptor(fd)
        stdin = BufferedWriter(OutputStreamWriter(FileOutputStream(wrapped)))
        stdout = BufferedReader(InputStreamReader(FileInputStream(wrapped)))
        thread(name = "ShellSession-pty-waiter[pid=$ptyPid]") {
            JNI.waitFor(ptyPid)
            alive.set(false)
        }
    }

    private fun survivedStartup(): Boolean {
        if (!alive.get()) return false
        try {
            Thread.sleep(STARTUP_PROBE_MS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return processStillRunning()
    }

    private fun processStillRunning(): Boolean {
        if (usePty) return alive.get()
        return try {
            process?.exitValue()
            false
        } catch (stillRunning: IllegalThreadStateException) {
            true
        }
    }

    actual fun exec(command: String): ShellSessionResult {
        var output = ""
        var stderrOutput = ""
        val exitCode = stream(
            command,
            onLine = { line -> output = line },
            onNeedInput = { null },
            onStderrLine = { line -> stderrOutput = line },
            onStyledLine = {}
        )
        return ShellSessionResult(output, exitCode, _workingDirectory, stderrOutput)
    }

    actual fun stream(
        command: String,
        onLine: (line: String) -> Unit,
        onNeedInput: (prompt: String) -> String?,
        onStderrLine: (line: String) -> Unit,
        onStyledLine: (lines: List<List<StyledSpan>>) -> Unit
    ): Int {
        if (!isAlive) {
            onLine("shell is not running")
            return -1
        }

        var exitCode = -1
        val myGeneration = generation.get()

        try {
            val sin = stdin ?: throw IOException("stdin is null")
            val sout = stdout ?: throw IOException("stdout is null")
            val serr = stderr
            val emulator = TerminalEmulator(DummyTerminalOutput(), 120, 24, 10, 10, 1000, null)
            val stderrEmulator = TerminalEmulator(DummyTerminalOutput(), 120, 24, 10, 10, 1000, null)

            sin.write("{\n")
            sin.write(command)
            sin.write("\n__hg2gui_status=$?\n")
            sin.write(
                "printf '%s%s%d:%s\\n' \"$SENTINEL_HEAD\" \"$SENTINEL_TAIL\" " +
                    "\"\$__hg2gui_status\" \"\$PWD\"\n"
            )
            sin.write("}\n")
            sin.flush()

            val pending = StringBuilder()
            var emittedUpTo = 0
            val pendingErr = StringBuilder()
            var emittedUpToErr = 0
            val errBuf = CharArray(4096)
            var deadline = System.currentTimeMillis() + TIMEOUT_MS
            var lastDataAt = System.currentTimeMillis()
            var promptOfferedForThisStall = false
            val buf = CharArray(4096)

            fun drainStderr() {
                if (serr == null) return
                if (serr.ready()) {
                    val n = serr.read(errBuf)
                    if (n > 0) pendingErr.append(errBuf, 0, n)
                }
                if (pendingErr.length <= emittedUpToErr) return
                emittedUpToErr = trimConsumedPrefix(
                    pendingErr,
                    flush(pendingErr, emittedUpToErr, pendingErr.length, stderrEmulator, onStderrLine)
                )
            }

            while (true) {
                drainStderr()

                if (System.currentTimeMillis() > deadline) {
                    val bytes = "\r\n[timed out after ${TIMEOUT_MS / 1000}s]".toByteArray()
                    emulator.append(bytes, bytes.size)
                    onLine(emulator.transcriptText())
                    if (generation.get() == myGeneration) {
                        alive.set(false)
                        killChild()
                        try { stdin?.close() } catch (ignored: IOException) {}
                        try { stdout?.close() } catch (ignored: IOException) {}
                        try { stderr?.close() } catch (ignored: IOException) {}
                    }
                    break
                }

                if (sout.ready()) {
                    val n = sout.read(buf)
                    if (n == -1) {
                        markDeadIfCurrent(myGeneration)
                        break
                    }
                    if (n > 0) {
                        pending.append(buf, 0, n)
                        lastDataAt = System.currentTimeMillis()
                        promptOfferedForThisStall = false
                        deadline = System.currentTimeMillis() + TIMEOUT_MS
                    }
                } else {
                    val marker = pending.indexOf(SENTINEL, emittedUpTo)
                    val idleMs = System.currentTimeMillis() - lastDataAt
                    val tailIsUnterminated = pending.length > emittedUpTo &&
                        pending[pending.length - 1] != '\n' && pending[pending.length - 1] != '\r'
                    val stalledText = if (pending.length > emittedUpTo) pending.substring(emittedUpTo) else ""
                    val isTransientStatus = ShellAliases.looksLikeTransientStatus(stalledText)

                    if (!promptOfferedForThisStall && marker < 0 && tailIsUnterminated &&
                        !isTransientStatus && idleMs > PROMPT_IDLE_MS
                    ) {
                        promptOfferedForThisStall = true
                        emittedUpTo = trimConsumedPrefix(
                            pending,
                            flush(pending, emittedUpTo, pending.length, emulator, onLine)
                        )
                        onStyledLine(emulator.styledTranscript())
                        val answer = onNeedInput(pending.substring(0, pending.length))
                        if (answer != null) {
                            sin.write(answer)
                            sin.write("\n")
                            sin.flush()
                            lastDataAt = System.currentTimeMillis()
                            deadline = System.currentTimeMillis() + TIMEOUT_MS
                        }
                    }

                    try {
                        Thread.sleep(10L)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                    if (!processStillRunning()) {
                        markDeadIfCurrent(myGeneration)
                        break
                    }
                    continue
                }

                val marker = pending.indexOf(SENTINEL, emittedUpTo)
                if (marker < 0) {
                    val safeEnd = (pending.length - SENTINEL.length).coerceAtLeast(emittedUpTo)
                    emittedUpTo = trimConsumedPrefix(
                        pending,
                        flush(pending, emittedUpTo, safeEnd, emulator, onLine)
                    )
                    onStyledLine(emulator.styledTranscript())
                    continue
                }

                val newlineIdx = pending.indexOf("\n", marker + SENTINEL.length)
                if (newlineIdx < 0) continue
                emittedUpTo = trimConsumedPrefix(
                    pending,
                    flush(pending, emittedUpTo, marker, emulator, onLine)
                )
                val tail = pending.substring(marker + SENTINEL.length, newlineIdx).trimEnd('\r')
                val split = tail.indexOf(':')
                if (split > 0) {
                    try {
                        exitCode = tail.substring(0, split).toInt()
                    } catch (ignored: NumberFormatException) {}
                    val pwd = tail.substring(split + 1)
                    if (pwd.isNotEmpty()) _workingDirectory = pwd
                }
                drainStderr()
                onLine(emulator.transcriptText())
                onStyledLine(emulator.styledTranscript())
                break
            }
        } catch (e: IOException) {
            markDeadIfCurrent(myGeneration)
            onLine("shell died: ${e.message}")
            return -1
        }

        return exitCode
    }

    private fun flush(
        pending: StringBuilder,
        from: Int,
        to: Int,
        emulator: TerminalEmulator,
        onLine: (String) -> Unit
    ): Int {
        if (to <= from) return from
        val chunk = pending.substring(from, to)
        val bytes = chunk.replace("\r\n", "\n").replace("\n", "\r\n").toByteArray(Charsets.UTF_8)
        emulator.append(bytes, bytes.size)
        onLine(emulator.transcriptText())
        return to
    }

    private fun trimConsumedPrefix(pending: StringBuilder, emittedUpTo: Int): Int {
        if (emittedUpTo <= PENDING_TRIM_THRESHOLD) return emittedUpTo
        pending.delete(0, emittedUpTo)
        return 0
    }

    private fun TerminalEmulator.transcriptText(): String {
        val screen = mScreen
        val truncated = screen.getActiveTranscriptRows() >= screen.mTotalRows - screen.mScreenRows
        val text = screen.transcriptTextWithFullLinesJoined
        return if (truncated) {
            "[earlier output truncated - exceeded ${screen.mTotalRows}-line buffer]\n$text"
        } else text
    }

    actual fun interrupt() {
        if (usePty) {
            try {
                stdin?.write(INTR_BYTE)
                stdin?.flush()
            } catch (ignored: IOException) {}
            return
        }
        if (!isAlive) return
        generation.incrementAndGet()
        try { stdin?.close() } catch (ignored: IOException) {}
        try { stdout?.close() } catch (ignored: IOException) {}
        try { stderr?.close() } catch (ignored: IOException) {}
        process?.destroyForcibly()
        process = null
        stdin = null
        stdout = null
        stderr = null
        try {
            startPipe(home, command, extraEnv)
            _workingDirectory = home?.absolutePath ?: "/"
            alive.set(true)
        } catch (ignored: IOException) {
            alive.set(false)
        }
    }

    actual fun close() {
        alive.set(false)
        try {
            stdin?.let {
                it.write("exit\n")
                it.flush()
                it.close()
            }
        } catch (ignored: IOException) {}
        try { stdout?.close() } catch (ignored: IOException) {}
        try { stderr?.close() } catch (ignored: IOException) {}
        killChild()
    }

    private fun killChild() {
        if (usePty) {
            if (ptyPid > 0) {
                try {
                    Os.kill(ptyPid, OsConstants.SIGKILL)
                } catch (ignored: ErrnoException) {}
            }
            if (ptyFd >= 0) {
                JNI.close(ptyFd)
                ptyFd = -1
            }
        } else {
            process?.destroy()
        }
    }
}
