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
import com.hereliesaz.hg2gui.ui.theme.appTypography

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
        typography = appTypography()
    ) {
        val base = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(base.density * scale, base.fontScale)
        ) {
            content()
        }
    }
}
