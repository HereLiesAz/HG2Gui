package com.hereliesaz.hg2gui.ui.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/*
 * A picked pill doesn't hand off to a new screen - it runs the full perimeter of the display and
 * becomes the screen. Where PillWrapReveal simplifies "run the perimeter" into one continuous
 * rect interpolation, this is the fuller, edge-by-edge choreography the Select File/Folder pill
 * uses: starting from wherever the pick's own trail crumb settled at the bottom of the screen, it
 * grows rightward along the bottom edge to the bottom-right corner, up the right edge to the
 * top-right corner, left across the top to the top-left corner, then down the left edge - closing
 * the loop by running back down over its own start. The instant that last leg begins, a downward
 * wipe starts filling the frame it has enclosed with the pill's own colour, revealing whatever it
 * opened underneath as the wipe descends.
 *
 * Before any of that, per "THE SIX BEATS" in docs/HG2Gui Run Transition.html, beats 1-3 (stretch,
 * break free, fall) play out via the shared [BreakFreeState] in PillBreakFree.kt - see that file
 * for the choreography itself. Per the reference spec's own bars, the perimeter legs below stay
 * anchored to the pill's original position throughout; the break-free pill is a flourish playing
 * out to the side of it (flying off toward the right edge before falling), not a relocation of
 * where the frame itself starts growing. A final beat (set) wipes the revealed content on once
 * the flood completes. Going back is the same beats played backwards, per the doc's own note.
 *
 * The real pill stack lives in PillMenu's own composable tree, not this one, so the break-free
 * pill is a small representative rendered inside this same Box, sized and positioned from
 * [PerimeterRevealState.origin] - the same crumb rect the perimeter legs already key off of.
 * "The rest of the stack sweeps off to the left" is PillMenu's own concern and isn't simulated
 * here.
 */

private val LEG_EASE = CubicBezierEasing(0f, .9f, .1f, 1f)
private const val LEG_MS = 260
private const val FLOOD_MS = 420
private const val SET_MS = 90
private val MIN_THICKNESS = 34.dp

// How close to the literal screen edge the break-free flight lands - keeps the pill's own
// rounded tip from visually clipping into the edge, matching the reference spec's own margin.
private const val EDGE_MARGIN_PX = 6f

// The break-free pill pivots on its own right tip - the edge it flies into and thuds against.
private val TIP_ORIGIN = TransformOrigin(1f, 0.5f)

class PerimeterRevealState {
    var origin: Rect by mutableStateOf(Rect.Zero)
    var active: Boolean by mutableStateOf(false)
    val bottomLeg = Animatable(0f)
    val rightLeg = Animatable(0f)
    val topLeg = Animatable(0f)
    val leftLeg = Animatable(0f)
    val flood = Animatable(0f)
    val breakFree = BreakFreeState()

    // Beat 4 "set": stays at 0 (content hidden behind the flat hue fill) through the perimeter
    // and flood legs, then wipes to 1 once flood completes - a plain top-level mirror of
    // GuideReaderScreen's WipeItem/wipeClip idiom (clip + slight slide) rather than a full
    // per-line wipe, since the wrapped content here is a whole screen, not a list of lines.
    val setWipe = Animatable(1f)

    /**
     * Beats 1-3 (stretch, break free, fall) via [BreakFreeState.run], then the perimeter legs +
     * flood, then beat 4. [fullWidthPx]/[fullHeightPx]/[defaultBaseWidthPx] come from the
     * composable's own BoxWithConstraints/density scope, which this plain suspend function has
     * no access to - see PillPerimeterReveal's own call site.
     */
    suspend fun open(fullWidthPx: Float, fullHeightPx: Float, defaultBaseWidthPx: Float, minThicknessPx: Float) {
        active = true
        // Reset every leg/flood/wipe animatable up front, not only the ones about to move next -
        // if a PREVIOUS open()/close() coroutine was cancelled mid-sequence (a back press, the
        // phase changing while the reveal was still running), whichever animatables its own
        // sequence hadn't reached yet are left holding a stale value from further back still.
        // Starting every run from a fully-specified slate means a cancelled run can never leave a
        // half-drawn frame for the next one to inherit.
        listOf(bottomLeg, rightLeg, topLeg, leftLeg, flood, setWipe).forEach { it.snapTo(0f) }
        val geometry = geometry(fullWidthPx, fullHeightPx, defaultBaseWidthPx, minThicknessPx)
        breakFree.run(geometry.baseWidthPx, geometry.flightPx, geometry.floorPx)
        bottomLeg.animateTo(1f, tween(LEG_MS, easing = LEG_EASE))
        rightLeg.animateTo(1f, tween(LEG_MS, easing = LEG_EASE))
        topLeg.animateTo(1f, tween(LEG_MS, easing = LEG_EASE))
        coroutineScope {
            launch { leftLeg.animateTo(1f, tween(LEG_MS, easing = LEG_EASE)) }
            launch { flood.animateTo(1f, tween(FLOOD_MS, easing = LEG_EASE)) }
        }
        setWipe.animateTo(1f, tween(SET_MS, easing = LEG_EASE))
    }

