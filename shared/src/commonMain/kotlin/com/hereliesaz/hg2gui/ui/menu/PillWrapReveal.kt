package com.hereliesaz.hg2gui.ui.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

/*
 * "The pill becomes the page": a pill runs out around the edge of the screen, closing the loop,
 * then the space it enclosed floods with a vertical wipe that reveals whatever it opened - the
 * design source's own name for this two-beat move is "run the perimeter" + "flood". Simplified
 * here (as elsewhere in this app) from the source's multi-stage keyframe sequence into one
 * continuous rect interpolation plus a clip-based wipe, rather than four separate animated edges.
 *
 * Per "THE SIX BEATS" in docs/HG2Gui Run Transition.html and specimens 8/9 in
 * docs/HG2Gui Motion Sheet.html, three lead-in beats precede "run the perimeter": stretch, break
 * free, and fall - the pill lengthens, snaps thin, flies into the corner it lands in, rocks on
 * the tip it just landed on, then drops onto the bottom edge. A trailing "set" beat wipes the
 * revealed content on once the flood completes. In keeping with this file's own simplification of
 * the source choreography, the three lead-in beats are driven by a single continuous [Animatable]
 * (leadIn) rather than PillPerimeterReveal's separate per-beat Animatables, with the composable
 * deriving the stretch/snap/fly/rock/fall visuals as piecewise functions of that one value.
 */

private val WRAP_EASE = CubicBezierEasing(0f, .9f, .1f, 1f)
private const val WRAP_MS = 640
private const val FLOOD_MS = 420
private val BORDER_WIDTH = 3.dp

// Lead-in beat durations - the doc's own approximate figures. WRAP_MS/FLOOD_MS above untouched.
private const val LEAD_IN_STRETCH_MS = 300
private const val LEAD_IN_BREAK_MS = 140
private const val LEAD_IN_FALL_MS = 300
private const val LEAD_IN_MS = LEAD_IN_STRETCH_MS + LEAD_IN_BREAK_MS + LEAD_IN_FALL_MS
private const val SET_MS = 90
private val LEAD_IN_FLY_DISTANCE = 150.dp
private val LEAD_IN_DROP_HEIGHT = 70.dp

class PillWrapRevealState {
    var origin: Rect by mutableStateOf(Rect.Zero)
    var active: Boolean by mutableStateOf(false)
    val wrap = Animatable(0f)
    val flood = Animatable(0f)

    // Beats 1-3 (stretch, break free, fall) collapsed into one continuous driver - see file
    // header for why this tier simplifies them this way.
    var leadInActive: Boolean by mutableStateOf(false)
    val leadIn = Animatable(0f)
    // Beat 4 "set": stays at 0 (content hidden) until flood completes, then wipes to 1 - see
    // setWipe in PillPerimeterReveal.kt for the identical idiom and rationale.
    val setWipe = Animatable(1f)

    suspend fun open() {
        active = true
        leadInActive = true
        leadIn.snapTo(0f)
        leadIn.animateTo(1f, tween(LEAD_IN_MS, easing = WRAP_EASE))
        leadInActive = false
        wrap.snapTo(0f)
        flood.snapTo(0f)
        // setWipe sits at 0 (content hidden behind the border-only frame) through wrap+flood,
        // rather than snapping there only after flood already finished - snapping afterward
        // briefly showed the fully-revealed content, then hid it, then wiped it back on, a
        // flicker rather than a single clean reveal.
        setWipe.snapTo(0f)
        wrap.animateTo(1f, tween(WRAP_MS, easing = WRAP_EASE))
        flood.animateTo(1f, tween(FLOOD_MS, easing = WRAP_EASE))
        setWipe.animateTo(1f, tween(SET_MS, easing = WRAP_EASE))
    }

