package com.hereliesaz.hg2gui.ui.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import kotlin.math.min

/*
 * A stack that moves as one sways: it is a pile, and the higher a pill sits the further it
 * swings, hinged on its visible right end, damping out in three diminishing swings. Applies to
 * every shared-clock stack move - arrive, leave, host drop - never to a single pill moving
 * alone, and never as a loop.
 */

private const val BASE_AMPLITUDE_DEG = 0.9f
private const val AMPLITUDE_PER_ROW_DEG = 0.7f
private const val MAX_AMPLITUDE_DEG = 3.4f

/** Degrees. Grows with height in the pile, capped so the top row never flails. */
fun wobbleAmplitude(row: Int): Float =
    min(BASE_AMPLITUDE_DEG + row * AMPLITUDE_PER_ROW_DEG, MAX_AMPLITUDE_DEG)

/**
 * Out, back, again smaller, settled - then it stops. Runs over the same duration as the move it
 * accompanies, so the sway ends exactly when the pill stops travelling. [amplitudeScale] doubles
 * it for [StackEntrance.Rally], where the pile is struck from both sides at once; every other
 * caller leaves it at 1.
 *
 * Additive by convention: this animates its own receiver from 0 back to 0, so callers sum its
 * value onto whatever rotation the pill already holds (`rotationZ = turn.value + sway.value` for
 * ChildPill's own hinge, say) rather than assigning it directly - the Web Animations
 * `composite: 'add'` the reference specimen uses, since a Compose [Animatable] has no built-in
 * composition of its own.
 */
suspend fun Animatable<Float, AnimationVector1D>.wobble(row: Int, durationMs: Int, amplitudeScale: Float = 1f) {
    val a = wobbleAmplitude(row) * amplitudeScale
    snapTo(0f)
    animateTo(0f, keyframes {
        durationMillis = durationMs
        0f at 0 using LinearEasing
        a at (durationMs * SWING_1_FRACTION).toInt() using LinearEasing
        (-a * SWING_2_SCALE) at (durationMs * SWING_2_FRACTION).toInt() using LinearEasing
        (a * SWING_3_SCALE) at (durationMs * SWING_3_FRACTION).toInt() using LinearEasing
        0f at durationMs
    })
}

// Three diminishing swings, each a fraction of the move's own duration and a scale of the first
// swing's own amplitude.
private const val SWING_1_FRACTION = 0.30f
private const val SWING_2_FRACTION = 0.58f
private const val SWING_2_SCALE = 0.55f
private const val SWING_3_FRACTION = 0.82f
private const val SWING_3_SCALE = 0.24f
