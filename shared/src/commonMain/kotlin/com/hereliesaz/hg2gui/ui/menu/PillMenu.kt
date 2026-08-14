package com.hereliesaz.hg2gui.ui.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
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
import kotlin.math.roundToInt

/*
 * The HG2Gui suggestion menu — the Azphalt capsule as a key.
 */

object Azphalt {
    val Ink = Color(0xFF1E1A17)
    // The real Azphalt design-system token is --yellow-bright: #F0D42A - distinct from --page
    // (#E8C81E, the ground colour). Every existing use of this constant is as a text/accent
    // colour on ink (selected pills, trail crumbs, badges), which is exactly what --yellow-bright
    // is for; it was previously set to --page's value by mistake.
    val Yellow = Color(0xFFF0D42A)
    val White = Color(0xFFFFFFFF)

    // Fourteen hues in fixed assignment order (hueOf hashes an id into this list - color carries
    // no meaning beyond telling a stack of pills apart). The first ten sit on the default yellow
    // ground; gray/sage/tan/brown extend the set for the rarer grounds and category recolors -
    // per the HG2Gui style guide's "01 - Ground" / "02 - Palette" sections.
    val hues = listOf(
        Color(0xFF6B4FBB), Color(0xFF2FA9C4), Color(0xFF1F9E86), Color(0xFF5AAE34),
        Color(0xFFD9A21C), Color(0xFFD9762A), Color(0xFFC6392F), Color(0xFFB03A6E),
        Color(0xFF8E4FA8), Color(0xFF2E6FB7),
        Color(0xFF8A9296), Color(0xFF4E7D6E), Color(0xFFC9A45C), Color(0xFF8C6E4E)
    )
    val caps = listOf(
        Color(0xFF4B3489), Color(0xFF1F7B90), Color(0xFF137060), Color(0xFF3E7D22),
        Color(0xFFA2760F), Color(0xFFA6551A), Color(0xFF93251D), Color(0xFF7F2A4D),
        Color(0xFF653578), Color(0xFF1E4E85),
        Color(0xFF616A6E), Color(0xFF365A4E), Color(0xFF96762F), Color(0xFF634B31)
    )
    val hueNames = listOf(
        "violet", "cyan", "teal", "green", "amber", "orange", "red", "magenta", "purple", "blue",
        "gray", "sage", "tan", "brown"
    )

    fun hueOf(id: String) = (id.hashCode().let { if (it < 0) -it else it }) % hues.size

    /** A named background - "the page is yellow. Not black, not white." - plus its own fold
     *  gradient. [weight] controls how often [randomGround] picks it; Mustard is weighted far
     *  heavier since it's the primary. */
    data class Ground(val name: String, val page: Color, val foldLight: Color, val foldDark: Color, val weight: Int = 1)

    // Mustard's fold-light/fold-dark are the style guide's own literal tokens. The other six
    // grounds are given as a single reference swatch each (no separate fold tokens in the
    // source) - foldLight/foldDark are derived from that one swatch with a fixed lighten/darken
    // step, matching Mustard's own light/dark offset, until real per-ground tokens exist.
    val grounds: List<Ground> = listOf(
        Ground("Mustard", Color(0xFFE8C81E), Color(0xFFF2D82C), Color(0xFFD9B615), weight = 6),
        Ground("Maroon", Color(0xFF8F1F34), Color(0xFFA22940), Color(0xFF7A1A2C)),
        Ground("Navy", Color(0xFF163A63), Color(0xFF204B7C), Color(0xFF0F2C4C)),
        Ground("Cerulean", Color(0xFF2D6EA8), Color(0xFF3C82C2), Color(0xFF215A8C)),
        Ground("Teal", Color(0xFF1D6B62), Color(0xFF267F74), Color(0xFF14554E)),
        Ground("Pink", Color(0xFFD4728F), Color(0xFFDD879F), Color(0xFFC15D7A)),
        Ground("Olive", Color(0xFF8F8A2E), Color(0xFFA29C36), Color(0xFF747024))
    )

    /** Picks a ground, weighted per [Ground.weight], optionally never returning [exclude] - used
     *  for a mid-session reroll so it doesn't just pick the same ground back. */
    fun randomGround(exclude: Ground? = null): Ground {
        val pool = grounds.filter { it !== exclude }.ifEmpty { grounds }
        val total = pool.sumOf { it.weight }
        val roll = (0 until total).random()
        return groundFrom(pool, roll)
    }

