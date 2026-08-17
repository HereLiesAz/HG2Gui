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
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.theme.appTypography

@Composable
fun HG2GuiTheme(scale: Float = 1f, content: @Composable () -> Unit) {
    // Reads Azphalt.currentGround (a mutableStateOf) so this recomposes on every ground reroll -
    // background/onBackground used to be pinned to Mustard's own Yellow/Ink regardless of which
    // ground was actually showing, a leftover from before ground-rotation existed. Any Text left
    // to Compose's default color (no explicit `color =`) reads via onBackground, so this is the
    // one place that fixes every one of those at once; text that sets an explicit Azphalt.Ink
    // still needs its own fix wherever it sits directly on the page.
    val ground = Azphalt.currentGround
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = ground.page,
            onBackground = ground.onPage,
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
