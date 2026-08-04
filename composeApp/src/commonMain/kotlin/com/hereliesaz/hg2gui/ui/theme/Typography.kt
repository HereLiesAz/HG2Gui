package com.hereliesaz.hg2gui.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import hg2gui.composeapp.generated.resources.Res
import hg2gui.composeapp.generated.resources.jost_black
import hg2gui.composeapp.generated.resources.jost_medium
import hg2gui.composeapp.generated.resources.jost_semibold
import hg2gui.composeapp.generated.resources.jost_extrabold
import org.jetbrains.compose.resources.Font

@Composable
fun jostFontFamily() = FontFamily(
    Font(Res.font.jost_black, FontWeight.Black),
    Font(Res.font.jost_medium, FontWeight.Medium),
    Font(Res.font.jost_semibold, FontWeight.SemiBold),
    Font(Res.font.jost_extrabold, FontWeight.ExtraBold)
)

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
