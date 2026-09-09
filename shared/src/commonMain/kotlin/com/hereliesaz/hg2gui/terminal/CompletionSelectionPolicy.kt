package com.hereliesaz.hg2gui.terminal

/**
 * Decides when HG2Gui can replace an open text operand with a finite native choice surface.
 * Only candidate kinds whose provider can describe a bounded set qualify. Paths stay with the
 * graphical file picker; arbitrary VALUE fields remain editable text.
 */
object CompletionSelectionPolicy {
    private val finiteKinds = setOf(
        CompletionKind.SUBCOMMAND,
        CompletionKind.OPTION,
        CompletionKind.PACKAGE,
        CompletionKind.BRANCH,
        CompletionKind.HOST,
        CompletionKind.SERVICE
    )

    fun enumerableCandidates(candidates: List<CompletionCandidate>): List<CompletionCandidate> =
        candidates.filter { it.kind in finiteKinds }

    fun shouldReplaceFreeForm(
        inputText: String,
        candidates: List<CompletionCandidate>
    ): Boolean = inputText.isBlank() && enumerableCandidates(candidates).isNotEmpty()
}
