package com.hereliesaz.hg2gui.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hereliesaz.hg2gui.ui.AzphaltChip
import com.hereliesaz.hg2gui.ui.AzphaltHeading
import com.hereliesaz.hg2gui.ui.BackStepState
import com.hereliesaz.hg2gui.ui.StatusKind
import com.hereliesaz.hg2gui.ui.screenInsets
import com.hereliesaz.hg2gui.ui.statusColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import com.hereliesaz.hg2gui.ui.menu.PillMenu

/*
 * The Guide and the command tree are both command-entry surfaces. The tree exposes the runtime
 * structurally; the Guide exposes the same world through explanation and narrative. Picking a
 * command in either surface writes it into the terminal input for review instead of executing it.
 *
 * A command tapped in the *reader* never leaves the Guide: it is appended to whatever the user is
 * composing ([onCommandAppended]) and the reader shows its own "added to input" confirmation, so
 * reading and composing can interleave. Only a finished pick from the pill tree below returns to
 * the terminal.
 */
@Composable
fun CommandGuideScreen(
    tree: List<MenuNode>,
    fullscreen: Boolean,
    /** tokens picked so far; complete = a whole command was chosen (terminal pill). */
    onCommandSelected: (tokens: List<String>, complete: Boolean) -> Unit,
    onBack: () -> Unit,
    backStep: BackStepState,
    modifier: Modifier = Modifier,
    /**
     * A command was tapped in the reader: append it to the composition, stay on the Guide. When
     * null it is reported through [onCommandSelected] as an incomplete single-token pick - never
     * as complete, which would close the Guide.
     */
    onCommandAppended: ((command: String) -> Unit)? = null
) {
    var readingGuide by remember { mutableStateOf(false) }
    val runtimeIndex = remember(tree) { GuideRuntimeIndex.fromTree(tree) }

    if (readingGuide) {
        GuideReaderScreen(
            fullscreen = fullscreen,
            onBack = { readingGuide = false },
            onCommandSelected = { command ->
                if (onCommandAppended != null) onCommandAppended(command) else onCommandSelected(listOf(command), false)
            },
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
            .screenInsets(fullscreen, ime = false)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AzphaltChip("‹ Back", onClick = onBack, contentDescription = "Back to the terminal")
            AzphaltChip(
                "The Guide", onClick = { readingGuide = true },
                tint = Azphalt.hues[6],
                contentDescription = "Open the Guide: command reference and walkthroughs"
            )
        }

        AzphaltHeading("Guide", Modifier.padding(horizontal = 20.dp, vertical = 6.dp))

        Text(
            "Pick a command to drop it into the input — nothing here runs on its own.",
            color = statusColor(StatusKind.Muted),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
        )

        PillMenu(
            roots = tree,
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 12.dp),
            onRun = { picked, isTerminal -> onCommandSelected(picked, isTerminal) }
        )
    }
}
