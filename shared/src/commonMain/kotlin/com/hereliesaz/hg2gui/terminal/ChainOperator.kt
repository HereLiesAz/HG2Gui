package com.hereliesaz.hg2gui.terminal

/**
 * W3 (docs/HG2Gui Termux Coverage.dc.html): the pill menu builds one command as a flat token
 * list, with no representation for the composition that makes a shell a shell - pipes, chains,
 * redirects. Rather than parse or model a real shell grammar, this keeps the same one-segment-
 * at-a-time menu language and just lets a segment fold into a growing literal prefix ahead of
 * it - "ls" chained with Pipe becomes the prefix "ls | ", ready for the next segment ("grep foo")
 * to land after it. The composed line is still just text handed to the shell in one piece, which
 * already knows how to run a pipeline - nothing here interprets the operators itself.
 */
enum class ChainOperator(val symbol: String, val cap: String) {
    Pipe("|", "PIPE"),
    And("&&", "AND"),
    Or("||", "OR"),
    Then(";", "THEN"),
    Redirect(">", "OUT"),
    Append(">>", "OUT+")
}

/** Folds [segment] (a resolved token list plus whatever free text followed it, already joined
 *  and trimmed by the caller) onto [prefix] with [operator] between them, ready for the next
 *  segment to be typed or picked. A blank [segment] leaves [prefix] untouched - there is nothing
 *  yet to chain from, so the pick is a no-op rather than emitting a dangling operator. */
fun chainSegment(prefix: String, segment: String, operator: ChainOperator): String {
    val trimmedSegment = segment.trim()
    if (trimmedSegment.isEmpty()) return prefix
    return "$prefix$trimmedSegment ${operator.symbol} "
}
