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
    // S2: stops whatever `stream()` call is currently in flight on this session, without ending
    // the session itself - see the actual implementation's own doc comment for what "stop" means
    // on each tier (a real signal on the pty tier, a kill-and-respawn on the pipe tier). A no-op
    // if nothing is running.
    fun interrupt()
    fun stream(
        command: String,
        onLine: (line: String) -> Unit,
        onNeedInput: (prompt: String) -> String? = { null },
        onStderrLine: (line: String) -> Unit = {},
        // D1: fires alongside onLine with the same transcript's per-run ANSI color/attribute
        // reading - see StyledTranscript.kt's own doc comment. Empty on every platform without a
        // real ANSI-parsing terminal emulator behind it; only the Android actual ever populates it.
        onStyledLine: (lines: List<List<StyledSpan>>) -> Unit = {}
    ): Int
    fun close()
}
