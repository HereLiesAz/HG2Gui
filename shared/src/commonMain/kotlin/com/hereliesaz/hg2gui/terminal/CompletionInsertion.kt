package com.hereliesaz.hg2gui.terminal

/**
 * Applies one semantic completion to the command text currently owned by the free-form input.
 * Only the token immediately before the cursor is replaced; earlier command/subcommand text is
 * preserved. "Token" is the same quote- and escape-aware word [CompletionRequest.tokenPrefix]
 * matched against ([CompletionWords.currentTokenStart]), so `cat My\ Fi` replaces the whole
 * `My\ Fi` and not just `Fi`, and a tab never splits one word in the matcher but not here. The
 * candidate's [CompletionCandidate.insertText] is inserted verbatim: it is expected to already be
 * shell-quoted ([CompletionNormalizer.shellSafe]); a candidate built by hand and never merged is
 * quoted here so no raw data reaches the command line either way. The returned cursor points
 * immediately after the inserted candidate (and optional trailing space).
 */
data class CompletionInsertion(
    val line: String,
    val cursor: Int
)

fun applyCompletion(
    line: String,
    cursor: Int = line.length,
    candidate: CompletionCandidate
): CompletionInsertion {
    val safeCursor = cursor.coerceIn(0, line.length)
    val before = line.substring(0, safeCursor)
    val after = line.substring(safeCursor)
    val tokenStart = CompletionWords.currentTokenStart(before)
    val spaceFollows = candidate.appendSpace && after.startsWith(" ")
    val insertText = CompletionNormalizer.shellSafe(candidate).insertText
    val inserted = insertText + if (candidate.appendSpace && !spaceFollows) " " else ""
    val next = before.substring(0, tokenStart) + inserted + after
    return CompletionInsertion(
        line = next,
        // An existing space after the cursor already is the trailing space: step over it.
        cursor = tokenStart + inserted.length + if (spaceFollows) 1 else 0
    )
}
