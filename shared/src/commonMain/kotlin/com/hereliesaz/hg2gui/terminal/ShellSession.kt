package com.hereliesaz.hg2gui.terminal

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
    fun stream(
        command: String,
        onLine: (line: String) -> Unit,
        onNeedInput: (prompt: String) -> String? = { null },
        onStderrLine: (line: String) -> Unit = {}
    ): Int
    fun close()
}
