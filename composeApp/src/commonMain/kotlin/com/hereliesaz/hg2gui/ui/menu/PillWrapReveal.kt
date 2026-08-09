package com.hereliesaz.hg2gui.ui.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/*
 * "The pill becomes the page": a pill runs out around the edge of the screen, closing the loop,
 * then the space it enclosed floods with a vertical wipe that reveals whatever it opened - the
 * design source's own name for this two-beat move is "run the perimeter" + "flood". Simplified
 * here (as elsewhere in this app) from the source's multi-stage keyframe sequence into one
 * continuous rect interpolation plus a clip-based wipe, rather than four separate animated edges.
 */

private val WRAP_EASE = CubicBezierEasing(0f, .9f, .1f, 1f)
private const val WRAP_MS = 640
private const val FLOOD_MS = 420
private val BORDER_WIDTH = 3.dp

class PillWrapRevealState {
    var origin: Rect by mutableStateOf(Rect.Zero)
    var active: Boolean by mutableStateOf(false)
    val wrap = Animatable(0f)
    val flood = Animatable(0f)

    suspend fun open() {
        active = true
        wrap.snapTo(0f)
        flood.snapTo(0f)
        wrap.animateTo(1f, tween(WRAP_MS, easing = WRAP_EASE))
        flood.animateTo(1f, tween(FLOOD_MS, easing = WRAP_EASE))
    }

    suspend fun close() {
        flood.animateTo(0f, tween(FLOOD_MS, easing = WRAP_EASE))
        wrap.animateTo(0f, tween(WRAP_MS, easing = WRAP_EASE))
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
        val left = lerp(o.left, 0f, w)
        val top = lerp(o.top, 0f, w)
        val right = lerp(o.right, fullW, w)
        val bottom = lerp(o.bottom, fullH, w)
        val curW = right - left
        val curH = bottom - top
        val corner = lerp(minOf(o.width, o.height) / 2f, 0f, w)

        with(density) {
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
                ) { content() }
                Box(Modifier.fillMaxSize().border(BORDER_WIDTH, hue, RoundedCornerShape(corner.toDp())))
            }
        }
    }
}
