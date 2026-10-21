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
}
