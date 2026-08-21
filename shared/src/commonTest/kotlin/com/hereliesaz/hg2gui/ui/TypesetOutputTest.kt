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

    // D6: `ps aux`-shaped fixture - a header naming five columns, data rows whose values are
    // packed at varying widths (a right-aligned PID column narrower than its own header label)
    // rather than lining up under a fixed grid, since real command output rarely does either.
    private val PS_AUX_LIKE = """
        USER       PID %CPU %MEM COMMAND
        root         1  0.0  0.1 init
        app       1234  2.5  1.2 com.example.app
    """.trimIndent()

    @Test
    fun wideHeaderAndRows_isWideTable() {
        assertTrue(looksLikeWideTable(PS_AUX_LIKE))
    }

    @Test
    fun wideTableRows_reflowIntoLabelledFields() {
        val rows = parseWideTable(PS_AUX_LIKE)
        assertEquals(2, rows.size)
        val first = rows[0].fields.toMap()
        assertEquals("root", first["USER"])
        assertEquals("1", first["PID"])
        assertEquals("0.0", first["%CPU"])
        assertEquals("0.1", first["%MEM"])
        assertEquals("init", first["COMMAND"])
        val second = rows[1].fields.toMap()
        assertEquals("app", second["USER"])
        assertEquals("1234", second["PID"])
        assertEquals("2.5", second["%CPU"])
        assertEquals("com.example.app", second["COMMAND"])
    }

    @Test
    fun twoColumnKeyValueOutput_isNotWideTable() {
        // isTable's own territory (label: value) - not what D6 targets ("beyond two columns").
        val text = "Address: 10.0.0.14\nGateway: 10.0.0.1\nSignal: -47 dBm"
        assertFalse(looksLikeWideTable(text))
    }

    @Test
    fun prose_isNotWideTable() {
        val text = """
            This is a completely ordinary paragraph of output.
            It has several lines of real words in it.
            Nothing here lines up into columns at all.
        """.trimIndent()
        assertFalse(looksLikeWideTable(text))
    }

    @Test
    fun raggedLastColumn_stillCountsAsWideEnough() {
        // COMMAND legitimately contains spaces (a full command line, not just a binary name) -
        // that shouldn't disqualify the line from having "enough" whitespace-separated fields,
        // since the field-count check only requires *at least* as many tokens as the header.
        val text = """
            USER       PID %CPU %MEM COMMAND
            root         1  0.0  0.1 /usr/bin/init --daemon --quiet
        """.trimIndent()
        assertTrue(looksLikeWideTable(text))
        assertEquals("/usr/bin/init --daemon --quiet", parseWideTable(text)[0].fields.last().second)
    }

    @Test
    fun plainText_isNotBinary() {
        assertFalse(looksLikeBinary("hello world\nsecond line of ordinary output\n"))
    }

    @Test
    fun emptyOutput_isNotBinary() {
        assertFalse(looksLikeBinary(""))
    }

    @Test
    fun replacementCharacters_areBinary() {
        // Stand-ins for what a UTF-8 decode leaves behind when the original bytes weren't valid
        // UTF-8 to begin with - the shape a real binary file decodes into by the time it's a
        // Kotlin String at all.
        val text = "����ELF����garbage����"
        assertTrue(looksLikeBinary(text))
    }

    @Test
    fun controlBytes_areBinary() {
        val text = "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007ELF header bytes here"
        assertTrue(looksLikeBinary(text))
    }

    @Test
    fun ordinaryWhitespaceControlChars_areNotBinary() {
        // Tabs, newlines, and carriage returns are control characters too, but they're entirely
        // ordinary in real terminal output and must not trip the binary heuristic on their own.
        assertFalse(looksLikeBinary("a\tb\nc\rd\n\t\t\t\t\t\t\t\t\t\t"))
    }
}
