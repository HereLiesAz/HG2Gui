package com.hereliesaz.hg2gui.ui

import org.junit.Assert.assertEquals
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

    @Test
    fun multiFieldLine_splitsIntoOneRowPerField() {
        // Regression: a `stat`-style line packing several `label: value` fields together with 2+
        // space gaps used to collapse into a single row whose value swallowed everything after
        // the first colon.
        val rows = parseKeyValueTable("Size: 2843  Blocks: 8  IO Block: 4096  regular file")
        assertEquals(
            listOf(
                KeyValueRow("Size", "2843"),
                KeyValueRow("Blocks", "8"),
                KeyValueRow("IO Block", "4096"),
            ),
            rows
        )
    }

    @Test
    fun singleFieldLine_staysOneRow() {
        val rows = parseKeyValueTable("Description: A shell for interacting with the system")
        assertEquals(listOf(KeyValueRow("Description", "A shell for interacting with the system")), rows)
    }
}
