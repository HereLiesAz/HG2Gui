package com.hereliesaz.hg2gui.managers

data class TerminalHistoryEntry(
    val command: String,
    val output: String = "",
    val isRunning: Boolean = false
)
