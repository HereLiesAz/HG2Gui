package com.hereliesaz.hg2gui.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class ShellAliasesCompletionTest {

    private val known = listOf("ls", "less", "ln", "locate", "ping", "pkg", "ps")

    @Test
    fun matchesByPrefix_sortedAlphabetically() {
        assertEquals(listOf("less", "ln", "locate", "ls"), ShellAliases.commandNameCompletions("l", known))
    }

    @Test
    fun excludesAnExactMatch_thereIsNothingLeftToCompleteTo() {
        assertEquals(emptyList(), ShellAliases.commandNameCompletions("ls", listOf("ls")))
    }

    @Test
    fun blankPrefix_returnsNothing() {
        assertEquals(emptyList(), ShellAliases.commandNameCompletions("", known))
        assertEquals(emptyList(), ShellAliases.commandNameCompletions("   ", known))
    }

    @Test
    fun noMatches_returnsEmpty() {
        assertEquals(emptyList(), ShellAliases.commandNameCompletions("zz", known))
    }

    @Test
    fun respectsTheLimit() {
        assertEquals(listOf("less", "ln"), ShellAliases.commandNameCompletions("l", known, limit = 2))
    }
}
