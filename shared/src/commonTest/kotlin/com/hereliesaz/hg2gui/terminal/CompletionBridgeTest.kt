package com.hereliesaz.hg2gui.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompletionBridgeTest {

    @Test
    fun fishTabOutput_preservesDescriptionsAndKinds() {
        val parsed = CompletionNormalizer.parseFish(
            "main\tgit branch\n" +
                "--help\tshow help\n" +
                "src/\tdirectory\n"
        )

        assertEquals(3, parsed.size)
        assertEquals(CompletionKind.BRANCH, parsed[0].kind)
        assertEquals(CompletionKind.OPTION, parsed[1].kind)
        assertEquals(CompletionKind.DIRECTORY, parsed[2].kind)
        assertFalse(parsed[2].appendSpace)
    }

    @Test
    fun merge_deduplicatesByInsertedTextAndKeepsRicherCandidate() {
        val merged = CompletionNormalizer.merge(
            listOf(
                CompletionCandidate("main", source = CompletionSource.STATIC),
                CompletionCandidate(
                    "main",
                    description = "current branch",
                    kind = CompletionKind.BRANCH,
                    source = CompletionSource.ZSH
                )
            )
        )

        assertEquals(1, merged.size)
        assertEquals("current branch", merged.single().description)
        assertEquals(CompletionSource.ZSH, merged.single().source)
    }

    @Test
    fun requestFiltersAgainstCurrentTokenOnly() {
        val request = CompletionRequest("git switch fe")
        val candidates = listOf(
            CompletionCandidate("feature/menu"),
            CompletionCandidate("fix/build"),
            CompletionCandidate("main")
        )

        val filtered = CompletionNormalizer.filterFor(request, candidates)
        assertEquals(listOf("feature/menu"), filtered.map { it.value })
    }

    @Test
    fun completionRequestClampsCursor() {
        val request = CompletionRequest(line = "git sw", cursor = 999)
        assertEquals(6, request.safeCursor)
        assertEquals("sw", request.tokenPrefix)
        assertTrue(request.beforeCursor.endsWith("sw"))
    }

    @Test
    fun tokenPrefixAndTokenValueFollowQuotingNotJustSpaces() {
        val escaped = CompletionRequest("cat My\\ Fi")
        assertEquals("My\\ Fi", escaped.tokenPrefix)
        assertEquals("My Fi", escaped.tokenValue)

        val quoted = CompletionRequest("cat \"My Fi")
        assertEquals("\"My Fi", quoted.tokenPrefix)
        assertEquals("My Fi", quoted.tokenValue)

        assertEquals("fo", CompletionRequest("ls\tfo").tokenPrefix)
    }

    @Test
    fun isCommandPositionIgnoresLeadingWhitespaceAndEscapedSpaces() {
        assertTrue(CompletionRequest("  gi").isCommandPosition)
        assertTrue(CompletionRequest("My\\ Scr").isCommandPosition)
        assertFalse(CompletionRequest("git sw").isCommandPosition)
        assertFalse(CompletionRequest("git\tsw").isCommandPosition)
    }

    @Test
    fun mergeQuotesEveryDefaultInsertTextExactlyOnce() {
        val hostile = listOf("a b", "x;y", "\$(id)", "`id`", "q'uote", "plain")
        val once = CompletionNormalizer.merge(hostile.map { CompletionCandidate(it) })
        val twice = CompletionNormalizer.merge(once)
        assertEquals(once.map { it.insertText }.toSet(), twice.map { it.insertText }.toSet())
        val byValue = once.associateBy { it.value }
        for (name in hostile) assertEquals(CompletionWords.quote(name), byValue.getValue(name).insertText, name)
        // The label shown to the user stays the raw name.
        assertEquals("a b", byValue.getValue("a b").label)
    }

    @Test
    fun mergeLeavesAProviderSuppliedInsertTextAlone() {
        val merged = CompletionNormalizer.merge(listOf(CompletionCandidate(value = "home", insertText = "\$HOME/")))
        assertEquals("\$HOME/", merged.single().insertText)
    }

    @Test
    fun parsedShellOutputIsQuotedByMerge() {
        val parsed = CompletionNormalizer.parseBash("My File.txt\nplain\n\$(id)\n")
        val merged = CompletionNormalizer.merge(parsed)
        assertEquals(
            setOf("My\\ File.txt", "plain", "\\\$\\(id\\)"),
            merged.map { it.insertText }.toSet()
        )
    }

    @Test
    fun filterMatchesAnEscapedTypedWordAgainstRawValues() {
        val request = CompletionRequest("cat My\\ Fi")
        val merged = CompletionNormalizer.merge(
            listOf(CompletionCandidate("My File.txt"), CompletionCandidate("My Other"), CompletionCandidate("Mine"))
        )
        assertEquals(listOf("My File.txt"), CompletionNormalizer.filterFor(request, merged).map { it.value })
    }

    @Test
    fun filterMatchesAQuotedTypedWordAgainstRawValues() {
        val request = CompletionRequest("cat \"My Fi")
        val merged = CompletionNormalizer.merge(listOf(CompletionCandidate("My File.txt"), CompletionCandidate("Mine")))
        assertEquals(listOf("My File.txt"), CompletionNormalizer.filterFor(request, merged).map { it.value })
    }

    @Test
    fun optionLookalikeFileNamesAreOfferedWithADotSlashUntilTheUserTypesTheDash() {
        assertEquals("./-rf", CompletionNormalizer.guardOptionLookalike("-rf", ""))
        assertEquals("-rf", CompletionNormalizer.guardOptionLookalike("-rf", "-"))
        assertEquals("notes", CompletionNormalizer.guardOptionLookalike("notes", ""))
    }
}
