package com.hereliesaz.hg2gui.ui.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Easing
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
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

    // Mustard's fold-light/fold-dark are the style guide's own literal tokens. The other five
    // grounds are given as a single reference swatch each (no separate fold tokens in the
    // source) - foldLight/foldDark are derived from that one swatch with a fixed lighten/darken
    // step, matching Mustard's own light/dark offset, until real per-ground tokens exist.
    // "Olive" (a dark yellow-green, #8F8A2E) used to be here too - close enough to Mustard's own
    // yellow in hue that rolling it read as "the yellow background is gone" rather than as a
    // deliberately different ground, so it's been dropped from the rotation entirely.
    val grounds: List<Ground> = listOf(
        Ground("Mustard", Color(0xFFE8C81E), Color(0xFFF2D82C), Color(0xFFD9B615), weight = 6),
        Ground("Maroon", Color(0xFF8F1F34), Color(0xFFA22940), Color(0xFF7A1A2C)),
        Ground("Navy", Color(0xFF163A63), Color(0xFF204B7C), Color(0xFF0F2C4C)),
        Ground("Cerulean", Color(0xFF2D6EA8), Color(0xFF3C82C2), Color(0xFF215A8C)),
        Ground("Teal", Color(0xFF1D6B62), Color(0xFF267F74), Color(0xFF14554E)),
        Ground("Pink", Color(0xFFD4728F), Color(0xFFDD879F), Color(0xFFC15D7A))
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
    // Stack Entrances' own Unfold variant - a capsule revealed by growing in length, the same
    // 420ms figure already documented for that primitive in the motion sheet and reused as-is
    // (FLOOD_MS in PillWrapReveal.kt/PillPerimeterReveal.kt) rather than a new duration.
    const val UNFOLD_MS = 420
    // The house curve, "fast out and long settle" - every *page*-level reveal already uses this
    // exact curve inline (PillWrapReveal's WRAP_EASE, PillPerimeterReveal's LEG_EASE); named here
    // once so StackEntrance.Unfold - the one entrance that grows in length rather than moving,
    // and so isn't bound by the menu's own "everything here is linear" rule - can reference it
    // without redeclaring it a fourth time.
    val PAGE_EASE: Easing = CubicBezierEasing(0f, .9f, .1f, 1f)
}

/** The flat two-stop page gradient for this ground - page/foldDark/page, the same shape every
 *  screen's local PageYellow constant used before a shared rotating ground existed. Top-level
 *  (not nested in [Azphalt]) so it's callable as `Azphalt.currentGround.pageBrush()`. */
fun Azphalt.Ground.pageBrush(): Brush = Brush.linearGradient(0f to page, 0.5f to foldDark, 1f to page)

// House rule: text (and any glyph carrying meaning - an icon, an end-cap) always reads as the
// opposite of whatever it sits on, never a fixed color assumed to work against every background.
// Mustard is light enough for Ink text, but four of the six grounds in Azphalt.grounds (Maroon,
// Navy, Teal, and near enough Cerulean) are dark - Ink text hardcoded against those disappears.
// The 0.35 luminance split is the same rough light/dark line sRGB relative luminance conventionally
// uses for a black-vs-white text choice (WCAG's own "which of black/white contrasts more" cutoffs
// straddle this range); exact value only matters near the boundary, where either color still reads.
private const val LEGIBLE_LUMINANCE_SPLIT = 0.35f

/** The one of [Azphalt.Ink] / [Azphalt.White] that actually contrasts with this background,
 *  instead of a color assumed to work against every ground/hue this sits on. Not used for a pill's
 *  own selected state (ink bg + yellow fg) - that pairing is a fixed UI signal, not "text on an
 *  arbitrary background," and stays exactly as designed regardless of ground or hue. */
fun Color.contrastingText(): Color = if (luminance() > LEGIBLE_LUMINANCE_SPLIT) Azphalt.Ink else Azphalt.White

/** The page ground's own text color - [Color.contrastingText] applied to [Azphalt.Ground.page] -
 *  for the many places text/icons sit directly on the rotating page background (or a background
 *  that's just the page lightly tinted, e.g. a translucent-ink chip), rather than on a pill's own
 *  fixed hue. */
val Azphalt.Ground.onPage: Color get() = page.contrastingText()

