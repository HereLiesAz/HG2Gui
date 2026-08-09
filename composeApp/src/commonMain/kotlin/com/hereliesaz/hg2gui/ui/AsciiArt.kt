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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Light -> dense, the same ramp image-to-ASCII converters use in reverse. A character's position
// in this ramp is read as its "ink density" for AsciiArtCanvas, not rendered as a literal glyph.
private const val DENSITY_RAMP = " .'`^\",:;Il!i><~+_-?][}{1)(|\\/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@\$"

internal fun charDensity(c: Char): Float {
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

// DENSITY_RAMP is a photo-to-ASCII print-density ramp, so most of the punctuation hand-drawn line
// art actually leans on - |, \, /, _, -, (, ), <, >, brackets - sits in its low-to-mid range, not
// near the top: those characters cover little of their own cell, but they're still the lines that
// draw a shape. 0.42 (this file's original threshold) sat above nearly all of them, so detected
// art rendered as a near-blank canvas. 0.1 draws the line just past the handful of characters that
// really do read as visually empty (space, '.', "'", '`', '^', '"', ',') and treats everything
// denser than that - which is most of an ASCII picture's actual linework - as ink.
internal const val ISO_LEVEL = 0.1f

/** Linearly interpolates the point along a-b where the density field crosses [ISO_LEVEL]. */
private fun edgePoint(pa: Offset, va: Float, pb: Offset, vb: Float): Offset {
    val t = if (vb == va) 0.5f else ((ISO_LEVEL - va) / (vb - va)).coerceIn(0f, 1f)
    return Offset(pa.x + (pb.x - pa.x) * t, pa.y + (pb.y - pa.y) * t)
}

private fun Path.polygon(points: List<Offset>) {
    if (points.isEmpty()) return
    moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    close()
}

/**
 * Traces the character-density field into a single filled vector silhouette via marching squares
 * - the same contour-tracing idea Potrace-style vectorizers use, applied to the density grid
 * instead of raster pixels. No model, no network call: each 2x2 neighborhood of sample points is
 * classified against [ISO_LEVEL] and contributes an interpolated sub-polygon, so the traced edges
 * come out smooth rather than stair-stepped along the character grid. All sub-polygons are filled
 * with one flat color into one [Path] - Azphalt's flat, no-gradient, no-blur look, constructed as
 * an actual vector shape rather than reproduced glyph-by-glyph.
 */
private fun buildContourPath(density: Array<FloatArray>, cellPx: Float): Path {
    val path = Path()
    val rows = density.size
    val cols = density[0].size
    for (r in 0 until rows - 1) {
        for (c in 0 until cols - 1) {
            val a = density[r][c]         // top-left
            val b = density[r][c + 1]     // top-right
            val cc = density[r + 1][c + 1] // bottom-right
            val d = density[r + 1][c]     // bottom-left
            val case = (if (a >= ISO_LEVEL) 1 else 0) or (if (b >= ISO_LEVEL) 2 else 0) or
                (if (cc >= ISO_LEVEL) 4 else 0) or (if (d >= ISO_LEVEL) 8 else 0)
            if (case == 0) continue

            val tl = Offset(c * cellPx, r * cellPx)
            val tr = Offset((c + 1) * cellPx, r * cellPx)
            val br = Offset((c + 1) * cellPx, (r + 1) * cellPx)
            val bl = Offset(c * cellPx, (r + 1) * cellPx)
            val top = edgePoint(tl, a, tr, b)
            val right = edgePoint(tr, b, br, cc)
            val bottom = edgePoint(bl, d, br, cc)
            val left = edgePoint(tl, a, bl, d)

            val polygon = when (case) {
                1 -> listOf(tl, top, left)
                2 -> listOf(top, tr, right)
                3 -> listOf(tl, tr, right, left)
                4 -> listOf(right, br, bottom)
                // 5 and 10 are the ambiguous saddle cases - resolved as two separate corners
                // joined by the path, a fixed (not topologically "correct") choice that's fine
                // for decorative tracing.
                5 -> { path.polygon(listOf(tl, top, left)); listOf(right, br, bottom) }
                6 -> listOf(top, tr, br, bottom)
                7 -> listOf(tl, tr, br, bottom, left)
                8 -> listOf(left, bl, bottom)
                9 -> listOf(tl, top, bottom, bl)
                10 -> { path.polygon(listOf(top, tr, right)); listOf(left, bl, bottom) }
                11 -> listOf(tl, tr, right, bottom, bl)
                12 -> listOf(left, bl, br, right)
                13 -> listOf(tl, top, right, br, bl)
                14 -> listOf(top, tr, br, bl, left)
                else -> listOf(tl, tr, br, bl) // 15: fully inside
            }
            path.polygon(polygon)
        }
    }
    return path
}

/**
 * Renders a block of ASCII/box-drawing art as one traced flat vector silhouette instead of
 * literal monospace glyphs - see [buildContourPath].
 */
@Composable
fun AsciiArtCanvas(text: String, ink: Color, modifier: Modifier = Modifier, maxCell: Dp = 7.dp) {
    val lines = text.lines()
    val cols = lines.maxOf { it.length }.coerceAtLeast(1)
    val rows = lines.size
    // A 1-sample zero border on every side so art touching the block's edge still closes into a
    // shape instead of being cut off mid-contour.
    val density = Array(rows + 2) { r ->
        FloatArray(cols + 2) { c ->
            if (r == 0 || c == 0 || r == rows + 1 || c == cols + 1) 0f
            else charDensity(lines[r - 1].getOrElse(c - 1) { ' ' })
        }
    }

    // maxWidth has to be read from constraints BoxWithConstraints itself is measured with, which
    // means horizontalScroll can't be on this Box - horizontalScroll always hands its child
    // Constraints.Infinity, so a BoxWithConstraints inside one never sees a real width to fit to.
    // Scrolling only kicks in on the Canvas below, once cell has already been sized against the
    // real available width.
    BoxWithConstraints(modifier) {
        val cell = min(maxCell, maxWidth / cols)
        Canvas(
            Modifier
                .horizontalScroll(rememberScrollState())
                .width(cell * (cols + 2))
                .height(cell * (rows + 2))
        ) {
            val path = buildContourPath(density, cell.toPx())
            drawPath(path, color = ink)
        }
    }
}

private fun min(a: Dp, b: Dp) = if (a.value < b.value) a else b
