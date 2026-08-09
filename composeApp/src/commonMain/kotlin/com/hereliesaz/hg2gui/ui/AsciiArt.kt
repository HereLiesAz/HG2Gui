package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Light -> dense, the same ramp image-to-ASCII converters use in reverse. A character's position
// in this ramp is read as its "ink density" for AsciiArtCanvas, not rendered as a literal glyph.
private const val DENSITY_RAMP = " .'`^\",:;Il!i><~+_-?][}{1)(|\\/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@\$"

private fun charDensity(c: Char): Float {
    if (c.isWhitespace()) return 0f
    // Box-drawing (U+2500-U+257F) and block elements (U+2580-U+259F) are literal solid/partial
    // blocks already, not glyphs to rank - treat them as fully dense.
    if (c.code in 0x2500..0x259F) return 1f
    val i = DENSITY_RAMP.indexOf(c)
    return if (i < 0) 0.7f else i.toFloat() / (DENSITY_RAMP.length - 1)
}

/**
 * Heuristic: does this block of text read as ASCII/box-drawing art rather than prose or a table?
 * Conservative on purpose - a false negative just falls back to plain monospace text (always
 * fine), a false positive would render real prose as an unreadable block grid. Requires at least
 * a few lines and a low ratio of alphanumeric characters among the non-whitespace ones, since art
 * leans on symbols/box-drawing where prose and tables lean on letters and digits.
 */
fun looksLikeAsciiArt(text: String): Boolean {
    val lines = text.lines().filter { it.isNotBlank() }
    if (lines.size < 3) return false
    val chars = lines.joinToString("").filter { !it.isWhitespace() }
    if (chars.length < 40) return false
    val alnum = chars.count { it.isLetterOrDigit() }
    return alnum.toFloat() / chars.length < 0.35f
}

/**
 * Renders a block of ASCII/box-drawing art as flat filled cells sized by character density,
 * instead of literal monospace glyphs - matching Azphalt's flat, no-gradient, no-blur look
 * rather than trying to reproduce a font's actual letterforms at a tiny size.
 */
@Composable
fun AsciiArtCanvas(text: String, ink: Color, modifier: Modifier = Modifier, maxCell: Dp = 7.dp) {
    val lines = text.lines()
    val cols = lines.maxOf { it.length }.coerceAtLeast(1)
    val rows = lines.size

    BoxWithConstraints(modifier.horizontalScroll(rememberScrollState())) {
        val cell = min(maxCell, maxWidth / cols)
        Canvas(Modifier.width(cell * cols).height(cell * rows)) {
            val cellPx = cell.toPx()
            lines.forEachIndexed { row, line ->
                line.forEachIndexed { col, c ->
                    val density = charDensity(c)
                    if (density <= 0f) return@forEachIndexed
                    val side = cellPx * (0.35f + 0.65f * density)
                    val offset = (cellPx - side) / 2f
                    drawRect(
                        color = ink.copy(alpha = density.coerceIn(0.15f, 1f)),
                        topLeft = Offset(col * cellPx + offset, row * cellPx + offset),
                        size = Size(side, side)
                    )
                }
            }
        }
    }
}

private fun min(a: Dp, b: Dp) = if (a.value < b.value) a else b