private val PILL_HEIGHT = 17.dp
private val ROW_PITCH = 20.dp
// Android's own accessibility minimum tap target - PILL_HEIGHT (17dp) is well under it, and rows
// are only ROW_PITCH (20dp) apart besides. Pill pads its *clickable* region out to this without
// touching PILL_HEIGHT itself - see the comment on Pill's own wrapping Box.
private val PILL_TOUCH_TARGET = 48.dp
private val OVERHANG = 62.dp
// Negative Arrangement.spacedBy for TrailRow - each crumb's left edge tucks this far under the
// crumb before it. A quarter of TrailCrumb's own 56dp floor width: enough to read as a deliberate
// overlap (the "each crumb overlapped by the one before it" fan) without hiding a short crumb
// like `ls` entirely behind its neighbor.
private val TRAIL_OVERLAP = (-14).dp
private const val HOST_WIDTH = 0.66f
// HostPill's own right edge lands at exactly HOST_WIDTH * HOST_RIGHT_EDGE of the full width (see
// the comment on that math below) - which, since HostPill is HOST_WIDTH wide, also happens to be
// the fraction of the host's *own* width left on-screen (the rest bleeds off the left edge). At
// 0.34 that was only 34% of the label visible - "Package management" read as an unrecognizable
// "...MANAGEMENT" fragment, not a deliberate bleed. Raised to 0.55 so most host labels stay
// legible; still bleeds some off the left edge, matching the house style used everywhere else in
// this menu (OVERHANG, the root stack's own absoluteBleed), just no longer past the point of
// losing the label's own meaning.
private const val HOST_RIGHT_EDGE = 0.55f
private const val CHILD_LEFT = 0.30f
private const val CHILD_WIDTH = 0.34f
// Menu Style Guide rule 01 (4%-per-row width variation, "so the stack reads as a stack") is
// applied only to StackPill's own resting width below, via ROOT_WIDTH_STEP / rootWidthFraction -
// never to HostPill. HostPill's constraint propagation (offsetByFractionOfParent / fillMaxWidth /
// absoluteBleed interacting to park the host at HOST_RIGHT_EDGE) has been hand-tuned across two
// prior PRs (#91/#92, and the HOST_RIGHT_EDGE value itself once more since); every line of
// HostPill is left exactly as it was, so this variation can never touch that math. It's safe to
// vary StackPill alone because of how offsetByFractionOfParent composes with fillMaxWidth:
// StackPill's fillMaxWidth(fraction) modifier runs *first*, narrowing the constraints handed to
// offsetByFractionOfParent, so with offset 0f (the resting, non-leaving state) a pill's right edge
// simply lands at `fraction` of the full screen width - changing that one fillMaxWidth fraction
// per row is the entire effect, with nothing downstream (absoluteBleed only extends the *left*
// bleed) able to feed back into HostPill's own right-edge math.
private const val ROOT_WIDTH_STEP = 0.04f

/** StackPill's resting width, cycling every five rows per the base cascade's own "cycling every
 *  five" language - triangling 0/1/2/1 steps down from [HOST_WIDTH] keeps every row's right edge
 *  at or below HOST_WIDTH (never above it), so rule 01's "right end never passes two thirds of the
 *  screen width" (HOST_WIDTH = 66% is already just under that) holds for every row, not just row 0. */
private fun rootWidthFraction(row: Int): Float {
    val stepsDown = intArrayOf(0, 1, 2, 1, 2)[row % 5]
    return HOST_WIDTH - ROOT_WIDTH_STEP * stepsDown
}

// Same "so the stack reads as a stack" cycling rootWidthFraction gives the root stack, scaled to
// CHILD_WIDTH's own narrower range - a child band is a stack too, and was the one place still
// rendering every pill at one identical width instead of varying by row.
private const val CHILD_WIDTH_STEP = 0.02f
private fun childWidthFraction(localIndex: Int): Float {
    val stepsDown = intArrayOf(0, 1, 2, 1, 2)[localIndex % 5]
    return CHILD_WIDTH - CHILD_WIDTH_STEP * stepsDown
}
// HostPill is HOST_WIDTH wide, offset left by (1 - HOST_RIGHT_EDGE) * HOST_WIDTH, so its right
// edge lands at HOST_WIDTH * HOST_RIGHT_EDGE of the full width. The trail row shares row 0 with
// the host, and reads as a continuation of it - "each pill overlapped by the one before it," the
// same shingled-fan language TRAIL_OVERLAP already gives every crumb after the first - so the
// trail's own left edge starts *before* that right edge, not after: a plain gap here would be the
// one seam in the chain that doesn't fan like the rest of it. -0.035 is TRAIL_OVERLAP's -14dp
// expressed as roughly the same fraction of a typical phone's width, so the host-to-first-crumb
// overlap reads the same size as every crumb-to-crumb one that follows it.
private const val TRAIL_LEFT_OF_FULL = HOST_WIDTH * HOST_RIGHT_EDGE - 0.035f
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
// band; scrolling never substitutes for the tap. -1 (never a real row) until the user has
// actually dragged or flung the stack at least once - without that guard, whichever pill starts
// out sitting at row 0 (the stack's own resting position, before any input) rendered ink from
// the very first frame, reading as something already auto-selected.
private class StackScroll(val modifier: Modifier, val offsetPx: Float, val alignedRow: Int)

