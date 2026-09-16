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
 * The Guide and the command tree are both command-entry surfaces. The tree exposes the runtime
 * structurally; the Guide exposes the same world through explanation and narrative. Picking a
 * command in either surface writes it into the terminal input for review instead of executing it.
 */
@Composable
fun CommandGuideScreen(
    tree: List<MenuNode>,
    fullscreen: Boolean,
    onCommandSelected: (List<String>) -> Unit,
    onBack: () -> Unit,
    backStep: BackStepState,
    modifier: Modifier = Modifier
) {
    var readingGuide by remember { mutableStateOf(false) }
    val runtimeIndex = remember(tree) { GuideRuntimeIndex.fromTree(tree) }

    if (readingGuide) {
        GuideReaderScreen(
            fullscreen = fullscreen,
            onBack = { readingGuide = false },
            onCommandSelected = { command -> onCommandSelected(listOf(command)) },
            backStep = backStep,
            runtimeIndex = runtimeIndex,
            modifier = modifier
        )
        return
    }

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
                Modifier.defaultMinSize(minHeight = 48.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Azphalt.Ink)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        "‹ BACK", color = Azphalt.Yellow,
                        fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                    )
                }
            }
            Box(
                Modifier.defaultMinSize(minHeight = 48.dp).clickable { readingGuide = true },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Azphalt.hues[6])
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        "THE GUIDE", color = Azphalt.White,
                        fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                    )
                }
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
            onRun = { picked, _ -> onCommandSelected(picked) }
        )
    }
}
