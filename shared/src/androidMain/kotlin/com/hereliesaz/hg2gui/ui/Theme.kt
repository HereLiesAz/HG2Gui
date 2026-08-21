package com.hereliesaz.hg2gui.ui

import androidx.compose.material3.LocalContentColor
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
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType
import com.hereliesaz.hg2gui.ui.theme.appTypography
import com.hereliesaz.hg2gui.ui.theme.azphaltType

@Composable
fun HG2GuiTheme(scale: Float = 1f, content: @Composable () -> Unit) {
    // Reads Azphalt.currentGround (a mutableStateOf) so this recomposes on every ground reroll -
    // background/onBackground used to be pinned to Mustard's own Yellow/Ink regardless of which
    // ground was actually showing, a leftover from before ground-rotation existed.
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
            LocalDensity provides Density(base.density * scale, base.fontScale),
            // colorScheme.onBackground above is inert on its own - Material3 only ever reads it
            // into LocalContentColor (what an uncolored Text() actually falls back to) from
            // inside a Surface, and nothing here wraps content in one. Without this, every
            // uncolored Text() - e.g. TerminalScreen's own Eyebrow("01 — Command tree"), which
            // sets no `color` at all - fell back to Compose's own hardcoded LocalContentColor
            // default (plain black) regardless of ground, the same "assumes the light Mustard
            // page" bug this whole fix exists to kill, just one level removed from onBackground.
            LocalContentColor provides ground.onPage,
            LocalAzphaltType provides azphaltType()
        ) {
            content()
        }
    }
}