@Composable
private fun rememberStackScroll(rowMin: Int, rowMax: Int, resetKey: Any? = Unit): StackScroll {
    val density = LocalDensity.current
    val pitchPx = with(density) { ROW_PITCH.toPx() }
    // Keyed on [resetKey], not bare `remember` - without it this state lives in the *call site's*
    // own remember scope rather than the current stack's. A child band's call is already wrapped
    // in key(anchor.id) at its own call site, so this is redundant there; the root stack's call
    // has no such wrapper (Phase.Browsing/Leaving is one continuous call site across every visit),
    // so its own scroll offset and "has this actually been scrolled" flag otherwise persist
    // forever - scroll a long stack, close it, reopen a short one, and row 0 still renders primed
    // from a scroll the new stack never had.
    var offsetPx by remember(resetKey) { mutableStateOf(0f) }
    var hasScrolled by remember(resetKey) { mutableStateOf(false) }
    // [offsetPx] is added on top of every pill's own already-correct resting `lift` (row R rests
    // at -pitchPx*R, so translationY = -pitchPx*R + offsetPx; row R lands exactly on the
    // breadcrumb, translationY 0, when offsetPx == +pitchPx*R - always non-negative, since every
    // row index is). A scroll is really the whole rigid stack sliding through that one fixed
    // breadcrumb slot: which row currently sits there changes continuously as offsetPx sweeps
    // from one end to the other, in row order - a pill passing *through* translationY 0 on its
    // way past is normal scrolling, not something to prevent. What must stay bounded is only the
    // two ends of the real content:
    //  - the closest-to-breadcrumb pill (rowMin) at the breadcrumb (offsetPx == pitchPx*rowMin,
    //    0 for the root stack, one pitch for a child band) - the smallest offsetPx worth
    //    reaching, since going lower would show nothing but blank space below where a real row
    //    ever sits.
    //  - the furthest-from-breadcrumb pill (rowMax) at the breadcrumb (offsetPx ==
    //    pitchPx*rowMax) - the largest offsetPx worth reaching, past which there's no further
    //    row to bring into that slot either.
    // Both ends stop dead at their pill, matching the "no bounce, no self-correcting" house rule
    // the settle already follows - unverified on a real device, since none was available while
    // building this; a backwards feel is a one-line sign flip, not a structural fix.
    val minOffsetPx = pitchPx * rowMin
    val maxOffsetPx = pitchPx * rowMax
    val scrollState = rememberScrollableState { delta ->
        hasScrolled = true
        val next = (offsetPx + delta).coerceIn(minOffsetPx, maxOffsetPx)
        val consumed = next - offsetPx
        offsetPx = next
        consumed
    }
    val flingBehavior = rememberSlotFlingBehavior(pitchPx = pitchPx) { offsetPx }
    val modifier = Modifier.scrollable(
        orientation = Orientation.Vertical,
        state = scrollState,
        flingBehavior = flingBehavior
    )
    val alignedRow = if (hasScrolled && pitchPx > 0f) (offsetPx / pitchPx).roundToInt() else -1
    return StackScroll(modifier, offsetPx, alignedRow)
}

/**
 * The *target* row is computed from the same natural-deceleration curve any fling has - no extra
 * pull while it's still moving fast - then rounded to the nearest row boundary, the tick a
 * slot-machine reel or a wheel-of-fortune wheel has as it slows down. The trick is computing
 * where a plain, un-snapped coast would already come to rest and rounding *that* to the nearest
 * row, rather than stopping the coast early to snap separately: a hard flick's natural stopping
 * point is many rows away, so rounding it barely nudges where a long coast ends up landing; a
 * gentle release's stopping point is right where the finger let go, so the same rounding snaps it
 * onto the nearest row almost immediately. One formula, and the "less effect the faster it's
 * going" feel falls out of it for free instead of needing a separate velocity threshold. The
 * actual motion *to* that target is a separate, critically-damped spring (see
 * [SlotFlingBehavior.performFling]) - the decay curve here only ever picks where the spring is
 * headed, it doesn't drive the frame-by-frame motion itself.
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
        // DampingRatioNoBouncy only rules out a spring that overshoots and corrects *back*; it
        // doesn't rule out overshoot itself. A release with velocity carrying past `distance`
        // (e.g. a quick flick that decays to a target just behind where the finger let go) can
        // still cross the target before the spring pulls it to rest there - it just won't bounce
        // past it a second time. In practice this reads as "no bounce" because the common case
        // (velocity already pointed at the target) settles without crossing it.
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
    // Bumped every time the root stack returns to Browsing - the identity the root's own
    // rememberStackScroll resets against (see B5's comment there), and the same identity a fresh
    // StackEntrance is rolled against below, since Phase.Browsing has no hostId of its own to key
    // on the way a child band already keys on its anchor's id.
    var rootArrivalToken by remember { mutableStateOf(0) }
    // Chosen once per stack build and handed down to every root pill - never rolled per pill, or
    // rows in the same arrival could disagree about which entrance they're playing. Root-only:
    // a child band keeps its own fixed cascade, unaffected by this roll.
    val entrance = remember(rootArrivalToken) { StackEntrance.roll() }
    val scope = rememberCoroutineScope()

    fun openHost(node: MenuNode) {
        // Guards the same race ChildBand's own `selected == null` check guards: without it, a
        // second root pill tapped while the first is still mid-Leaving would overwrite `phase`
        // with a new hostId, stranding the first pill's in-flight leave animation and letting two
        // picks fire from the one gesture.
        if (phase != Phase.Browsing) return
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
                val stackScroll = rememberStackScroll(
                    rowMin = 0,
                    rowMax = (roots.size - 1).coerceAtLeast(0),
                    resetKey = rootArrivalToken
                )

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
                                rowCount = roots.size,
                                entrance = entrance,
                                entranceKey = rootArrivalToken,
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
                    // ChildBand's own `selected == null` check only guards against a second tap
                    // landing on one of ITS OWN sibling pills - it has no way to know about
                    // HostPill or TrailRow, which sit outside it and keep accepting taps for the
                    // whole DROP_MS+SWING_MS hand-off delay below, before `trail` actually
                    // updates and swaps in the next band. A tap landing there during that window
                    // still fires against the *old* anchor - e.g. HostPill resetting all the way
                    // back to Browsing - even though a pick already committed and is mid-flight.
                    // Resets itself the instant `anchor.id` actually changes (remember's key),
                    // exactly when the next band takes over.
                    var pendingPick by remember(anchor.id) { mutableStateOf(false) }

                    if (effectiveChildren.isNotEmpty()) {
                        key(anchor.id) {
                            ChildBand(
                                children = effectiveChildren,
                                onPick = { child ->
                                    pendingPick = true
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

                    // TrailRow first, HostPill second: with neither given an explicit zIndex, a
                    // plain Box stacks its children in declaration order, later on top - so
                    // whichever of these two is declared last wins the shared boundary where the
                    // trail's own TRAIL_LEFT_OF_FULL overlap runs it into the host. The host reads
                    // as the anchor the whole trail hangs off of - conceptually "before" crumb
                    // zero, the same way crumb zero sits on top of crumb one below - so it has to
                    // be declared after TrailRow, not before, to stay on top of that first crumb
                    // instead of being covered by it.
                    TrailRow(
                        trail = trail,
                        hueOwner = host.id,
                        onTapCrumb = { i ->
                            if (!pendingPick) {
                                trail = trail.take(i)
                                tokens = trail.mapNotNull { it.tokenValue() }
                                onRun(tokens, false)
                            }
                        },
                        onCrumbPositioned = onCrumbPositioned
                    )

                    HostPill(
                        node = host,
                        rowsBelow = rowsBelow,
                        onClick = {
                            if (!pendingPick) {
                                phase = Phase.Browsing
                                trail = emptyList()
                                tokens = emptyList()
                                rootArrivalToken++
                                onRun(tokens, false)
                            }
                        }
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
    rowCount: Int,
    entrance: StackEntrance,
    entranceKey: Int,
    scrollOffsetPx: Float,
    leaving: Boolean,
    isHost: Boolean,
    entering: Boolean,
    aligned: Boolean,
    onClick: () -> Unit
) {
    val restWidthFraction = if (isHost) HOST_WIDTH else rootWidthFraction(row)
    val target = when {
        !leaving -> 0f
        isHost -> HOST_RIGHT_EDGE - 1f
        else -> -1.7f
    }
    val density = LocalDensity.current
    val pitchPx = with(density) { ROW_PITCH.toPx() }
    // The row a pill rests on is now a fixed value, not an Animatable of its own - only a
    // StackEntrance variant's own riseY (see StackEntranceMotion) supplies any vertical entrance
    // motion, always settling back to 0, so translationY = restLift + riseY.value is correct on
    // every frame without a LaunchedEffect keeping it in sync.
    val restLift = -pitchPx * row
    val geometry = StackRowGeometry(row, rowCount, pitchPx, restWidthFraction)

    // `offset` doubles as the leaving sweep's own translateX and Stack Entrances' `slideX`
    // (fraction of the pill's OWN width, per offsetByFractionOfParent's own semantics - it runs
    // after fillMaxWidth in this modifier chain, so its `constraints.maxWidth` is already this
    // pill's own target width, not the full screen's).
    val motion = remember { StackEntranceMotion() }
    val sway = remember { Animatable(0f) } // degrees, additive wobble - see PillWobble.kt

    // Keyed on the stack's own arrival identity (entranceKey), not `entering` alone: a pill
    // composed while already resting must still animate when a later stack arrives with a new
    // entrance (audit bug B1's fix, generalised - `remember` seeding from a value that changes is
    // the trap here, not any one Animatable in particular).
    LaunchedEffect(leaving, entering, entrance, entranceKey) {
        motion.enterOrLeave(entering, entrance, target, geometry, sway)
    }
    LaunchedEffect(entering, entrance, entranceKey) {
        playStackSway(sway, entering, entrance, row)
    }

    StackPillVisual(
        StackPillContext(node, row, restWidthFraction, restLift, scrollOffsetPx, leaving, isHost, aligned, entrance),
        motion,
        sway,
        onClick
    )
}

/** One row's worth of the geometry a [StackEntrance] variant needs - see [StackEntranceMotion.play]. */
private data class StackRowGeometry(val row: Int, val rowCount: Int, val pitchPx: Float, val restWidthFraction: Float)

