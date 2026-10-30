package com.hereliesaz.hg2gui.ui.terminal

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure parts of the RAW terminal's sticky CTRL/ALT keys and the TUI wrapper's failure copy. */
class TerminalInputTest {

    @Test
    fun ctrlMapsLettersToControlBytesInEitherCase() {
        assertEquals(3, controlCodePoint('c'.code))
        assertEquals(3, controlCodePoint('C'.code))
        assertEquals(1, controlCodePoint('a'.code))
        assertEquals(26, controlCodePoint('z'.code))
    }

    @Test
    fun ctrlMapsTheShellPunctuationKeys() {
        assertEquals(0x1c, controlCodePoint('\\'.code)) // SIGQUIT
        assertEquals(0x1b, controlCodePoint('['.code))
        assertEquals(0, controlCodePoint(' '.code))
        assertEquals(0x7f, controlCodePoint('?'.code))
        assertNull(controlCodePoint('7'.code))
    }

    @Test
    fun plainKeyIsSentUnchanged() {
        assertArrayEquals(intArrayOf('x'.code), terminalCodePoints('x'.code, ctrl = false, alt = false))
    }

    @Test
    fun altPrefixesEsc() {
        assertArrayEquals(intArrayOf(0x1b, 'b'.code), terminalCodePoints('b'.code, ctrl = false, alt = true))
    }

    @Test
    fun ctrlAndAltCombine() {
        assertArrayEquals(intArrayOf(0x1b, 3), terminalCodePoints('c'.code, ctrl = true, alt = true))
    }

    @Test
    fun ctrlOnAKeyWithNoControlByteFallsThrough() {
        assertArrayEquals(intArrayOf('7'.code), terminalCodePoints('7'.code, ctrl = true, alt = false))
    }

    @Test
    fun emptyScreenIsDescribedAsEmpty() {
        assertEquals("Terminal. The screen is empty.", terminalScreenDescription(emptyList()))
    }

    @Test
    fun screenDescriptionIsTheVisibleLines() {
        assertEquals("Terminal. one\ntwo", terminalScreenDescription(listOf("one", "two")))
    }

    @Test
    fun screenDescriptionKeepsTheLastLinesOfALongScreen() {
        val lines = (1..100).map { "line $it" }
        val description = terminalScreenDescription(lines)
        assertTrue(description.endsWith("line 100"))
        assertTrue(!description.contains("line 1\n"))
    }

    @Test
    fun interactionFailureOffersRawInsteadOfSwitching() {
        val withReason = interactionFailureMessage("target disappeared")
        assertTrue(withReason.contains("target disappeared"))
        assertTrue(withReason.contains("raw terminal"))
        assertTrue(interactionFailureMessage(null).contains("raw terminal"))
        assertEquals(interactionFailureMessage(null), interactionFailureMessage("  "))
    }
}
