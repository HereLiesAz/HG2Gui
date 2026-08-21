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
 * Per "THE SIX BEATS" in docs/HG2Gui Run Transition.html, beats 1-3 (stretch, break free, fall)
 * play out via the shared [BreakFreeState] in PillBreakFree.kt before "run the perimeter" - see
 * that file for the choreography itself, and PillPerimeterReveal.kt's own header for why the
 * break-free pill flying off toward the right edge doesn't move where the wrap growth below
 * itself starts from. A trailing "set" beat wipes the revealed content on once the flood
 * completes.
 */

private val WRAP_EASE = CubicBezierEasing(0f, .9f, .1f, 1f)
private const val WRAP_MS = 640
private const val FLOOD_MS = 420
private const val SET_MS = 90
private val BORDER_WIDTH = 3.dp

// How close to the literal screen edge the break-free flight lands - keeps the pill's own
// rounded tip from visually clipping into the edge, matching the reference spec's own margin.
private const val EDGE_MARGIN_PX = 6f

class PillWrapRevealState {
    var origin: Rect by mutableStateOf(Rect.Zero)
    var active: Boolean by mutableStateOf(false)
    val wrap = Animatable(0f)
    val flood = Animatable(0f)
    val breakFree = BreakFreeState()

    // Beat 4 "set": stays at 0 (content hidden) until flood completes, then wipes to 1 - see
    // setWipe in PillPerimeterReveal.kt for the identical idiom and rationale.
    val setWipe = Animatable(1f)

    /**
     * [fullWidthPx]/[fullHeightPx]/[defaultBaseWidthPx] come from the composable's own
     * BoxWithConstraints/density scope, which this plain suspend function has no access to - see
     * PillWrapReveal's own call site.
     */
    suspend fun open(fullWidthPx: Float, fullHeightPx: Float, defaultBaseWidthPx: Float, minThicknessPx: Float) {
        active = true
        // Reset every animatable up front, not only the ones about to move next - if a PREVIOUS
        // open()/close() coroutine was cancelled mid-sequence (a back press, the phase changing
        // while the reveal was still running), whichever animatables its own sequence hadn't
        // reached yet are left holding a stale value from further back still. Starting every run
        // from a fully-specified slate means a cancelled run can never leave a half-drawn frame
        // for the next one to inherit.
        wrap.snapTo(0f)
        flood.snapTo(0f)
        setWipe.snapTo(0f)
        val geometry = geometry(fullWidthPx, fullHeightPx, defaultBaseWidthPx, minThicknessPx)
        breakFree.run(geometry.baseWidthPx, geometry.flightPx, geometry.floorPx)
        wrap.animateTo(1f, tween(WRAP_MS, easing = WRAP_EASE))
        flood.animateTo(1f, tween(FLOOD_MS, easing = WRAP_EASE))
        setWipe.animateTo(1f, tween(SET_MS, easing = WRAP_EASE))
    }

    suspend fun close(fullWidthPx: Float, fullHeightPx: Float, defaultBaseWidthPx: Float, minThicknessPx: Float) {
        // Same defensive snap open() makes, mirrored to the fully-open end of the range - a
        // cancelled open() otherwise leaves an arbitrary animatable short, and close() would
        // retreat whatever partial frame that left rather than the intended fully-drawn one.
        wrap.snapTo(1f)
        flood.snapTo(1f)
        // Left at 0 (content hidden) through the rest of the closing sequence below - the frame
        // itself is retreating anyway, so there's nothing to "gate" by leaving it hidden; forcing
        // it back to 1 here used to pop content instantly back into view mid-close.
        setWipe.animateTo(0f, tween(SET_MS, easing = WRAP_EASE))
        flood.animateTo(0f, tween(FLOOD_MS, easing = WRAP_EASE))
        wrap.animateTo(0f, tween(WRAP_MS, easing = WRAP_EASE))
        val geometry = geometry(fullWidthPx, fullHeightPx, defaultBaseWidthPx, minThicknessPx)
        breakFree.reverse(geometry.baseWidthPx, geometry.flightPx, geometry.floorPx)
        active = false
    }

    private fun geometry(
        fullWidthPx: Float,
        fullHeightPx: Float,
        defaultBaseWidthPx: Float,
        minThicknessPx: Float
    ): WrapBreakFreeGeometry {
        val thickness = if (origin.height > 0f) origin.height else minThicknessPx
        val originLeft = if (origin.width > 0f) origin.left else 0f
        val originTop = if (origin.height > 0f) origin.top else fullHeightPx - thickness
        val baseWidthPx = if (origin.width > 0f) origin.width else defaultBaseWidthPx
        val flightPx = (fullWidthPx - EDGE_MARGIN_PX - originLeft - baseWidthPx).coerceAtLeast(0f)
        val floorPx = (fullHeightPx - thickness - originTop).coerceAtLeast(0f)
        return WrapBreakFreeGeometry(baseWidthPx, flightPx, floorPx)
    }
}

private data class WrapBreakFreeGeometry(val baseWidthPx: Float, val flightPx: Float, val floorPx: Float)

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
            // The wrap growth always starts from [origin] itself - the break-free pill above is
            // a flourish flying off to the side, not a relocation of where this frame grows from
            // (see PillPerimeterReveal.kt's own header for the reasoning this file shares).
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

            if (state.breakFree.active) {
                // Beats 1-3: a stand-in pill (the real stack lives in PillMenu's own tree) that
                // strains, breaks free, flies toward the right edge, thuds, and falls onto the
                // bottom edge - ending clear of where the wrap rect above grows from.
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
                            transformOrigin = TransformOrigin(1f, 0.5f) // the tip, not the corner
                            rotationZ = state.breakFree.tilt.value
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
