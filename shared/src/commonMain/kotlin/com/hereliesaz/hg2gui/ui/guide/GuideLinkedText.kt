package com.hereliesaz.hg2gui.ui.guide

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.hereliesaz.hg2gui.ui.StatusKind
import com.hereliesaz.hg2gui.ui.statusColor

/** One recognised command in a run of prose: [start] inclusive, [end] exclusive. */
internal data class GuideCommandHit(val start: Int, val end: Int, val command: String)

/**
 * Prose in which every recognised command name is a link that hands the command to
 * [onCommandSelected] (the Guide stays open; see GuideReaderScreen).
 *
 * Links are underlined as well as coloured, so colour is never the only cue, and by default take
 * `statusColor(StatusKind.Link)`, which clears 4.5:1 on every ground. A caller that passes its own
 * [commandColor] owns its contrast (use `readableOn`).
 */
@Composable
internal fun GuideLinkedText(
    text: String,
    commands: Set<String>,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    modifier: Modifier = Modifier,
    commandColor: Color = Color.Unspecified,
    onCommandSelected: (String) -> Unit
) {
    // LinkAnnotation (rather than ClickableText + offset hit-testing) makes each command a real
    // link: TalkBack lists and activates it, and it takes focus from a keyboard. The click lambda
    // is read through rememberUpdatedState so a new callback doesn't rebuild the string.
    val latestSelected by rememberUpdatedState(onCommandSelected)
    val linkColor = if (commandColor == Color.Unspecified) statusColor(StatusKind.Link) else commandColor
    val hits = remember(text, commands) { findCommandHits(text, commands) }
    val annotated = remember(text, hits, linkColor) {
        annotateCommands(text, hits, linkColor) { latestSelected(it) }
    }
    // A link's own spoken label is its text ("chmod"); the action it performs is not. Each distinct
    // command is also offered as a named custom action ("Add chmod to command") on the paragraph.
    val actions = remember(hits) {
        hits.map { it.command }.distinct().map { command ->
            CustomAccessibilityAction(guideAddActionLabel(command)) {
                latestSelected(command)
                true
            }
        }
    }
    Text(
        text = annotated,
        modifier = modifier.semantics { customActions = actions },
        style = TextStyle(color = color, fontSize = fontSize, lineHeight = lineHeight)
    )
}

/**
 * Finds the command names in [text], longest first so "git commit" wins over "git", never
 * overlapping and never inside a longer word or path. Returned in text order.
 */
internal fun findCommandHits(text: String, commands: Set<String>): List<GuideCommandHit> {
    val occupied = BooleanArray(text.length)
    val hits = mutableListOf<GuideCommandHit>()
    commands.asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .sortedByDescending(String::length)
        .forEach { command ->
            // A trailing '.' ends a sentence ("...after chmod.") unless a word character follows it
            // ("ls.txt"), so it is allowed after the name; a leading '.' never is ("my.ls").
            val regex = Regex("(?<![A-Za-z0-9_.-])${Regex.escape(command)}(?![A-Za-z0-9_-]|\\.[A-Za-z0-9_])")
            regex.findAll(text).forEach { match ->
                val range = match.range
                if ((range.first..range.last).none { occupied[it] }) {
                    (range.first..range.last).forEach { occupied[it] = true }
                    hits += GuideCommandHit(range.first, range.last + 1, command)
                }
            }
        }
    return hits.sortedBy { it.start }
}

private fun annotateCommands(
    text: String,
    hits: List<GuideCommandHit>,
    commandColor: Color,
    onCommandSelected: (String) -> Unit
): AnnotatedString {
    val commandStyle = TextLinkStyles(
        SpanStyle(color = commandColor, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)
    )
    return buildAnnotatedString {
        var cursor = 0
        hits.forEach { hit ->
            append(text.substring(cursor, hit.start))
            withLink(LinkAnnotation.Clickable(hit.command, commandStyle) { onCommandSelected(hit.command) }) {
                append(text.substring(hit.start, hit.end))
            }
            cursor = hit.end
        }
        append(text.substring(cursor))
    }
}
