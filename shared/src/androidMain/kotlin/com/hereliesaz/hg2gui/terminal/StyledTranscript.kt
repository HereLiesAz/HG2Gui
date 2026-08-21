package com.hereliesaz.hg2gui.terminal

import com.hereliesaz.hg2gui.managers.StyledSpan
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.termux.terminal.TerminalBuffer
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TextStyle

/*
 * D1 (HG2Gui Termux Coverage.dc.html): TerminalEmulator already parses every ANSI/SGR escape a
 * real shell command emits and tracks each screen cell's own foreground color/attributes in
 * TerminalRow.mStyle - ShellSession's plain transcriptText() throws all of that away and keeps
 * only the rendered characters. This walks the same row range TerminalBuffer's own
 * transcriptTextWithFullLinesJoined does, grouping consecutive same-styled columns into runs
 * instead of collapsing straight to plain text. Carriage-return-driven progress-bar redraws need
 * no special handling here: a `\r` is pure cursor movement to TerminalEmulator (it moves the
 * write position, not a line break), so the emulator's own row buffer already only ever holds
 * each row's final overwritten state by the time this reads it - "collapse to final value" is
 * inherent to reading the buffer's current state rather than replaying every byte that arrived.
 */

// Reuses Azphalt's own named hue list (ui/menu/PillMenu.kt) so this mapping can't silently drift
// out of sync with its ordering if that ever changes - built once, not per lookup. Only the base
// 16-color ANSI palette (SGR 30-37/40-47, 90-97/100-107 - what git/grep/ls/npm/pytest/cargo all
// actually emit under a default TERM) maps to a named hue; a 256-color-cube/greyscale index or a
// 24-bit truecolor cell (rare outside prompt themes and image previews) reports null - "no mapped
// color", not "black" - and renders in the surrounding text's own normal ink instead of guessing.
private val ANSI_HUE_BY_COLOR_INDEX: Map<Int, Int> = buildMap {
    fun mapHue(hueName: String, vararg colorIndices: Int) {
        val hue = Azphalt.hueNames.indexOf(hueName)
        colorIndices.forEach { put(it, hue) }
    }
    mapHue("gray", 0, 8)
    mapHue("red", 1, 9)
    mapHue("green", 2, 10)
    mapHue("amber", 3, 11)
    mapHue("blue", 4, 12)
    mapHue("magenta", 5, 13)
    mapHue("cyan", 6, 14)
    // 7/15 (white/bright white) intentionally absent - that's this terminal's own default-ish
    // foreground in most themes, not a semantic color worth breaking out of the normal ink.
}

internal fun ansiHueOf(colorIndex: Int): Int? = ANSI_HUE_BY_COLOR_INDEX[colorIndex]

/** One entry per terminal row, oldest first - active scrollback then the visible screen, the
 *  same range [TerminalBuffer.transcriptTextWithFullLinesJoined] draws from. Unlike that method,
 *  wrapped continuation rows are NOT rejoined into one logical line here; a very long styled line
 *  can render with a different break than the plain-text [output] field's own reading of the same
 *  transcript. Accepted for this scope - the styled view is a supplementary rendering, not a
 *  replacement for [output], which keeps using the joined reading for its own heuristics/copy. */
internal fun TerminalEmulator.styledTranscript(): List<List<StyledSpan>> {
    val screen = mScreen
    val y1 = -screen.getActiveTranscriptRows()
    val y2 = screen.mScreenRows - 1
    return (y1..y2).map { row -> styledLine(screen, row) }
}

private fun styledLine(screen: TerminalBuffer, row: Int): List<StyledSpan> {
    val lineObject = screen.mLines[screen.externalToInternalRow(row)] ?: return emptyList()
    val text = lineObject.mText
    val used = lineObject.getSpaceUsed()
    val columns = screen.mColumns

    // findStartOfColumn walks from the row's own start every time it's called, so this computes
    // each column's char-index boundary exactly once up front rather than repeating that walk on
    // every access below.
    val charIndexAt = IntArray(columns + 1) { col -> lineObject.findStartOfColumn(col) }

    var lastPrintingCol = -1
    for (col in 0 until columns) {
        val idx = charIndexAt[col]
        if (idx < used && text[idx] != ' ') lastPrintingCol = col
    }
    if (lastPrintingCol < 0) return emptyList()

    val spans = mutableListOf<StyledSpan>()
    var runStartCol = 0
    var runStyle = lineObject.getStyle(0)
    for (col in 1..lastPrintingCol) {
        val style = lineObject.getStyle(col)
        if (style != runStyle) {
            spans += styledSpan(text, charIndexAt[runStartCol], charIndexAt[col], runStyle)
            runStartCol = col
            runStyle = style
        }
    }
    spans += styledSpan(text, charIndexAt[runStartCol], charIndexAt[lastPrintingCol + 1], runStyle)
    return spans
}

private fun styledSpan(text: CharArray, fromIdx: Int, toIdx: Int, style: Long): StyledSpan {
    val length = (toIdx - fromIdx).coerceAtLeast(0)
    val effect = TextStyle.decodeEffect(style)
    val bold = (effect and TextStyle.CHARACTER_ATTRIBUTE_BOLD) != 0
    return StyledSpan(String(text, fromIdx, length), ansiHueOf(TextStyle.decodeForeColor(style)), bold)
}
