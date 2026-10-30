package com.hereliesaz.hg2gui.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import hg2gui.shared.generated.resources.Res
import hg2gui.shared.generated.resources.jost_black
import hg2gui.shared.generated.resources.jost_semibold
import hg2gui.shared.generated.resources.jost_extrabold
import org.jetbrains.compose.resources.Font

@Composable
fun jostFontFamily() = FontFamily(
    Font(Res.font.jost_black, FontWeight.Black),
    Font(Res.font.jost_semibold, FontWeight.SemiBold),
    Font(Res.font.jost_extrabold, FontWeight.ExtraBold)
)

/**
 * The steps of the HG2Gui scale. One family; weight and tracking do the rest.
 * Everything is uppercase except [AzphaltType.body], the only lowercase in the system.
 * Call sites uppercase their own strings - a TextStyle cannot do it.
 *
 * Read the result from [LocalAzphaltType] (provided by `HG2GuiTheme`); do not hard-code `fontSize`
 * literals in screens. Nothing in this scale is below 11sp: that is the floor for text a person is
 * expected to read, so a one-line change here moves every consumer together.
 */
@Composable
fun azphaltType(): AzphaltType = buildAzphaltType(jostFontFamily())

/**
 * The same scale on the platform default font. This is what [LocalAzphaltType] holds when no theme
 * has provided one (the desktop preview, a unit test, a `@Preview`), so a composable that reads the
 * scale never crashes outside `HG2GuiTheme`; it just renders in the system face instead of Jost.
 */
fun fallbackAzphaltType(): AzphaltType = buildAzphaltType(FontFamily.Default)

@Suppress("LongMethod")
private fun buildAzphaltType(f: FontFamily): AzphaltType = AzphaltType(
    hero = TextStyle(
        fontFamily = f, fontWeight = FontWeight.Black,
        fontSize = 58.sp, lineHeight = 49.sp, letterSpacing = (-0.03).em
    ),
    section = TextStyle(
        fontFamily = f, fontWeight = FontWeight.Black,
        fontSize = 40.sp, lineHeight = 40.sp, letterSpacing = (-0.02).em
    ),
    // A screen title. Large enough to read as the heading of the page (the 11sp [eyebrow] cannot),
    // small enough to share a row with a BACK chip on a 360dp phone. Pair with AzphaltHeading.
    heading = TextStyle(
        fontFamily = f, fontWeight = FontWeight.Black,
        fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em
    ),
    lead = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 32.sp),
    // only lowercase
    body = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 25.5.sp),
    capsule = TextStyle(
        fontFamily = f, fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp, lineHeight = 18.sp, letterSpacing = 0.09.em
    ),
    // The label of a pill in the command stack (PillMenu's Pill). 13sp keeps most labels whole in
    // the stack's right-anchored geometry while clearing Android's 12sp readability guidance.
    pill = TextStyle(
        fontFamily = f, fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.09.em
    ),
    // The little badge at a pill's right end (a count, a type tag).
    pillCap = TextStyle(
        fontFamily = f, fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.06.em
    ),
    // Label of AzphaltChip, the one shared small button. Every chip in the app takes this size.
    chip = TextStyle(
        fontFamily = f, fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.09.em
    ),
    eyebrow = TextStyle(
        fontFamily = f, fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.28.em
    ),
    endCap = TextStyle(
        fontFamily = f, fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.10.em
    ),
    // Kept for source compatibility. It used to be 9sp; nothing below 11sp is legible enough to
    // ship, so it is now the same size as [endCap] with wider tracking.
    micro = TextStyle(
        fontFamily = f, fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.18.em
    )
)

/**
 * The HG2Gui type scale. [hero] .. [micro] are ordered large to small; [heading], [pill],
 * [pillCap] and [chip] are the working sizes for screen titles, command-stack pills, pill badges
 * and shared chips. Obtain via [LocalAzphaltType].
 */
data class AzphaltType(
    val hero: TextStyle,
    val section: TextStyle,
    val heading: TextStyle,
    val lead: TextStyle,
    val body: TextStyle,
    val capsule: TextStyle,
    val pill: TextStyle,
    val pillCap: TextStyle,
    val chip: TextStyle,
    val eyebrow: TextStyle,
    val endCap: TextStyle,
    val micro: TextStyle
)

/**
 * The active [AzphaltType]. `HG2GuiTheme` provides the Jost version; outside it this is
 * [fallbackAzphaltType] (system font, same sizes) rather than an error.
 */
val LocalAzphaltType = staticCompositionLocalOf { fallbackAzphaltType() }

// Provided alongside LocalContentColor in HG2GuiTheme (androidMain ui/Theme.kt). Keep appTypography()
// below for the Material components that demand a Typography, but no HG2Gui screen should read
// MaterialTheme.typography directly once migration is done.
@Composable
fun appTypography() = Typography().let {
    val family = jostFontFamily()
    it.copy(
        displayLarge = it.displayLarge.copy(fontFamily = family),
        displayMedium = it.displayMedium.copy(fontFamily = family),
        displaySmall = it.displaySmall.copy(fontFamily = family),
        headlineLarge = it.headlineLarge.copy(fontFamily = family),
        headlineMedium = it.headlineMedium.copy(fontFamily = family),
        headlineSmall = it.headlineSmall.copy(fontFamily = family),
        titleLarge = it.titleLarge.copy(fontFamily = family),
        titleMedium = it.titleMedium.copy(fontFamily = family),
        titleSmall = it.titleSmall.copy(fontFamily = family),
        bodyLarge = it.bodyLarge.copy(fontFamily = family),
        bodyMedium = it.bodyMedium.copy(fontFamily = family),
        bodySmall = it.bodySmall.copy(fontFamily = family),
        labelLarge = it.labelLarge.copy(fontFamily = family),
        labelMedium = it.labelMedium.copy(fontFamily = family),
        labelSmall = it.labelSmall.copy(fontFamily = family)
    )
}
