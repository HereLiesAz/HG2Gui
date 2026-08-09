package com.hereliesaz.hg2gui.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypesetOutputTest {

    @Test
    fun keyValueLines_isTable() {
        val text = """
            Address: 10.0.0.14/24
            Gateway: 10.0.0.1
            Signal: -47 dBm
        """.trimIndent()
        assertTrue(looksLikeKeyValueTable(text))
    }

    @Test
    fun prose_isNotTable() {
        val text = """
            This is a completely ordinary paragraph of output.
            It has several lines of real words in it.
            Nothing here is a label: value pair on every line.
        """.trimIndent()
        assertFalse(looksLikeKeyValueTable(text))
    }

    @Test
    fun tooFewLines_isNotTable() {
        assertFalse(looksLikeKeyValueTable("Address: 10.0.0.14"))
    }

    @Test
    fun mixedLines_isNotTable() {
        val text = """
            Address: 10.0.0.14/24
            just some plain text here
            Signal: -47 dBm
        """.trimIndent()
        assertFalse(looksLikeKeyValueTable(text))
    }
}
