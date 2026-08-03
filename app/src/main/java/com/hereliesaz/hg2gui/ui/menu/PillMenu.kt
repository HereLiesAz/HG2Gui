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
 *
 * Choreography (see "HG2Gui Menu Style Guide"):
 *   1. Tapping a host sends the whole stack left. The host stops with its RIGHT END at 34% of
 *      the screen; every other pill leaves entirely.
 *   2. The host then drops to the bottom of the screen and stays pinned there.
 *   3. Children cascade UPWARD from it. Each starts hidden behind the pill before it (the first
 *      behind the host, on the host's own row), swings a full 360° hinged on an end — alternating
 *      left end, right end — and lifts exactly one row during the final 10% of its swing.
 *   4. Strictly sequential: a pill does not begin until the one before it has landed.
 *   5. Returning to a stack is animated too: the pills slide back in from off-screen left.
 */

// ---------------------------------------------------------------- tokens

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

    /** Hue is assigned by hashing the identifier — it carries no meaning. */
    fun hueOf(id: String) = (id.hashCode().let { if (it < 0) -it else it }) % hues.size

    // Three times as fast, linear throughout — no eased curves.
    const val SLIDE_MS = 420 / 3
    const val DROP_MS = 420 / 3
    const val SWING_MS = 520 / 3
    const val LIFT_FRACTION = 0.90f   // the lift happens in the last 10%
}

private val PILL_HEIGHT = 17.dp
private val ROW_PITCH = 20.dp        // pill + gap
private val OVERHANG = 62.dp         // how far a stack pill runs off the left edge
private const val HOST_RIGHT_EDGE = 0.34f
private const val CHILD_LEFT = 0.30f
private const val CHILD_WIDTH = 0.34f

// ---------------------------------------------------------------- model

data class MenuNode(
    val id: String,
    val label: String,
    val cap: String? = null,
    val children: List<MenuNode> = emptyList()
)

private sealed interface Phase {
    object Browsing : Phase
    data class Leaving(val hostId: String) : Phase
    data class Open(val hostId: String, val selectedChild: String?) : Phase
}

// ---------------------------------------------------------------- menu

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
                Column(
                    Modifier.fillMaxSize().padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.Bottom)
                ) {
                    roots.forEachIndexed { i, node ->
                        StackPill(
                            node = node,
                            index = i,
                            leaving = leavingHost != null,
                            isHost = node.id == leavingHost,
                            entering = leavingHost == null,
                            onClick = {
                                phase = Phase.Leaving(node.id)
                                tokens = listOf(node.label)
                                scope.launch {
                                    delay(Azphalt.SLIDE_MS.toLong())
                                    phase = Phase.Open(node.id, null)
                                }
                            }
                        )
                    }
                }
            }

            is Phase.Open -> {
                val host = roots.first { it.id == p.hostId }
                val rowsBelow = roots.size - 1 - roots.indexOf(host)
                val chain = buildList {
                    host.children.forEach { child ->
                        add(child to false)
                        if (child.id == p.selectedChild) child.children.forEach { add(it to true) }
                    }
                }

                ChildChain(
                    chain = chain,
                    hueOwner = host.id,
                    onPick = { node, isLeaf ->
                        tokens = if (isLeaf) listOf(host.label, node.label) else listOf(node.label)
                        if (!isLeaf) phase = Phase.Open(host.id, node.id)
                    }
                )

                HostPill(
                    node = host,
                    rowsBelow = rowsBelow,
                    onClick = { phase = Phase.Browsing }
                )
            }
        }
    }
}

// ---------------------------------------------------------------- the stack (browsing)

