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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
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
 */

private val LEG_EASE = CubicBezierEasing(0f, .9f, .1f, 1f)
private const val LEG_MS = 260
private const val FLOOD_MS = 420
private val MIN_THICKNESS = 34.dp

class PerimeterRevealState {
    var origin: Rect by mutableStateOf(Rect.Zero)
    var active: Boolean by mutableStateOf(false)
    val bottomLeg = Animatable(0f)
    val rightLeg = Animatable(0f)
    val topLeg = Animatable(0f)
    val leftLeg = Animatable(0f)
    val flood = Animatable(0f)

    /** Runs the four legs in order - bottom, right, top - then the last leg (left) and the
     *  content wipe together, since the wipe is defined to start the moment that leg does. */
    suspend fun open() {
        active = true
        listOf(bottomLeg, rightLeg, topLeg, leftLeg, flood).forEach { it.snapTo(0f) }
        bottomLeg.animateTo(1f, tween(LEG_MS, easing = LEG_EASE))
        rightLeg.animateTo(1f, tween(LEG_MS, easing = LEG_EASE))
        topLeg.animateTo(1f, tween(LEG_MS, easing = LEG_EASE))
        coroutineScope {
            launch { leftLeg.animateTo(1f, tween(LEG_MS, easing = LEG_EASE)) }
            launch { flood.animateTo(1f, tween(FLOOD_MS, easing = LEG_EASE)) }
        }
    }

    /** The exact reverse of [open]: the wipe and the left leg retreat together first, then the
     *  remaining three legs unwind in reverse order back to the crumb's own rect. */
    suspend fun close() {
        coroutineScope {
            launch { flood.animateTo(0f, tween(FLOOD_MS, easing = LEG_EASE)) }
            launch { leftLeg.animateTo(0f, tween(LEG_MS, easing = LEG_EASE)) }
        }
        topLeg.animateTo(0f, tween(LEG_MS, easing = LEG_EASE))
        rightLeg.animateTo(0f, tween(LEG_MS, easing = LEG_EASE))
        bottomLeg.animateTo(0f, tween(LEG_MS, easing = LEG_EASE))
        active = false
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
                    ) { content() }
                }
            }
        }
    }
}
