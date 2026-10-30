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
    private val usePty: Boolean = false,
    private val shellFamily: ShellFamily = ShellFamily.SH
) {
    companion object {
        private const val SENTINEL_HEAD = "__HG2GUI_EOC_"
        private const val START_HEAD = "__HG2GUI_SOC_"
        private const val NATIVE_BASH = "libbin_bash.so"
        private const val DEFAULT_SHELL = "/system/bin/sh"
        // How long a command may stay silent before the transcript gets a transient "still running"
        // notice. It is only a notice: silence never kills a command (a quiet compile step, a big
        // `curl -s`, an idle `ssh`).
        private const val QUIET_NOTICE_MS = 15_000L
        // Pipe-mode cancellation: nothing to signal for this long means the shell itself is the
        // blocked party (a builtin `read`); SIGTERM is upgraded to SIGKILL after the second figure;
        // past the third the shell is discarded and a new one starts with the next command.
        private const val CANCEL_NO_CHILD_MS = 300L
        private const val CANCEL_KILL_AFTER_MS = 1_500L
        private const val CANCEL_ABANDON_AFTER_MS = 4_000L
        private const val CANCEL_STEP_MS = 100L
        private const val CANCEL_NONE = 0
        private const val CANCEL_INTERRUPT = 1
        private const val CANCEL_DECLINED = 2
        private const val CANCEL_TIMEOUT = 3
        private const val EXIT_INTERRUPTED = 130
        private const val EXIT_TIMED_OUT = 124
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
            val env = mutableMapOf(
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
            // A damaged bootstrap (missing exec preload, unwritable PRoot tmp) must not crash
            // session creation: fall back to the bare system shell and say why in the log.
            try {
                Hg2ExecEnvironment.apply(context, env, bootstrapHome)
            } catch (e: Exception) {
                android.util.Log.w("ShellSession", "Termux exec environment unavailable; using the bare system shell", e)
                return null
            }
            return bootstrapHome to env
        }

        fun forAndroid(home: File?, context: Context): ShellSession {
            val reasons = mutableListOf<String>()
            val usePty = PtyPreference.isEnabled(context)
            val bootstrap = bootstrapBashEnv(context, home)
            if (bootstrap != null) {
                val (bootstrapHome, env) = bootstrap
                val selected = ShellPreference.selected(context)
                val selectedExecutable = ShellPreference.executable(context, selected)
                val family = ShellCommandProtocol.family(selected)
                if (selectedExecutable != null) {
                    val selectedSession = ShellSession(
                        bootstrapHome,
                        ShellCommandProtocol.loginArgs(family, selectedExecutable.absolutePath),
                        env,
                        "$selected (Termux bootstrap)",
                        usePty,
                        family
                    )
                    if (selectedSession.survivedStartup()) return selectedSession
                    selectedSession.close()
                    reasons += "selected shell '$selected' started but exited immediately"
                } else {
                    reasons += "selected shell '$selected' is unavailable"
                }

                if (selected != ShellPreference.BASH) {
                    val bash = File(env.getValue("HG2GUI_BASH"))
                    val bashSession = ShellSession(
                        bootstrapHome,
                        arrayOf(bash.absolutePath, "-l"),
                        env,
                        "bash (fallback after unavailable $selected)",
                        usePty,
                        ShellFamily.BASH
                    )
                    if (bashSession.survivedStartup()) return bashSession
                    bashSession.close()
                    reasons += "the exec-exempt Termux bash fallback started but exited immediately"
                }
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
                usePty,
                ShellFamily.SH
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

    // Per-session random nonce: a fixed marker could be forged by any command's output (a cat'd
    // file, a malicious package) to end a command early or spoof its exit status and cwd. The
    // framing still sends HEAD and TAIL as separate printf arguments, so the echoed command text
    // in PTY mode never contains the assembled marker.
    private val sentinelTail: String = java.security.SecureRandom().let { random ->
        (1..16).joinToString("") { Integer.toHexString(random.nextInt(16)) } + "__"
    }
    private val sentinel = SENTINEL_HEAD + sentinelTail
    private val startMarker = START_HEAD + sentinelTail

    // The child's handles. They are only ever replaced under this object's monitor (startChild,
    // restartChild, killChild, close) but are read without it by stream() on IO threads and by
    // isAlive on any thread, hence @Volatile (audit n-6). interrupt() no longer touches any of
    // them: it only raises a flag that the running stream() acts on, so there is nothing for it
    // to race with.
    @Volatile
    private var process: Process? = null
    @Volatile
    private var ptyFd: Int = -1
    // The one owner of the pty fd: stdin/stdout wrap this same FileDescriptor object, and closing
    // it through Os.close invalidates it for them too, so nothing closes the raw int twice (a
    // second close could hit an unrelated fd that reused the number).
    @Volatile
    private var ptyDescriptor: FileDescriptor? = null
    @Volatile
    private var ptyPid: Int = -1
    // Set once the waiter thread has reaped the pty child; its pid may then be reused by an
    // unrelated process and must not be signalled. One instance per child, so the waiter of a
    // replaced child can never mark its successor reaped.
    @Volatile
    private var ptyReaped: AtomicBoolean = AtomicBoolean(true)
    @Volatile
    private var stdin: BufferedWriter? = null
    @Volatile
    private var stdout: BufferedReader? = null
    @Volatile
    private var stderr: BufferedReader? = null
    // The shell's own pid, read from the start marker of the first command; -1 until then. Pipe
    // mode needs it to find the shell's foreground children, which Process cannot tell it on API 24.
    @Volatile
    private var shellPid: Int = -1
    private val alive = AtomicBoolean(false)
    private val generation = AtomicInteger(0)
    // True while a stream() call owns the shell; commands are strictly one at a time.
    private val running = AtomicBoolean(false)
    // Raised by interrupt(), consumed by the stream() that is running.
    @Volatile
    private var interruptRequested = false
    @Volatile
    private var closed = false

    /** Silence longer than this makes a running command show a "still running" notice. Tests shorten it. */
    @Volatile
    internal var quietNoticeMs: Long = QUIET_NOTICE_MS

    @Volatile
    private var _workingDirectory: String = home?.absolutePath ?: "/"

    actual val workingDirectory: String get() = _workingDirectory
    actual val isAlive: Boolean get() = !closed && alive.get() && processStillRunning()

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

    @Synchronized
    @Suppress("TooGenericExceptionCaught")
    private fun startChild(home: File?, command: Array<String>, extraEnv: Map<String, String>) {
        shellPid = -1
        try {
            if (usePty) startPty(home, command, extraEnv) else startPipe(home, command, extraEnv)
            alive.set(true)
        } catch (ignored: IOException) {
            alive.set(false)
        } catch (ignored: RuntimeException) {
            alive.set(false)
        }
    }

    /**
     * Replaces a dead (or abandoned) child with a fresh one and resets the per-shell state (cwd;
     * the environment and shell functions are gone with the old process). False when the session
     * is closed or the new child could not be started.
     */
    @Synchronized
    private fun restartChild(): Boolean {
        if (closed) return false
        generation.incrementAndGet()
        alive.set(false)
        try { stdin?.close() } catch (ignored: IOException) {}
        try { stdout?.close() } catch (ignored: IOException) {}
        try { stderr?.close() } catch (ignored: IOException) {}
        killChild()
        process = null
        stdin = null
        stdout = null
        stderr = null
        _workingDirectory = home?.absolutePath ?: "/"
        startChild(home, command, extraEnv)
        return alive.get() && !closed
    }

    private fun startPipe(home: File?, command: Array<String>, extraEnv: Map<String, String>) {
        val builder = ProcessBuilder(*command)
        if (home != null && home.isDirectory) builder.directory(home)
        if (extraEnv.isNotEmpty()) builder.environment().putAll(extraEnv)
        val p = builder.start()
        stdin = BufferedWriter(OutputStreamWriter(p.outputStream))
        stdout = BufferedReader(InputStreamReader(p.inputStream))
        stderr = BufferedReader(InputStreamReader(p.errorStream))
        process = p
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
        val childPid = pid[0]
        val reaped = AtomicBoolean(false)
        val childGeneration = generation.get()
        ptyFd = fd
        ptyPid = childPid
        ptyReaped = reaped
        val wrapped = wrapFileDescriptor(fd)
        ptyDescriptor = wrapped
        stdin = BufferedWriter(OutputStreamWriter(FileOutputStream(wrapped)))
        stdout = BufferedReader(InputStreamReader(FileInputStream(wrapped)))
        thread(name = "ShellSession-pty-waiter[pid=$childPid]") {
            JNI.waitFor(childPid)
            reaped.set(true)
            // A replaced child's waiter must not mark its successor dead.
            if (generation.get() == childGeneration) alive.set(false)
        }
    }

    private fun survivedStartup(): Boolean {
        if (!alive.get()) return false
        // Some callers (a service's onStartCommand) build their engine on the main thread; never
        // block it for the probe. There the check is immediate only, and a shell that dies a
        // moment later surfaces as "shell died" on its first command instead of falling back.
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return processStillRunning()
        try {
            Thread.sleep(STARTUP_PROBE_MS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return processStillRunning()
    }

    private fun processStillRunning(): Boolean {
        if (usePty) return alive.get()
        val p = process ?: return false
        return try {
            p.exitValue()
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
            onStyledLine = {},
            timeoutMs = 0L
        )
        return ShellSessionResult(output, exitCode, _workingDirectory, stderrOutput)
    }

    /**
     * Runs [command] in the long-lived shell and reports its output through the callbacks (each
     * [onLine] call is the whole transcript so far and supersedes the previous one).
     *
     * - A shell that has died (EOF, crash, `exit`, killed) is restarted here, with a "[shell
     *   restarted]" line at the top of the transcript; the tab never stays stuck.
     * - Silence is not a failure: after [quietNoticeMs] without output a transient "[still
     *   running ...]" line is appended to the snapshot (and dropped again by the next one) and
     *   the call keeps waiting. A command stops only through [interrupt], a prompt that
     *   [onNeedInput] declined (null) and that stayed silent for a quiet period, or the opt-in
     *   [timeoutMs] cap (> 0) for headless callers. Stopping cancels the command, not the shell.
     * - One command at a time: a second concurrent call is refused with -1.
     *
     * Returns the command's exit status, 130 for an interrupted or declined-prompt command, 124
     * for [timeoutMs], and -1 when the shell ended mid-command or could not be (re)started.
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    actual fun stream(
        command: String,
        onLine: (line: String) -> Unit,
        onNeedInput: (prompt: String) -> String?,
        onStderrLine: (line: String) -> Unit,
        onStyledLine: (lines: List<List<StyledSpan>>) -> Unit,
        timeoutMs: Long
    ): Int {
        if (closed) {
            onLine("shell is not running")
            return -1
        }
        if (!running.compareAndSet(false, true)) {
            onLine("another command is still running in this shell")
            return -1
        }
        interruptRequested = false
        try {
            return runCommand(command, onLine, onNeedInput, onStderrLine, onStyledLine, timeoutMs)
        } finally {
            interruptRequested = false
            running.set(false)
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "ComplexCondition")
    private fun runCommand(
        command: String,
        onLine: (line: String) -> Unit,
        onNeedInput: (prompt: String) -> String?,
        onStderrLine: (line: String) -> Unit,
        onStyledLine: (lines: List<List<StyledSpan>>) -> Unit,
        timeoutMs: Long
    ): Int {
        val emulator = TerminalEmulator(DummyTerminalOutput(), 120, 24, 10, 10, 1000, null)
        val stderrEmulator = TerminalEmulator(DummyTerminalOutput(), 120, 24, 10, 10, 1000, null)

        if (!isAlive) {
            if (!restartChild()) {
                onLine("shell is not running: it exited and could not be restarted")
                return -1
            }
            // Part of the transcript (not a separate emission) so the command's own output,
            // which supersedes each snapshot, keeps the line.
            safeAppend(emulator, "[shell restarted]\r\n".toByteArray())
            onLine(emulator.transcriptText())
        }

        val myGeneration = generation.get()
        val quietMs = quietNoticeMs.coerceAtLeast(1L)
        val pending = StringBuilder()
        var emittedUpTo = 0
        val pendingErr = StringBuilder()
        var emittedUpToErr = 0
        val errBuf = CharArray(4096)
        val buf = CharArray(4096)
        var exitCode = -1
        var completed = false // the end marker arrived and the final transcript was emitted
        var shellEnded = false // EOF / process exit / close() before the end marker
        // PTY mode echoes the framed input (and the shell prompt) back on the same stream as
        // real output; nothing is real output until the start marker arrives. Pipes don't echo,
        // but print the start marker line too, so it must be consumed in both modes.
        var started = false
        var cancelReason = CANCEL_NONE
        var cancelAt = 0L
        var nextCancelStepAt = 0L
        val startedAt = System.currentTimeMillis()
        var lastDataAt = startedAt
        var lastProgressAt = startedAt
        var nextNoticeAt = startedAt + quietMs
        var noticeVisible = false
        var promptOffered = false
        var promptDeclined = false

        try {
            val sin = stdin ?: throw IOException("stdin is null")
            val sout = stdout ?: throw IOException("stdout is null")
            val serr = stderr
            // The shell's descendants as they were before this command: background jobs of earlier
            // commands, which an interrupt of this one must not kill. Taken while the shell is idle.
            val preexisting: Set<Int> =
                if (!usePty && shellPid > 1) ShellProcessTree.descendants(shellPid) else emptySet()

            sin.write(ShellCommandProtocol.frame(shellFamily, command, START_HEAD, SENTINEL_HEAD, sentinelTail))
            sin.flush()

            // Output or stderr arrived or an answer was sent: the command is alive. Drops the
            // transient notice by re-emitting the plain transcript.
            fun noteProgress() {
                val now = System.currentTimeMillis()
                lastProgressAt = now
                nextNoticeAt = now + quietMs
                if (noticeVisible) {
                    noticeVisible = false
                    onLine(emulator.transcriptText())
                }
            }

            fun drainStderr() {
                if (serr == null) return
                if (serr.ready()) {
                    val n = serr.read(errBuf)
                    if (n > 0) {
                        pendingErr.append(errBuf, 0, n)
                        // A command that only reports progress on stderr is still making progress.
                        noteProgress()
                    }
                }
                if (pendingErr.length <= emittedUpToErr) return
                emittedUpToErr = trimConsumedPrefix(
                    pendingErr,
                    flush(pendingErr, emittedUpToErr, pendingErr.length, stderrEmulator, onStderrLine)
                )
            }

            // Starts cancelling the command (never the shell, unless that proves impossible).
            fun beginCancel(reason: Int) {
                if (cancelAt != 0L) return
                cancelAt = System.currentTimeMillis()
                cancelReason = reason
                if (usePty) {
                    try {
                        sin.write(INTR_BYTE)
                        sin.flush()
                    } catch (ignored: IOException) {}
                }
            }

            // Pipe mode: signals the command's processes until the shell reports the command's
            // end marker. True when that did not work and the shell has to be discarded.
            fun advanceCancel(): Boolean {
                val now = System.currentTimeMillis()
                val elapsed = now - cancelAt
                if (elapsed > CANCEL_ABANDON_AFTER_MS) return true
                if (now < nextCancelStepAt) return false
                nextCancelStepAt = now + CANCEL_STEP_MS
                val pid = shellPid
                if (!started || pid <= 1) return false
                val targets = ShellProcessTree.descendants(pid) - preexisting
                // Nothing to signal: the shell itself is the blocked party (a builtin `read`).
                if (targets.isEmpty()) return elapsed > CANCEL_NO_CHILD_MS
                ShellProcessTree.signal(targets, if (elapsed < CANCEL_KILL_AFTER_MS) "TERM" else "KILL")
                return false
            }

            while (true) {
                drainStderr()

                if (closed) {
                    shellEnded = true
                    break
                }

                val now = System.currentTimeMillis()
                if (cancelAt == 0L) {
                    if (interruptRequested) {
                        beginCancel(CANCEL_INTERRUPT)
                    } else if (timeoutMs > 0 && now - startedAt > timeoutMs) {
                        beginCancel(CANCEL_TIMEOUT)
                    } else if (promptDeclined && now - lastDataAt >= quietMs) {
                        beginCancel(CANCEL_DECLINED)
                    } else if (now >= nextNoticeAt) {
                        val shown = emulator.transcriptText()
                        val secs = (now - lastProgressAt) / 1000
                        onLine((if (shown.isEmpty()) "" else "$shown\n") + "[still running - no output for ${secs}s]")
                        noticeVisible = true
                        nextNoticeAt = now + quietMs
                    }
                }
                if (cancelAt != 0L) {
                    // The pty's line discipline turns the ^C into a SIGINT for the foreground
                    // job; whatever the shell prints afterwards is discarded before the next
                    // command's start marker.
                    if (usePty) break
                    if (advanceCancel()) {
                        abandonShell(myGeneration)
                        break
                    }
                }

                if (sout.ready()) {
                    val n = sout.read(buf)
                    if (n == -1) {
                        markDeadIfCurrent(myGeneration)
                        shellEnded = true
                        break
                    }
                    if (n > 0) {
                        pending.append(buf, 0, n)
                        if (!started) started = discardBeforeStart(pending)
                        lastDataAt = System.currentTimeMillis()
                        promptOffered = false
                        promptDeclined = false
                        noteProgress()
                    }
                } else {
                    val marker = pending.indexOf(sentinel, emittedUpTo)
                    val idleMs = System.currentTimeMillis() - lastDataAt
                    val tailIsUnterminated = pending.length > emittedUpTo &&
                        pending[pending.length - 1] != '\n' && pending[pending.length - 1] != '\r'
                    val stalledText = if (pending.length > emittedUpTo) pending.substring(emittedUpTo) else ""
                    val isTransientStatus = ShellAliases.looksLikeTransientStatus(stalledText)

                    if (started && cancelAt == 0L && !promptOffered && marker < 0 && tailIsUnterminated &&
                        !isTransientStatus && idleMs > PROMPT_IDLE_MS
                    ) {
                        promptOffered = true
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
                            noteProgress()
                        } else {
                            // Nobody can answer. The command may just be printing a partial line
                            // and thinking, so it gets a quiet period of grace before it is
                            // cancelled (it was killed after 15 s of silence before).
                            promptDeclined = true
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
                        shellEnded = true
                        break
                    }
                    continue
                }

                if (!started) continue
                val marker = pending.indexOf(sentinel, emittedUpTo)
                if (marker < 0) {
                    val safeEnd = (pending.length - sentinel.length).coerceAtLeast(emittedUpTo)
                    emittedUpTo = trimConsumedPrefix(
                        pending,
                        flush(pending, emittedUpTo, safeEnd, emulator, onLine)
                    )
                    onStyledLine(emulator.styledTranscript())
                    continue
                }

                val newlineIdx = pending.indexOf("\n", marker + sentinel.length)
                if (newlineIdx < 0) continue
                emittedUpTo = trimConsumedPrefix(
                    pending,
                    flush(pending, emittedUpTo, marker, emulator, onLine)
                )
                val tail = pending.substring(marker + sentinel.length, newlineIdx).trimEnd('\r')
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
                completed = true
                break
            }

            val cancelNote = when (cancelReason) {
                CANCEL_INTERRUPT -> "[interrupted]"
                CANCEL_TIMEOUT -> "[timed out after ${timeoutMs / 1000}s]"
                CANCEL_DECLINED -> "[cancelled: the prompt was not answered]"
                else -> null
            }
            fun appendNote(note: String) {
                val lead = if (emulator.getCursorCol() == 0) "" else "\r\n"
                safeAppend(emulator, (lead + note).toByteArray())
                onLine(emulator.transcriptText())
                onStyledLine(emulator.styledTranscript())
            }
            if (!completed) {
                // Left the loop without the end marker: cancelled, or the shell ended. Everything
                // read so far is real output (including what the end-marker hold-back kept back).
                if (started) {
                    emittedUpTo = trimConsumedPrefix(
                        pending,
                        flush(pending, emittedUpTo, pending.length, emulator, onLine)
                    )
                }
                drainStderr()
                val note = cancelNote ?: if (shellEnded) "[shell exited - a new one starts with the next command]" else null
                if (note != null) {
                    appendNote(note)
                } else {
                    onLine(emulator.transcriptText())
                    onStyledLine(emulator.styledTranscript())
                }
            } else if (cancelNote != null && exitCode != 0) {
                // The shell reported the cancelled command's end (the usual pipe-mode outcome).
                appendNote(cancelNote)
            }
            if (cancelReason != CANCEL_NONE && exitCode != 0) {
                exitCode = if (cancelReason == CANCEL_TIMEOUT) EXIT_TIMED_OUT else EXIT_INTERRUPTED
            }
        } catch (e: IOException) {
            markDeadIfCurrent(myGeneration)
            val lead = if (emulator.getCursorCol() == 0) "" else "\r\n"
            safeAppend(emulator, (lead + "shell died: ${e.message}").toByteArray())
            onLine(emulator.transcriptText())
            onStyledLine(emulator.styledTranscript())
            return -1
        }

        return exitCode
    }

    /**
     * Drops everything up to and including the start marker's line; true once it has been seen.
     * The marker line ends with `:<shell pid>`, which is remembered for [interrupt] to find the
     * shell's foreground children.
     */
    private fun discardBeforeStart(pending: StringBuilder): Boolean {
        val at = pending.indexOf(startMarker)
        if (at < 0) {
            // Keep only a tail long enough to hold a marker split across reads.
            val keep = startMarker.length
            if (pending.length > keep) pending.delete(0, pending.length - keep)
            return false
        }
        val eol = pending.indexOf("\n", at + startMarker.length)
        if (eol < 0) {
            pending.delete(0, at)
            return false
        }
        ShellCommandProtocol.parseShellPid(pending.substring(at + startMarker.length, eol))?.let { shellPid = it }
        pending.delete(0, eol + 1)
        return true
    }

    /** Feeds [bytes] to [emulator]; a parser failure on odd output drops that chunk, not the session. */
    private fun safeAppend(emulator: TerminalEmulator, bytes: ByteArray) {
        try {
            emulator.append(bytes, bytes.size)
        } catch (e: Exception) {
            android.util.Log.w("ShellSession", "Emulator failed on a chunk of output", e)
        }
    }

    private fun flush(
        pending: StringBuilder,
        from: Int,
        to: Int,
        emulator: TerminalEmulator,
        onLine: (String) -> Unit
    ): Int {
        // Never end a flush between the two halves of a surrogate pair (an emoji split across
        // reads or by the sentinel hold-back): the lone high surrogate would decode as U+FFFD.
        // It stays pending and goes out with its low half on the next flush.
        val end = if (to > from && pending[to - 1].isHighSurrogate()) to - 1 else to
        if (end <= from) return from
        val chunk = pending.substring(from, end)
        val bytes = chunk.replace("\r\n", "\n").replace("\n", "\r\n").toByteArray(Charsets.UTF_8)
        safeAppend(emulator, bytes)
        onLine(emulator.transcriptText())
        return end
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

    /**
     * Asks the running [stream] call to stop its command; a no-op when nothing is running. The
     * stream loop does the work on its own thread (so this is safe from the UI thread and never
     * races the child's streams): the pty tier gets a Ctrl-C, the pipe tier a SIGTERM (then
     * SIGKILL) to the shell's foreground children found through /proc, so the shell survives with
     * its working directory and environment. Only if the shell still has not reported the
     * command's end after a few seconds, or has no child to signal (it is blocked in a builtin
     * `read`), is it discarded - the next command starts a fresh one. The stream returns 130.
     */
    actual fun interrupt() {
        if (!running.get()) return
        interruptRequested = true
    }

    actual fun close() {
        closeChild()
    }

    @Synchronized
    private fun closeChild() {
        closed = true
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

    /** Discards the shell of [expectedGeneration] (unless it was already replaced); the next command restarts it. */
    @Synchronized
    private fun abandonShell(expectedGeneration: Int) {
        if (generation.get() != expectedGeneration) return
        alive.set(false)
        killChild()
    }

    @Synchronized
    private fun killChild() {
        if (usePty) {
            val pid = ptyPid
            if (pid > 0 && !ptyReaped.get()) {
                try {
                    Os.kill(pid, OsConstants.SIGKILL)
                } catch (ignored: ErrnoException) {}
            }
            ptyDescriptor?.let { descriptor ->
                if (descriptor.valid()) {
                    try { Os.close(descriptor) } catch (ignored: ErrnoException) {}
                }
            }
            ptyDescriptor = null
            ptyFd = -1
        } else {
            // The shell's children first: killing only the shell would orphan a silent `sleep`.
            val pid = shellPid
            if (pid > 1 && processStillRunning()) {
                ShellProcessTree.signal(ShellProcessTree.descendants(pid), "KILL")
            }
            process?.destroyForcibly()
        }
    }
}

/**
 * Finds and signals the descendants of a pid through /proc. Best effort by design: every failure
 * (no /proc, a process that vanished, no permission) yields "nothing found" or "nothing signalled",
 * never an exception. Signals go through `kill` in a short-lived `sh` rather than android.system.Os
 * so the same code runs in host-JVM tests.
 */
internal object ShellProcessTree {
    /** The parent pid from the text of a `/proc/<pid>/stat`: `pid (comm) state ppid ...`, where comm may hold spaces and parentheses. */
    fun parentOf(stat: String): Int? {
        val closeParen = stat.lastIndexOf(')')
        if (closeParen < 0) return null
        val fields = stat.substring(closeParen + 1).trim().split(' ')
        return fields.getOrNull(1)?.toIntOrNull()
    }

    /** Every transitive child of [rootPid] (not [rootPid] itself), by a snapshot scan of [procRoot]. */
    fun descendants(rootPid: Int, procRoot: File = File("/proc")): Set<Int> {
        if (rootPid <= 1) return emptySet()
        val entries: Array<File>? = try {
            procRoot.listFiles()
        } catch (ignored: SecurityException) {
            null
        }
        if (entries == null) return emptySet()
        val childrenOf = HashMap<Int, MutableList<Int>>()
        for (entry in entries) {
            val pid = entry.name.toIntOrNull() ?: continue
            val stat = try {
                FileInputStream(File(entry, "stat")).use { String(it.readBytes(), Charsets.ISO_8859_1) }
            } catch (ignored: IOException) {
                continue
            }
            val parent = parentOf(stat) ?: continue
            childrenOf.getOrPut(parent) { mutableListOf() }.add(pid)
        }
        val found = LinkedHashSet<Int>()
        val queue = ArrayList<Int>()
        queue.add(rootPid)
        var index = 0
        while (index < queue.size) {
            val next = queue[index]
            index++
            for (child in childrenOf[next].orEmpty()) {
                if (found.add(child)) queue.add(child)
            }
        }
        return found
    }

    /** Sends SIG[signal] (`TERM`, `KILL`) to every pid in [pids] above 1. */
    @Suppress("TooGenericExceptionCaught")
    fun signal(pids: Collection<Int>, signal: String) {
        val targets = pids.filter { it > 1 }
        if (targets.isEmpty()) return
        try {
            val sh = if (File("/system/bin/sh").canExecute()) "/system/bin/sh" else "/bin/sh"
            val command = ArrayList<String>()
            command.add(sh)
            command.add("-c")
            command.add("kill -$signal \"\$@\" >/dev/null 2>&1")
            command.add("hg2gui-kill")
            targets.forEach { command.add(it.toString()) }
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            try { process.outputStream.close() } catch (ignored: IOException) {}
            try { process.inputStream.use { it.readBytes() } } catch (ignored: IOException) {}
            process.waitFor()
        } catch (ignored: IOException) {
        } catch (ignored: RuntimeException) {
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
