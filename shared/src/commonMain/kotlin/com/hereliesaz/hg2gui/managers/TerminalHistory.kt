package com.hereliesaz.hg2gui.managers

data class TerminalHistoryEntry(
    val command: String,
    val output: String = "",
    val isRunning: Boolean = false,
    // Null while running, and stays null for the bootstrap/Builtins branches (neither is a real
    // shell command with an exit status of its own) - only ever set once, in the same finally
    // block that flips isRunning back to false.
    val exitCode: Int? = null
)
