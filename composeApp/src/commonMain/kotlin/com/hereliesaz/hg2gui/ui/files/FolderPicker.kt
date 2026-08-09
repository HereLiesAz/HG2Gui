package com.hereliesaz.hg2gui.ui.files

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/*
 * The Move/Copy destination picker: a grid of folder tiles where tapping one morphs it into an
 * open panel while its siblings shrink into thin coloured rods beside it - not a fade between
 * two screens, a single continuous move. Tapping a rod swaps which tile is open; tapping the
 * open panel's own header descends into it, its former siblings' rods becoming the new grid.
 *
 * Geometry is tracked per item as a target Rect (position + size in px), animated with one
 * Animatable<Rect> each rather than the design source's multi-stage keyframes - fewer stops
 * along the way, but the same shape of motion: a tile becoming a rod, a rod becoming a panel.
 */

private const val COLUMNS = 3
private val GRID_GAP = 6.dp
private val ROD_WIDTH = 14.dp
private val ROD_GAP = 4.dp
private const val MORPH_MS = 480

private data class Rect(val x: Float, val y: Float, val w: Float, val h: Float)

@Composable
fun FolderPicker(
    title: String,
    listDir: suspend (path: String) -> List<VfsEntry>,
    onConfirm: (targetPath: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf("/") }
    var folders by remember { mutableStateOf<List<VfsEntry>>(emptyList()) }
    var openIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(currentPath) {
        folders = listDir(currentPath).filter { it.isDirectory }.sortedBy { it.name.lowercase() }
        openIndex = null
    }

    val density = LocalDensity.current

    Column(modifier.fillMaxSize().background(PageYellowBrush)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PickerChip("‹ CANCEL", onClick = onCancel)
            PickerChip(title.uppercase(), filled = false, clickable = false)
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val crumbs = currentPath.trim('/').split('/').filter { it.isNotEmpty() }
            Text(
                "/" + crumbs.joinToString("/"),
                color = Azphalt.Ink.copy(alpha = .55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp)) {
            if (folders.isEmpty()) {
                Text(
                    "NO SUBFOLDERS HERE",
                    color = Azphalt.Ink.copy(alpha = .4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.18.em
                )
            } else {
                var canvasWidthPx by remember { mutableStateOf(0f) }
                Box(
                    Modifier.fillMaxSize().onGloballyPositioned { canvasWidthPx = it.size.width.toFloat() }
                ) {
                    if (canvasWidthPx > 0f) {
                        folders.forEachIndexed { i, folder ->
                            key(folder.path) {
                                MorphTile(
                                    label = folder.name,
                                    hue = Azphalt.hues[Azphalt.hueOf(folder.path)],
                                    rect = targetRect(i, folders.size, openIndex, canvasWidthPx, density),
                                    isOpen = openIndex == i,
                                    onClick = {
                                        openIndex = if (openIndex == i) null else i
                                    },
                                    onOpenDescend = { currentPath = folder.path }
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PickerChip("USE THIS FOLDER", background = Azphalt.hues[6], foreground = Azphalt.White, fillWidth = true) {
                onConfirm(currentPath)
            }
        }
    }
}

/** The grid rect (nothing open), the panel rect (this tile is open), or a rod rect (some other
 *  tile is open) for item [i] of [count], in px against a canvas [canvasWidthPx] wide. */
private fun targetRect(i: Int, count: Int, openIndex: Int?, canvasWidthPx: Float, density: Density): Rect = with(density) {
    val gapPx = GRID_GAP.toPx()
    val tileSize = (canvasWidthPx - gapPx * (COLUMNS - 1)) / COLUMNS

    when {
        openIndex == null -> {
            val row = i / COLUMNS
            val col = i % COLUMNS
            Rect(col * (tileSize + gapPx), row * (tileSize + gapPx), tileSize, tileSize)
        }
        openIndex == i -> {
            Rect(0f, 0f, canvasWidthPx, tileSize * 1.6f)
        }
        else -> {
            val rodWidthPx = ROD_WIDTH.toPx()
            val rodGapPx = ROD_GAP.toPx()
            val others = (0 until count).filter { it != openIndex }
            val slot = others.indexOf(i)
            val panelHeight = tileSize * 1.6f
            Rect(slot * (rodWidthPx + rodGapPx), panelHeight + GRID_GAP.toPx(), rodWidthPx, tileSize * 0.7f)
        }
    }
}

@Composable
private fun MorphTile(
    label: String,
    hue: Color,
    rect: Rect,
    isOpen: Boolean,
    onClick: () -> Unit,
    onOpenDescend: () -> Unit
) {
    val density = LocalDensity.current
    val x = remember { Animatable(rect.x) }
    val y = remember { Animatable(rect.y) }
    val w = remember { Animatable(rect.w) }
    val h = remember { Animatable(rect.h) }
    val easing = CubicBezierEasing(0f, .9f, .1f, 1f)

    LaunchedEffect(rect) {
        launchAll(
            { x.animateTo(rect.x, tween(MORPH_MS, easing = easing)) },
            { y.animateTo(rect.y, tween(MORPH_MS, easing = easing)) },
            { w.animateTo(rect.w, tween(MORPH_MS, easing = easing)) },
            { h.animateTo(rect.h, tween(MORPH_MS, easing = easing)) }
        )
    }

    with(density) {
        Box(
            Modifier
                .offset { IntOffset(x.value.toInt(), y.value.toInt()) }
                .width(w.value.toDp())
                .height(h.value.toDp())
                .clip(RoundedCornerShape(if (isOpen) 18.dp else if (w.value.toDp() < 20.dp) 999.dp else 13.dp))
                .background(hue)
                .clickable { if (isOpen) onOpenDescend() else onClick() }
                .padding(if (isOpen) 14.dp else 6.dp),
            contentAlignment = if (isOpen) Alignment.TopStart else Alignment.BottomStart
        ) {
            if (w.value.toDp() > 24.dp) {
                Column {
                    Text(
                        label.uppercase(),
                        color = Azphalt.White,
                        fontSize = if (isOpen) 15.sp else 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.06.em,
                        maxLines = if (isOpen) 1 else 2
                    )
                    if (isOpen) {
                        Text(
                            "TAP TO GO INSIDE",
                            color = Azphalt.White.copy(alpha = .7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private suspend fun launchAll(vararg blocks: suspend () -> Unit) {
    coroutineScope {
        blocks.forEach { block -> launch { block() } }
    }
}

private val PageYellowBrush = Brush.linearGradient(
    0f to Color(0xFFE8C81E), 0.5f to Color(0xFFD9B615), 1f to Color(0xFFE8C81E)
)

@Composable
private fun PickerChip(
    label: String,
    modifier: Modifier = Modifier,
    background: Color = Azphalt.Ink,
    foreground: Color = Azphalt.Yellow,
    filled: Boolean = true,
    clickable: Boolean = true,
    fillWidth: Boolean = false,
    onClick: () -> Unit = {}
) {
    val bg = if (filled) background else Azphalt.Ink.copy(alpha = .14f)
    val fg = if (filled) foreground else Azphalt.Ink.copy(alpha = .55f)
    Box(
        modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(percent = 50))
            .background(bg)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em)
    }
}
