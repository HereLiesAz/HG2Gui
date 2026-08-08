package com.hereliesaz.hg2gui.ui.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * The HG2Gui suggestion menu — the Azphalt capsule as a key.
 */

object Azphalt {
    val Ink = Color(0xFF1E1A17)
    val Yellow = Color(0xFFE8C81E)
    val White = Color(0xFFFFFFFF)

    val hues = listOf(
        Color(0xFF6B4FBB), Color(0xFF2FA9C4), Color(0xFF1F9E86), Color(0xFF5AAE34),
        Color(0xFFD9A21C), Color(0xFFD9762A), Color(0xFFC6392F), Color(0xFFB03A6E),
        Color(0xFF8E4FA8), Color(0xFF2E6FB7)
    )
    val caps = listOf(
        Color(0xFF4B3489), Color(0xFF1F7B90), Color(0xFF137060), Color(0xFF3E7D22),
        Color(0xFFA2760F), Color(0xFFA6551A), Color(0xFF93251D), Color(0xFF7F2A4D),
        Color(0xFF653578), Color(0xFF1E4E85)
    )

    fun hueOf(id: String) = (id.hashCode().let { if (it < 0) -it else it }) % hues.size

    const val SLIDE_MS = 420 / 3
    const val DROP_MS = 420 / 3
    const val SWING_MS = 520 / 3
    const val LIFT_FRACTION = 0.90f
}

private val PILL_HEIGHT = 17.dp
private val ROW_PITCH = 20.dp
private val OVERHANG = 62.dp
private const val HOST_WIDTH = 0.66f
private const val HOST_RIGHT_EDGE = 0.34f
private const val CHILD_LEFT = 0.30f
private const val CHILD_WIDTH = 0.34f
// HostPill is HOST_WIDTH wide, offset left by (1 - HOST_RIGHT_EDGE) * HOST_WIDTH, so its right
// edge lands at HOST_WIDTH * HOST_RIGHT_EDGE of the full width. A band of children clears that
// safely by rising into row 1+, but the trail row shares row 0 with the host, so it has to start
// past that edge instead - lining up with the band's own left edge (which sits well inside that
// span) would overlap the host it's sitting next to.
private const val TRAIL_LEFT_OF_FULL = HOST_WIDTH * HOST_RIGHT_EDGE + 0.02f
// Row 0 is reserved for the host and the trail of picks below it; every band of choices fans
// out starting one row above that, never on top of it.
private const val BAND_BASE_ROW = 1

data class MenuNode(
    val id: String,
    val label: String,
    val cap: String? = null,
    val children: List<MenuNode> = emptyList()
)

private sealed interface Phase {
    object Browsing : Phase
    data class Leaving(val hostId: String) : Phase
    data class Open(val hostId: String) : Phase
}

