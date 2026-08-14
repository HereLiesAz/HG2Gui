package com.hereliesaz.hg2gui.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AsciiArtTest {

    @Test
    fun prose_isNotArt() {
        val text = """
            This is a completely ordinary paragraph of output.
            It has several lines of real words in it.
            Nothing here should look like a picture.
        """.trimIndent()
        assertFalse(looksLikeAsciiArt(text))
    }

    @Test
    fun shortBlock_isNotArt() {
        assertFalse(looksLikeAsciiArt("###\n###"))
    }

    @Test
    fun symbolDenseBlock_isArt() {
        val text = """
            /\_/\
            ( o.o )
            > ^ <
            #===========#
            |///////////|
            #===========#
        """.trimIndent()
        assertTrue(looksLikeAsciiArt(text))
    }

    @Test
    fun commonLineArtCharacters_clearIsoLevel() {
        // Regression for a miscalibrated ISO_LEVEL that put nearly all real line-art punctuation
        // below the fill threshold, so detected art rendered as an almost-blank canvas - these are
        // exactly the characters a hand-drawn ASCII picture is built from.
        for (c in "|\\/_-()<>[]{}") {
            assertTrue("'$c' should count as ink at ISO_LEVEL", charDensity(c) >= ISO_LEVEL)
        }
    }

    @Test
    fun trulyBlankPunctuation_staysBelowIsoLevel() {
        for (c in " .'`") {
            assertTrue("'$c' should stay below ISO_LEVEL", charDensity(c) < ISO_LEVEL)
        }
    }
}
