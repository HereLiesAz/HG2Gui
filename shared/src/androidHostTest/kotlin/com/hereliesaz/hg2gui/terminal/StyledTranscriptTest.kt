package com.hereliesaz.hg2gui.terminal

import com.hereliesaz.hg2gui.managers.StyledSpan
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.termux.terminal.TerminalEmulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StyledTranscriptTest {

    private fun emulatorFor(text: String): TerminalEmulator {
        val emulator = TerminalEmulator(DummyTerminalOutput(), 120, 24, 10, 10, 1000, null)
        val bytes = text.toByteArray(Charsets.UTF_8)
        emulator.append(bytes, bytes.size)
        return emulator
    }

    private fun firstNonBlankLine(emulator: TerminalEmulator): List<StyledSpan> =
        emulator.styledTranscript().first { it.isNotEmpty() }

    @Test
    fun ansiHueOf_mapsBaseSixteenColorsToNamedHues() {
        assertEquals(Azphalt.hueNames.indexOf("red"), ansiHueOf(1))
        assertEquals(Azphalt.hueNames.indexOf("red"), ansiHueOf(9))
        assertEquals(Azphalt.hueNames.indexOf("green"), ansiHueOf(2))
        assertEquals(Azphalt.hueNames.indexOf("green"), ansiHueOf(10))
        assertEquals(Azphalt.hueNames.indexOf("amber"), ansiHueOf(3))
        assertEquals(Azphalt.hueNames.indexOf("blue"), ansiHueOf(4))
        assertEquals(Azphalt.hueNames.indexOf("magenta"), ansiHueOf(5))
        assertEquals(Azphalt.hueNames.indexOf("cyan"), ansiHueOf(6))
        assertEquals(Azphalt.hueNames.indexOf("gray"), ansiHueOf(0))
        assertEquals(Azphalt.hueNames.indexOf("gray"), ansiHueOf(8))
    }

    @Test
    fun ansiHueOf_leavesWhiteAndOutOfPaletteColorsUnmapped() {
        // 7/15 (white/bright white) is this terminal's own default-ish foreground in most
        // themes, not a semantic color - see StyledTranscript.kt's own doc comment.
        assertNull(ansiHueOf(7))
        assertNull(ansiHueOf(15))
        assertNull(ansiHueOf(200)) // inside the 256-color cube/greyscale range
        assertNull(ansiHueOf(256)) // TextStyle.COLOR_INDEX_FOREGROUND - "no color set at all"
    }

    @Test
    fun plainTextWithNoEscapes_producesOneUnstyledSpan() {
        val line = firstNonBlankLine(emulatorFor("hello world"))
        assertEquals(listOf(StyledSpan("hello world")), line)
    }

    @Test
    fun sgrRedForeground_isMappedToRedHue() {
        val line = firstNonBlankLine(emulatorFor("\u001B[31merror\u001B[0m: build failed"))
        assertEquals(
            listOf(
                StyledSpan("error", hue = Azphalt.hueNames.indexOf("red")),
                StyledSpan(": build failed")
            ),
            line
        )
    }

    @Test
    fun sgrGreenForeground_isMappedToGreenHue() {
        // git diff's own convention: additions in green, deletions in red.
        val line = firstNonBlankLine(emulatorFor("\u001B[32m+added line\u001B[0m"))
        assertEquals(listOf(StyledSpan("+added line", hue = Azphalt.hueNames.indexOf("green"))), line)
    }

    @Test
    fun sgrBold_isCarriedAsItsOwnFlag() {
        val line = firstNonBlankLine(emulatorFor("\u001B[1mBOLD\u001B[0m plain"))
        assertEquals(listOf(StyledSpan("BOLD", bold = true), StyledSpan(" plain")), line)
    }

    @Test
    fun sgrBoldAndColorTogether_carryBothOnTheSameSpan() {
        val line = firstNonBlankLine(emulatorFor("\u001B[1;31mFATAL\u001B[0m: out of memory"))
        assertEquals(
            listOf(
                StyledSpan("FATAL", hue = Azphalt.hueNames.indexOf("red"), bold = true),
                StyledSpan(": out of memory")
            ),
            line
        )
    }

    @Test
    fun carriageReturnRedraw_collapsesToFinalValue() {
        // A progress-bar-style redraw: draws "loading...", then \r (pure cursor movement, no
        // line break in a real terminal) rewrites the same row with "done!      " - the row
        // buffer only ever holds whichever text actually landed in each column last, so reading
        // its current state should give the final frame, not a stack of every intermediate one.
        val line = firstNonBlankLine(emulatorFor("loading...\rdone!      "))
        assertEquals("done!", line.joinToString("") { it.text })
    }
}
