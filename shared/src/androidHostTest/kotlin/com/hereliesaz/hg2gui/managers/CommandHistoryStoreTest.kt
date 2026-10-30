package com.hereliesaz.hg2gui.managers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CommandHistoryStoreTest {

    private fun roundTrip(text: String) =
        assertEquals(text, CommandHistoryStore.unescape(CommandHistoryStore.escape(text)))

    @Test
    fun literalBackslashNIsNotTurnedIntoANewline() {
        roundTrip("printf 'a\\nb'")
        assertEquals("printf 'a\\\\nb'", CommandHistoryStore.escape("printf 'a\\nb'"))
    }

    @Test
    fun realControlCharactersRoundTripWithoutBreakingTheLineFormat() {
        val text = "echo a\tb\nc\rd"
        val escaped = CommandHistoryStore.escape(text)
        assertFalse(escaped.any { it == '\t' || it == '\n' || it == '\r' })
        roundTrip(text)
    }

    @Test
    fun backslashRunsRoundTrip() {
        roundTrip("\\")
        roundTrip("\\\\\\t\\n")
        roundTrip("ends with \\")
    }

    private fun line(ts: Long, session: String, command: String) =
        "$ts\t${CommandHistoryStore.escape(session)}\t${CommandHistoryStore.escape(command)}"

    @Test
    fun withoutEntryDropsOnlyTheMatchingLine() {
        val lines = listOf(line(1, "main", "ls"), line(2, "main", "pwd"), line(2, "session 2", "pwd"))
        val kept = CommandHistoryStore.withoutEntry(lines, CommandHistoryEntry(2, "main", "pwd"))
        assertEquals(listOf(lines[0], lines[2]), kept)
    }

    @Test
    fun withoutEntryMatchesEscapedCommandsAndKeepsGarbageLines() {
        val tricky = line(5, "main", "echo a\tb\\n")
        val lines = listOf("not a valid line", tricky)
        val kept = CommandHistoryStore.withoutEntry(lines, CommandHistoryEntry(5, "main", "echo a\tb\\n"))
        assertEquals(listOf("not a valid line"), kept)
    }
}