    private fun groundFrom(pool: List<Ground>, rollIn: Int): Ground {
        var roll = rollIn
        for (g in pool) {
            if (roll < g.weight) return g
            roll -= g.weight
        }
        return pool.last()
    }

    /** Max ground rerolls in one app session - "may reroll once, twice at most" per the style
     *  guide, so a long session doesn't feel static without ever feeling arbitrary. Process-wide
     *  state (not `remember`-scoped) since EditorScreen runs in its own Activity
     *  (EditorActivity) with its own composition - a per-composition roll would let the editor
     *  disagree with the rest of the app about what ground is current. */
    const val MAX_GROUND_REROLLS = 2

    var currentGround: Ground by mutableStateOf(randomGround())
        private set
    private var groundRerollsUsed = 0

    val canRerollGround: Boolean get() = groundRerollsUsed < MAX_GROUND_REROLLS

    /** Call only when no pill gesture (drag, swing, cascade) is in flight - a no-op past
     *  [MAX_GROUND_REROLLS]. */
    fun rerollGround() {
        if (!canRerollGround) return
        currentGround = randomGround(exclude = currentGround)
        groundRerollsUsed++
    }

    /** A coordinated multi-hue combo pulled from a single film scene, for a screen that needs
     *  more than one hue to cohere (a header plus its pills) - see the style guide's "02b -
     *  Reference groupings". Picking freehand is the thing this replaces. */
    data class Grouping(val headerBg: Color, val hues: List<Color>)

    val groupings: Map<String, Grouping> = mapOf(
        "Vogon stage" to Grouping(Color(0xFF8B6420), listOf(Color(0xFFC6392F), Color(0xFF2E6FB7), Color(0xFF0F2447))),
        "Oolon Colluphid" to Grouping(Color(0xFF8B3350), listOf(Color(0xFF2E6FB7), Color(0xFFD9A21C), Color(0xFFC9A45C))),
        "Babel fish" to Grouping(Color(0xFF8B0021), listOf(Color(0xFFB03A6E), Color(0xFFD9762A), Color(0xFF8C6E4E))),
        "Improbability party" to Grouping(Color(0xFF123A52), listOf(Color(0xFF5AAE34), Color(0xFFC6392F), Color(0xFF8E4FA8))),
        "Improbability den" to Grouping(Color(0xFF4E7D6E), listOf(Color(0xFF8C6E4E), Color(0xFF8A9296), Color(0xFF365A4E))),
        "Hyperspace" to Grouping(Color(0xFF163A63), listOf(Color(0xFF8A9296), Color(0xFFC9A45C), Color(0xFFC6392F))),
        "Point-of-View Gun" to Grouping(Color(0xFFB03A6E), listOf(Color(0xFFC6392F), Color(0xFF5AAE34), Color(0xFF8B6420)))
    )

    const val SLIDE_MS = 420 / 3
    const val DROP_MS = 420 / 3
    const val SWING_MS = 520 / 3
    const val LIFT_FRACTION = 0.90f
}

/** The flat two-stop page gradient for this ground - page/foldDark/page, the same shape every
 *  screen's local PageYellow constant used before a shared rotating ground existed. Top-level
 *  (not nested in [Azphalt]) so it's callable as `Azphalt.currentGround.pageBrush()`. */
fun Azphalt.Ground.pageBrush(): Brush = Brush.linearGradient(0f to page, 0.5f to foldDark, 1f to page)

private val PILL_HEIGHT = 17.dp
private val ROW_PITCH = 20.dp
private val OVERHANG = 62.dp
private const val HOST_WIDTH = 0.66f
private const val HOST_RIGHT_EDGE = 0.34f
private const val CHILD_LEFT = 0.30f
private const val CHILD_WIDTH = 0.34f
// Menu Style Guide rule 8 (4%-per-depth width variation) is NOT implemented here. StackPill's
// resting-state math was confirmed safe to vary by hand-derivation, but HostPill's constraint
// propagation (offsetByFractionOfParent / fillMaxWidth / absoluteBleed interacting to park the
// host at HOST_RIGHT_EDGE = 34%) couldn't be confidently resolved without a device to verify
// against - HostPill's width/offset has been hand-tuned across two prior PRs (#91/#92), and this
// gap is left open rather than risk regressing it on an unverified derivation.
// HostPill is HOST_WIDTH wide, offset left by (1 - HOST_RIGHT_EDGE) * HOST_WIDTH, so its right
// edge lands at HOST_WIDTH * HOST_RIGHT_EDGE of the full width. A band of children clears that
// safely by rising into row 1+, but the trail row shares row 0 with the host, so it has to start
// past that edge instead - lining up with the band's own left edge (which sits well inside that
// span) would overlap the host it's sitting next to.
private const val TRAIL_LEFT_OF_FULL = HOST_WIDTH * HOST_RIGHT_EDGE + 0.02f
// Row 0 is reserved for the host and the trail of picks below it; every band of choices fans
// out starting one row above that, never on top of it.
private const val BAND_BASE_ROW = 1