@Composable
private fun StackPill(
    node: MenuNode,
    index: Int,
    leaving: Boolean,
    isHost: Boolean,
    entering: Boolean,
    onClick: () -> Unit
) {
    // fraction of the parent width; -1.7 is fully off the left edge
    val target = when {
        !leaving -> 0f
        isHost -> HOST_RIGHT_EDGE - 1f   // right end parks at 34%
        else -> -1.7f
    }
    val offset = remember { Animatable(if (entering) -1.7f else 0f) }

    LaunchedEffect(leaving, entering) {
        if (entering) offset.animateTo(
            0f,
            tween(Azphalt.SLIDE_MS, delayMillis = index * 70, easing = LinearEasing)
        ) else offset.animateTo(target, tween(Azphalt.SLIDE_MS, easing = LinearEasing))
    }

    Pill(
        label = node.label,
        cap = node.cap,
        hue = Azphalt.hueOf(node.id),
        selected = leaving && isHost,
        modifier = Modifier
            .fillMaxWidth(0.66f)          // never past two thirds
            .offsetByFractionOfParent(offset.value)
            .padding(start = 0.dp)
            .absoluteBleed(OVERHANG)
            .clickable(enabled = !leaving, onClick = onClick)
    )
}

// ---------------------------------------------------------------- the host

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

// ---------------------------------------------------------------- the children

@Composable
private fun ChildChain(
    chain: List<Pair<MenuNode, Boolean>>,
    hueOwner: String,
    onPick: (MenuNode, Boolean) -> Unit
) {
    val density = LocalDensity.current
    val pitchPx = with(density) { ROW_PITCH.toPx() }

    Box(Modifier.fillMaxSize()) {
        chain.forEachIndexed { i, (node, isLeaf) ->
            // rotation: 360° hinged on alternating ends; lift: one row, in the last 10%
            val turn = remember(node.id, i) { Animatable(if (i % 2 == 0) -360f else 360f) }
            val lift = remember(node.id, i) { Animatable(0f) }

            LaunchedEffect(node.id, i) {
                delay(
                    (Azphalt.DROP_MS + i * Azphalt.SWING_MS).toLong()   // strictly sequential
                )
                launch {
                    turn.animateTo(0f, keyframes {
                        durationMillis = Azphalt.SWING_MS
                        (if (i % 2 == 0) -360f else 360f) at 0 using LinearEasing
                        (if (i % 2 == 0) -36f else 36f) at (Azphalt.SWING_MS * 0.9f).toInt() using LinearEasing
                        0f at Azphalt.SWING_MS
                    })
                }
                launch {
                    // holds its predecessor's row, then rises exactly one row at the tail
                    lift.animateTo(-pitchPx * i, keyframes {
                        durationMillis = Azphalt.SWING_MS
                        (-pitchPx * (i - 1).coerceAtLeast(0)) at (Azphalt.SWING_MS * 0.9f).toInt() using LinearEasing
                    })
                }
            }

            Pill(
                label = node.label,
                cap = node.cap,
                hue = Azphalt.hueOf(hueOwner),
                selected = false,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(CHILD_WIDTH)
                    .offsetByFractionOfParent(CHILD_LEFT)
                    .zIndex(50f - i)                       // hidden behind the pill before it
                    .graphicsLayer {
                        transformOrigin =
                            if (i % 2 == 0) TransformOrigin(0f, 0.5f) else TransformOrigin(1f, 0.5f)
                        rotationZ = turn.value
                        translationY = lift.value
                    }
                    .clickable { onPick(node, isLeaf) }
            )
        }
    }
}

// ---------------------------------------------------------------- the capsule

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
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End)  // label rides the right
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

// ---------------------------------------------------------------- helpers

/** Offsets by a fraction of the PARENT's width — the pills are positioned in screen fractions. */
private fun Modifier.offsetByFractionOfParent(fraction: Float) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.placeRelative((constraints.maxWidth * fraction).toInt(), 0)
    }
}

/** Lets a pill run past the left edge of its parent. */
private fun Modifier.absoluteBleed(by: Dp) = layout { measurable, constraints ->
    val extra = by.roundToPx()
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra))
    layout(placeable.width - extra, placeable.height) { placeable.placeRelative(-extra, 0) }
}
