package com.hereliesaz.hg2gui.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hereliesaz.hg2gui.ui.BackStepState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.Eyebrow
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import com.hereliesaz.hg2gui.ui.menu.PillMenu

/*
 * The guide is a read-through of the exact tree PillMenu itself runs on - picking a command
 * here writes it into the input instead of running it, so browsing and picking are the same
 * interaction the terminal already teaches, not a second UI to learn. Staying open across
 * picks (no auto-close) mirrors how the terminal's own PillMenu behaves: nothing closes it but
 * the user.
 */
@Composable
fun CommandGuideScreen(
    tree: List<MenuNode>,
    fullscreen: Boolean,
    onCommandSelected: (List<String>) -> Unit,
    onBack: () -> Unit,
    // UI-2: reports whether there's an internal level (right now, only "the reader is open") for
    // system back/the edge gesture to step up through before onBack closes this whole screen. When
    // readingGuide is true, GuideReaderScreen (which owns its own index/entry drill-down) takes
    // over reporting to the very same instance - see its own doc comment for how the two levels
    // combine into one step-at-a-time back stack without either screen knowing about the other's
    // state directly.
    backStep: BackStepState,
    modifier: Modifier = Modifier
) {
    // The real Hitchhiker's Guide - a chapter index of parody command entries, not a picker -
    // is nested inside this screen rather than replacing it: this remains "pick a command",
    // that remains "read about one", one pill apart.
    var readingGuide by remember { mutableStateOf(false) }

    if (readingGuide) {
        GuideReaderScreen(
            fullscreen = fullscreen,
            onBack = { readingGuide = false },
            backStep = backStep,
            modifier = modifier
        )
        return
    }

    // Nothing left to step back through at this level - GuideReaderScreen (above) owns backStep
    // entirely while it's showing instead, since this branch never composes at the same time.
    SideEffect {
        backStep.canStepBack = false
        backStep.stepBack = {}
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .then(if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    "‹ BACK", color = Azphalt.Yellow,
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.hues[6])
                    .clickable { readingGuide = true }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    "THE GUIDE", color = Azphalt.White,
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
        }

        Eyebrow("Guide")

        Text(
            "Pick a command to drop it into the input — nothing here runs on its own.",
            color = Azphalt.currentGround.onPage.copy(alpha = .6f),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
        )

        PillMenu(
            roots = tree,
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 12.dp),
            // The guide only ever writes a pick into the input - see the doc comment above -
            // so isTerminal (the auto-run signal) is irrelevant here.
            onRun = { picked, _ -> onCommandSelected(picked) }
        )
    }
}
