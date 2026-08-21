package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import com.hereliesaz.hg2gui.managers.PtyPreference
import com.hereliesaz.hg2gui.managers.StyledSpan
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
import kotlin.concurrent.thread

import com.termux.terminal.JNI
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalOutput

class DummyTerminalOutput : TerminalOutput() {
    override fun write(data: ByteArray, offset: Int, count: Int) {}
    override fun titleChanged(oldTitle: String?, newTitle: String?) {}
    override fun onCopyTextToClipboard(text: String) {}
    override fun onPasteTextFromClipboard() {}
    override fun onBell() {}
    override fun onColorsChanged() {}
}

/**
 * UX-5: this session's process is a plain [ProcessBuilder] with piped stdin/stdout - not a real
 * pseudoterminal. The child never gets a controlling tty: `isatty()` reports false, there's no
 * termios to carry echo/raw-mode state, no SIGWINCH to learn the screen resized, no job-control
 * signals. Line-buffered tools work fine through a pipe, but anything that draws a full-screen UI
 * against a tty directly - `vim`, `less`, `top`, an interactive `python3`/`node` REPL - either
 * falls back to a dumb-terminal mode or breaks outright, since those programs branch on `isatty`
 * and drive the screen through termios/ioctl calls a plain pipe has no way to answer.
 *
 * This isn't a missing feature so much as an unfinished one: the app already bundles a real,
 * working native pty bridge (`terminal-emulator` module - `JNI.kt`'s `createSubprocess`/
 * `setPtyWindowSize`, backed by `jni/termux.c`, the same one upstream Termux's own
 * `TerminalSession` uses to open `/dev/ptmx` and fork the child onto the slave side). The
 * `TerminalEmulator` this file already constructs (see `stream()` below) is exactly the class
 * that bridge is designed to feed - today it only ever gets fed after-the-fact, from a pipe, to
 * flatten output for display, never wired up as the live consumer of a real pty's master fd.
 * Rewiring ShellSession onto that bridge is a real, bounded task, not a design unknown - but it
 * replaces the process-I/O path every terminal command in the app runs through, and every change
 * in this file already carries the same caveat repeated elsewhere in this class: there is no
 * physical device in this environment to verify behavior on. Swapping the one shared session
 * backend blind, with no way to catch a regression before it ships, is a worse trade than leaving
 * this documented and unstarted.
 */
