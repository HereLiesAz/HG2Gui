package com.hereliesaz.hg2gui.terminal

/**
 * Decides when HG2Gui can replace an open text operand with a finite native choice surface.
 * A candidate must come from a provider that explicitly guarantees its enumeration is complete.
 * Paths stay with the graphical file picker; arbitrary values and advisory candidates remain
 * editable text.
 */
object CompletionSelectionPolicy {
    fun enumerableCandidates(candidates: List<CompletionCandidate>): List<CompletionCandidate> =
        candidates.filter(CompletionCandidate::enumerationComplete)

    fun shouldReplaceFreeForm(
        inputText: String,
        candidates: List<CompletionCandidate>
    ): Boolean = inputText.isBlank() && enumerableCandidates(candidates).isNotEmpty()
}
