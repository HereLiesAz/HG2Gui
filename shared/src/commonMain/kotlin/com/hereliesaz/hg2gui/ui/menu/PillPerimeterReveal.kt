package com.hereliesaz.hg2gui.ui.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
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
 * Before any of that, per "THE SIX BEATS" in docs/HG2Gui Run Transition.html and specimens
 * 8/9 ("HAND-OFF" / "RUNNING A COMMAND") in docs/HG2Gui Motion Sheet.html, three lead-in beats
 * play out: the chosen pill stretches, snaps thin, and flies into the corner it lands in
 * (stretch); it rocks on the tip it just flew into as the rest of the stack sweeps away (break
 * free); then it drops straight down onto the bottom edge (fall) - which is exactly where the
 * perimeter run above begins. A final beat (set) wipes the revealed content on once the flood
 * completes. Going back is the same beats played backwards, per the doc's own note.
 *
 * The real pill stack lives in PillMenu's own composable tree, not this one, so the lead-in beats
 * are played by a small representative pill rendered inside this same Box, sized and positioned
 * from [PerimeterRevealState.origin] - the same crumb rect the perimeter legs already key off of.
 * "The rest of the stack sweeps off to the left" is PillMenu's own concern and isn't simulated
 * here.
 */

private val LEG_EASE = CubicBezierEasing(0f, .9f, .1f, 1f)
private const val LEG_MS = 260
private const val FLOOD_MS = 420
private val MIN_THICKNESS = 34.dp

// The three lead-in beats (stretch, break free, fall) plus the trailing "set" beat. Durations are
// the doc's own approximate figures - LEG_MS/FLOOD_MS above are untouched per instruction.
private const val STRETCH_MS = 300
private const val BREAK_MS = 140
private const val FALL_MS = 300
private const val SET_MS = 90
private val LEAD_IN_FLY_DISTANCE = 150.dp
private val LEAD_IN_DROP_HEIGHT = 70.dp

class PerimeterRevealState {
    var origin: Rect by mutableStateOf(Rect.Zero)
    var active: Boolean by mutableStateOf(false)
    val bottomLeg = Animatable(0f)
    val rightLeg = Animatable(0f)
    val topLeg = Animatable(0f)
    val leftLeg = Animatable(0f)
    val flood = Animatable(0f)

    // Beat 1 "stretch": stretchWidth models the pill lengthening rightward then snapping to
    // under half its width; flyProgress carries it the rest of the way horizontally. Both run
    // over the same STRETCH_MS window - see runLeadIn for the keyframe split between them.
    var leadInActive: Boolean by mutableStateOf(false)
    val stretchWidth = Animatable(1f)
    val flyProgress = Animatable(0f)
    // Beat 2 "break free": a rapid, damping rock pivoting on the tip it just flew into.
    val rockAngle = Animatable(0f)
    // Beat 3 "fall": drops straight down from the break point onto the bottom edge (= origin).
    val fallProgress = Animatable(0f)
    // Beat 4 "set": sits at 1 (no-op) except during its own brief window right after flood
    // completes, where it snaps to 0 and wipes back to 1 - a plain top-level mirror of
    // GuideReaderScreen's WipeItem/wipeClip idiom (clip + slight slide) rather than a full
    // per-line wipe, since the wrapped content here is a whole screen, not a list of lines.
    val setWipe = Animatable(1f)

    /** Beats 1-3 (stretch, break free, fall), then the perimeter legs + flood, then beat 4. */
    suspend fun open() {
        active = true
        runLeadIn(reverse = false)
        listOf(bottomLeg, rightLeg, topLeg, leftLeg, flood).forEach { it.snapTo(0f) }
        bottomLeg.animateTo(1f, tween(LEG_MS, easing = LEG_EASE))
        rightLeg.animateTo(1f, tween(LEG_MS, easing = LEG_EASE))
        topLeg.animateTo(1f, tween(LEG_MS, easing = LEG_EASE))
        coroutineScope {
            launch { leftLeg.animateTo(1f, tween(LEG_MS, easing = LEG_EASE)) }
            launch { flood.animateTo(1f, tween(FLOOD_MS, easing = LEG_EASE)) }
        }
        setWipe.snapTo(0f)
        setWipe.animateTo(1f, tween(SET_MS, easing = LEG_EASE))
    }

