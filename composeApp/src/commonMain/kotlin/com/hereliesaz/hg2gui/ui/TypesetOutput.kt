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
 * `label: value` lines, the convention a lot of real command output already follows (`ifconfig`,
 * `stat`, `dpkg -s`, and similar). The spec's other views (a manual page, a file index, a diff)
 * each need real semantic parsing of that specific command's output and are out of scope here -
 * this is the one generic, command-agnostic slice. A Plain text toggle (BufferEntry) always falls
 * back to exactly what the command printed, because a reading can be wrong.
 */

private val KEY_VALUE_LINE = Regex("^([^:\\n]{1,32}):\\s+(.+)$")

/** Conservative on purpose: every non-blank line must match `label: value`, or this returns
 *  false - prose and code essentially never satisfy that on every line, so a false positive
 *  (typesetting something that wasn't actually a table) is unlikely. */
fun looksLikeKeyValueTable(text: String): Boolean {
    val lines = text.lines().filter { it.isNotBlank() }
    if (lines.size < 3) return false
    return lines.all { KEY_VALUE_LINE.matches(it.trim()) }
}

private data class KeyValueRow(val label: String, val value: String)

private fun parseKeyValueTable(text: String): List<KeyValueRow> =
    text.lines().filter { it.isNotBlank() }.mapNotNull { line ->
        KEY_VALUE_LINE.matchEntire(line.trim())?.let { m ->
            KeyValueRow(m.groupValues[1].trim(), m.groupValues[2].trim())
        }
    }

@Composable
fun KeyValueTable(text: String, ink: Color, modifier: Modifier = Modifier) {
    val rows = remember(text) { parseKeyValueTable(text) }
    val rule = ink.copy(alpha = .16f)
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(rule))
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    row.label.uppercase(),
                    color = ink.copy(alpha = .55f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.14.em
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