/** Everything [StackPillVisual] needs besides the two Animatable bundles - see its own doc. */
private data class StackPillContext(
    val node: MenuNode,
    val row: Int,
    val restWidthFraction: Float,
    val restLift: Float,
    val scrollOffsetPx: Float,
    val leaving: Boolean,
    val isHost: Boolean,
    val aligned: Boolean,
    val entrance: StackEntrance
)

// The wobble accompanies any entrance that travels as a pile. Three are excluded, for the same
// reason: Unfold does not move at all, while Cascade and Deal move one row at a time. A sway
// needs a pile in motion together; a single pill swinging alone has nothing to sway against.
private suspend fun playStackSway(
    sway: Animatable<Float, AnimationVector1D>,
    entering: Boolean,
    entrance: StackEntrance,
    row: Int
) {
    if (!entering) return
    val amplitudeScale = when (entrance) {
        StackEntrance.Unfold, StackEntrance.Cascade, StackEntrance.Deal -> return
        StackEntrance.Rally -> 2f
        else -> 1f
    }
    sway.wobble(row, Azphalt.SLIDE_MS, amplitudeScale)
}

@Composable
private fun StackPillVisual(
    ctx: StackPillContext,
    motion: StackEntranceMotion,
    sway: Animatable<Float, AnimationVector1D>,
    onClick: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Pill(
            label = ctx.node.label,
            cap = ctx.node.cap,
            hue = Azphalt.hueOf(ctx.node.id),
            // Ink is already what "open"/selected reads as everywhere in the menu; a pill that's
            // scrolled into the fixed row-0 spot gets the same treatment - primed to be tapped
            // next, not yet picked.
            selected = (ctx.leaving && ctx.isHost) || (ctx.aligned && !ctx.leaving),
            touchTargetHeight = ROW_PITCH,
            modifier = Modifier
                .align(Alignment.BottomStart)
                // The row-varied width is only for pills staying in the browsing stack - the one
                // becoming the host has to already be HOST_WIDTH wide, matching exactly what
                // HostPill renders once this phase completes, or `target`'s offset below (derived
                // assuming HOST_WIDTH) lands the wrong row's right edge somewhere other than
                // HOST_RIGHT_EDGE and then visibly pops into place the instant HostPill takes over.
                .fillMaxWidth(ctx.restWidthFraction * motion.lenFrac.value)
                .offsetByFractionOfParent(motion.offset.value)
                .absoluteBleed(OVERHANG)
                .graphicsLayer {
                    transformOrigin = when (ctx.entrance) {
                        StackEntrance.Deal -> TransformOrigin(1f, 1f) // hinged bottom-right
                        StackEntrance.Cascade ->
                            if (ctx.row % 2 == 0) TransformOrigin(0f, .5f) else TransformOrigin(1f, .5f)
                        else -> TransformOrigin(1f, .5f) // the visible right end
                    }
                    rotationZ = motion.turn.value + motion.tilt.value + sway.value
                    translationY = ctx.restLift + motion.riseY.value + ctx.scrollOffsetPx
                }
                .clickable(enabled = !ctx.leaving, onClick = onClick)
        )
    }
}

