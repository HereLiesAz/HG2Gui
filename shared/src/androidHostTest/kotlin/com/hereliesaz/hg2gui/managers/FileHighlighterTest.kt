package com.hereliesaz.hg2gui.managers

import com.hereliesaz.hg2gui.ui.menu.Azphalt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ESC = ""

class FileHighlighterTest {

    @Test
    fun looksBinary_trueForALiteralNullByte() {
        assertTrue(looksBinary(byteArrayOf(1, 2, 0, 3)))
    }

    @Test
    fun looksBinary_falseForPlainUtf8Text() {
        assertFalse(looksBinary("hello, world - éè".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun parseAnsiLine_plainTextHasNoColor() {
        val spans = parseAnsiLine("no escapes here")
        assertEquals(1, spans.size)
        assertEquals("no escapes here", spans[0].text)
        assertEquals(null, spans[0].hue)
        assertEquals(false, spans[0].bold)
    }

    @Test
    fun parseAnsiLine_mapsAForegroundColorCodeToItsNamedHue() {
        val spans = parseAnsiLine("$ESC[31mred text$ESC[0m plain")
        assertEquals(2, spans.size)
        assertEquals("red text", spans[0].text)
        assertEquals(Azphalt.hueNames.indexOf("red"), spans[0].hue)
        assertEquals(" plain", spans[1].text)
        assertEquals(null, spans[1].hue)
    }

    @Test
    fun parseAnsiLine_boldCombinesWithAColorInOneRun() {
        val spans = parseAnsiLine("$ESC[1;32mbold green$ESC[0m")
        assertEquals(1, spans.size)
        assertEquals("bold green", spans[0].text)
        assertEquals(Azphalt.hueNames.indexOf("green"), spans[0].hue)
        assertEquals(true, spans[0].bold)
    }

    @Test
    fun parseAnsi_splitsIntoOneEntryPerLine() {
        val lines = parseAnsi("$ESC[31mone$ESC[0m\ntwo\n")
        assertEquals(3, lines.size)
        assertEquals("one", lines[0][0].text)
        assertEquals("two", lines[1][0].text)
        assertTrue(lines[2].isEmpty())
    }
}
