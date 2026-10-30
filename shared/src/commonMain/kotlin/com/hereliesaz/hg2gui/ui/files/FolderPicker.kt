package com.hereliesaz.hg2gui.ui.files

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hereliesaz.hg2gui.ui.AzphaltChip
import com.hereliesaz.hg2gui.ui.AzphaltHeading
import com.hereliesaz.hg2gui.ui.BackStepState
import com.hereliesaz.hg2gui.ui.StatusKind
import com.hereliesaz.hg2gui.ui.liveRegionPolite
import com.hereliesaz.hg2gui.ui.maxContentWidth
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.contrastingText
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.statusColor
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType
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
private const val HUE_PRIMARY = 6
private const val MIN_TAP_DP = 48
private const val LABEL_MIN_WIDTH_DP = 24

@Composable
fun FolderPicker(
    title: String,
    listDir: suspend (path: String) -> List<VfsEntry>,
    onConfirm: (targetPath: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    // Lets the host's system-back handling step up one folder at a time instead of cancelling the
    // whole picker; null (the default) keeps the old cancel-on-back behaviour.
    backStep: BackStepState? = null
) {
    var currentPath by remember { mutableStateOf("/") }
    var folders by remember { mutableStateOf<List<VfsEntry>>(emptyList()) }
    var openIndex by remember { mutableStateOf<Int?>(null) }
    var listError by remember { mutableStateOf<String?>(null) }
    // Which path [folders] was last read for - an empty list is "NO SUBFOLDERS" only once the
    // listing for the current path has actually come back, never while it is still in flight.
    var loadedPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPath) {
        // A listing that throws used to take the whole picker down with it.
        try {
            folders = listDir(currentPath).filter { it.isDirectory }.sortedBy { it.name.lowercase() }
            listError = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            folders = emptyList()
            listError = "Couldn't open this folder: ${e.message ?: "unknown error"}"
        }
        loadedPath = currentPath
        openIndex = null
    }

    val atRoot = currentPath.trim('/').isEmpty()
    SideEffect {
        backStep?.canStepBack = !atRoot
        backStep?.stepBack = { currentPath = vfsParentPath(currentPath) }
    }
    DisposableEffect(backStep) {
        onDispose { backStep?.reset() }
    }

    val density = LocalDensity.current

    Column(modifier.fillMaxSize().background(Azphalt.currentGround.pageBrush()).maxContentWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AzphaltChip("‹ CANCEL", onClick = onCancel, contentDescription = "Cancel")
            if (!atRoot) {
                AzphaltChip(
                    "‹ UP", onClick = { currentPath = vfsParentPath(currentPath) },
                    contentDescription = "Up one folder"
                )
            }
        }
        AzphaltHeading(title, Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp))

        // Each crumb jumps straight back to that ancestor.
        PathCrumbs(currentPath, onNavigate = { currentPath = it }, modifier = Modifier.padding(horizontal = 20.dp))

        listError?.let {
            Text(
                it,
                color = statusColor(StatusKind.Error),
                style = captionTextStyle(),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp).liveRegionPolite()
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp)) {
            if (folders.isEmpty()) {
                StatusLabel(if (loadedPath == currentPath) "NO SUBFOLDERS HERE" else "LOADING…")
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
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AzphaltChip(
                "USE THIS FOLDER", onClick = { onConfirm(currentPath) },
                modifier = Modifier.fillMaxWidth(), tint = Azphalt.hues[HUE_PRIMARY],
                contentDescription = "Use this folder: $currentPath"
            )
        }
    }
}

/**
 * The breadcrumb strip both pickers share: "/" then one tappable crumb per ancestor, each a
 * 48dp-tall target that calls [onNavigate] with that ancestor's path. It starts scrolled to the
 * *end*, so a long path shows the folder you are in rather than the first few segments (the start
 * is a swipe away).
 */
