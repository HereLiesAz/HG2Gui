package com.hereliesaz.hg2gui.terminal

import com.termux.terminal.TerminalEmulator
import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalRowTextTest {

    private fun rowOf(text: String, columns: Int = 10) =
        TerminalEmulator(DummyTerminalOutput(), columns, 3, 10, 10, 100, null).let { emulator ->
            val bytes = text.toByteArray(Charsets.UTF_8)
            emulator.append(bytes, bytes.size)
            emulator.screen.mLines[emulator.screen.externalToInternalRow(0)]
        }

    @Test
    fun plainText_roundTrips() {
        assertEquals("abc", terminalRowText(rowOf("abc"), 0, 3))
    }

    @Test
    fun wideCharacter_isEmittedOnceWithoutAFillerSpace() {
        // U+4E2D occupies columns 0-1; "x" is column 2.
        assertEquals("中x", terminalRowText(rowOf("中x"), 0, 3))
    }

    @Test
    fun runStartingOnTheSecondCellOfAWideCharacter_skipsIt() {
        assertEquals("x", terminalRowText(rowOf("中x"), 1, 3).trim())
    }

    @Test
    fun supplementaryPlaneCodePoint_isKeptWhole() {
        // U+1F600 is wide and a surrogate pair in UTF-16.
        assertEquals("😀!", terminalRowText(rowOf("😀!"), 0, 3))
    }

    @Test
    fun combiningMark_staysWithItsBase() {
        assertEquals("éx", terminalRowText(rowOf("éx"), 0, 2))
    }

    @Test
    fun nullRow_isBlank() {
        assertEquals("   ", terminalRowText(null, 0, 3))
    }
}
