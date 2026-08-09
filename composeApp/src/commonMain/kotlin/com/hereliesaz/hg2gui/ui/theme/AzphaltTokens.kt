package com.hereliesaz.hg2gui.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * Design tokens ported 1:1 from the Azphalt capsule style guide (see docs/DESIGN.md / the
 * HG2Gui style-guide DC). PillMenu.kt keeps its own local `Azphalt` object for now — these are
 * the same numbers, exposed as a proper token set for screens outside the menu (Files, the
 * command reference reader, the splash) and for anything that wants to override a slice of the
 * palette per category via [LocalAzphaltPalette].
 */
object AzphaltColors {
    val Ink = Color(0xFF1E1A17)
    val Yellow = Color(0xFFE8C81E)
    val YellowFold = Color(0xFFD9B615)
    val White = Color(0xFFFFFFFF)

    // Fixed assignment order: violet, cyan, teal, green, amber, orange, red, magenta, purple, blue.
    val hues = listOf(
        Color(0xFF6B4FBB), Color(0xFF2FA9C4), Color(0xFF1F9E86), Color(0xFF5AAE34),
        Color(0xFFD9A21C), Color(0xFFD9762A), Color(0xFFC6392F), Color(0xFFB03A6E),
        Color(0xFF8E4FA8), Color(0xFF2E6FB7)
    )
    val hueCaps = listOf(
        Color(0xFF4B3489), Color(0xFF1F7B90), Color(0xFF137060), Color(0xFF3E7D22),
        Color(0xFFA2760F), Color(0xFFA6551A), Color(0xFF93251D), Color(0xFF7F2A4D),
        Color(0xFF653578), Color(0xFF1E4E85)
    )

    // Semantic overrides — hue alone carries no meaning, these five do.
    val Acquire = Color(0xFFC6392F) // red — primary / run
    val Done = Color(0xFF1F9E86)    // teal — acquired / receipts
    val Selected = Ink              // selected / open
    val IdleWash = Ink.copy(alpha = 0.14f)

    val page: Brush = Brush.linearGradient(0f to Yellow, 0.5f to YellowFold, 1f to Yellow)

    /** Deterministic hue assignment by hashing an identifier — never by hand. */
    fun hueOf(id: String): Int = (id.hashCode().let { if (it < 0) -it else it }) % hues.size
}

object AzphaltType {
    val displayTracking = (-0.03).em
    val capsuleLabelTracking = 0.09.em
    val eyebrowTracking = 0.28.em
    val microTracking = 0.18.em

    val displaySize = 54.sp
    val titleSize = 22.sp
    val eyebrowSize = 11.sp
    val microSize = 9.sp
    val bodySize = 15.sp
    val capsuleLabelSize = 11.sp
}

object AzphaltSpacing {
    val capsuleGap = 9.dp
    val sectionGap = 76.dp
    val sidePadding = 44.dp
    val recordRadius = 26.dp
    val noteRadius = 20.dp
    val previewRadius = 18.dp
    val pillRadius = 999.dp
    val pillHeight = 17.dp
    val rowPitch = 20.dp
}

object AzphaltMotion {
    // Fast-out, long-settle — the one curve the whole system uses. Nothing else.
    val unfoldEasing = CubicBezierEasing(0f, .9f, .1f, 1f)
    const val UNFOLD_MS = 420
    const val STAGGER_MS = 26
    const val MAX_STAGGER_ROWS = 20
}

/** A palette slice that can be swapped per top-level category (e.g. mauve/gold/navy grounds). */
data class AzphaltPalette(
    val ground: Brush = AzphaltColors.page,
    val ink: Color = AzphaltColors.Ink,
    val accent: Color = AzphaltColors.hues[0],
    val accentCap: Color = AzphaltColors.hueCaps[0]
)

val LocalAzphaltPalette = staticCompositionLocalOf { AzphaltPalette() }

@Composable
fun AzphaltCategoryTheme(palette: AzphaltPalette, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAzphaltPalette provides palette, content = content)
}
