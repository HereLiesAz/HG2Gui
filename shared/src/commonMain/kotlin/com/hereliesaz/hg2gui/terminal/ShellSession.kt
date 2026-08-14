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
    fun stream(command: String, onLine: (line: String) -> Unit, onNeedInput: (prompt: String) -> String? = { null }): Int
    fun close()
}
