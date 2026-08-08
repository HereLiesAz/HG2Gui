package com.hereliesaz.hg2gui.terminal

data class ShellSessionResult(
    val output: String,
    val exitCode: Int,
    val workingDirectory: String
)

expect class ShellSession {
    constructor(homePath: String?)
    constructor(homePath: String?, shellPath: String)

    val isAlive: Boolean
    val workingDirectory: String
    fun exec(command: String): ShellSessionResult
    // onNeedInput is called (blocking) with the prompt text whenever the running command
    // appears to be stalled waiting on stdin - e.g. "Overwrite file? [y/N] ", which never
    // prints its own newline while it waits. Returning null declines to answer; a non-null
    // return is written back to the process as its next line of input.
    fun stream(command: String, onLine: (line: String) -> Unit, onNeedInput: (prompt: String) -> String? = { null }): Int
    fun close()
}
