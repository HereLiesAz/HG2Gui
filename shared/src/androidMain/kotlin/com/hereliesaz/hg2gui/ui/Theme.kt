package com.hereliesaz.hg2gui.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.database.ContentObserver
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType
import com.hereliesaz.hg2gui.ui.theme.appTypography
import com.hereliesaz.hg2gui.ui.theme.azphaltType

/**
 * The root of every HG2Gui Activity's content. Provides, for everything beneath it:
 * - the Material colour scheme, typography and `LocalContentColor`, all following the rolled
 *   [Azphalt.currentGround] (recomposes on a ground reroll);
 * - [LocalAzphaltType] (the Jost type scale) and [LocalAzphaltUiScale];
 * - a root background painted with the ground's [pageBrush], so the very first composed frame, a
 *   "Loading..." screen and any area a screen forgets to fill all show the ground rather than the
 *   window's own colour;
 * - the system animator scale into [AzphaltMotion] (live, via a settings observer);
 * - the window chrome: window background and status/navigation icon tint matched to the ground,
 *   instead of following the *system* light/dark theme.
 *
 * [scale] is the user's Text-size setting (1 = 100%): it multiplies the whole UI's density, while
 * font scaling stays the system's.
 */
@Composable
fun HG2GuiTheme(scale: Float = 1f, content: @Composable () -> Unit) {
    // Reads Azphalt.currentGround (a mutableStateOf) so this recomposes on every ground reroll -
    // background/onBackground used to be pinned to Mustard's own Yellow/Ink regardless of which
    // ground was actually showing, a leftover from before ground-rotation existed.
    val ground = Azphalt.currentGround
    val animatorScale = rememberSystemAnimatorScale()
    val view = LocalView.current
    SideEffect {
        AzphaltMotion.update(animatorScale)
        if (!view.isInEditMode) view.context.findActivity()?.window?.applyAzphaltGround(ground)
    }
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
            LocalAzphaltType provides azphaltType(),
            LocalAzphaltUiScale provides scale
        ) {
            Box(Modifier.fillMaxSize().background(ground.pageBrush())) {
                content()
            }
        }
    }
}

/**
 * Paints this window with [ground] before (or independently of) Compose: the window background, so
 * nothing flashes a different colour behind the first frame, a keyboard resize or an overscroll;
 * and light/dark status- and navigation-bar icons chosen for the ground's own text colour (dark
 * icons on a light ground), not for the system theme.
 *
 * `HG2GuiTheme` calls it on every composition. An Activity should also call it from `onCreate`
 * *before* `setContent` (after `enableEdgeToEdge()`), so a launch on a dark ground does not start
 * with a light window. The ground is already rolled at process start, so the right colour is known
 * by then.
 */
fun Window.applyAzphaltGround(ground: Azphalt.Ground = Azphalt.currentGround) {
    setBackgroundDrawable(ColorDrawable(ground.page.toArgb()))
    val lightGround = ground.onPage == Azphalt.Ink
    val controller = WindowCompat.getInsetsController(this, decorView)
    controller.isAppearanceLightStatusBars = lightGround
    controller.isAppearanceLightNavigationBars = lightGround
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun readAnimatorScale(context: Context): Float =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

/**
 * The system "Animator duration scale" (Developer options; "Remove animations" sets it to 0),
 * kept live: a settings observer updates it while the screen is up, so toggling the preference
 * takes effect without relaunching.
 */
@Composable
private fun rememberSystemAnimatorScale(): Float {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(readAnimatorScale(context)) }
    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scale = readAnimatorScale(context)
            }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return scale
}