/**
 * The five Animatables a [StackEntrance] variant can drive for one [StackPill], bundled so
 * [play] - and every per-variant function it dispatches to - stays a short, single-purpose
 * function instead of one function switching on all eight variants at once.
 */
private class StackEntranceMotion {
    // Fraction of the pill's own width - see StackPill's own comment on why offsetByFractionOfParent
    // makes that true, not full-screen fraction.
    val offset = Animatable(0f)
    val riseY = Animatable(0f) // px, added to the pill's resting row lift
    val lenFrac = Animatable(1f) // fraction of the pill's own resting width
    val turn = Animatable(0f) // degrees
    val tilt = Animatable(0f) // degrees

    suspend fun resetToRest() {
        riseY.snapTo(0f)
        lenFrac.snapTo(1f)
        turn.snapTo(0f)
        tilt.snapTo(0f)
    }

    /**
     * The exit never varies - always the same sweep left, whichever entrance brought this pill
     * in - so this is also where a pill leaving mid-entrance (a host tapped again before a slow
     * entrance like Drop's wobble has settled) gets reset: a stray riseY/turn/tilt left over from
     * the interrupted entrance would otherwise leave the pill visibly off its row while it sweeps
     * away.
     */
    suspend fun enterOrLeave(
        entering: Boolean,
        entrance: StackEntrance,
        leaveTarget: Float,
        geometry: StackRowGeometry,
        sway: Animatable<Float, AnimationVector1D>
    ) {
        if (!entering) {
            resetToRest()
            sway.snapTo(0f)
            offset.animateTo(leaveTarget, tween(Azphalt.SLIDE_MS, easing = LinearEasing))
            return
        }
        play(entrance, geometry)
    }

    private suspend fun play(entrance: StackEntrance, geometry: StackRowGeometry) {
        val (row, rowCount, pitchPx, restWidthFraction) = geometry
        when (entrance) {
            StackEntrance.Slide -> playSlide(row, rowCount)
            StackEntrance.Unfold -> playUnfold(row)
            StackEntrance.Drop -> playDrop(pitchPx)
            StackEntrance.Cascade -> playCascade(row, rowCount, pitchPx)
            StackEntrance.Deal -> playDeal(row, rowCount, pitchPx)
            StackEntrance.Split -> playSplit(row, pitchPx, restWidthFraction)
            StackEntrance.Telescope -> playTelescope(row, rowCount, pitchPx)
            StackEntrance.Rally -> playRally(row)
        }
    }

    private suspend fun playSlide(row: Int, rowCount: Int) {
        val slide = Azphalt.SLIDE_MS
        offset.snapTo(-1.7f)
        if (row == rowCount - 1) {
            // The top row alone leaves late and overshoots, correcting once it has landed - the
            // one piece of character in an otherwise rigid, one-clock arrival (Amendment 7: no
            // per-row stagger here - that belongs to Cascade).
            offset.animateTo(0f, keyframes {
                durationMillis = slide + 90
                -1.7f at 0 using LinearEasing
                0.06f at slide using LinearEasing
                0f at slide + 90
            })
        } else {
            offset.animateTo(0f, tween(slide, easing = LinearEasing))
        }
    }

    private suspend fun playUnfold(row: Int) {
        // Length only, so nothing wobbles. `offset` stays 0 (its resting value) the whole time -
        // the pill is anchored at its own LEFT edge (the end that bleeds offscreen in the real
        // menu) via offsetByFractionOfParent(0), and lenFrac scales the width from that fixed
        // left edge, so the right tip is what advances. Anchoring at the right tip instead would
        // make the tip the fixed point and the offscreen edge would travel - invisible, reading
        // as nothing happening.
        lenFrac.snapTo(0f)
        delay(row * 26L)
        lenFrac.animateTo(1f, tween(Azphalt.UNFOLD_MS, easing = Azphalt.PAGE_EASE))
    }

    private suspend fun playDrop(pitchPx: Float) {
        val slide = Azphalt.SLIDE_MS
        riseY.snapTo(-pitchPx * 1.4f)
        // Stops short by 4% of a row pitch; the wobble (StackPill's own sway) resolves the
        // remainder - the one entrance where the sway is load-bearing rather than decorative.
        riseY.animateTo(pitchPx * 0.04f, tween(slide, easing = LinearEasing))
        riseY.animateTo(0f, tween(60, easing = LinearEasing))
    }

