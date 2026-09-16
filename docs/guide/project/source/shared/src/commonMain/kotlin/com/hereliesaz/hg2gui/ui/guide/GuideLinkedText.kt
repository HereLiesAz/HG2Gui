package com.hereliesaz.hg2gui.ui.guide

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

private const val COMMAND_TAG = "guide-command"

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
    val annotated = remember(text, commands, commandColor) {
        annotateCommands(text, commands, commandColor)
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = fontSize, lineHeight = lineHeight),
        onClick = { offset ->
            annotated.getStringAnnotations(COMMAND_TAG, offset, offset)
                .firstOrNull()
                ?.let { onCommandSelected(it.item) }
        }
    )
}

private fun annotateCommands(text: String, commands: Set<String>, commandColor: Color): AnnotatedString {
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

    return buildAnnotatedString {
        append(text)
        hits.forEach { hit ->
            addStringAnnotation(COMMAND_TAG, hit.command, hit.start, hit.end)
            addStyle(SpanStyle(color = commandColor, fontWeight = FontWeight.SemiBold), hit.start, hit.end)
        }
    }
}
