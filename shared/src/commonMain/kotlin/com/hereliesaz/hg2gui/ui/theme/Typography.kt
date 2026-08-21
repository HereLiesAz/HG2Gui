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
 * The eight steps of the HG2Gui scale. One family; weight and tracking do the rest.
 * Everything is uppercase except [body], the only lowercase in the system.
 * Call sites uppercase their own strings - a TextStyle cannot do it.
 */
@Composable
fun azphaltType(): AzphaltType {
    val f = jostFontFamily()
    return AzphaltType(
        hero = TextStyle(
            fontFamily = f, fontWeight = FontWeight.Black,
            fontSize = 58.sp, lineHeight = 49.sp, letterSpacing = (-0.03).em
        ),
        section = TextStyle(
            fontFamily = f, fontWeight = FontWeight.Black,
            fontSize = 40.sp, lineHeight = 40.sp, letterSpacing = (-0.02).em
        ),
        lead = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 32.sp),
        // only lowercase
        body = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 25.5.sp),
        capsule = TextStyle(
            fontFamily = f, fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp, lineHeight = 15.sp, letterSpacing = 0.09.em
        ),
        eyebrow = TextStyle(
            fontFamily = f, fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp, lineHeight = 11.sp, letterSpacing = 0.28.em
        ),
        endCap = TextStyle(
            fontFamily = f, fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp, lineHeight = 10.sp, letterSpacing = 0.10.em
        ),
        micro = TextStyle(
            fontFamily = f, fontWeight = FontWeight.ExtraBold,
            fontSize = 9.sp, lineHeight = 9.sp, letterSpacing = 0.18.em
        )
    )
}

data class AzphaltType(
    val hero: TextStyle, val section: TextStyle, val lead: TextStyle, val body: TextStyle,
    val capsule: TextStyle, val eyebrow: TextStyle, val endCap: TextStyle, val micro: TextStyle
)

val LocalAzphaltType = staticCompositionLocalOf<AzphaltType> { error("No AzphaltType") }

// Provided alongside LocalContentColor in ui/Theme.kt's existing CompositionLocalProvider.
// Keep appTypography() below for the Material components that demand a Typography, but no
// HG2Gui screen should read MaterialTheme.typography directly once migration is done.
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
