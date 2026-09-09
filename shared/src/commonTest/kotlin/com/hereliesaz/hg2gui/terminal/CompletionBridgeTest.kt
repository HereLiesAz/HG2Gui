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
}