    private suspend fun playCascade(row: Int, rowCount: Int, pitchPx: Float) {
        // The child hinge, applied to roots. Strictly sequential: a row does not begin until the
        // one below it finishes.
        val step = stackInterval(Azphalt.SWING_MS, rowCount)
        riseY.snapTo(pitchPx) // stacked on the row below
        turn.snapTo(if (row % 2 == 0) -360f else 360f)
        delay(row * step.toLong())
        coroutineScope {
            launch { turn.animateTo(0f, tween(step, easing = LinearEasing)) }
            launch {
                // Lift folded into the final tenth, so turn and lift land on one frame.
                riseY.animateTo(0f, keyframes {
                    durationMillis = step
                    pitchPx at 0 using LinearEasing
                    pitchPx at (step * Azphalt.LIFT_FRACTION).toInt() using LinearEasing
                    0f at step
                })
            }
        }
    }

    private suspend fun playDeal(row: Int, rowCount: Int, pitchPx: Float) {
        val step = stackInterval(90, rowCount)
        offset.snapTo(0.5f)
        riseY.snapTo(pitchPx * 2f)
        tilt.snapTo(18f)
        delay(row * step.toLong())
        coroutineScope {
            launch { offset.animateTo(0f, tween(step * 2, easing = LinearEasing)) }
            launch { riseY.animateTo(0f, tween(step * 2, easing = LinearEasing)) }
            launch { tilt.animateTo(0f, tween(step * 2, easing = LinearEasing)) }
        }
    }

    private suspend fun playSplit(row: Int, pitchPx: Float, restWidthFraction: Float) {
        // Enters as ONE pill at host width on row 0, then divides upward.
        val slide = Azphalt.SLIDE_MS
        offset.snapTo(-1.7f)
        riseY.snapTo(pitchPx * row)
        lenFrac.snapTo(HOST_WIDTH / restWidthFraction)
        offset.animateTo(0f, tween(slide, easing = LinearEasing))
        coroutineScope {
            launch { riseY.animateTo(0f, tween(slide, easing = LinearEasing)) }
            launch { lenFrac.animateTo(1f, tween(slide, easing = LinearEasing)) }
        }
    }

    private suspend fun playTelescope(row: Int, rowCount: Int, pitchPx: Float) {
        val slide = Azphalt.SLIDE_MS
        val step = stackInterval(120, rowCount)
        if (row == 0) {
            offset.snapTo(-1.7f)
            offset.animateTo(0f, tween(slide, easing = LinearEasing))
        } else {
            // Drawn out from behind the row below, already at final length.
            riseY.snapTo(pitchPx * row)
            delay(slide + (row - 1) * step.toLong())
            riseY.animateTo(0f, tween(step, easing = LinearEasing))
        }
    }

    private suspend fun playRally(row: Int) {
        // Alternate sides, one clock, no stagger. Struck from both directions, so StackPill's
        // own sway runs at double amplitude.
        val slide = Azphalt.SLIDE_MS
        offset.snapTo(if (row % 2 == 0) -1.7f else 1.7f)
        offset.animateTo(0f, tween(slide, easing = LinearEasing))
    }
}