    /** The exact reverse: beat 4 first, then perimeter+flood retreat, then beats 3-1 backwards. */
    suspend fun close(fullWidthPx: Float, fullHeightPx: Float, defaultBaseWidthPx: Float, minThicknessPx: Float) {
        // Same defensive snap open() makes, mirrored to the fully-open end of the range - a
        // cancelled open() otherwise leaves an arbitrary leg short, and close() would retreat
        // whatever partial frame that left rather than the intended fully-drawn one.
        listOf(bottomLeg, rightLeg, topLeg, leftLeg, flood).forEach { it.snapTo(1f) }
        // Left at 0 (content hidden) through the rest of the closing sequence below - the frame
        // itself is retreating anyway, so there's nothing to "gate" by leaving it hidden; forcing
        // it back to 1 here used to pop content instantly back into view mid-close.
        setWipe.animateTo(0f, tween(SET_MS, easing = LEG_EASE))
        coroutineScope {
            launch { flood.animateTo(0f, tween(FLOOD_MS, easing = LEG_EASE)) }
            launch { leftLeg.animateTo(0f, tween(LEG_MS, easing = LEG_EASE)) }
        }
        topLeg.animateTo(0f, tween(LEG_MS, easing = LEG_EASE))
        rightLeg.animateTo(0f, tween(LEG_MS, easing = LEG_EASE))
        bottomLeg.animateTo(0f, tween(LEG_MS, easing = LEG_EASE))
        val geometry = geometry(fullWidthPx, fullHeightPx, defaultBaseWidthPx, minThicknessPx)
        breakFree.reverse(geometry.baseWidthPx, geometry.flightPx, geometry.floorPx)
        active = false
    }

    private fun geometry(
        fullWidthPx: Float,
        fullHeightPx: Float,
        defaultBaseWidthPx: Float,
        minThicknessPx: Float
    ): PerimeterBreakFreeGeometry {
        val thickness = if (origin.height > 0f) origin.height else minThicknessPx
        val originLeft = if (origin.width > 0f) origin.left else 0f
        val originTop = if (origin.height > 0f) origin.top else fullHeightPx - thickness
        val baseWidthPx = if (origin.width > 0f) origin.width else defaultBaseWidthPx
        // "Distance from the pill's right tip to the right edge of the screen" - the pill's own
        // RESTING tip, not the stretched one BreakFreeState briefly grows to internally; the few
        // px that discrepancy leaves short of the literal edge is imperceptible next to a 6%
        // stretch, and keeping that stretch factor private to PillBreakFree.kt is worth it.
        val flightPx = (fullWidthPx - EDGE_MARGIN_PX - originLeft - baseWidthPx).coerceAtLeast(0f)
        val floorPx = (fullHeightPx - thickness - originTop).coerceAtLeast(0f)
        return PerimeterBreakFreeGeometry(baseWidthPx, flightPx, floorPx)
    }
}

private data class PerimeterBreakFreeGeometry(val baseWidthPx: Float, val flightPx: Float, val floorPx: Float)

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * Renders [content] inside a [hue]-coloured frame that runs the screen's perimeter one edge at a
 * time from [PerimeterRevealState.origin] (the crumb that opened it) - see the file header for
 * the exact choreography. Renders nothing while [state] is inactive.
 */
