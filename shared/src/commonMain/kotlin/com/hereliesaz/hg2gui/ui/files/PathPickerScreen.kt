package com.hereliesaz.hg2gui.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hereliesaz.hg2gui.ui.BackStepState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.pageBrush

/**
 * The graphical file/folder browser behind the Select File/Folder pill (see PillPerimeterReveal
 * for the entrance/exit motion this renders inside of). Tapping a folder descends into it;
 * tapping a file selects it immediately. A fixed SELECT THIS FOLDER pill lets a folder-typed
 * parameter be satisfied by the currently open directory itself, since nothing here knows ahead
 * of time whether the command it's filling in wants a file or a directory.
 */
@Composable
fun PathPickerScreen(
    startPath: String,
    listDir: suspend (path: String) -> List<VfsEntry>,
    onSelectFile: (path: String) -> Unit,
    onSelectFolder: (path: String) -> Unit,
    onCancel: () -> Unit,
    // UI-3: reports whether a folder tap has drilled this picker deeper than [startPath], so
    // system back/the edge gesture steps up one directory at a time instead of always cancelling
    // the whole picker the way [onCancel] itself does. See BackStepState's own doc.
    backStep: BackStepState,
    modifier: Modifier = Modifier
) {
    var currentPath by remember(startPath) { mutableStateOf(startPath) }
    // Every folder tapped into on the way to [currentPath], oldest first - popping the last entry
    // off this is exactly "go up one directory," the same drill-history PillMenu's own trail and
    // FilesScreen's own openChain already track for their own back-one-level affordances.
    var pathHistory by remember(startPath) { mutableStateOf<List<String>>(emptyList()) }
    var entries by remember { mutableStateOf<List<VfsEntry>>(emptyList()) }

    LaunchedEffect(currentPath) {
        entries = listDir(currentPath).sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    SideEffect {
        backStep.canStepBack = pathHistory.isNotEmpty()
        backStep.stepBack = {
            pathHistory.lastOrNull()?.let { parent ->
                currentPath = parent
                pathHistory = pathHistory.dropLast(1)
            }
        }
    }

    Column(modifier.fillMaxSize().background(Azphalt.currentGround.pageBrush())) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PickerPill("‹ CANCEL", onClick = onCancel)
        }

        Text(
            currentPath,
            color = Azphalt.Ink.copy(alpha = .55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(entries, key = { it.path }) { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (entry.isDirectory) Azphalt.hues[Azphalt.hueOf(entry.path)]
                            else Azphalt.Ink.copy(alpha = .06f)
                        )
                        .clickable {
                            if (entry.isDirectory) {
                                pathHistory = pathHistory + currentPath
                                currentPath = entry.path
                            } else {
                                onSelectFile(entry.path)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        (if (entry.isDirectory) entry.name + "/" else entry.name).uppercase(),
                        color = if (entry.isDirectory) Azphalt.White else Azphalt.Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.09.em,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PickerPill(
                "SELECT THIS FOLDER",
                background = Azphalt.hues[6],
                foreground = Azphalt.White,
                fillWidth = true,
                onClick = { onSelectFolder(currentPath) }
            )
        }
    }
}

@Composable
private fun PickerPill(
    label: String,
    modifier: Modifier = Modifier,
    background: androidx.compose.ui.graphics.Color = Azphalt.Ink,
    foreground: androidx.compose.ui.graphics.Color = Azphalt.Yellow,
    fillWidth: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = foreground, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em)
    }
}
