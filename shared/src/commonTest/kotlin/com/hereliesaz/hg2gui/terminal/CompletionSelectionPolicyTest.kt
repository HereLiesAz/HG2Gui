package com.hereliesaz.hg2gui.terminal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompletionSelectionPolicyTest {
    @Test
    fun exhaustiveChoicesReplaceBlankFreeFormInput() {
        val candidates = listOf(
            CompletionCandidate("main", kind = CompletionKind.BRANCH, enumerationComplete = true),
            CompletionCandidate("develop", kind = CompletionKind.BRANCH, enumerationComplete = true)
        )

        assertTrue(CompletionSelectionPolicy.shouldReplaceFreeForm("", candidates))
    }

    @Test
    fun typedFilterKeepsTextInputAvailable() {
        val candidates = listOf(
            CompletionCandidate("main", kind = CompletionKind.BRANCH, enumerationComplete = true)
        )

        assertFalse(CompletionSelectionPolicy.shouldReplaceFreeForm("ma", candidates))
    }

    @Test
    fun advisoryCandidatesDoNotReplaceFreeFormInput() {
        val candidates = listOf(
            CompletionCandidate("known-host", kind = CompletionKind.HOST, enumerationComplete = false),
            CompletionCandidate("Documents/", kind = CompletionKind.DIRECTORY),
            CompletionCandidate("needle", kind = CompletionKind.VALUE)
        )

        assertFalse(CompletionSelectionPolicy.shouldReplaceFreeForm("", candidates))
    }
}
