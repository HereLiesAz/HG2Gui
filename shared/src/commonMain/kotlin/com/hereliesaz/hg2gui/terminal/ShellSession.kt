package com.hereliesaz.hg2gui.terminal

import com.hereliesaz.hg2gui.managers.StyledSpan

data class ShellSessionResult(
    val output: String,
    val exitCode: Int,
    val workingDirectory: String,
    // D3: empty whenever the command wrote nothing to stderr, and always empty on the pty tier
    // (a single fd - see ShellSession.stream's own doc comment on why that tier has nothing to
    // separate stderr from).
    val stderr: String = ""
)

expect class ShellSession {
    constructor(homePath: String?)
    constructor(homePath: String?, shellPath: String)

    val isAlive: Boolean
    val workingDirectory: String
    fun exec(command: String): ShellSessionResult
    // S2: stops the command a `stream()` call is currently running on this session, without
    // ending the session itself: a Ctrl-C on the pty tier, a signal to the shell's foreground
    // children on the pipe tier (the shell is only discarded if that does not work, and a new one
    // starts with the next command). A no-op if nothing is running; the in-flight `stream()`
    // returns 130.
    fun interrupt()
    // The shell is restarted by the next `stream()` call when it has died (exit, crash, kill), so a
    // tab never stays stuck on "shell is not running". A command that is merely silent is never
    // killed: the transcript gets a transient "still running" notice and the call keeps waiting.
    // [timeoutMs] is the opt-in cap for headless callers: when it is > 0 and the command has run
    // that long, it is cancelled like an interrupt and the call returns 124. 0 (the default) is
    // no cap - interactive callers stop a command with `interrupt()`.
    fun stream(
        command: String,
        onLine: (line: String) -> Unit,
        onNeedInput: (prompt: String) -> String? = { null },
        onStderrLine: (line: String) -> Unit = {},
        // D1: fires alongside onLine with the same transcript's per-run ANSI color/attribute
        // reading - see StyledTranscript.kt's own doc comment. Empty on every platform without a
        // real ANSI-parsing terminal emulator behind it; only the Android actual ever populates it.
        onStyledLine: (lines: List<List<StyledSpan>>) -> Unit = {},
        timeoutMs: Long = 0L
    ): Int
    fun close()
}