/**
 * A stack (root pills or a child band) fans up row by row from a shared base with no cap on how
 * many rows exist - a category discovered live from PATH can easily hold more than one screen's
 * height at [ROW_PITCH] apart. Since every pill in a stack is positioned by an absolute animated
 * `translationY` rather than real flow layout (so the "fly up from a shared anchor" entrance
 * motion works), a stock `verticalScroll`/`LazyColumn` can't be dropped in - Compose would size
 * the scroll region to each pill's own (viewport-sized) Box, not to the stack's actual extent.
 * This adds a plain drag-tracked offset instead, applied on top of each pill's own animated lift.
 * Drag direction is "content follows the finger" (drag down reveals rows further up the stack,
 * same as pulling down to see earlier messages in a chat) - unverified on a real device, since
 * none was available while building this; a backwards feel is a one-line sign flip, not a
 * structural fix.
 */
// Row 0 - where a stack's front pill rests - is also where a host's trail of picks lives once
// one is open. [StackScroll.alignedRow] names whichever row currently sits at that fixed spot,
// which reads as "primed" (ink) purely as a cosmetic marker of what's nearest the front of the
// stack - it never fires anything by itself. Every pill is tappable wherever it sits in the
// band; scrolling never substitutes for the tap.
private class StackScroll(val modifier: Modifier, val offsetPx: Float, val alignedRow: Int)

@Composable
private fun rememberStackScroll(): StackScroll {
    val density = LocalDensity.current
    val pitchPx = with(density) { ROW_PITCH.toPx() }
    var offsetPx by remember { mutableStateOf(0f) }
    // Unbounded on both ends - dragging or flinging past either end of the stack's own content
    // reveals blank space above the top row or below the bottom one, rather than stopping dead
    // at the content edge. Nothing auto-corrects it back: it stays wherever the gesture leaves
    // it, the same "no bounce, no self-correcting" house rule the settle already follows.
    val scrollState = rememberScrollableState { delta ->
        offsetPx += delta
        delta
    }
    val flingBehavior = rememberSlotFlingBehavior(pitchPx = pitchPx) { offsetPx }
    val modifier = Modifier.scrollable(
        orientation = Orientation.Vertical,
        state = scrollState,
        flingBehavior = flingBehavior
    )
    val alignedRow = if (pitchPx > 0f) (offsetPx / pitchPx).roundToInt() else 0
    return StackScroll(modifier, offsetPx, alignedRow)
}

/**
 * Coasts on the same natural deceleration any fling has - no extra pull while it's still moving
 * fast - then settles onto the nearest row boundary, the tick a slot-machine reel or a
 * wheel-of-fortune wheel has as it slows down. The trick is computing where a plain, un-snapped
 * coast would already come to rest and rounding *that* to the nearest row, rather than stopping
 * the coast early to snap separately: a hard flick's natural stopping point is many rows away, so
 * rounding it barely nudges where a long coast ends up landing; a gentle release's stopping point
 * is right where the finger let go, so the same rounding snaps it onto the nearest row almost
 * immediately. One formula, and the "less effect the faster it's going" feel falls out of it for
 * free instead of needing a separate velocity threshold.
 */
@Composable
private fun rememberSlotFlingBehavior(pitchPx: Float, currentOffsetPx: () -> Float): FlingBehavior {
    val decay = rememberSplineBasedDecay<Float>()
    return remember(pitchPx, decay) { SlotFlingBehavior(pitchPx, currentOffsetPx, decay) }
}

