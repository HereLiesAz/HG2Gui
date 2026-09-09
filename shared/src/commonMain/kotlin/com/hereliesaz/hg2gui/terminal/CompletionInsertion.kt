package com.hereliesaz.hg2gui.terminal

/**
 * Applies one semantic completion to the command text currently owned by the free-form input.
 * Only the token immediately before the cursor is replaced; earlier command/subcommand text is
 * preserved. The returned cursor points immediately after the inserted candidate (and optional
 * trailing space).
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
    val tokenStart = before.indexOfLast { it.isWhitespace() }.let { if (it < 0) 0 else it + 1 }
    val inserted = candidate.insertText + if (candidate.appendSpace && !after.startsWith(" ")) " " else ""
    val next = before.substring(0, tokenStart) + inserted + after
    return CompletionInsertion(
        line = next,
        cursor = tokenStart + inserted.length
    )
}
