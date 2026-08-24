package com.hereliesaz.hg2gui.ui.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * F3: ".md rendered rather than shown as source." A line-based renderer for the handful of
 * constructs that actually show up in real-world markdown (headers, fenced code, bullet lists,
 * inline bold/italic/code, paragraph breaks) - not a CommonMark-complete parser, which this
 * screen's own scope doesn't call for.
 */

private const val HEADER_1_SP = 17
private const val HEADER_2_SP = 15
private const val HEADER_3_SP = 13
private const val BODY_SP = 12
private val LIST_INDENT = 10.dp
private val CODE_INDENT = 8.dp
private val PARAGRAPH_GAP = 6.dp

@Composable
internal fun MarkdownPreview(source: String, onPage: Color, codeInk: Color) {
    Column {
        var inCodeFence = false
        val codeLines = mutableListOf<String>()
        @Composable
        fun flushCode() {
            if (codeLines.isEmpty()) return
            Text(
                codeLines.joinToString("\n"), color = codeInk, fontSize = BODY_SP.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = CODE_INDENT, top = PARAGRAPH_GAP, bottom = PARAGRAPH_GAP)
            )
            codeLines.clear()
        }
        source.split("\n").forEach { rawLine ->
            if (rawLine.trimStart().startsWith("```")) {
                if (inCodeFence) flushCode()
                inCodeFence = !inCodeFence
                return@forEach
            }
            if (inCodeFence) {
                codeLines += rawLine
                return@forEach
            }
            markdownLine(rawLine, onPage)
        }
        flushCode()
    }
}

@Composable
private fun markdownLine(line: String, onPage: Color) {
    val trimmed = line.trimStart()
    when {
        trimmed.isBlank() -> Text("", modifier = Modifier.padding(vertical = PARAGRAPH_GAP / 2))
        trimmed.startsWith("### ") -> Header(trimmed.removePrefix("### "), HEADER_3_SP, onPage)
        trimmed.startsWith("## ") -> Header(trimmed.removePrefix("## "), HEADER_2_SP, onPage)
        trimmed.startsWith("# ") -> Header(trimmed.removePrefix("# "), HEADER_1_SP, onPage)
        trimmed.startsWith("- ") || trimmed.startsWith("* ") -> Bullet(trimmed.drop(2), onPage)
        else -> Text(inlineMarkdown(line, onPage), fontSize = BODY_SP.sp)
    }
}

@Composable
private fun Header(text: String, sizeSp: Int, onPage: Color) {
    Text(
        inlineMarkdown(text, onPage), fontSize = sizeSp.sp, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(top = PARAGRAPH_GAP)
    )
}

@Composable
private fun Bullet(text: String, onPage: Color) {
    Text(
        buildAnnotatedString {
            append("· ")
            append(inlineMarkdown(text, onPage))
        },
        fontSize = BODY_SP.sp,
        modifier = Modifier.padding(start = LIST_INDENT)
    )
}

private class InlineMarker(val token: String, val start: Int, val style: (Color) -> SpanStyle)

/** The earliest of `**`/`` ` ``/`*` at or after [from] - `*` is excluded when it's really the
 *  start of a `**` bold marker already found, so bold doesn't get misread as an empty italic
 *  run followed by stray text. */
private fun nextInlineMarker(line: String, from: Int): InlineMarker? {
    val bold = line.indexOf("**", from)
    val code = line.indexOf('`', from)
    val italic = line.indexOf('*', from).takeIf { it != bold }
    val candidates = listOfNotNull(
        bold.takeIf { it >= 0 }?.let { InlineMarker("**", it) { SpanStyle(fontWeight = FontWeight.ExtraBold) } },
        code.takeIf { it >= 0 }?.let { InlineMarker("`", it) { c -> SpanStyle(fontFamily = FontFamily.Monospace, color = c) } },
        italic?.takeIf { it >= 0 }?.let { InlineMarker("*", it) { SpanStyle(fontStyle = FontStyle.Italic) } }
    )
    return candidates.minByOrNull { it.start }
}

/** `**bold**`, `*italic*`, and `` `code` `` within one line - the inline subset that actually
 *  shows up outside a fenced block. */
private fun inlineMarkdown(line: String, onPage: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < line.length) {
        val marker = nextInlineMarker(line, i)
        val end = marker?.let { line.indexOf(it.token, it.start + it.token.length) } ?: -1
        if (marker == null || end < 0) {
            append(line.substring(i))
            i = line.length
        } else {
            append(line.substring(i, marker.start))
            withStyle(marker.style(onPage)) { append(line.substring(marker.start + marker.token.length, end)) }
            i = end + marker.token.length
        }
    }
}