private class SlotFlingBehavior(
    private val pitchPx: Float,
    private val currentOffsetPx: () -> Float,
    private val decay: DecayAnimationSpec<Float>
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (pitchPx <= 0f) return initialVelocity
        val current = currentOffsetPx()
        val naturalTarget = decay.calculateTargetValue(0f, initialVelocity)
        val snapped = ((current + naturalTarget) / pitchPx).roundToInt() * pitchPx
        val distance = snapped - current
        var traveled = 0f
        // No bounce: the house rule is nothing in this menu ever overshoots and corrects, and a
        // spring with any bounce would visibly overshoot the row it's settling onto.
        AnimationState(initialValue = 0f, initialVelocity = initialVelocity).animateTo(
            targetValue = distance,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        ) {
            scrollBy(value - traveled)
            traveled = value
        }
        return 0f
    }
}

data class MenuNode(
    val id: String,
    val label: String,
    val cap: String? = null,
    val children: List<MenuNode> = emptyList(),
    // Resolves this node's real children on demand - a directory listing, a PATH scan - instead
    // of a list fully materialized up front. Takes priority over [children] when present. Called
    // once per navigation into this node (see PillMenu's `effectiveChildren`), not on every
    // recomposition, since it can do real I/O.
    val resolveChildren: (() -> List<MenuNode>)? = null,
    // The token text this node contributes when picked, if different from what's shown on the
    // pill - e.g. a file leaf displays just its name but contributes its full path.
    val value: String? = null,
    // False for purely navigational nodes (a "browse for a file" trigger, a directory on the way
    // to one) that should never themselves become part of the command line - only the leaf a
    // pick chain actually resolves to should.
    val emitsToken: Boolean = true,
    // Non-null for a pill that launches a multi-step UI-state wizard (e.g. the ssh "new…" leaf,
    // which collects host/port/key through SessionUiState's prompt machinery) instead of
    // contributing a token or drilling into more children. Picking it still settles into the
    // trail like any other pick, but PillMenu calls onWizard instead of onRun for it.
    val wizardId: String? = null,
    // True for a wizard that anchors its own animation to where this pick's trail crumb lands
    // (e.g. the Select File/Folder pill running the screen's perimeter from that exact spot) -
    // onWizard fires only after the crumb has actually settled into the trail and reported its
    // position via onCrumbPositioned, instead of firing immediately like every other wizard.
    val settleBeforeWizard: Boolean = false,
)

/** The literal command-line text this node contributes when picked, or null if it never does. */
private fun MenuNode.tokenValue(): String? = if (emitsToken) (value ?: label) else null

/** True once picking this node fully resolves a parameter - nothing more to drill into. */
private fun MenuNode.isTerminal(): Boolean = children.isEmpty() && resolveChildren == null

private sealed interface Phase {
    object Browsing : Phase
    data class Leaving(val hostId: String) : Phase
    data class Open(val hostId: String) : Phase
}