    suspend fun close() {
        // Left at 0 (content hidden) through the rest of the closing sequence below - the frame
        // itself is retreating anyway, so there's nothing to "gate" by leaving it hidden; forcing
        // it back to 1 here used to pop content instantly back into view mid-close.
        setWipe.animateTo(0f, tween(SET_MS, easing = WRAP_EASE))
        flood.animateTo(0f, tween(FLOOD_MS, easing = WRAP_EASE))
        wrap.animateTo(0f, tween(WRAP_MS, easing = WRAP_EASE))
        leadInActive = true
        leadIn.animateTo(0f, tween(LEAD_IN_MS, easing = WRAP_EASE))
        leadInActive = false
        active = false
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * Renders [content] inside a rect that grows from [PillWrapRevealState.origin] (the pill that
 * opened it) out to the full available space, framed by a [hue]-coloured border, revealed by a
 * bottom-to-top wipe once the loop has closed. Renders nothing while [state] is inactive.
 */
@Composable
fun PillWrapReveal(state: PillWrapRevealState, hue: Color, content: @Composable () -> Unit) {
    if (!state.active) return
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fullW = with(density) { maxWidth.toPx() }
        val fullH = with(density) { maxHeight.toPx() }
        val o = state.origin
        val w = state.wrap.value

        with(density) {
            // The lead-in beats always land at the bottom edge (per the doc: stretch/break/fall
            // bring the pill down onto the bottom of the screen before the perimeter run starts),
            // regardless of where [origin] itself sits (e.g. the Files pill lives in the top
            // header). The wrap growth below has to start from that same landed rect, not from
            // [origin] directly, or the frame visibly teleports from the bottom back up to the
            // header the instant leadInActive flips off.
            val originLeft = if (o.width > 0f) o.left else 0f
            val thickness = if (o.height > 0f) o.height else 34.dp.toPx()
            val baseW = if (o.width > 0f) o.width else 64.dp.toPx()
            val landedWidth = (baseW * 0.45f).coerceAtLeast(1f)
            val landed = Rect(
                left = originLeft, top = fullH - thickness,
                right = originLeft + landedWidth, bottom = fullH
            )

            val left = lerp(landed.left, 0f, w)
            val top = lerp(landed.top, 0f, w)
            val right = lerp(landed.right, fullW, w)
            val bottom = lerp(landed.bottom, fullH, w)
            val curW = right - left
            val curH = bottom - top
            val corner = lerp(minOf(landed.width, landed.height) / 2f, 0f, w)

            if (state.leadInActive) {
                // Beats 1-3, all derived from the single leadIn driver - see file header. Phase
                // boundaries are proportional to each beat's share of LEAD_IN_MS.
                val stretchFrac = LEAD_IN_STRETCH_MS / LEAD_IN_MS.toFloat()
                val breakFrac = LEAD_IN_BREAK_MS / LEAD_IN_MS.toFloat()
                val t = state.leadIn.value

                val widthMul = if (t < stretchFrac) {
                    val local = (t / stretchFrac).coerceIn(0f, 1f)
                    when {
                        local < 0.35f -> lerp(1f, 1.6f, local / 0.35f)
                        local < 0.55f -> lerp(1.6f, 0.45f, (local - 0.35f) / 0.2f)
                        else -> 0.45f
                    }
                } else 0.45f

                val flyT = if (t < stretchFrac) {
                    val local = (t / stretchFrac).coerceIn(0f, 1f)
                    if (local < 0.55f) 0f else (local - 0.55f) / 0.45f
                } else 1f

                val rockDeg = if (t in stretchFrac..(stretchFrac + breakFrac)) {
                    val local = ((t - stretchFrac) / breakFrac).coerceIn(0f, 1f)
                    when {
                        local < 0.25f -> lerp(0f, 14f, local / 0.25f)
                        local < 0.5f -> lerp(14f, -8f, (local - 0.25f) / 0.25f)
                        local < 0.75f -> lerp(-8f, 4f, (local - 0.5f) / 0.25f)
                        else -> lerp(4f, 0f, (local - 0.75f) / 0.25f)
                    }
                } else 0f

                val fallStart = stretchFrac + breakFrac
                val fallT = ((t - fallStart) / (1f - fallStart)).coerceIn(0f, 1f)

                val flyDistancePx = LEAD_IN_FLY_DISTANCE.toPx()
                val dropHeightPx = LEAD_IN_DROP_HEIGHT.toPx()
                val pillW = (baseW * widthMul).coerceAtLeast(1f)
                val x = lerp(originLeft - flyDistancePx, originLeft, flyT)
                val landedY = fullH - thickness
                val y = lerp(landedY - dropHeightPx, landedY, fallT)

                Box(
                    Modifier
                        .offset { IntOffset(x.toInt(), y.toInt()) }
                        .size(pillW.toDp(), thickness.toDp())
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(1f, 1f)
                            rotationZ = rockDeg
                        }
                        .clip(RoundedCornerShape(percent = 50))
                        .background(hue)
                )
            }

            Box(
                Modifier
                    .offset { IntOffset(left.toInt(), top.toInt()) }
                    .size(curW.toDp(), curH.toDp())
                    .clip(RoundedCornerShape(corner.toDp()))
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            clipRect(top = size.height * (1f - state.flood.value)) {
                                this@drawWithContent.drawContent()
                            }
                        }
                ) {
                    // Beat 4 "set": a plain top-level left-to-right wipe (plus slight slide)
                    // mirroring WipeItem.wipeClip in GuideReaderScreen.kt, played once flood
                    // completes.
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
                Box(Modifier.fillMaxSize().border(BORDER_WIDTH, hue, RoundedCornerShape(corner.toDp())))
            }
        }
    }
}
