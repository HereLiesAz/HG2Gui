package com.hereliesaz.hg2gui.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.pageBrush

/*
 * A handful of Azphalt tokens for screens outside the menu (the splash, the design-system
 * reference screen) that don't otherwise depend on PillMenu.kt. Everything here delegates to
 * PillMenu.kt's `Azphalt` object rather than keeping its own copy of the numbers - a hand-copied
 * palette drifted out of sync with the real one before (the wrong Yellow, and only 10 of what are
 * now 14 hues), silently, since nothing compared the two. Delegating makes that class of drift
 * impossible instead of merely documented.
 */
object AzphaltColors {
    val Ink = Azphalt.Ink
    val Yellow = Azphalt.Yellow
    val White = Azphalt.White

    val hues = Azphalt.hues
    val hueCaps = Azphalt.caps

    // Semantic overrides — hue alone carries no meaning, these two do.
    val Acquire = Azphalt.hues[6] // red — primary / run
    val Done = Azphalt.hues[2]    // teal — acquired / receipts
    val Selected = Ink            // selected / open
    val IdleWash = Ink.copy(alpha = 0.14f)

    val page: Brush = Azphalt.grounds.first().pageBrush()

    /** Deterministic hue assignment by hashing an identifier — never by hand. */
    fun hueOf(id: String): Int = Azphalt.hueOf(id)
}

object AzphaltMotion {
    // Fast-out, long-settle — the one curve the whole system uses. Nothing else.
    val unfoldEasing = CubicBezierEasing(0f, .9f, .1f, 1f)
    const val UNFOLD_MS = 420
}
