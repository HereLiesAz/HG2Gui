package com.hereliesaz.hg2gui.mcp

import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class McpServerServiceTest {

    @Test
    fun constantTimeEquals_equalStrings() {
        assertEquals(true, constantTimeEquals("same-token", "same-token"))
    }

    @Test
    fun constantTimeEquals_differentStrings() {
        assertEquals(false, constantTimeEquals("token-a", "token-b"))
    }

    @Test
    fun constantTimeEquals_differentLengths() {
        assertEquals(false, constantTimeEquals("short", "a-much-longer-token"))
    }

    @Test
    fun readLineBounded_readsANormalLine() {
        val reader = BufferedReader(StringReader("hello world\n"))
        assertEquals("hello world", reader.readLineBounded(1024))
    }

    @Test
    fun readLineBounded_handlesCrlf() {
        val reader = BufferedReader(StringReader("hello\r\nworld\r\n"))
        assertEquals("hello", reader.readLineBounded(1024))
        assertEquals("world", reader.readLineBounded(1024))
    }

    @Test
    fun readLineBounded_returnsTrailingContentWithNoFinalNewlineAtEof() {
        val reader = BufferedReader(StringReader("no newline at all"))
        assertEquals("no newline at all", reader.readLineBounded(1024))
    }

    @Test
    fun readLineBounded_returnsNullAtCleanEof() {
        val reader = BufferedReader(StringReader(""))
        assertNull(reader.readLineBounded(1024))
    }

    @Test
    fun readLineBounded_throwsInsteadOfBufferingAnOverlongLine() {
        // MCP-3: an attacker-controlled line with no newline must not be allowed to grow this
        // buffer without limit on the pre-auth path - it has to fail loudly instead.
        val reader = BufferedReader(StringReader("x".repeat(2000)))
        assertThrows(IOException::class.java) { reader.readLineBounded(1024) }
    }
}
