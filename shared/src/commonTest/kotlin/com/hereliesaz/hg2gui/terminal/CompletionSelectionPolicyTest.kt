package com.hereliesaz.hg2gui.terminal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompletionSelectionPolicyTest {
    @Test
    fun finiteSemanticChoicesReplaceBlankFreeFormInput() {
        val candidates = listOf(
            CompletionCandidate("main", kind = CompletionKind.BRANCH),
            CompletionCandidate("develop", kind = CompletionKind.BRANCH)
        )

        assertTrue(CompletionSelectionPolicy.shouldReplaceFreeForm("", candidates))
    }

    @Test
    fun typedFilterKeepsTextInputAvailable() {
        val candidates = listOf(CompletionCandidate("main", kind = CompletionKind.BRANCH))

        assertFalse(CompletionSelectionPolicy.shouldReplaceFreeForm("ma", candidates))
    }

    @Test
    fun filesAndOpenValuesDoNotReplaceFreeFormInput() {
        val candidates = listOf(
            CompletionCandidate("Documents/", kind = CompletionKind.DIRECTORY),
            CompletionCandidate("needle", kind = CompletionKind.VALUE)
        )

        assertFalse(CompletionSelectionPolicy.shouldReplaceFreeForm("", candidates))
    }
}
