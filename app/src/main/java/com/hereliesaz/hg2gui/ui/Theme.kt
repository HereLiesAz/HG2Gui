package com.hereliesaz.hg2gui.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun HG2GuiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Azphalt.Yellow,
            onBackground = Azphalt.Ink,
            surface = Azphalt.Ink,
            onSurface = Azphalt.Yellow,
            primary = Azphalt.hues[6],      // red = the primary action, per Azphalt
            onPrimary = Azphalt.White
        ),
        typography = AzphaltType,
        content = content
    )
}
