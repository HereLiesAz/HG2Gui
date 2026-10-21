package com.hereliesaz.hg2gui.terminal

import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalRow
import com.termux.terminal.TextStyle

/** Converts the live emulator into neutral semantic rows for the adaptive TUI parser. */
object TuiTerminalAdapter {

    fun snapshot(holder: FullScreenPtySession): TuiSnapshot? {
        val emulator = holder.session.emulator ?: return null
        val cooperative = CooperativeTuiSnapshot.forCommand(holder.commandLine)
        if (cooperative != null && emulator.isAlternateBufferActive()) return cooperative
        return snapshot(emulator)
    }

    fun snapshot(emulator: TerminalEmulator): TuiSnapshot {
        val screen = emulator.screen
        val cursorRow = emulator.getCursorRow()
        val cursorCol = emulator.getCursorCol()
        val rows = buildList(screen.mScreenRows) {
            for (row in 0 until screen.mScreenRows) {
                val line = screen.mLines.getOrNull(screen.externalToInternalRow(row))
                val style = rowStyle(line, screen.mColumns)
                add(
                    TuiRow(
                        index = row,
                        text = rowText(line, screen.mColumns),
                        highlighted = style.inverse,
                        emphasized = style.bold || style.underline,
                        styled = style.any,
                        cursorColumn = cursorCol.takeIf { row == cursorRow }
                    )
                )
            }
        }
        return TuiSemanticParser.parse(
            rows = rows,
            alternateScreen = emulator.isAlternateBufferActive(),
            mouseAware = emulator.isMouseTrackingActive()
        )
    }

    private data class RowStyle(
        val inverse: Boolean,
        val bold: Boolean,
        val underline: Boolean,
        val any: Boolean
    )

    private fun rowStyle(line: TerminalRow?, columns: Int): RowStyle {
        if (line == null) return RowStyle(false, false, false, false)
        var inverse = false
        var bold = false
        var underline = false
        var any = false
        for (column in 0 until columns) {
            val effect = TextStyle.decodeEffect(line.getStyle(column))
            if (effect != 0) any = true
            if (effect and TextStyle.CHARACTER_ATTRIBUTE_INVERSE != 0) inverse = true
            if (effect and TextStyle.CHARACTER_ATTRIBUTE_BOLD != 0) bold = true
            if (effect and TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE != 0) underline = true
        }
        return RowStyle(inverse, bold, underline, any)
    }

    private fun rowText(line: TerminalRow?, columns: Int): String =
        if (line == null) "" else terminalRowText(line, 0, columns).trimEnd()
}

/**
 * The text of columns [from] until [to] of [line], one entry per *cell run* rather than per
 * `mText` char: a wide (CJK/emoji) character occupies two columns but one code point, so its
 * second cell is skipped instead of emitting a stray space or a lone surrogate half, and
 * supplementary-plane code points plus trailing zero-width combiners are appended whole. Same
 * column-to-char-index boundaries StyledTranscript uses. A null line is all blanks.
 */
internal fun terminalRowText(line: TerminalRow?, from: Int, to: Int): String {
    if (to <= from) return ""
    if (line == null) return " ".repeat(to - from)
    val used = line.getSpaceUsed()
    // Boundaries for [from - 1, to]: findStartOfColumn rescans from the row start on each call,
    // so compute each once. Index 0 of the array is column (from - 1), when there is one.
    val firstCol = (from - 1).coerceAtLeast(0)
    val starts = IntArray(to - firstCol + 1) { line.findStartOfColumn(firstCol + it) }
    fun startAt(col: Int) = starts[col - firstCol]
    val builder = StringBuilder(to - from)
    var col = from
    while (col < to) {
        val start = startAt(col)
        if (start >= used) {
            builder.append(' ')
            col++
            continue
        }
        // A column whose start equals its left neighbour's is the second cell of a wide char.
        if (col > 0 && startAt(col - 1) == start) {
            col++
            continue
        }
        var next = col + 1
        while (next < to && startAt(next) == start) next++
        val end = startAt(next).coerceAtMost(used)
        builder.append(line.mText, start, (end - start).coerceAtLeast(1))
        col = next
    }
    return builder.toString()
}
