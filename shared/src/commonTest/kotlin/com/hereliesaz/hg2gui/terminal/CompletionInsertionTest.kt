package com.hereliesaz.hg2gui.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class CompletionInsertionTest {
    @Test
    fun replacesOnlyCurrentToken() {
        val result = applyCompletion(
            line = "git switch fea",
            candidate = CompletionCandidate("feature/login", kind = CompletionKind.BRANCH)
        )

        assertEquals("git switch feature/login ", result.line)
        assertEquals(result.line.length, result.cursor)
    }

    @Test
    fun directoryCompletionDoesNotAppendSpace() {
        val result = applyCompletion(
            line = "cd Do",
            candidate = CompletionCandidate(
                value = "Documents/",
                insertText = "Documents/",
                kind = CompletionKind.DIRECTORY,
                appendSpace = false
            )
        )

        assertEquals("cd Documents/", result.line)
    }

    @Test
    fun preservesTextAfterCursor() {
        val result = applyCompletion(
            line = "git sw old",
            cursor = 6,
            candidate = CompletionCandidate("switch", kind = CompletionKind.SUBCOMMAND)
        )

        assertEquals("git switch old", result.line)
        assertEquals(11, result.cursor)
    }
}