@Composable
internal fun PathCrumbs(path: String, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val crumbs = path.trim('/').split('/').filter { it.isNotEmpty() }
    // A fresh state per path: a reversed scroller sitting at 0 is pinned to its end.
    val scroll = remember(path) { ScrollState(0) }
    val muted = statusColor(StatusKind.Muted)
    val here = Azphalt.currentGround.onPage
    Row(modifier.fillMaxWidth().horizontalScroll(scroll, reverseScrolling = true)) {
        Crumb("/", "Go to the top folder", if (crumbs.isEmpty()) here else muted) { onNavigate("/") }
        crumbs.forEachIndexed { i, name ->
            val target = "/" + crumbs.take(i + 1).joinToString("/")
            val isLast = i == crumbs.lastIndex
            Crumb(
                name + if (isLast) "" else "/", "Go to folder $name", if (isLast) here else muted
            ) { onNavigate(target) }
        }
    }
}

@Composable
private fun Crumb(text: String, description: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = MIN_TAP_DP.dp, minHeight = MIN_TAP_DP.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, style = captionTextStyle().copy(fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

/** The grid rect (nothing open), the panel rect (this tile is open), or a rod rect (some other
 *  tile is open) for item [i] of [count], in px against a canvas [canvasWidthPx] wide. */
private fun targetRect(i: Int, count: Int, openIndex: Int?, canvasWidthPx: Float, density: Density): TileRect = when {
    openIndex == null -> gridTileRect(i, COLUMNS, canvasWidthPx, density)
    openIndex == i -> with(density) {
        TileRect(0f, 0f, canvasWidthPx, gridTileSizePx(COLUMNS, canvasWidthPx, density) * 1.6f)
    }
    else -> {
        val tileSize = gridTileSizePx(COLUMNS, canvasWidthPx, density)
        val panelHeight = tileSize * 1.6f
        val rodLengthPx = tileSize * 0.7f
        val others = (0 until count).filter { it != openIndex }
        rodTileRect(
            others.indexOf(i), canvasWidthPx, rodLengthPx, density,
            startY = panelHeight + with(density) { GRID_GAP.toPx() }
        )
    }
}

@Composable
private fun MorphTile(
    label: String,
    hue: Color,
    rect: TileRect,
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
    // Text on a hue is whichever of Ink/White reads better on *that* hue, not a fixed White.
    val textColor = hue.contrastingText()
    val type = LocalAzphaltType.current

    LaunchedEffect(rect) {
        launchAll(
            { x.animateTo(rect.x, tween(TILE_MORPH_MS, easing = easing)) },
            { y.animateTo(rect.y, tween(TILE_MORPH_MS, easing = easing)) },
            { w.animateTo(rect.w, tween(TILE_MORPH_MS, easing = easing)) },
            { h.animateTo(rect.h, tween(TILE_MORPH_MS, easing = easing)) }
        )
    }

    with(density) {
        val showLabel = w.value.toDp() > LABEL_MIN_WIDTH_DP.dp
        Box(
            Modifier
                .offset { IntOffset(x.value.toInt(), y.value.toInt()) }
                .width(w.value.toDp())
                .height(h.value.toDp())
                .clip(
                    if (isOpen) {
                        AzphaltSurface.recordTile
                    } else if (w.value.toDp() < 20.dp) {
                        AzphaltSurface.capsule
                    } else {
                        AzphaltSurface.note
                    }
                )
                .background(hue)
                // A rod is too thin to carry its name, so it is spoken instead.
                .then(if (showLabel) Modifier else Modifier.semantics { contentDescription = "Folder $label" })
                .clickable(
                    onClickLabel = if (isOpen) "Go inside $label" else "Open $label",
                    role = Role.Button
                ) { if (isOpen) onOpenDescend() else onClick() }
                .padding(if (isOpen) 14.dp else 6.dp),
            contentAlignment = if (isOpen) Alignment.TopStart else Alignment.BottomStart
        ) {
            if (showLabel) {
                Column {
                    Text(
                        label.uppercase(),
                        color = textColor,
                        style = if (isOpen) type.capsule else type.endCap,
                        // Closed tiles are ~100dp squares: three lines of 11sp, then an ellipsis,
                        // rather than a name clipped mid-word at two.
                        maxLines = if (isOpen) 1 else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isOpen) {
                        Text(
                            "TAP TO GO INSIDE",
                            color = textColor,
                            style = type.endCap,
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
