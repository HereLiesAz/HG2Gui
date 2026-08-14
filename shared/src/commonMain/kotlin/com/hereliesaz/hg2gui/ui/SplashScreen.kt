package com.hereliesaz.hg2gui.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.theme.AzphaltColors
import com.hereliesaz.hg2gui.ui.theme.AzphaltMotion
import kotlinx.coroutines.delay

/*
 * Launch sequence: pops up, counts down "4", then "2", then a thumbs-up, then hands off to the
 * app. Timing/easing reuses the system's one curve (see AzphaltMotion) so it reads as the same
 * hand as everywhere else in the app, just larger.
 *
 * `stageContent` is a seam for the real gesture artwork (the app already has a static logo) —
 * default content is a plain glyph sequence so this compiles and plays standalone until that
 * asset is wired in.
 */
private enum class SplashStage { HIDDEN, FOUR, TWO, THUMBS_UP, DONE }

@Composable
fun SplashScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    stageContent: @Composable (stage: Int) -> Unit = { s -> DefaultGlyph(s) }
) {
    var stage by remember { mutableStateOf(SplashStage.HIDDEN) }
    val scale = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        stage = SplashStage.FOUR
        scale.animateTo(1f, tween(AzphaltMotion.UNFOLD_MS, easing = AzphaltMotion.unfoldEasing))
        delay(650)
        stage = SplashStage.TWO
        delay(650)
        stage = SplashStage.THUMBS_UP
        delay(750)
        stage = SplashStage.DONE
        onFinish()
    }

    Box(modifier.fillMaxSize().background(AzphaltColors.Ink), contentAlignment = Alignment.Center) {
        AnimatedContent(targetState = stage, label = "splash-stage") { s ->
            Box(Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
                when (s) {
                    SplashStage.FOUR -> stageContent(4)
                    SplashStage.TWO -> stageContent(2)
                    SplashStage.THUMBS_UP -> stageContent(1)
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun DefaultGlyph(stage: Int) {
    Text(
        text = if (stage == 1) "\uD83D\uDC4D" else stage.toString(),
        color = AzphaltColors.Yellow,
        fontSize = 96.sp,
        fontWeight = FontWeight.Black
    )
}
