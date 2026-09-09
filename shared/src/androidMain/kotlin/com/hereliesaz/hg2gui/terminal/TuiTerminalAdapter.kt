package com.hereliesaz.hg2gui.terminal

import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalRow
import com.termux.terminal.TextStyle

/**
 * Converts the live Termux terminal emulator into the neutral semantic input consumed by
 * [TuiSemanticParser]. This layer deliberately reads terminal state only; it does not know the
 * identity or toolkit of the program that produced the screen.
 */
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
                add(
                    TuiRow(
                        index = row,
                        text = rowText(line, screen.mColumns),
                        highlighted = rowHasSelectionStyle(line, screen.mColumns),
                        cursorColumn = cursorCol.takeIf { row == cursorRow }
                    )
                )
            }
        }
        return TuiSemanticParser.parse(rows, emulator.isAlternateBufferActive())
    }

    private fun rowHasSelectionStyle(line: TerminalRow?, columns: Int): Boolean {
        if (line == null) return false
        for (column in 0 until columns) {
            val effect = TextStyle.decodeEffect(line.getStyle(column))
            if (effect and TextStyle.CHARACTER_ATTRIBUTE_INVERSE != 0) return true
        }
        return false
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
