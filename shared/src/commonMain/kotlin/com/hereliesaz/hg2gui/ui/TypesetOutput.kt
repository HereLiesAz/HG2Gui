package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * "Output is set, not echoed" (HG2Gui_Reading.dc.html): a terminal draws a grid of characters
 * because a printer once did. Where output is genuinely structured - not prose, not a one-off
 * value - it's set on the page instead: hierarchy from weight/size, a two-column grid, rules at
 * 16% ink, figures right-aligned. This covers the one broadly, safely detectable case: a block of
 * `label: value` lines - real examples include HTTP response headers (`curl -I`) and single-field-
 * per-line output like `openssl x509 -noout -subject -issuer`. Multi-value lines packed with 2+
 * spaces between fields, the way `stat`'s default format does, are split into one row per field.
 * Output whose colons don't line-delimit cleanly (`ifconfig`, `dpkg -s`'s wrapped Description
 * field) just doesn't qualify - it falls back to plain monospace text, which is always correct.
 * The spec's other views (a manual page, a file index, a diff) each need real semantic parsing of
 * that specific command's output and are out of scope here - this is the one generic,
 * command-agnostic slice. A Plain text toggle (BufferEntry) always falls back to exactly what the
 * command printed, because a reading can be wrong.
 */

private val KEY_VALUE_LINE = Regex("^([^:\\n]{1,32}):\\s+(.+)$")
// Finds every `label: value` field within one line, where a field ends at a 2+ space gap (the
// next field's start) or end of line - lets a single-line, multi-field record like `stat`'s
// `Size: 2843  Blocks: 8  IO Block: 4096  regular file` become three rows instead of one row
// whose value swallows the rest of the line from the first colon on.
private val KEY_VALUE_FIELD = Regex("([^:\\n]{1,32}):\\s+(\\S.*?)(?=\\s{2,}|$)")

/** Conservative on purpose: every non-blank line must match `label: value` at least once, or
 *  this returns false - prose and code essentially never satisfy that on every line, so a false
 *  positive (typesetting something that wasn't actually a table) is unlikely. */
fun looksLikeKeyValueTable(text: String): Boolean {
    val lines = text.lines().filter { it.isNotBlank() }
    if (lines.size < 3) return false
    return lines.all { KEY_VALUE_LINE.matches(it.trim()) }
}

internal data class KeyValueRow(val label: String, val value: String)

internal fun parseKeyValueTable(text: String): List<KeyValueRow> =
    text.lines().filter { it.isNotBlank() }.flatMap { line ->
        KEY_VALUE_FIELD.findAll(line.trim()).map { m ->
            KeyValueRow(m.groupValues[1].trim(), m.groupValues[2].trim())
        }.toList()
    }

@Composable
fun KeyValueTable(text: String, ink: Color, modifier: Modifier = Modifier) {
    val rows = remember(text) { parseKeyValueTable(text) }
    val rule = ink.copy(alpha = .16f)
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(rule))
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    row.label.uppercase(),
                    color = ink.copy(alpha = .55f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.18.em
                )
                Text(
                    row.value,
                    color = ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(rule))
        }
    }
}