actual class ShellSession private constructor(
    home: File?,
    command: Array<String>,
    extraEnv: Map<String, String>,
    // Which of forAndroid()'s three tiers this session actually ended up on, and - only for the
    // last-resort system shell, since that's the one with no apt/pkg/coreutils - why the better
    // ones weren't used. forAndroid() used to pick silently: a session that landed on
    // /system/bin/sh because zsh isn't bundled for this device's ABI, or because the Termux
    // bootstrap's bash exists on disk but the OS won't actually execute a binary extracted into
    // app-private storage (the write-xor-execute restriction Android's enforced since API 29),
    // looked identical to one that landed there for any other reason - "command not found" with
    // no way to tell which. TerminalEngine surfaces this once per session pick, in-transcript.
    val backendDescription: String = "the bare system shell ($DEFAULT_SHELL) - no fallback reached",
    // See PtyPreference's own doc comment: off by default, since a plain pipe is what every
    // command in the app has always run through and the real pty path (below) has never been
    // verified on a physical device. Only forAndroid() ever passes true.
    private val usePty: Boolean = false
) {

    companion object {
        private const val SENTINEL = "__HG2GUI_EOC_a7f3__"
        private const val DEFAULT_SHELL = "/system/bin/sh"
        private const val TIMEOUT_MS = 15_000L
        private const val STARTUP_PROBE_MS = 300L
        private const val PROMPT_IDLE_MS = 400L

        // D5: [pending] below accumulates every raw byte read for the life of one stream() call -
        // flush() only ever advances a read cursor into it, never shrinks it, so a command that
        // produces a huge amount of output before the sentinel line arrives (cat on a large file,
        // a verbose build) grew it without bound even though the rendered TerminalEmulator
        // transcript stays capped at 1000 rows. Once the already-flushed prefix passes this many
        // chars, it's dropped from [pending] and the cursor reset to 0 - transparent, since
        // nothing downstream ever reads before the cursor.
        private const val PENDING_TRIM_THRESHOLD = 65_536

        // Matches the width/height stream()'s own per-command TerminalEmulator(...) already
        // assumes below - so the pty's actual window size (what ioctl(TIOCGWINSZ) reports to the
        // child) doesn't disagree with the geometry every command's output gets re-wrapped to
        // when flattened back into plain text.
        private const val PTY_ROWS = 24
        private const val PTY_COLUMNS = 120
        private const val PTY_CELL_WIDTH = 10
        private const val PTY_CELL_HEIGHT = 10

        // Zsh was bundled the same way as the Termux bootstrap (a flattened, exec-exempt native
        // lib) but its own RUNPATH/dependency chain was never patched the way the bootstrap's
        // was - see the bootstrap RUNPATH fix's commit for the underlying bug. Pulled out for now
        // rather than shipping a shell tier known to carry the same defect unfixed; forAndroid()
        // goes straight to the Termux bootstrap.
        fun forAndroid(home: File?, context: Context): ShellSession {
            // Collected only to explain the last-resort fallback below, if it comes to that -
            // never surfaced when the bootstrap actually works.
            val reasons = mutableListOf<String>()
            val usePty = PtyPreference.isEnabled(context)

            if (DistroManager.isInstalled(context)) {
                val prefix = DistroManager.prefixDir(context)
                val bash = File(prefix, "bin/bash")
                if (bash.canExecute()) {
                    val bootstrapHome = home ?: DistroManager.homeDir(context)
                    if (!bootstrapHome.exists()) bootstrapHome.mkdirs()
                    val env = mapOf(
                        "HOME" to bootstrapHome.absolutePath,
                        "PREFIX" to prefix.absolutePath,
                        "PATH" to "${prefix.absolutePath}/bin",
                        "LD_LIBRARY_PATH" to "${prefix.absolutePath}/lib",
                        "TMPDIR" to "${prefix.absolutePath}/tmp",
                        "LANG" to "en_US.UTF-8",
                        // apt's own Dir::* defaults are compiled in against Termux's own package -
                        // this points it at the apt.conf DistroManager writes (with every Dir::
                        // pointed at this app's real prefix instead) before it ever falls back to
                        // those. See DistroManager.writeAptConf's own doc comment for why a config
                        // file, not an env var alone, is what apt actually needs here.
                        "APT_CONFIG" to "${prefix.absolutePath}/etc/apt/apt.conf",
                        // apt.conf's Acquire::https::CaInfo covers apt itself; these are the same
                        // bundle for every other bootstrap-tier tool that talks TLS (curl, wget,
                        // git, ...) via the conventions each already knows to check.
                        "SSL_CERT_FILE" to "${prefix.absolutePath}/etc/tls/cert.pem",
                        "CURL_CA_BUNDLE" to "${prefix.absolutePath}/etc/tls/cert.pem",
                        // Only matters on the pty tier (a plain pipe was never a tty to begin
                        // with, so no ncurses program would trust TERM over it either way), but
                        // harmless to set regardless. ncurses' own compiled-in terminfo search
                        // path is baked in against Termux's own package the same way apt's Dir::*
                        // defaults are - TERMINFO overrides that with the copy this bootstrap
                        // actually ships, confirmed present at share/terminfo/x/xterm-256color.
                        "TERM" to "xterm-256color",
                        "TERMINFO" to "${prefix.absolutePath}/share/terminfo"
                    )
                    val session = ShellSession(
                        bootstrapHome, arrayOf(bash.absolutePath, "-l"), env, "bash (Termux bootstrap)", usePty
                    )
                    if (session.survivedStartup()) return session
                    session.close()
                    // The most likely real cause on a modern device: Android's blocked executing
                    // binaries out of app-private storage since API 29 (write-xor-execute) unless
                    // they're the APK's own bundled native libs - a downloaded-and-extracted
                    // bin/bash never qualifies for that exemption on its own.
                    reasons += "the Termux bootstrap's bash started but exited immediately - " +
                        "possibly blocked from executing a binary extracted into app-private " +
                        "storage (Android disallows this since API 29 unless it shipped in the APK)"
                } else {
                    reasons += "the Termux bootstrap's bash isn't executable"
                }
            } else {
                reasons += "no Termux bootstrap is installed"
            }

            // The pipe tier gets a real PATH for free (ProcessBuilder inherits this Android
            // process's own environment, unlike the pty tier's clearenv() - see startPty's own
            // doc comment); the pty tier would otherwise leave /system/bin/sh with nothing set
            // at all, unable to resolve even a bare "ls" typed at its prompt.
            val fallbackEnv = if (usePty) mapOf("PATH" to "/system/bin:/system/xbin", "HOME" to (home?.absolutePath ?: "/")) else emptyMap()
            return ShellSession(
                home, arrayOf(DEFAULT_SHELL), fallbackEnv,
                "the bare system shell ($DEFAULT_SHELL), without apt/pkg/coreutils - " +
                    reasons.joinToString("; "),
                usePty
            )
        }

        // Reflection mirrors terminal-emulator's own TerminalSession.wrapFileDescriptor - there
        // is no public API to build a FileDescriptor from a raw native fd int, only from opening
        // a real file, so this is the same private-field trick that module's own pty usage
        // already relies on.
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

    // Only ever populated on the plain-pipe tier - the pty tier has no java Process object at
    // all, only a raw native fd and pid (ptyFd/ptyPid below).
    private var process: Process? = null
    private var ptyFd: Int = -1
    private var ptyPid: Int = -1
    private var stdin: BufferedWriter? = null
    private var stdout: BufferedReader? = null
    // D3: only ever populated on the plain-pipe tier (startPipe, below) - a real pty is a single
    // fd, so startPty has no second stream to separate this from. Null here is exactly the
    // signal stream()'s own drainStderr uses to skip stderr handling entirely on that tier.
    private var stderr: BufferedReader? = null

    private val alive = AtomicBoolean(false)

    @Volatile
    private var _workingDirectory: String = home?.absolutePath ?: "/"

    actual val workingDirectory: String
        get() = _workingDirectory

    actual val isAlive: Boolean
        get() = alive.get() && processStillRunning()

    constructor(home: File?) : this(home, arrayOf(DEFAULT_SHELL), emptyMap())

    constructor(home: File?, shellPath: String) : this(home, arrayOf(shellPath), emptyMap())

    actual constructor(homePath: String?) : this(homePath?.let { File(it) })

    actual constructor(homePath: String?, shellPath: String) : this(homePath?.let { File(it) }, shellPath)

    init {
        startChild(home, command, extraEnv)
    }

    // @Suppress: JNI.createSubprocess (see termux.c's throw_runtime_exception) only ever throws
    // the bare java.lang.RuntimeException class itself for a native pty failure (ptmx open,
    // grantpt/unlockpt) - there is no more specific type to narrow the catch below to. Treated
    // exactly like a pipe-tier IOException: this session never came up, and forAndroid()'s own
    // fallback chain is what handles that, not a crash. (Kotlin doesn't allow annotating an
    // `init` block directly, hence this being its own function.)
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
        // D3: used to be redirectErrorStream(true), which folds stderr into the same pipe as
        // stdout before either ever reaches this class - by the time stream() sees a byte there
        // is no way left to tell which stream it came from. Keeping them apart here is what makes
        // a separate stderr transcript possible at all; stream() drains this reader on its own
        // schedule, same non-blocking style as stdout, so the child never stalls on a full stderr
        // pipe buffer just because nothing was reading it.
        if (home != null && home.isDirectory) builder.directory(home)
        if (extraEnv.isNotEmpty()) {
            builder.environment().putAll(extraEnv)
        }

        val p = builder.start()
        process = p
        stdin = BufferedWriter(OutputStreamWriter(p.outputStream))
        stdout = BufferedReader(InputStreamReader(p.inputStream))
        stderr = BufferedReader(InputStreamReader(p.errorStream))
    }

    /** A real pseudoterminal via the app's own bundled native pty bridge (terminal-emulator's
     *  JNI/termux.c - the same one upstream Termux's own TerminalSession uses to open
     *  /dev/ptmx and fork the child onto the slave side), instead of a plain OS pipe - see this
     *  file's own top-of-file doc comment for why a pipe alone can't satisfy isatty()/termios-
     *  checking tools. Unlike [startPipe]'s ProcessBuilder, the child's environment here is NOT
     *  inherited from this Android process at all - the native side clearenv()s before exec, so
     *  every variable the shell needs has to already be in [extraEnv], nothing carries over
     *  silently. [stream]/[exec] read and write this exactly like the pipe tier below - only how
     *  stdin/stdout are opened differs. */
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

        // processStillRunning() below is a plain flag check on this tier, not a poll - nothing
        // else calls JNI.waitFor() for this pid, so this thread existing is what notices the
        // child actually exiting at all.
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
        // The pty tier has no exitValue()-style poll - startPty()'s own waiter thread is the
        // only thing that ever learns the child exited, by blocking on JNI.waitFor(); `alive`
        // is what it flips when that happens.
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

        try {
            val sin = stdin ?: throw IOException("stdin is null")
            val sout = stdout ?: throw IOException("stdout is null")
            val serr = stderr

            val emulator = TerminalEmulator(DummyTerminalOutput(), 120, 24, 10, 10, 1000, null)
            val stderrEmulator = TerminalEmulator(DummyTerminalOutput(), 120, 24, 10, 10, 1000, null)

            sin.write(command)
            sin.write("\n")
            sin.write("printf '%s%d:%s\\n' \"$SENTINEL\" \"$?\" \"\$PWD\"\n")
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

            // D3: a local function, not a private member, so it can close over this call's own
            // mutable emittedUpToErr directly instead of threading it through as a parameter and
            // a return value on every call site - [serr] is null outright on the pty tier (see
            // the [stderr] field's own doc comment), so this is a no-op there and stderr stays
            // folded into the single pty transcript the way it always has.
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
                    // The shell is genuinely stuck here - a declined/unanswerable prompt, or
                    // something that produced no output at all for the whole timeout - so this
                    // has to actually end it, not just give up on this one call. Leaving the
                    // process alive but abandoned mid-read means the *next* stream() call writes
                    // its command straight into that stale read: the new command never runs, and
                    // its text gets silently consumed as the old prompt's answer instead.
                    alive.set(false)
                    killChild()
                    try { stdin?.close() } catch (ignored: IOException) {}
                    try { stdout?.close() } catch (ignored: IOException) {}
                    try { stderr?.close() } catch (ignored: IOException) {}
                    break
                }

                if (sout.ready()) {
                    val n = sout.read(buf)
                    if (n == -1) {
                        alive.set(false)
                        break
                    }
                    if (n > 0) {
                        pending.append(buf, 0, n)
                        lastDataAt = System.currentTimeMillis()
                        promptOfferedForThisStall = false
                        // Still actively producing output - that's working, not stalled. The
                        // timeout exists to catch a genuinely stuck process, not to cap how long
                        // a real job (an install, a clone, a build) is allowed to keep running.
                        deadline = System.currentTimeMillis() + TIMEOUT_MS
                    }
                } else {
                    val marker = pending.indexOf(SENTINEL, emittedUpTo)
                    val idleMs = System.currentTimeMillis() - lastDataAt
                    val tailIsUnterminated = pending.length > emittedUpTo &&
                        pending[pending.length - 1] != '\n' && pending[pending.length - 1] != '\r'

                    if (!promptOfferedForThisStall && marker < 0 && tailIsUnterminated && idleMs > PROMPT_IDLE_MS) {
                        promptOfferedForThisStall = true
                        emittedUpTo = trimConsumedPrefix(pending, flush(pending, emittedUpTo, pending.length, emulator, onLine))
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
                        alive.set(false)
                        break
                    }
                    continue
                }

                val marker = pending.indexOf(SENTINEL, emittedUpTo)
                if (marker < 0) {
                    val safeEnd = (pending.length - SENTINEL.length).coerceAtLeast(emittedUpTo)
                    emittedUpTo = trimConsumedPrefix(pending, flush(pending, emittedUpTo, safeEnd, emulator, onLine))
                    onStyledLine(emulator.styledTranscript())
                    continue
                }

                val newlineIdx = pending.indexOf("\n", marker + SENTINEL.length)
                if (newlineIdx < 0) continue

                emittedUpTo = trimConsumedPrefix(pending, flush(pending, emittedUpTo, marker, emulator, onLine))
                val tail = pending.substring(marker + SENTINEL.length, newlineIdx).trimEnd('\r')
                val split = tail.indexOf(':')
                if (split > 0) {
                    try {
                        exitCode = tail.substring(0, split).toInt()
                    } catch (ignored: NumberFormatException) {
                    }
                    val pwd = tail.substring(split + 1)
                    if (pwd.isNotEmpty()) _workingDirectory = pwd
                }
                drainStderr()
                onLine(emulator.transcriptText())
                onStyledLine(emulator.styledTranscript())
                break
            }
        } catch (e: IOException) {
            alive.set(false)
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

    // D5: called with flush()'s own return value - drops [pending]'s already-flushed prefix once
    // it's grown past PENDING_TRIM_THRESHOLD, since nothing downstream ever reads before the
    // cursor flush() just returned.
    private fun trimConsumedPrefix(pending: StringBuilder, emittedUpTo: Int): Int {
        if (emittedUpTo <= PENDING_TRIM_THRESHOLD) return emittedUpTo
        pending.delete(0, emittedUpTo)
        return 0
    }

    // MCP-13: mScreen is a fixed-size circular scrollback (1000 rows, see the TerminalEmulator
    // constructor above) - once a command's output exceeds that, the buffer silently starts
    // overwriting its own oldest lines. An interactive user watching the screen scroll live
    // barely notices; an MCP caller reading back transcriptTextWithFullLinesJoined() has no way
    // to tell a short, complete transcript from the tail end of one that lost its beginning.
    // getActiveTranscriptRows() maxing out at mTotalRows - mScreenRows is exactly that "the
    // buffer is now full and about to start overwriting" moment, so once true it stays true for
    // the rest of this stream() call - once truncation can happen it doesn't become un-true.
    private fun TerminalEmulator.transcriptText(): String {
        val screen = mScreen
        val truncated = screen.getActiveTranscriptRows() >= screen.mTotalRows - screen.mScreenRows
        val text = screen.transcriptTextWithFullLinesJoined
        return if (truncated) "[earlier output truncated - exceeded ${screen.mTotalRows}-line buffer]\n$text" else text
    }

    actual fun close() {
        alive.set(false)
        try {
            stdin?.let {
                it.write("exit\n")
                it.flush()
                it.close()
            }
        } catch (ignored: IOException) {
        }
        try {
            stdout?.close()
        } catch (ignored: IOException) {
        }
        try {
            stderr?.close()
        } catch (ignored: IOException) {
        }
        killChild()
    }

    /** Force-stops the child regardless of which tier it's running on - called both by [close]
     *  and by [stream]'s own timeout branch (a genuinely stuck command has to actually be ended,
     *  not just have its streams closed out from under it - see that branch's own comment). */
    private fun killChild() {
        if (usePty) {
            if (ptyPid > 0) {
                try {
                    Os.kill(ptyPid, OsConstants.SIGKILL)
                } catch (ignored: ErrnoException) {
                    // Already exited - nothing left to kill.
                }
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
