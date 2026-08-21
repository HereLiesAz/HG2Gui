package com.hereliesaz.hg2gui.managers

// D1 (HG2Gui Termux Coverage.dc.html): one contiguous run of same-styled text within one
// terminal line, as parsed off the real shell's own ANSI/SGR escapes - not stripped, mapped.
// [hue] indexes into Azphalt.hues/caps (ui/menu/PillMenu.kt) when the source used a color this
// maps to a named hue; null means "no mapped color" (an unrecognized 256-color/truecolor cell,
// or the terminal's own default foreground) and renders in the surrounding text's normal ink.
data class StyledSpan(val text: String, val hue: Int? = null, val bold: Boolean = false)

data class TerminalHistoryEntry(
    val command: String,
    val output: String = "",
    val isRunning: Boolean = false,
    // Null while running, and stays null for the bootstrap/Builtins branches (neither is a real
    // shell command with an exit status of its own) - only ever set once, in the same finally
    // block that flips isRunning back to false.
    val exitCode: Int? = null,
    // D1: one entry per terminal row, in order, empty for the bootstrap/Builtins branches
    // (neither produces real ANSI-styled output) and for a real shell command with no ANSI
    // color/attribute escapes in it at all - OutputLines falls back to plain [output] text
    // whenever this is empty, so an entry from before this existed, or with nothing to style,
    // renders exactly as it always has.
    val styledOutput: List<List<StyledSpan>> = emptyList()
)
