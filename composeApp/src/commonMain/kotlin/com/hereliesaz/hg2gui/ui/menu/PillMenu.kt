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
private const val HOST_RIGHT_EDGE = 0.34f
private const val CHILD_LEFT = 0.30f
private const val CHILD_WIDTH = 0.34f

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
    var tokens by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    Box(modifier.fillMaxSize()) {
        when (val p = phase) {
            is Phase.Browsing, is Phase.Leaving -> {
                val leavingHost = (p as? Phase.Leaving)?.hostId
                Box(Modifier.fillMaxSize().padding(bottom = 12.dp)) {
                    roots.forEachIndexed { i, node ->
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
                                    phase = Phase.Open(node.id)
                                }
                            }
                        )
                    }
                }
            }

            is Phase.Open -> {
                val host = roots.first { it.id == p.hostId }
                val rowsBelow = roots.size - 1 - roots.indexOf(host)

                ChildChain(
                    parent = host,
                    baseRow = 0,
                    hueOwner = host.id,
                    pathPrefix = emptyList(),
                    onPick = { path ->
                        tokens = path.map { it.label }
                        onRun(tokens)
                    }
                )

                HostPill(
                    node = host,
                    rowsBelow = rowsBelow,
                    onClick = {
                        phase = Phase.Browsing
                        tokens = emptyList()
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
    // Animatable driving translationY, nothing else places it there. It starts one pitch below
    // its resting row so entering the stack is what puts it on that row, not a side effect of it
    // already being there.
    val density = LocalDensity.current
    val pitchPx = with(density) { ROW_PITCH.toPx() }
    val lift = remember { Animatable(-pitchPx * (row - 1)) }
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
                .fillMaxWidth(0.66f)
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
                .fillMaxWidth(0.66f)
                .offsetByFractionOfParent(HOST_RIGHT_EDGE - 1f)
                .graphicsLayer { translationY = drop.value * density }
                .clickable(onClick = onClick)
        )
    }
}

/*
 * A band of sibling pills cascading up from `parent`'s own row (`baseRow`), exactly like the
 * very first band cascades up from the host. Tapping a pill with children makes it the new
 * anchor: its siblings play the same "leaving" motion the root stack uses when a host is
 * chosen, and a fresh ChildChain recurses for its children — based at that pill's own row, so
 * the new band stacks next to it rather than growing the total height. Tapping the anchor
 * again undoes the drill: its siblings slide back in, mirroring the stack's own re-entry.
 */
@Composable
private fun ChildChain(
    parent: MenuNode,
    baseRow: Int,
    hueOwner: String,
    pathPrefix: List<MenuNode>,
    onPick: (path: List<MenuNode>) -> Unit
) {
    var selected by remember(parent.id) { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        parent.children.forEachIndexed { idx, child ->
            key(child.id) {
                val isSelected = child.id == selected
                ChildPill(
                    node = child,
                    localIndex = idx,
                    baseRow = baseRow,
                    hueOwner = hueOwner,
                    leaving = selected != null && !isSelected,
                    onClick = {
                        if (isSelected) {
                            selected = null
                            onPick(pathPrefix)
                        } else {
                            onPick(pathPrefix + child)
                            if (child.children.isNotEmpty()) selected = child.id
                        }
                    }
                )
            }
        }
    }

    val sel = selected
    if (sel != null) {
        val selIdx = parent.children.indexOfFirst { it.id == sel }
        val selNode = parent.children[selIdx]
        ChildChain(
            parent = selNode,
            baseRow = baseRow + selIdx,
            hueOwner = hueOwner,
            pathPrefix = pathPrefix + selNode,
            onPick = onPick
        )
    }
}

@Composable
private fun ChildPill(
    node: MenuNode,
    localIndex: Int,
    baseRow: Int,
    hueOwner: String,
    leaving: Boolean,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val pitchPx = with(density) { ROW_PITCH.toPx() }

    val absoluteRow = baseRow + localIndex
    // The row this pill sits behind until its own turn: the anchor's row for the first child
    // of a band, otherwise the row the pill immediately before it lands on.
    val predecessorRow = baseRow + (localIndex - 1).coerceAtLeast(0)

    val turn = remember(node.id) { Animatable(if (localIndex % 2 == 0) -360f else 360f) }
    val lift = remember(node.id) { Animatable(-pitchPx * predecessorRow) }
    val leaveOffset = remember(node.id) { Animatable(0f) }

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
            lift.animateTo(-pitchPx * absoluteRow, tween(Azphalt.SWING_MS, easing = LinearEasing))
        }
    }

    // Siblings of a newly-chosen pill leave exactly like the root stack's unselected pills do;
    // toggling the choice off brings them back the same way the stack re-enters.
    LaunchedEffect(leaving) {
        leaveOffset.animateTo(if (leaving) -1.7f else 0f, tween(Azphalt.SLIDE_MS, easing = LinearEasing))
    }

    Box(Modifier.fillMaxSize().zIndex(50f - absoluteRow)) {
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
                }
                .clickable(onClick = onClick)
        )
    }
}

@Composable
private fun Pill(
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
