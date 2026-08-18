package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean

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
    val backendDescription: String = "the bare system shell ($DEFAULT_SHELL) - no fallback reached"
) {

    companion object {
        private const val SENTINEL = "__HG2GUI_EOC_a7f3__"
        private const val DEFAULT_SHELL = "/system/bin/sh"
        private const val TIMEOUT_MS = 15_000L
        private const val STARTUP_PROBE_MS = 300L
        private const val PROMPT_IDLE_MS = 400L

        // Zsh was bundled the same way as the Termux bootstrap (a flattened, exec-exempt native
        // lib) but its own RUNPATH/dependency chain was never patched the way the bootstrap's
        // was - see the bootstrap RUNPATH fix's commit for the underlying bug. Pulled out for now
        // rather than shipping a shell tier known to carry the same defect unfixed; forAndroid()
        // goes straight to the Termux bootstrap.
        fun forAndroid(home: File?, context: Context): ShellSession {
            // Collected only to explain the last-resort fallback below, if it comes to that -
            // never surfaced when the bootstrap actually works.
            val reasons = mutableListOf<String>()

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
                        "LANG" to "en_US.UTF-8"
                    )
                    val session = ShellSession(bootstrapHome, arrayOf(bash.absolutePath, "-l"), env, "bash (Termux bootstrap)")
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

            return ShellSession(
                home, arrayOf(DEFAULT_SHELL), emptyMap(),
                "the bare system shell ($DEFAULT_SHELL), without apt/pkg/coreutils - " +
                    reasons.joinToString("; ")
            )
        }
    }

    private var process: Process? = null
    private var stdin: BufferedWriter? = null
    private var stdout: BufferedReader? = null

    private val alive = AtomicBoolean(false)

    @Volatile
    private var _workingDirectory: String = home?.absolutePath ?: "/"

    actual val workingDirectory: String
        get() = _workingDirectory

    actual val isAlive: Boolean
        get() = alive.get() && process != null && processStillRunning()

    constructor(home: File?) : this(home, arrayOf(DEFAULT_SHELL), emptyMap())

    constructor(home: File?, shellPath: String) : this(home, arrayOf(shellPath), emptyMap())

    actual constructor(homePath: String?) : this(homePath?.let { File(it) })

    actual constructor(homePath: String?, shellPath: String) : this(homePath?.let { File(it) }, shellPath)

    init {
        try {
            val builder = ProcessBuilder(*command)
            builder.redirectErrorStream(true)
            if (home != null && home.isDirectory) builder.directory(home)
            if (extraEnv.isNotEmpty()) {
                builder.environment().putAll(extraEnv)
            }

            val p = builder.start()
            process = p
            stdin = BufferedWriter(OutputStreamWriter(p.outputStream))
            stdout = BufferedReader(InputStreamReader(p.inputStream))

            alive.set(true)
        } catch (e: IOException) {
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
        return try {
            process?.exitValue()
            false
        } catch (stillRunning: IllegalThreadStateException) {
            true
        }
    }

    actual fun exec(command: String): ShellSessionResult {
        var output = ""
        val exitCode = stream(command, onLine = { line -> output = line }, onNeedInput = { null })
        return ShellSessionResult(output, exitCode, _workingDirectory)
    }

    actual fun stream(command: String, onLine: (line: String) -> Unit, onNeedInput: (prompt: String) -> String?): Int {
        if (!isAlive) {
            onLine("shell is not running")
            return -1
        }

        var exitCode = -1

        try {
            val sin = stdin ?: throw IOException("stdin is null")
            val sout = stdout ?: throw IOException("stdout is null")

            val emulator = TerminalEmulator(DummyTerminalOutput(), 120, 24, 10, 10, 1000, null)

            sin.write(command)
            sin.write("\n")
            sin.write("printf '%s%d:%s\\n' \"$SENTINEL\" \"$?\" \"\$PWD\"\n")
            sin.flush()

            val pending = StringBuilder()
            var emittedUpTo = 0
            var deadline = System.currentTimeMillis() + TIMEOUT_MS
            var lastDataAt = System.currentTimeMillis()
            var promptOfferedForThisStall = false
            val buf = CharArray(4096)

            while (true) {
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
                    process?.destroy()
                    try { stdin?.close() } catch (ignored: IOException) {}
                    try { stdout?.close() } catch (ignored: IOException) {}
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
                        emittedUpTo = flush(pending, emittedUpTo, pending.length, emulator, onLine)
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
                    emittedUpTo = flush(pending, emittedUpTo, safeEnd, emulator, onLine)
                    continue
                }

                val newlineIdx = pending.indexOf("\n", marker + SENTINEL.length)
                if (newlineIdx < 0) continue

                emittedUpTo = flush(pending, emittedUpTo, marker, emulator, onLine)
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
                onLine(emulator.transcriptText())
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
        process?.destroy()
    }
}