    /** The exact reverse: beat 4 first, then perimeter+flood retreat, then beats 3-1 backwards. */
    suspend fun close() {
        setWipe.animateTo(0f, tween(SET_MS, easing = LEG_EASE))
        setWipe.snapTo(1f) // reset so it doesn't gate the flood/perimeter retreat below
        coroutineScope {
            launch { flood.animateTo(0f, tween(FLOOD_MS, easing = LEG_EASE)) }
            launch { leftLeg.animateTo(0f, tween(LEG_MS, easing = LEG_EASE)) }
        }
        topLeg.animateTo(0f, tween(LEG_MS, easing = LEG_EASE))
        rightLeg.animateTo(0f, tween(LEG_MS, easing = LEG_EASE))
        bottomLeg.animateTo(0f, tween(LEG_MS, easing = LEG_EASE))
        runLeadIn(reverse = true)
        active = false
    }

    private suspend fun runLeadIn(reverse: Boolean) {
        leadInActive = true
        if (!reverse) {
            stretchWidth.snapTo(1f)
            flyProgress.snapTo(0f)
            rockAngle.snapTo(0f)
            fallProgress.snapTo(0f)
            coroutineScope {
                launch {
                    // Stretch out, then snap thin - the "snap" sub-beat is the tight middle leg.
                    stretchWidth.animateTo(0.45f, keyframes {
                        durationMillis = STRETCH_MS
                        1f at 0 using LinearEasing
                        1.6f at (STRETCH_MS * 0.35f).toInt() using LinearEasing
                        0.45f at (STRETCH_MS * 0.55f).toInt() using LEG_EASE
                    })
                }
                launch {
                    // Stays put through the stretch+snap, then flies for the rest of the beat.
                    flyProgress.animateTo(1f, keyframes {
                        durationMillis = STRETCH_MS
                        0f at 0
                        0f at (STRETCH_MS * 0.55f).toInt()
                        1f at STRETCH_MS using LEG_EASE
                    })
                }
            }
            rockAngle.animateTo(0f, keyframes {
                durationMillis = BREAK_MS
                0f at 0 using LinearEasing
                14f at (BREAK_MS * 0.25f).toInt() using LinearEasing
                (-8f) at (BREAK_MS * 0.5f).toInt() using LinearEasing
                4f at (BREAK_MS * 0.75f).toInt() using LinearEasing
            })
            fallProgress.animateTo(1f, tween(FALL_MS, easing = LEG_EASE))
        } else {
            fallProgress.animateTo(0f, tween(FALL_MS, easing = LEG_EASE))
            rockAngle.animateTo(0f, keyframes {
                durationMillis = BREAK_MS
                0f at 0 using LinearEasing
                4f at (BREAK_MS * 0.25f).toInt() using LinearEasing
                (-8f) at (BREAK_MS * 0.5f).toInt() using LinearEasing
                14f at (BREAK_MS * 0.75f).toInt() using LinearEasing
            })
            coroutineScope {
                launch {
                    flyProgress.animateTo(0f, keyframes {
                        durationMillis = STRETCH_MS
                        1f at 0 using LEG_EASE
                        0f at (STRETCH_MS * 0.45f).toInt()
                    })
                }
                launch {
                    stretchWidth.animateTo(1f, keyframes {
                        durationMillis = STRETCH_MS
                        0.45f at 0
                        0.45f at (STRETCH_MS * 0.45f).toInt() using LEG_EASE
                        1.6f at (STRETCH_MS * 0.65f).toInt() using LinearEasing
                    })
                }
            }
        }
        leadInActive = false
    }
}

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
            if (state.leadInActive) {
                // Beats 1-3: a stand-in pill (the real stack lives in PillMenu's own tree) that
                // stretches/snaps/flies in from the left, rocks on its landed tip, then drops the
                // remaining distance onto the bottom edge - ending exactly at the crumb's own
                // rect, where the perimeter legs above pick up.
                val flyDistancePx = LEAD_IN_FLY_DISTANCE.toPx()
                val dropHeightPx = LEAD_IN_DROP_HEIGHT.toPx()
                val baseW = if (o.width > 0f) o.width else 64.dp.toPx()
                val pillW = (baseW * state.stretchWidth.value).coerceAtLeast(1f)
                val x = lerp(originLeft - flyDistancePx, originLeft, state.flyProgress.value)
                val landedY = fullH - thickness
                val y = lerp(landedY - dropHeightPx, landedY, state.fallProgress.value)
                Box(
                    Modifier
                        .offset { IntOffset(x.toInt(), y.toInt()) }
                        .size(pillW.toDp(), thickness.toDp())
                        .graphicsLayer {
                            // Rocks on its right tip, the edge it just flew into.
                            transformOrigin = TransformOrigin(1f, 1f)
                            rotationZ = state.rockAngle.value
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
