package com.hereliesaz.hg2gui.managers

data class TerminalHistoryEntry(
    val command: String,
    val output: String = "",
    val isRunning: Boolean = false,
    // Null while running, and stays null for the bootstrap/Builtins branches (neither is a real
    // shell command with an exit status of its own) - only ever set once, in the same finally
    // block that flips isRunning back to false.
    val exitCode: Int? = null,
    // D3: stays empty for the bootstrap/Builtins branches (neither has a real stderr of its own)
    // and, on the real-shell branch, empty whenever ShellSession's pty tier is in use - a pty is
    // one fd, so there is nothing to separate there. On the pipe tier this carries stderr's own
    // cumulative transcript, kept apart from [output] rather than interleaved into it.
    val stderr: String = ""
)
