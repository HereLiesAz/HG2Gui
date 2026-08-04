package com.hereliesaz.hg2gui.terminal

data class ShellSessionResult(
    val output: String,
    val exitCode: Int,
    val workingDirectory: String
)

expect class ShellSession {
    val isAlive: Boolean
    val workingDirectory: String
    fun exec(command: String): ShellSessionResult
    fun close()
}