@Composable
fun PillMenu(
    roots: List<MenuNode>,
    modifier: Modifier = Modifier,
    // isTerminal is true only when this call reports a pick that just fully resolved a
    // parameter (nothing left to drill into) - the signal a caller needs to auto-run instead of
    // waiting for a separate confirmation.
    onRun: (tokens: List<String>, isTerminal: Boolean) -> Unit = { _, _ -> },
    // Called instead of onRun when the picked child has a wizardId - the caller owns whatever
    // multi-step flow that id names.
    onWizard: (wizardId: String) -> Unit = {},
    // Reports a trail crumb's on-screen rect (root coordinates) every time it's laid out - only
    // consumed by a settleBeforeWizard pick that needs to anchor an animation to it.
    onCrumbPositioned: (id: String, rect: Rect) -> Unit = { _, _ -> }
) {
    var phase by remember { mutableStateOf<Phase>(Phase.Browsing) }
    // Everything picked below the root host. Each pick drops out of the band it was chosen
    // from and settles here for good; the band above always cascades fresh for whatever is
    // currently last, so drilling deeper never grows the total height - it just swaps what's
    // fanned out for the next choice's own children.
    var trail by remember { mutableStateOf<List<MenuNode>>(emptyList()) }
    var tokens by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    fun openHost(node: MenuNode) {
        phase = Phase.Leaving(node.id)
        tokens = emptyList()
        onRun(tokens, false)
        scope.launch {
            delay(Azphalt.SLIDE_MS.toLong())
            trail = emptyList()
            phase = Phase.Open(node.id)
        }
    }

    Box(modifier.fillMaxSize()) {
        when (val p = phase) {
            is Phase.Browsing, is Phase.Leaving -> {
                val leavingHost = (p as? Phase.Leaving)?.hostId
                val stackScroll = rememberStackScroll()

                Box(Modifier.fillMaxSize().padding(bottom = 12.dp).then(stackScroll.modifier)) {
                    roots.forEachIndexed { i, node ->
                        // Roots aren't always a fixed static list - a contextual entry like the
                        // suggestions host can appear or disappear between recompositions - so
                        // each pill's remembered animation state has to travel with its id, not
                        // its position in the list, or a size change would hand one pill's
                        // in-flight state to a different node.
                        key(node.id) {
                            val row = roots.size - 1 - i
                            StackPill(
                                node = node,
                                row = row,
                                scrollOffsetPx = stackScroll.offsetPx,
                                leaving = leavingHost != null,
                                isHost = node.id == leavingHost,
                                entering = leavingHost == null,
                                aligned = row == stackScroll.alignedRow,
                                onClick = { openHost(node) }
                            )
                        }
                    }
                }
            }

            is Phase.Open -> {
                // A contextual root (the suggestion/answer host) can vanish from `roots` between
                // recompositions - e.g. picking it clears the input text that made it appear -
                // while this phase is still mid-animation toward it. Closing back to Browsing
                // here avoids crashing on a lookup that can no longer succeed.
                val host = roots.firstOrNull { it.id == p.hostId }
                if (host == null) {
                    LaunchedEffect(p.hostId) {
                        trail = emptyList()
                        phase = Phase.Browsing
                    }
                } else {
                    val rowsBelow = roots.size - 1 - roots.indexOf(host)
                    val anchor = trail.lastOrNull() ?: host
                    // Resolved once per anchor, not on every recomposition - resolveChildren can do
                    // real I/O (a directory listing, a PATH scan), and remember(anchor.id) keeps
                    // that to one call per navigation into this node.
                    val effectiveChildren = remember(anchor.id) { anchor.resolveChildren?.invoke() ?: anchor.children }

                    if (effectiveChildren.isNotEmpty()) {
                        key(anchor.id) {
                            ChildBand(
                                children = effectiveChildren,
                                hueOwner = host.id,
                                onPick = { child ->
                                    if (child.wizardId != null && child.settleBeforeWizard) {
                                        // This wizard anchors an animation to the crumb's actual
                                        // landing spot, so it can't fire until the crumb exists and
                                        // has reported its position - unlike every other wizard,
                                        // which fires immediately below.
                                        scope.launch {
                                            delay((Azphalt.DROP_MS + Azphalt.SWING_MS).toLong())
                                            trail = trail + child
                                            // One extra beat so the new crumb actually gets laid
                                            // out and reports its position before onWizard reads it.
                                            delay(32)
                                            onWizard(child.wizardId)
                                        }
                                    } else {
                                        if (child.wizardId != null) {
                                            onWizard(child.wizardId)
                                        } else {
                                            tokens = (trail + child).mapNotNull { it.tokenValue() }
                                            onRun(tokens, child.isTerminal())
                                        }
                                        scope.launch {
                                            // Let the pick's own drop-and-grow finish, plus one beat
                                            // to settle, before swapping the band for its children -
                                            // so the next cascade always starts after the hand-off is
                                            // visible, never on top of it.
                                            delay((Azphalt.DROP_MS + Azphalt.SWING_MS).toLong())
                                            trail = trail + child
                                        }
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
                            onRun(tokens, false)
                        }
                    )

                    TrailRow(
                        trail = trail,
                        hueOwner = host.id,
                        onTapCrumb = { i ->
                            trail = trail.take(i)
                            tokens = trail.mapNotNull { it.tokenValue() }
                            onRun(tokens, false)
                        },
                        onCrumbPositioned = onCrumbPositioned
                    )
                }
            }
        }
    }
}

@Composable
private fun StackPill(
    node: MenuNode,
    row: Int,
    scrollOffsetPx: Float,
    leaving: Boolean,
    isHost: Boolean,
    entering: Boolean,
    aligned: Boolean,
    onClick: () -> Unit
) {
    val target = when {
        !leaving -> 0f
        isHost -> HOST_RIGHT_EDGE - 1f
        else -> -1.7f
    }
    val offset = remember { Animatable(if (entering) -1.7f else 0f) }

    // One shared clock, no per-pill stagger - "dismissing the host plays the exact same arrival
    // as opening the app... because returning to a stack and arriving at it are the same event."
    // Every root pill's entrance and leaving-stack animation shares this same unstaggered timing.
    LaunchedEffect(leaving, entering) {
        if (entering) offset.animateTo(
            0f,
            tween(Azphalt.SLIDE_MS, easing = LinearEasing)
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
            tween(Azphalt.SLIDE_MS, easing = LinearEasing)
        )
    }

    Box(Modifier.fillMaxSize()) {
        Pill(
            label = node.label,
            cap = node.cap,
            hue = Azphalt.hueOf(node.id),
            // Ink is already what "open"/selected reads as everywhere in the menu; a pill that's
            // scrolled into the fixed row-0 spot gets the same treatment - primed to be tapped
            // next, not yet picked.
            selected = (leaving && isHost) || (aligned && !leaving),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(HOST_WIDTH)
                .offsetByFractionOfParent(offset.value)
                .absoluteBleed(OVERHANG)
                .graphicsLayer { translationY = lift.value + scrollOffsetPx }
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
    children: List<MenuNode>,
    hueOwner: String,
    onPick: (MenuNode) -> Unit
) {
    // No key needed here - the call site already wraps this whole band in key(anchor.id), so a
    // new anchor tears down and recreates this state automatically.
    var selected by remember { mutableStateOf<String?>(null) }
    val stackScroll = rememberStackScroll()

    Box(Modifier.fillMaxSize().then(stackScroll.modifier)) {
        children.forEachIndexed { idx, child ->
            key(child.id) {
                val isSelected = child.id == selected
                ChildPill(
                    node = child,
                    localIndex = idx,
                    hueOwner = hueOwner,
                    scrollOffsetPx = stackScroll.offsetPx,
                    leaving = selected != null && !isSelected,
                    droppingOut = isSelected,
                    alignedRow = stackScroll.alignedRow,
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
    scrollOffsetPx: Float,
    leaving: Boolean,
    droppingOut: Boolean,
    alignedRow: Int,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val pitchPx = with(density) { ROW_PITCH.toPx() }

    val absoluteRow = BAND_BASE_ROW + localIndex
    val aligned = absoluteRow == alignedRow && !leaving && !droppingOut

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
                (if (localIndex % 2 == 0) -36f else 36f) at (Azphalt.SWING_MS * Azphalt.LIFT_FRACTION).toInt() using LinearEasing
                0f at Azphalt.SWING_MS
            })
        }
        launch {
            lift.animateTo(-pitchPx * absoluteRow, keyframes {
                durationMillis = Azphalt.SWING_MS
                (-pitchPx * (absoluteRow - 1).coerceAtLeast(BAND_BASE_ROW)) at (Azphalt.SWING_MS * Azphalt.LIFT_FRACTION).toInt() using LinearEasing
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
            // Same "ink means primed" treatment StackPill gives its own row-0-aligned pill.
            selected = aligned,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(CHILD_WIDTH)
                .offsetByFractionOfParent(CHILD_LEFT + leaveOffset.value)
                .graphicsLayer {
                    transformOrigin =
                        if (localIndex % 2 == 0) TransformOrigin(0f, 0.5f) else TransformOrigin(1f, 0.5f)
                    rotationZ = turn.value
                    translationY = lift.value + scrollOffsetPx
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
private fun TrailRow(
    trail: List<MenuNode>,
    hueOwner: String,
    onTapCrumb: (Int) -> Unit,
    onCrumbPositioned: (id: String, rect: Rect) -> Unit = { _, _ -> }
) {
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
                    TrailCrumb(
                        node = node,
                        hueOwner = hueOwner,
                        onClick = { onTapCrumb(i) },
                        onPositioned = { onCrumbPositioned(node.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrailCrumb(node: MenuNode, hueOwner: String, onClick: () -> Unit, onPositioned: (Rect) -> Unit = {}) {
    Pill(
        label = node.label,
        cap = node.cap,
        hue = Azphalt.hueOf(hueOwner),
        selected = false,
        // Every other pill in the menu gets its width from fillMaxWidth against a fraction of
        // the screen; a Row of these can't do that (they'd all fight for the full row), so this
        // is the one pill sized by content alone - give it a floor so a short label like "ls"
        // doesn't shrink-wrap into something visibly smaller than everything around it.
        modifier = Modifier
            .defaultMinSize(minWidth = 56.dp)
            .clickable(onClick = onClick)
            .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
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
        // "Label and end-cap sit together at the right end of the pill, in that order, 9px
        // apart" - the style guide's own literal gap value.
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.End)
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
