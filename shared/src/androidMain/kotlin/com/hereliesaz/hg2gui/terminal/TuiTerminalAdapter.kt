package com.hereliesaz.hg2gui.terminal

import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalRow
import com.termux.terminal.TextStyle

/** Converts the live emulator into neutral semantic rows for the adaptive TUI parser. */
object TuiTerminalAdapter {

    fun snapshot(holder: FullScreenPtySession): TuiSnapshot? {
        val emulator = holder.session.emulator ?: return null
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
        return TuiSemanticParser.parse(rows, emulator.isAlternateBufferActive())
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

    private fun rowText(line: TerminalRow?, columns: Int): String {
        if (line == null) return ""
        val builder = StringBuilder(columns)
        for (column in 0 until columns) {
            val index = line.findStartOfColumn(column)
            builder.append(if (index < line.getSpaceUsed()) line.mText[index] else ' ')
        }
        return builder.toString().trimEnd()
    }
}
