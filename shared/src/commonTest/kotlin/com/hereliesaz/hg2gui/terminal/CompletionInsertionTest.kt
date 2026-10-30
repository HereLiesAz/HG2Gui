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

    @Test
    fun insertsANameWithASpaceAsOneEscapedWord() {
        val result = applyCompletion("cat ", candidate = CompletionCandidate("My File.txt"))
        assertEquals("cat My\\ File.txt ", result.line)
    }

    @Test
    fun insertsCommandSeparatorsAndSubstitutionsInert() {
        val semicolon = applyCompletion("cat ", candidate = CompletionCandidate("a;rm -rf x"))
        assertEquals("cat a\\;rm\\ -rf\\ x ", semicolon.line)

        val substitution = applyCompletion("cat ", candidate = CompletionCandidate("\$(id)"))
        assertEquals("cat \\\$\\(id\\) ", substitution.line)

        val backtick = applyCompletion("cat ", candidate = CompletionCandidate("`id`"))
        assertEquals("cat \\`id\\` ", backtick.line)
    }

    @Test
    fun aMergedCandidateIsNotQuotedASecondTime() {
        val merged = CompletionNormalizer.merge(listOf(CompletionCandidate("My File.txt"))).single()
        assertEquals("My\\ File.txt", merged.insertText)
        val result = applyCompletion("cat ", candidate = merged)
        assertEquals("cat My\\ File.txt ", result.line)
    }

    @Test
    fun aProviderSuppliedInsertTextIsInsertedVerbatim() {
        val result = applyCompletion(
            "echo ",
            candidate = CompletionCandidate(value = "HOME", insertText = "\"\$HOME\"")
        )
        assertEquals("echo \"\$HOME\" ", result.line)
    }

    @Test
    fun replacesTheWholeEscapedWordNotJustTheTailAfterTheEscapedSpace() {
        val candidate = CompletionCandidate("My File.txt")
        val result = applyCompletion("cat My\\ Fi", candidate = candidate)
        assertEquals("cat My\\ File.txt ", result.line)
        assertEquals(result.line.length, result.cursor)
    }

    @Test
    fun replacesTheWholeQuotedWord() {
        val result = applyCompletion("cat \"My Fi", candidate = CompletionCandidate("My File.txt"))
        assertEquals("cat My\\ File.txt ", result.line)
    }

    @Test
    fun tabSeparatedWordIsReplacedFromTheTabNotFromTheLineStart() {
        // The matcher (CompletionRequest.tokenPrefix) and the inserter must agree on the word.
        val line = "ls\tfo"
        assertEquals("fo", CompletionRequest(line).tokenPrefix)
        val result = applyCompletion(line, candidate = CompletionCandidate("foo"))
        assertEquals("ls\tfoo ", result.line)
    }

    @Test
    fun tokenPrefixAndInsertionStartAgreeForEveryShape() {
        val lines = listOf(
            "git sw", "git  sw", "ls\tfo", "cat My\\ Fi", "cat \"My Fi", "cat 'My Fi", "cat \$'My\\' Fi",
            "x  ", "x 'a b' c", "x a\\\\ b"
        )
        for (line in lines) {
            val request = CompletionRequest(line)
            val result = applyCompletion(line, candidate = CompletionCandidate("Z", appendSpace = false))
            assertEquals(line.substring(0, line.length - request.tokenPrefix.length) + "Z", result.line, line)
        }
    }
}