@Composable
fun PillMenu(
    roots: List<MenuNode>,
    modifier: Modifier = Modifier,
    onRun: (List<String>) -> Unit = {}
) {
    var phase by remember { mutableStateOf<Phase>(Phase.Browsing) }
    // Everything picked below the root host. Each pick drops out of the band it was chosen
    // from and settles here for good; the band above always cascades fresh for whatever is
    // currently last, so drilling deeper never grows the total height - it just swaps what's
    // fanned out for the next choice's own children.
    var trail by remember { mutableStateOf<List<MenuNode>>(emptyList()) }
    var tokens by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    Box(modifier.fillMaxSize()) {
        when (val p = phase) {
            is Phase.Browsing, is Phase.Leaving -> {
                val leavingHost = (p as? Phase.Leaving)?.hostId
                Box(Modifier.fillMaxSize().padding(bottom = 12.dp)) {
                    roots.forEachIndexed { i, node ->
                        // Roots aren't always a fixed static list - a contextual entry like the
                        // suggestions host can appear or disappear between recompositions - so
                        // each pill's remembered animation state has to travel with its id, not
                        // its position in the list, or a size change would hand one pill's
                        // in-flight state to a different node.
                        key(node.id) {
                            StackPill(
                                node = node,
                                index = i,
                                row = roots.size - 1 - i,
                                leaving = leavingHost != null,
                                isHost = node.id == leavingHost,
                                entering = leavingHost == null,
                                onClick = {
                                    phase = Phase.Leaving(node.id)
                                    tokens = emptyList()
                                    onRun(tokens)
                                    scope.launch {
                                        delay(Azphalt.SLIDE_MS.toLong())
                                        trail = emptyList()
                                        phase = Phase.Open(node.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            is Phase.Open -> {
                val host = roots.first { it.id == p.hostId }
                val rowsBelow = roots.size - 1 - roots.indexOf(host)
                val anchor = trail.lastOrNull() ?: host

                if (anchor.children.isNotEmpty()) {
                    key(anchor.id) {
                        ChildBand(
                            parent = anchor,
                            hueOwner = host.id,
                            onPick = { child ->
                                tokens = trail.map { it.label } + child.label
                                onRun(tokens)
                                scope.launch {
                                    // Let the pick's own drop-and-grow finish, plus one beat to
                                    // settle, before swapping the band for its children - so the
                                    // next cascade always starts after the hand-off is visible,
                                    // never on top of it.
                                    delay((Azphalt.DROP_MS + Azphalt.SWING_MS).toLong())
                                    trail = trail + child
                                }
                            }
                        )
                    }
                }

                HostPill(
                    node = host,
                    rowsBelow = rowsBelow,
                    onClick = {
                        phase = Phase.Browsing
                        trail = emptyList()
                        tokens = emptyList()
                        onRun(tokens)
                    }
                )

                TrailRow(
                    trail = trail,
                    hueOwner = host.id,
                    onTapCrumb = { i ->
                        trail = trail.take(i)
                        tokens = trail.map { it.label }
                        onRun(tokens)
                    }
                )
            }
        }
    }
}

@Composable
private fun StackPill(
    node: MenuNode,
    index: Int,
    row: Int,
    leaving: Boolean,
    isHost: Boolean,
    entering: Boolean,
    onClick: () -> Unit
) {
    val target = when {
        !leaving -> 0f
        isHost -> HOST_RIGHT_EDGE - 1f
        else -> -1.7f
    }
    val offset = remember { Animatable(if (entering) -1.7f else 0f) }

    LaunchedEffect(leaving, entering) {
        if (entering) offset.animateTo(
            0f,
            tween(Azphalt.SLIDE_MS, delayMillis = index * 70, easing = LinearEasing)
        ) else offset.animateTo(target, tween(Azphalt.SLIDE_MS, easing = LinearEasing))
    }

    // The row this pill rests on is reached the same way HostPill reaches its own row: a single
    // Animatable driving translationY, nothing else places it there. Every pill starts from the
    // same shared origin - row 0, the bottom of the stack - and rises to its own row, so it reads
    // as pills flying up into a stack rather than each one nudging up off the pill below it.
    val density = LocalDensity.current
    val pitchPx = with(density) { ROW_PITCH.toPx() }
    val lift = remember { Animatable(0f) }
    LaunchedEffect(entering) {
        if (entering) lift.animateTo(
            -pitchPx * row,
            tween(Azphalt.SLIDE_MS, delayMillis = index * 70, easing = LinearEasing)
        )
    }

    Box(Modifier.fillMaxSize()) {
        Pill(
            label = node.label,
            cap = node.cap,
            hue = Azphalt.hueOf(node.id),
            selected = leaving && isHost,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(HOST_WIDTH)
                .offsetByFractionOfParent(offset.value)
                .absoluteBleed(OVERHANG)
                .graphicsLayer { translationY = lift.value }
                .clickable(enabled = !leaving, onClick = onClick)
        )
    }
}

@Composable
private fun HostPill(node: MenuNode, rowsBelow: Int, onClick: () -> Unit) {
    val drop = remember { Animatable(-(ROW_PITCH.value * rowsBelow)) }
    LaunchedEffect(node.id) {
        drop.animateTo(0f, tween(Azphalt.DROP_MS, easing = LinearEasing))
    }
    Box(Modifier.fillMaxSize()) {
        Pill(
            label = node.label,
            cap = node.cap,
            hue = Azphalt.hueOf(node.id),
            selected = true,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(HOST_WIDTH)
                .offsetByFractionOfParent(HOST_RIGHT_EDGE - 1f)
                .graphicsLayer { translationY = drop.value * density }
                .clickable(onClick = onClick)
        )
    }
}

/*
 * A single band of sibling pills fanning up from BAND_BASE_ROW, exactly like the root stack
 * fans up from the host. Tapping a pill hands it off to the trail below: its siblings play the
 * same "leaving" motion the root stack uses when a host is chosen, and the picked pill itself
 * drops to the shared bottom row and grows a little as it settles there - the same "become the
 * anchor" motion HostPill already plays - instead of sliding away like an unpicked sibling.
 * PillMenu swaps this whole band for a fresh one over the pick's own children once that
 * hand-off finishes, so drilling deeper always looks like this exact same band, never a taller
 * one.
 */
@Composable
private fun ChildBand(
    parent: MenuNode,
    hueOwner: String,
    onPick: (MenuNode) -> Unit
) {
    var selected by remember(parent.id) { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        parent.children.forEachIndexed { idx, child ->
            key(child.id) {
                val isSelected = child.id == selected
                ChildPill(
                    node = child,
                    localIndex = idx,
                    hueOwner = hueOwner,
                    leaving = selected != null && !isSelected,
                    droppingOut = isSelected,
                    onClick = {
                        if (selected == null) {
                            selected = child.id
                            onPick(child)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChildPill(
    node: MenuNode,
    localIndex: Int,
    hueOwner: String,
    leaving: Boolean,
    droppingOut: Boolean,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val pitchPx = with(density) { ROW_PITCH.toPx() }

    val absoluteRow = BAND_BASE_ROW + localIndex

    val turn = remember(node.id) { Animatable(if (localIndex % 2 == 0) -360f else 360f) }
    // Every pill in a band rises from the same place: the anchor's own row, exactly where
    // HostPill (or the parent pill that opened this band) already rests. That shared origin is
    // what makes it read as pills flying up out of the anchor into a stack, rather than each one
    // nudging up off the pill before it.
    val lift = remember(node.id) { Animatable(-pitchPx * BAND_BASE_ROW) }
    val leaveOffset = remember(node.id) { Animatable(0f) }
    val scale = remember(node.id) { Animatable(1f) }

    LaunchedEffect(node.id) {
        delay((Azphalt.DROP_MS + localIndex * Azphalt.SWING_MS).toLong())
        launch {
            turn.animateTo(0f, keyframes {
                durationMillis = Azphalt.SWING_MS
                (if (localIndex % 2 == 0) -360f else 360f) at 0 using LinearEasing
                (if (localIndex % 2 == 0) -36f else 36f) at (Azphalt.SWING_MS * 0.9f).toInt() using LinearEasing
                0f at Azphalt.SWING_MS
            })
        }
        launch {
            lift.animateTo(-pitchPx * absoluteRow, keyframes {
                durationMillis = Azphalt.SWING_MS
                (-pitchPx * (absoluteRow - 1).coerceAtLeast(BAND_BASE_ROW)) at (Azphalt.SWING_MS * 0.9f).toInt() using LinearEasing
            })
        }
    }

    // A newly-chosen pill drops to the shared bottom row and grows a little as it settles there;
    // its siblings leave exactly like the root stack's unselected pills do.
    LaunchedEffect(leaving, droppingOut) {
        if (droppingOut) {
            launch { turn.animateTo(0f, tween(Azphalt.DROP_MS, easing = LinearEasing)) }
            launch { lift.animateTo(0f, tween(Azphalt.DROP_MS, easing = LinearEasing)) }
            launch {
                scale.animateTo(1.15f, tween(Azphalt.DROP_MS * 2 / 3, easing = LinearEasing))
                scale.animateTo(1f, tween(Azphalt.DROP_MS / 3, easing = LinearEasing))
            }
        } else if (leaving) {
            leaveOffset.animateTo(-1.7f, tween(Azphalt.SLIDE_MS, easing = LinearEasing))
        }
    }

    Box(Modifier.fillMaxSize().zIndex(if (droppingOut) 100f else 50f - absoluteRow)) {
        Pill(
            label = node.label,
            cap = node.cap,
            hue = Azphalt.hueOf(hueOwner),
            selected = false,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(CHILD_WIDTH)
                .offsetByFractionOfParent(CHILD_LEFT + leaveOffset.value)
                .graphicsLayer {
                    transformOrigin =
                        if (localIndex % 2 == 0) TransformOrigin(0f, 0.5f) else TransformOrigin(1f, 0.5f)
                    rotationZ = turn.value
                    translationY = lift.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clickable(onClick = onClick)
        )
    }
}

/*
 * The chain of picks made below the root host, settled into one row alongside it. Each entry
 * only ever animates once, on the hand-off from ChildPill's own drop-and-grow (which finishes
 * before PillMenu swaps the band in for this), so it reads as one continuous motion rather than
 * a second animation stacked on the first. Tapping any crumb pops the trail back to just before
 * it, re-opening that pill's own band of choices.
 */
@Composable
private fun TrailRow(trail: List<MenuNode>, hueOwner: String, onTapCrumb: (Int) -> Unit) {
    if (trail.isEmpty()) return
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .offsetByFractionOfParent(TRAIL_LEFT_OF_FULL),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            trail.forEachIndexed { i, node ->
                key(node.id) {
                    TrailCrumb(node = node, hueOwner = hueOwner, onClick = { onTapCrumb(i) })
                }
            }
        }
    }
}

@Composable
private fun TrailCrumb(node: MenuNode, hueOwner: String, onClick: () -> Unit) {
    Pill(
        label = node.label,
        cap = node.cap,
        hue = Azphalt.hueOf(hueOwner),
        selected = false,
        // Every other pill in the menu gets its width from fillMaxWidth against a fraction of
        // the screen; a Row of these can't do that (they'd all fight for the full row), so this
        // is the one pill sized by content alone - give it a floor so a short label like "ls"
        // doesn't shrink-wrap into something visibly smaller than everything around it.
        modifier = Modifier.defaultMinSize(minWidth = 56.dp).clickable(onClick = onClick)
    )
}

@Composable
internal fun Pill(
    label: String,
    cap: String?,
    hue: Int,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Azphalt.Ink else Azphalt.hues[hue]
    val fg = if (selected) Azphalt.Yellow else Azphalt.White
    val capBg = if (selected) Azphalt.Yellow else Azphalt.caps[hue]
    val capFg = if (selected) Azphalt.Ink else Azphalt.White

    Row(
        modifier
            .height(PILL_HEIGHT)
            .clip(RoundedCornerShape(percent = 50))
            .background(bg)
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End)
    ) {
        Text(
            text = label.uppercase(),
            color = fg,
            fontSize = 6.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.09.em,
            maxLines = 1,
            textAlign = TextAlign.End
        )
        if (cap != null) {
            Box(
                Modifier
                    .height(10.dp)
                    .defaultMinSize(minWidth = 12.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(capBg)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cap.uppercase(),
                    color = capFg,
                    fontSize = 5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

private fun Modifier.offsetByFractionOfParent(fraction: Float) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.placeRelative((constraints.maxWidth * fraction).toInt(), 0)
    }
}

private fun Modifier.absoluteBleed(by: Dp) = layout { measurable, constraints ->
    val extra = by.roundToPx()
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra))
    layout(placeable.width - extra, placeable.height) { placeable.placeRelative(-extra, 0) }
}
