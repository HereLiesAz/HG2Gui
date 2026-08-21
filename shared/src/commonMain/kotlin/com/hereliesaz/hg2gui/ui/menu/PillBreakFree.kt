package com.hereliesaz.hg2gui.ui.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/*
 * Beats 1-3 of the run transition ("THE SIX BEATS" in docs/HG2Gui Run Transition.html), as ONE
 * continuous motion rather than four chained animations, shared by PillPerimeterReveal.kt and
 * PillWrapReveal.kt so the choreography is authored once instead of drifting between the two
 * tiers. The pill strains against the stack and only gets longer; it releases from its left
 * edge; it flies right at the speed the release was retracting it; it hits the right edge and
 * jiggles on that tip because it hit something; then it falls. The perimeter/wrap run that
 * follows (beat 4 onward) stays anchored to the pill's own original position throughout, per the
 * reference spec's own bars - this is a flourish playing out to the side of it, not a rename of
 * where the frame itself starts growing.
 *
 * Nothing here is authored twice: the flight duration comes out of the snap's own px/ms rate.
 */

private const val STRETCH_MS = 520 // slow strain. Length is the ONLY thing that changes.
private const val SNAP_MS = 40 // the release, retracting from the LEFT edge
private const val JIGGLE_MS = 180 // on the right tip, after impact
private const val FALL_MS = 280

private const val STRETCH = 1.06f // NOT 1.6 - a visibly longer pill, not a new shape
private const val SHORT = 0.40f

private val HOUSE = CubicBezierEasing(0f, .9f, .1f, 1f)

class BreakFreeState {
    /** Multiplier on the pill's own resting width. */
    val width = Animatable(1f)
    /** Pixels, added to the pill's resting left edge. */
    val offsetX = Animatable(0f)
    val offsetY = Animatable(0f)
    /** Degrees, pivoting on the right tip. */
    val tilt = Animatable(0f)
    var active: Boolean by mutableStateOf(false)
        internal set
}

/**
 * @param baseWidthPx the pill's resting width
 * @param flightPx distance from the pill's right tip to the right edge of the screen
 * @param floorPx distance from the pill's row down to the bottom edge
 */
suspend fun BreakFreeState.run(baseWidthPx: Float, flightPx: Float, floorPx: Float) {
    active = true
    // Reset every animatable up front, not only the ones about to move next - a run cancelled
    // mid-sequence (a back press, the phase changing) would otherwise leave the next run
    // continuing from an arbitrary stale value instead of the pill's own true resting pose.
    width.snapTo(1f)
    offsetX.snapTo(0f)
    offsetY.snapTo(0f)
    tilt.snapTo(0f)

    // 1. STRUGGLE. It slowly gets longer, and that is all - no shake, no wobble, no rotation.
    //    Strain is length. Linear: an eased strain reads as a spring instead.
    width.animateTo(STRETCH, tween(STRETCH_MS, easing = LinearEasing))

    // 2. BREAK FREE. Shortens from the LEFT, so the right tip stays exactly where it is: the
    //    width shrinks and the left edge advances by the same amount, one clock.
    val stretchedPx = baseWidthPx * STRETCH
    val shortPx = baseWidthPx * SHORT
    val retractedPx = stretchedPx - shortPx
    coroutineScope {
        launch { offsetX.animateTo(retractedPx, tween(SNAP_MS, easing = LinearEasing)) }
        width.animateTo(SHORT, tween(SNAP_MS, easing = LinearEasing))
    }

    // 3. FLY. Same speed the snap was retracting it - derived, never authored. This is the line
    //    that makes it read as a rubber band released rather than a tween playing.
    val ratePxPerMs = retractedPx / SNAP_MS
    val flightMs = (flightPx / ratePxPerMs).toInt().coerceIn(1, 2000)
    offsetX.animateTo(retractedPx + flightPx, tween(flightMs, easing = LinearEasing))

    // 4. THUD. It jiggles because it just hit the edge - rapid, damping, pivoting on the right
    //    tip's own centre. Rotation only: a length bounce here reads as rubber.
    tilt.animateTo(0f, keyframes {
        durationMillis = JIGGLE_MS
        0f at 0 using LinearEasing
        3.2f at 40 using LinearEasing
        (-2.1f) at 85 using LinearEasing
        1.1f at 130 using LinearEasing
        0f at JIGGLE_MS
    })

    // 5. FALL. Straight down onto the bottom edge, where the perimeter/wrap run picks up.
    offsetY.animateTo(floorPx, tween(FALL_MS, easing = HOUSE))
    active = false
}

/** The exact reverse, for closing. */
suspend fun BreakFreeState.reverse(baseWidthPx: Float, flightPx: Float, floorPx: Float) {
    active = true
    offsetX.snapTo(retractedPxOf(baseWidthPx) + flightPx)
    offsetY.snapTo(floorPx)
    width.snapTo(SHORT)
    tilt.snapTo(0f)
    offsetY.animateTo(0f, tween(FALL_MS, easing = HOUSE))
    val retractedPx = retractedPxOf(baseWidthPx)
    val ratePxPerMs = retractedPx / SNAP_MS
    val flightMs = (flightPx / ratePxPerMs).toInt().coerceIn(1, 2000)
    offsetX.animateTo(retractedPx, tween(flightMs, easing = LinearEasing))
    coroutineScope {
        launch { offsetX.animateTo(0f, tween(SNAP_MS, easing = LinearEasing)) }
        width.animateTo(STRETCH, tween(SNAP_MS, easing = LinearEasing))
    }
    width.animateTo(1f, tween(STRETCH_MS, easing = LinearEasing))
    active = false
}

private fun retractedPxOf(baseWidthPx: Float) = baseWidthPx * STRETCH - baseWidthPx * SHORT