@Composable
private fun HostPill(node: MenuNode, rowsBelow: Int, onClick: () -> Unit) {
    val drop = remember { Animatable(-(ROW_PITCH.value * rowsBelow)) }
    // It lands like it weighs something: the same pile-sway every shared-clock stack move gets
    // (see PillWobble.kt), keyed to the drop rather than the slide since a host drop is its own
    // clock, not the entrance's.
    val sway = remember { Animatable(0f) }
    LaunchedEffect(node.id) {
        drop.animateTo(0f, tween(Azphalt.DROP_MS, easing = LinearEasing))
    }
    LaunchedEffect(node.id) {
        sway.wobble(rowsBelow, Azphalt.DROP_MS)
    }
    Box(Modifier.fillMaxSize()) {
        Pill(
            label = node.label,
            cap = node.cap,
            hue = Azphalt.hueOf(node.id),
            selected = true,
            touchTargetHeight = ROW_PITCH,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(HOST_WIDTH)
                .offsetByFractionOfParent(HOST_RIGHT_EDGE - 1f)
                .absoluteBleed(OVERHANG)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(1f, .5f) // the visible right end
                    rotationZ = sway.value
                    translationY = drop.value * density
                }
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
    onPick: (MenuNode) -> Unit
) {
    // No key needed here - the call site already wraps this whole band in key(anchor.id), so a
    // new anchor tears down and recreates this state automatically.
    var selected by remember { mutableStateOf<String?>(null) }
    val stackScroll = rememberStackScroll(
        rowMin = BAND_BASE_ROW,
        rowMax = (BAND_BASE_ROW + children.size - 1).coerceAtLeast(BAND_BASE_ROW)
    )

    Box(Modifier.fillMaxSize().then(stackScroll.modifier)) {
        children.forEachIndexed { idx, child ->
            key(child.id) {
                val isSelected = child.id == selected
                ChildPill(
                    node = child,
                    localIndex = idx,
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
    // A child begins exactly behind the pill before it - the first one row above the host (row 0
    // belongs to the host's own trail), every later one behind wherever its predecessor lands -
    // matching the keyframe's own 90%-hold target below exactly, or a pill three or more deep in
    // the band would visibly drift upward through its held rows instead of sitting invisibly at
    // the one it's hidden behind until the final 10% lift.
    val lift = remember(node.id) { Animatable(-pitchPx * (absoluteRow - 1).coerceAtLeast(BAND_BASE_ROW)) }
    val leaveOffset = remember(node.id) { Animatable(0f) }
    val scale = remember(node.id) { Animatable(1f) }
    // Only while leaving - a cascading child turns alone, not with a pile, so it has nothing to
    // sway against; the band's siblings leaving together are a pile again. See PillWobble.kt.
    val sway = remember(node.id) { Animatable(0f) }

    // Keyed on localIndex too, not just node.id: a band re-sort or a sibling filtered out changes
    // every subsequent localIndex without changing any id, so the delay computed from it would go
    // stale silently - the cascade would keep the old ordering, arriving in a sequence that no
    // longer matches where the pills actually sit.
    LaunchedEffect(node.id, localIndex) {
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
            launch { leaveOffset.animateTo(-1.7f, tween(Azphalt.SLIDE_MS, easing = LinearEasing)) }
            launch { sway.wobble(absoluteRow, Azphalt.SLIDE_MS) }
        }
    }

    Box(Modifier.fillMaxSize().zIndex(if (droppingOut) 100f else 50f - absoluteRow)) {
        Pill(
            label = node.label,
            cap = node.cap,
            // Each pill in the band its own hue, the same way StackPill's root stack already
            // varies by the pill's own id rather than sharing one color across every row -
            // hueOwner is still threaded through for TrailCrumb, where a shared color is the
            // point (the trail reads as one continuous path off the host), just not here.
            hue = Azphalt.hueOf(node.id),
            // Same "ink means primed" treatment StackPill gives its own row-0-aligned pill.
            selected = aligned,
            touchTargetHeight = ROW_PITCH,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(childWidthFraction(localIndex))
                .offsetByFractionOfParent(CHILD_LEFT + leaveOffset.value)
                .graphicsLayer {
                    transformOrigin =
                        if (localIndex % 2 == 0) TransformOrigin(0f, 0.5f) else TransformOrigin(1f, 0.5f)
                    rotationZ = turn.value + sway.value
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
            // Negative spacing overlaps each crumb into the one after it, like a fanned hand of
            // cards - the same shingled read as the rest of this menu's overlaps/bleeds. A plain
            // Row draws later children over earlier ones, which would read backwards for a trail
            // (the newest pick burying the path that led to it); TrailCrumb's own zIndex below
            // reverses that, so each crumb draws on top of every crumb that comes after it -
            // earlier picks stay on top, the way an actual stack of cards fans out.
            horizontalArrangement = Arrangement.spacedBy(TRAIL_OVERLAP)
        ) {
            trail.forEachIndexed { i, node ->
                key(node.id) {
                    TrailCrumb(
                        node = node,
                        hueOwner = hueOwner,
                        zIndex = (trail.size - i).toFloat(),
                        onClick = { onTapCrumb(i) },
                        onPositioned = { onCrumbPositioned(node.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrailCrumb(
    node: MenuNode,
    hueOwner: String,
    zIndex: Float,
    onClick: () -> Unit,
    onPositioned: (Rect) -> Unit = {}
) {
    Pill(
        label = node.label,
        cap = node.cap,
        hue = Azphalt.hueOf(hueOwner),
        selected = false,
        // Every other pill in the menu gets its width from fillMaxWidth against a fraction of
        // the screen and stretches its capsule to fill it; a Row of these can't do that (they'd
        // all fight for the full row), so this is the one pill sized by content alone - give it a
        // floor so a short label like "ls" doesn't shrink-wrap into something visibly smaller
        // than everything around it. Anchored BottomStart so a short crumb's floor padding falls
        // after its label, keeping the shingled trail's overlap landing on real crumb content
        // instead of an empty margin ahead of it.
        anchor = Alignment.BottomStart,
        stretch = false,
        // Guarantees real trailing space behind every crumb's own label/cap, long or short - see
        // extraEndPadding's own doc comment on Pill() for why a floor-width crumb's padding alone
        // isn't enough once a label's content outgrows that floor.
        extraEndPadding = -TRAIL_OVERLAP,
        modifier = Modifier
            .defaultMinSize(minWidth = 56.dp)
            .zIndex(zIndex)
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
    modifier: Modifier = Modifier,
    // Every stack pill (StackPill/HostPill/ChildPill) is sized via fillMaxWidth(fraction) against
    // a fraction of the screen, then bled further left by absoluteBleed. [stretch] (below) makes
    // the visible capsule actually fill that whole box - the *label* still sits at the box's own
    // right edge, the one fixed point DESIGN.md's "anchored by its right end" means, via
    // horizontalArrangement below; [anchor] only still matters for TrailCrumb, the one caller
    // that passes stretch=false, where BottomStart puts its 56dp floor-padding after a short
    // label instead of before it.
    anchor: Alignment = Alignment.BottomEnd,
    // True for every pill fanned out in a stack (StackPill/HostPill/ChildPill): the visible
    // capsule fills [modifier]'s own width - the fraction-of-screen-width box described above -
    // instead of shrinking to the label, so a stack's row-to-row width variation (or a child
    // band's uniform one) actually shows up as the capsule's own bled-left length, per DESIGN.md
    // rule 1, rather than being swallowed by a label-sized capsule floating inside an invisible
    // wider box. False only for TrailCrumb, the one pill sized by its own content - a Row of
    // those can't all stretch to fill their shared row without fighting each other for width.
    stretch: Boolean = true,
    // The accessibility-minimum default, overridden by every row-stacked pill - see the comment
    // where this is actually consumed, on why a row-stacked pill can't safely use the default.
    touchTargetHeight: Dp = PILL_TOUCH_TARGET,
    // Zero for every pill except TrailCrumb, which passes -TRAIL_OVERLAP here: a short crumb's
    // own floor-padding already keeps a shingled neighbor's overlap off its label (see [anchor]'s
    // own comment), but a crumb whose label is long enough to fill or exceed that 56dp floor has
    // no such buffer - the next crumb's overlap would land squarely on this one's own trailing
    // glyphs otherwise. Padding the content out by at least the overlap amount guarantees real,
    // opaque space back there for every crumb, long or short, so the overlap zone can never reach
    // actual text - independent of which crumb z-order happens to paint on top.
    extraEndPadding: Dp = 0.dp
) {
    val bg = if (selected) Azphalt.Ink else Azphalt.hues[hue]
    val capBg = if (selected) Azphalt.Yellow else Azphalt.caps[hue]
    // Selected is a fixed ink-bg/yellow-fg UI signal, not "text on an arbitrary background" - it
    // keeps its own fixed pairing. Unselected sits on this pill's own hue/cap, which - unlike the
    // page ground - never rotates, but still spans light (amber, tan) to dark (violet, teal), so a
    // fixed White reads fine on most of them and nearly vanishes on the lightest few.
    val fg = if (selected) Azphalt.Yellow else bg.contrastingText()
    val capFg = if (selected) Azphalt.Ink else capBg.contrastingText()
    // Local copy so the semantics block below reads the parameter, not its own
    // SemanticsPropertyReceiver.selected property of the same name.
    val isSelected = selected

    // [modifier] carries this pill's real geometry - offsetByFractionOfParent / absoluteBleed /
    // the lift-and-scroll graphicsLayer - and ends in the clickable that fires onClick, all sized
    // against the pill's own footprint. PILL_HEIGHT (17dp) is a deliberate Menu Style Guide
    // constant the width-variation and row-pitch math elsewhere in this file depends on staying
    // exact, so it can't simply grow to Android's PILL_TOUCH_TARGET (48dp) minimum. Instead this
    // outer Box pads out the *clickable* region to [touchTargetHeight] while the Row painted below
    // stays exactly PILL_HEIGHT tall - the same "invisible larger tap zone around a small visible
    // element" pattern GuideReaderScreen's Chip (UI-7) uses. The padding is pinned to the bottom
    // edge (defaultMinSize + a Bottom* anchor) rather than split evenly above and below: every
    // pill here is itself bottom-anchored (aligned to a Bottom* row position, then lifted into its
    // row by translationY), so bottom-anchoring the touch target too means the extra height only
    // ever extends upward, toward the next row up - never nudging the visible pill's own on-screen
    // position by even a pixel, but eating into that next row's own space by however much the
    // target overshoots PILL_HEIGHT. [touchTargetHeight] is a real, enforced bound on that
    // overshoot, not a "should be fine" hope: a row-stacked pill (StackPill/HostPill/ChildPill)
    // passes ROW_PITCH itself here, capping the touch target at exactly the gap to the next row so
    // two rows' tap zones can never overlap - PILL_TOUCH_TARGET's full 48dp would reach 31dp past
    // a 17dp pill into rows *above* it, and since later-declared rows paint (and hit-test) on top
    // of earlier ones, a lower row's oversized target would silently steal taps aimed at whatever
    // pill actually sits under the user's finger higher up the stack. TrailCrumb - a single
    // horizontal row, not lifted into a stack of its own - keeps the full accessibility-minimum
    // default since it has no neighboring row to overlap. [anchor]'s Start/End half is the
    // separate, horizontal half of this same alignment - see the comment on that parameter.
    Box(
        modifier.defaultMinSize(minHeight = touchTargetHeight),
        contentAlignment = anchor
    ) {
        Row(
            Modifier
                .height(PILL_HEIGHT)
                .then(if (stretch) Modifier.fillMaxWidth() else Modifier)
                // The state that reads as a color shift (ink vs. hue) needs its own semantics
                // entry to reach a screen reader at all - color alone carries no signal there.
                // Merging descendants folds the label/cap Text children into one announced unit
                // ("LABEL, button") instead of TalkBack stopping on each one separately.
                .semantics(mergeDescendants = true) {
                    this.role = Role.Button
                    this.selected = isSelected
                }
                .clip(RoundedCornerShape(percent = 50))
                .background(bg)
                .padding(start = 12.dp, end = 6.dp + extraEndPadding),
            verticalAlignment = Alignment.CenterVertically,
            // "Label and end-cap sit together at the right end of the pill, in that order, 9px
            // apart" - the style guide's own literal gap value.
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.End)
        ) {
            Text(
                text = label.uppercase(),
                color = fg,
                fontSize = 6.sp,
                // Text() with no `style` inherits LocalTextStyle - MaterialTheme's own
                // ProvideTextStyle(typography.bodyLarge), whose lineHeight is a fixed 24.sp. An
                // overridden fontSize alone doesn't touch that: the merged style keeps the
                // inherited 24.sp line box around a 6sp glyph, and both this Row and the cap Box
                // below clip to their own much shorter fixed height, cutting most of the glyph
                // away. Matching lineHeight to fontSize is what actually fixes it, not the clip.
                lineHeight = 6.sp,
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
                        // Same inherited-lineHeight bug as the label above, worse here since this
                        // Box is only 10dp tall - the inherited 24.sp line box left only the very
                        // top sliver of the cap's own glyphs inside that clip.
                        lineHeight = 5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
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
