package com.hereliesaz.hg2gui.ui.files

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/*
 * F5/F6: the tile <-> rod <-> panel geometry shared between FolderPicker's destination picker
 * and FilesScreen's own folder band, so the browser and the picker are the same idea at two
 * sizes rather than two different implementations. A closed tile sits in a grid; once any tile
 * in the group is open, every other one collapses into a thin rod - advancing on y, not x, down
 * a single column pinned to the trailing edge (F5's fix: the design put these in a vertical
 * column of their own, not a horizontal row).
 */

internal const val TILE_MORPH_MS = 480
internal val ROD_WIDTH = 14.dp
internal val ROD_GAP = 4.dp
internal val GRID_GAP = 6.dp

/** A tile's on-canvas target - position and size in px. */
internal data class TileRect(val x: Float, val y: Float, val w: Float, val h: Float)

/** The grid slot for item [i] of [count] tiles, [columns] wide, against a canvas [canvasWidthPx]
 *  wide - always square, one edge-to-edge row of tiles per [columns] items. */
internal fun gridTileRect(i: Int, columns: Int, canvasWidthPx: Float, density: Density): TileRect = with(density) {
    val gapPx = GRID_GAP.toPx()
    val tileSize = (canvasWidthPx - gapPx * (columns - 1)) / columns
    val row = i / columns
    val col = i % columns
    TileRect(col * (tileSize + gapPx), row * (tileSize + gapPx), tileSize, tileSize)
}

/** One tile's grid-slot square size, for callers that need it outside [gridTileRect] (e.g. to
 *  size the rod column's own rod length off the same tile edge). */
internal fun gridTileSizePx(columns: Int, canvasWidthPx: Float, density: Density): Float = with(density) {
    (canvasWidthPx - GRID_GAP.toPx() * (columns - 1)) / columns
}

/** [slot]'s own place in the trailing-edge rod column - the [slot]-th closed sibling, stacked
 *  downward from [startY] (below wherever the open panel's own header sits), each [rodLengthPx]
 *  tall. */
internal fun rodTileRect(
    slot: Int,
    canvasWidthPx: Float,
    rodLengthPx: Float,
    density: Density,
    startY: Float = 0f
): TileRect = with(density) {
    val rodWidthPx = ROD_WIDTH.toPx()
    val rodGapPx = ROD_GAP.toPx()
    TileRect(canvasWidthPx - rodWidthPx, startY + slot * (rodLengthPx + rodGapPx), rodWidthPx, rodLengthPx)
}
