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
}