@Composable
fun PillPerimeterReveal(state: PerimeterRevealState, hue: Color, content: @Composable () -> Unit) {
    if (!state.active) return
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fullW = with(density) { maxWidth.toPx() }
        val fullH = with(density) { maxHeight.toPx() }
        val o = state.origin
        val thickness = if (o.height > 0f) o.height else with(density) { MIN_THICKNESS.toPx() }
        val originLeft = if (o.width > 0f) o.left else 0f

        val bottomRight = lerp(originLeft, fullW, state.bottomLeg.value)
        val rightTop = lerp(fullH, 0f, state.rightLeg.value)
        val topLeft = lerp(fullW, 0f, state.topLeg.value)
        val leftBottom = lerp(0f, fullH, state.leftLeg.value)

        with(density) {
            if (state.breakFree.active) {
                // Beats 1-3: a stand-in pill (the real stack lives in PillMenu's own tree) that
                // strains, breaks free, flies toward the right edge, thuds, and falls onto the
                // bottom edge - a flourish playing out to the side of the perimeter legs below,
                // which stay anchored to the crumb's own position throughout.
                val baseW = if (o.width > 0f) o.width else 64.dp.toPx()
                val originTop = if (o.height > 0f) o.top else fullH - thickness
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (originLeft + state.breakFree.offsetX.value).toInt(),
                                (originTop + state.breakFree.offsetY.value).toInt()
                            )
                        }
                        .size((baseW * state.breakFree.width.value).coerceAtLeast(1f).toDp(), thickness.toDp())
                        .graphicsLayer {
                            transformOrigin = TIP_ORIGIN // the tip, not the corner
                            rotationZ = state.breakFree.tilt.value
                        }
                        .clip(RoundedCornerShape(percent = 50))
                        .background(hue)
                )
            }

            // Bottom edge: the crumb's own left end, growing right to the bottom-right corner.
            Box(
                Modifier
                    .offset { IntOffset(originLeft.toInt(), (fullH - thickness).toInt()) }
                    .size((bottomRight - originLeft).coerceAtLeast(0f).toDp(), thickness.toDp())
                    .background(hue)
            )
            // Right edge: the bottom-right corner, growing up to the top-right corner.
            Box(
                Modifier
                    .offset { IntOffset((fullW - thickness).toInt(), rightTop.toInt()) }
                    .size(thickness.toDp(), (fullH - rightTop).toDp())
                    .background(hue)
            )
            // Top edge: the top-right corner, growing left to the top-left corner.
            Box(
                Modifier
                    .offset { IntOffset(topLeft.toInt(), 0) }
                    .size((fullW - topLeft).toDp(), thickness.toDp())
                    .background(hue)
            )
            // Left edge: the top-left corner, growing down - closing the loop back over the
            // crumb's own start. The wipe below only appears once this leg is under way.
            Box(
                Modifier
                    .offset { IntOffset(0, 0) }
                    .size(thickness.toDp(), leftBottom.toDp())
                    .background(hue)
            )

            if (state.leftLeg.value > 0f || state.flood.value > 0f) {
                Box(
                    Modifier
                        .offset { IntOffset(thickness.toInt(), thickness.toInt()) }
                        .size(
                            (fullW - thickness * 2).coerceAtLeast(0f).toDp(),
                            (fullH - thickness * 2).coerceAtLeast(0f).toDp()
                        )
                        .background(hue)
                ) {
                    // The frame's interior fills with hue immediately; content wipes downward
                    // over it as flood advances, revealing the browser in place of the flat fill.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(bottom = size.height * state.flood.value) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    ) {
                        // Beat 4 "set": once flood is done, an extra left-to-right wipe (plus a
                        // slight slide, mirroring WipeItem.wipeClip in GuideReaderScreen.kt)
                        // plays over the now-fully-flooded content.
                        Box(
                            Modifier
                                .fillMaxSize()
                                .drawWithContent {
                                    clipRect(right = size.width * state.setWipe.value) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                                .graphicsLayer {
                                    translationX = -14.dp.toPx() * (1f - state.setWipe.value)
                                }
                        ) { content() }
                    }
                }
            }
        }
    }
}
