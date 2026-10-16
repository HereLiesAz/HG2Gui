package com.hereliesaz.hg2gui.ui.guide

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

@Composable
internal fun GuideLinkedText(
    text: String,
    commands: Set<String>,
    color: Color,
    commandColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    modifier: Modifier = Modifier,
    onCommandSelected: (String) -> Unit
) {
    // LinkAnnotation (rather than ClickableText + offset hit-testing) makes each command a real
    // link: TalkBack lists and activates it, and it takes focus from a keyboard. The click lambda
    // is read through rememberUpdatedState so a new callback doesn't rebuild the string.
    val latestSelected by rememberUpdatedState(onCommandSelected)
    val annotated = remember(text, commands, commandColor) {
        annotateCommands(text, commands, commandColor) { latestSelected(it) }
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = fontSize, lineHeight = lineHeight)
    )
}

private fun annotateCommands(
    text: String,
    commands: Set<String>,
    commandColor: Color,
    onCommandSelected: (String) -> Unit
): AnnotatedString {
    data class Hit(val start: Int, val end: Int, val command: String)

    val occupied = BooleanArray(text.length)
    val hits = mutableListOf<Hit>()
    commands.asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .sortedByDescending(String::length)
        .forEach { command ->
            val regex = Regex("(?<![A-Za-z0-9_.-])${Regex.escape(command)}(?![A-Za-z0-9_.-])")
            regex.findAll(text).forEach { match ->
                val range = match.range
                if ((range.first..range.last).none { occupied[it] }) {
                    (range.first..range.last).forEach { occupied[it] = true }
                    hits += Hit(range.first, range.last + 1, command)
                }
            }
        }

    val commandStyle = TextLinkStyles(SpanStyle(color = commandColor, fontWeight = FontWeight.SemiBold))
    return buildAnnotatedString {
        var cursor = 0
        hits.sortedBy { it.start }.forEach { hit ->
            append(text.substring(cursor, hit.start))
            withLink(LinkAnnotation.Clickable(hit.command, commandStyle) { onCommandSelected(hit.command) }) {
                append(text.substring(hit.start, hit.end))
            }
            cursor = hit.end
        }
        append(text.substring(cursor))
    }
}
