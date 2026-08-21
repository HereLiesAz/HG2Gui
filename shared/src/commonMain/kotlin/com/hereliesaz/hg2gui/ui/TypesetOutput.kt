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

/*
 * D6 (HG2Gui Termux Coverage.dc.html): "TypesetOutput handles the two-column case well, but
 * plenty of real output is neither two columns nor prose: `ps aux` is eleven columns, `ls -l` is
 * nine ... a design that refuses horizontal scrolling needs an answer for data that is genuinely
 * wide." [looksLikeWideTable]/[parseWideTable] cover that: a header line names each column, every
 * data line beneath it packs at least that many whitespace-separated fields - `ps aux`, `ls -l`,
 * `df -h`, `netstat`. Each row reflows as its own labelled-field record ([WideTableRecords]),
 * "which suits this design better than a table anyway" per the doc's own recommendation, rather
 * than either wrapping into unreadable ribbons or being clipped at the screen edge.
 */

private val WORD = Regex("\\S+")

// A 2-column block already reads fine as KeyValueTable above; this is specifically the "beyond
// two columns" case the doc calls out, so a header naming fewer than 3 fields isn't wide enough
// to bother reflowing.
private const val MIN_WIDE_TABLE_COLUMNS = 3

/** Conservative like [looksLikeKeyValueTable]: every data line has to carry at least as many
 *  whitespace-separated fields as the header names, or this reports false and the raw text
 *  fallback (always correct) is what renders instead. */
fun looksLikeWideTable(text: String): Boolean {
    val lines = text.lines().filter { it.isNotBlank() }
    if (lines.size < 3) return false
    val headerTokens = WORD.findAll(lines[0]).count()
    if (headerTokens < MIN_WIDE_TABLE_COLUMNS) return false
    return lines.drop(1).all { WORD.findAll(it).count() >= headerTokens }
}

internal data class WideTableRow(val fields: List<Pair<String, String>>)

/** Slices every data line at the header's own column start offsets - the same visual alignment
 *  `ps`/`ls`/`df`/`netstat` already lean on to be readable as a fixed terminal grid in the first
 *  place. A column's start is pulled left to whichever is earliest between the header's own label
 *  and every data row's own token in that slot, since a right-aligned numeric column (`ps aux`'s
 *  PID, VSZ, ...) pads its header label further right than a narrower value needs - anchoring on
 *  the header alone would clip a wider value's leading digits. The last column always takes the
 *  rest of its line, so a field that legitimately contains spaces (`ps aux`'s COMMAND) survives
 *  intact instead of being cut at its own first space. */
internal fun parseWideTable(text: String): List<WideTableRow> {
    val lines = text.lines().filter { it.isNotBlank() }
    val headerMatches = WORD.findAll(lines[0]).toList()
    val labels = headerMatches.map { it.value }
    val dataLines = lines.drop(1)
    val starts = headerMatches.map { it.range.first }.toMutableList()
    dataLines.forEach { row ->
        val rowTokens = WORD.findAll(row).toList()
        for (i in labels.indices) {
            if (i < rowTokens.size) starts[i] = minOf(starts[i], rowTokens[i].range.first)
        }
    }
    return dataLines.map { row ->
        val fields = labels.indices.map { i ->
            val from = starts[i].coerceIn(0, row.length)
            val to = if (i + 1 < starts.size) starts[i + 1].coerceIn(from, row.length) else row.length
            labels[i] to row.substring(from, to).trim()
        }
        WideTableRow(fields)
    }
}

// Mirrors OutputLines' own OUTPUT_WIPE_STAGGER_CAP rule: a very wide command (`ps aux` on a busy
// system, a large `netstat`) can have far more rows than are worth laying out as individual
// labelled-field records - the rest still exists in the raw/plain-text fallback, just not
// reflowed one full record at a time.
private const val WIDE_TABLE_ROW_CAP = 40

@Composable
fun WideTableRecords(text: String, ink: Color, modifier: Modifier = Modifier) {
    val rows = remember(text) { parseWideTable(text) }
    val rule = ink.copy(alpha = .16f)
    Column(modifier.fillMaxWidth()) {
        rows.take(WIDE_TABLE_ROW_CAP).forEach { row ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(rule))
            row.fields.forEach { (label, value) ->
                if (value.isNotBlank()) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            label.uppercase(),
                            color = ink.copy(alpha = .55f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.18.em
                        )
                        Text(
                            value,
                            color = ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(rule))
        if (rows.size > WIDE_TABLE_ROW_CAP) {
            Text(
                "and ${rows.size - WIDE_TABLE_ROW_CAP} more rows - see PLAIN TEXT for the rest",
                color = ink.copy(alpha = .45f),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 9.dp)
            )
        }
    }
}

// D6: "non-UTF8 bytes are not text at all ... binary content should be refused with an
// explanation rather than rendered." By the time output reaches here it has already gone through
// a UTF-8 decode - real binary content shows up as either the Unicode replacement character
// (invalid byte sequences) or a run of C0 control bytes that were never valid printable text to
// begin with (a compiled binary's own header, mostly). A real terminal transcript essentially
// never contains either, so a low, conservative ratio is enough to tell the two apart without
// false-flagging an ordinary command's output.
private const val BINARY_SUSPICIOUS_RATIO = 0.01

fun looksLikeBinary(text: String): Boolean {
    if (text.isEmpty()) return false
    val suspicious = text.count { c -> c == '\uFFFD' || (c.code < 0x20 && c != '\t' && c != '\n' && c != '\r') }
    return suspicious.toDouble() / text.length > BINARY_SUSPICIOUS_RATIO
}
