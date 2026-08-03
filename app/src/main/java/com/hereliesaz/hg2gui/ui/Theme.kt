package com.hereliesaz.hg2gui.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.R
import com.hereliesaz.hg2gui.ui.menu.Azphalt

/*
 * Azphalt: one family, Jost. Weight, case and tracking carry every distinction.
 * Drop jost_medium/semibold/extrabold/black .ttf into app/src/main/res/font/.
 */

val Jost = FontFamily(
    Font(R.font.jost_medium, FontWeight.Medium),
    Font(R.font.jost_semibold, FontWeight.SemiBold),
    Font(R.font.jost_extrabold, FontWeight.ExtraBold),
    Font(R.font.jost_black, FontWeight.Black)
)

private val AzphaltType = Typography(
    displayLarge = TextStyle(
        fontFamily = Jost, fontWeight = FontWeight.Black,
        fontSize = 46.sp, lineHeight = 39.sp, letterSpacing = (-0.02).em
    ),
    titleMedium = TextStyle(   // capsule label
        fontFamily = Jost, fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp, letterSpacing = 0.09.em
    ),
    labelSmall = TextStyle(    // eyebrow
        fontFamily = Jost, fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp, letterSpacing = 0.28.em
    ),
    bodyMedium = TextStyle(
        fontFamily = Jost, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp
    )
)

/**
 * [scale] resizes the whole UI — pills, text, padding, and the pixel-space every pill
 * animation measures itself against — uniformly, by overriding the ambient density rather than
 * threading a multiplier through every dp/sp literal in TerminalScreen/PillMenu.
 *
 * Compose resolves every `Dp` as `value * density.density`, and every `TextUnit` (sp) as
 * `value * density.fontScale * density.density`. Multiplying only `density` (and leaving
 * `fontScale` — the system accessibility text-size setting — untouched) scales both by the same
 * factor, so the whole layout grows or shrinks together instead of only the text resizing
 * against fixed-size pills. Every animation in PillMenu already converts its own dp constants
 * to px via `LocalDensity.current` at the point it runs, so it picks up the scaled space with no
 * changes of its own.
 */
@Composable
fun HG2GuiTheme(scale: Float = 1f, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Azphalt.Yellow,
            onBackground = Azphalt.Ink,
            surface = Azphalt.Ink,
            onSurface = Azphalt.Yellow,
            primary = Azphalt.hues[6],      // red = the primary action, per Azphalt
            onPrimary = Azphalt.White
        ),
        typography = AzphaltType
    ) {
        val base = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(base.density * scale, base.fontScale)
        ) {
            content()
        }
    }
}
