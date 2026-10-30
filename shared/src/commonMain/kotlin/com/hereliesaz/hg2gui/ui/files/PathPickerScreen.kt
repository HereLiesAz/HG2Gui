package com.hereliesaz.hg2gui.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hereliesaz.hg2gui.ui.AzphaltChip
import com.hereliesaz.hg2gui.ui.BackStepState
import com.hereliesaz.hg2gui.ui.StatusKind
import com.hereliesaz.hg2gui.ui.liveRegionPolite
import com.hereliesaz.hg2gui.ui.maxContentWidth
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.contrastingText
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.statusColor
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface

private const val HUE_PRIMARY = 6
private const val ROW_MIN_HEIGHT_DP = 48

/**
 * The graphical file/folder browser behind the Select File/Folder pill (see PillPerimeterReveal
 * for the entrance/exit motion this renders inside of). Tapping a folder descends into it;
 * tapping a file selects it immediately. A fixed SELECT THIS FOLDER pill lets a folder-typed
 * parameter be satisfied by the currently open directory itself, since nothing here knows ahead
 * of time whether the command it's filling in wants a file or a directory.
 *
 * Going up is possible from anywhere: an UP row at the top of the list goes to the parent, and the
 * breadcrumb strip (shared with the Move/Copy [FolderPicker]) jumps to any ancestor - so a picker
 * that opens in the session's working directory can still reach `/sdcard/Download`. System back
 * undoes the navigation in the order it happened.
 */
@Composable
fun PathPickerScreen(
    startPath: String,
    listDir: suspend (path: String) -> List<VfsEntry>,
    onSelectFile: (path: String) -> Unit,
    onSelectFolder: (path: String) -> Unit,
    onCancel: () -> Unit,
    // UI-3: reports whether this picker has navigated away from where it opened, so system
    // back/the edge gesture steps back one folder at a time instead of always cancelling
    // the whole picker the way [onCancel] itself does. See BackStepState's own doc.
    backStep: BackStepState,
    modifier: Modifier = Modifier
) {
    var currentPath by remember(startPath) { mutableStateOf(startPath) }
    // Every folder this picker has been in before [currentPath], oldest first - popping the last
    // entry is "back", the same drill-history PillMenu's own trail and FilesScreen's own openChain
    // already track. Going *up* pushes too (the folder you left), so back always returns to where
    // you just were.
    var pathHistory by remember(startPath) { mutableStateOf<List<String>>(emptyList()) }
    var entries by remember { mutableStateOf<List<VfsEntry>>(emptyList()) }

    var listError by remember { mutableStateOf<String?>(null) }
    // Which path [entries] was last read for, so an empty list is "empty" only once the listing
    // for [currentPath] has really come back.
    var loadedPath by remember { mutableStateOf<String?>(null) }

    fun navigateTo(path: String) {
        if (path == currentPath) return
        pathHistory = pathHistory + currentPath
        currentPath = path
    }

    LaunchedEffect(currentPath) {
        // A listing that throws (permissions revoked, the directory vanished) used to crash the
        // composition; show it and leave an empty list so the user can still go up or cancel.
        try {
            entries = listDir(currentPath).sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            listError = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            entries = emptyList()
            listError = "Couldn't open this folder: ${e.message ?: "unknown error"}"
        }
        loadedPath = currentPath
    }

    DisposableEffect(backStep) {
        onDispose { backStep.reset() }
    }

    SideEffect {
        backStep.canStepBack = pathHistory.isNotEmpty()
        backStep.stepBack = {
            pathHistory.lastOrNull()?.let { previous ->
                currentPath = previous
                pathHistory = pathHistory.dropLast(1)
            }
        }
    }

    val atRoot = currentPath.trim('/').isEmpty()
    val muted = statusColor(StatusKind.Muted)

    Column(modifier.fillMaxSize().background(Azphalt.currentGround.pageBrush()).maxContentWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AzphaltChip("‹ CANCEL", onClick = onCancel, contentDescription = "Cancel")
        }

        // The breadcrumb doubles as the header: it opens scrolled to the end, so a deep working
        // directory shows the folder you are in, and every ancestor is one tap away.
        PathCrumbs(currentPath, onNavigate = { navigateTo(it) }, modifier = Modifier.padding(horizontal = 20.dp))

        listError?.let {
            Text(
                it,
                color = statusColor(StatusKind.Error),
                style = captionTextStyle(),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp).liveRegionPolite()
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!atRoot) {
                item(key = "__parent__") {
                    val parent = vfsParentPath(currentPath)
                    PickerRow(
                        background = Azphalt.Ink.copy(alpha = .10f),
                        onClickLabel = "Go up to $parent",
                        onClick = { navigateTo(parent) }
                    ) {
                        Text("‹ UP", color = Azphalt.currentGround.onPage, style = nameTextStyle(), maxLines = 1)
                        Text(
                            parent, color = muted, style = captionTextStyle(),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            if (entries.isEmpty() && listError == null) {
                item(key = "__status__") {
                    StatusLabel(
                        if (loadedPath == currentPath) "THIS FOLDER IS EMPTY" else "LOADING…",
                        Modifier.padding(top = 8.dp)
                    )
                }
            }
            items(entries, key = { it.path }) { entry ->
                val hue: Color? = if (entry.isDirectory) Azphalt.hues[Azphalt.hueOf(entry.path)] else null
                PickerRow(
                    background = hue ?: Azphalt.Ink.copy(alpha = .06f),
                    onClickLabel = if (entry.isDirectory) "Open folder ${entry.name}" else "Select file ${entry.name}",
                    onClick = {
                        if (entry.isDirectory) navigateTo(entry.path) else onSelectFile(entry.path)
                    }
                ) {
                    // Real case, always: a filename is a literal, and on a case-sensitive
                    // filesystem README and readme are different files. The text colour is
                    // whichever of Ink/White reads better on the folder's own hue.
                    Text(
                        if (entry.isDirectory) entry.name + "/" else entry.name,
                        color = hue?.contrastingText() ?: Azphalt.currentGround.onPage,
                        style = nameTextStyle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AzphaltChip(
                "SELECT THIS FOLDER", onClick = { onSelectFolder(currentPath) },
                modifier = Modifier.fillMaxWidth(), tint = Azphalt.hues[HUE_PRIMARY],
                contentDescription = "Select this folder: $currentPath"
            )
        }
    }
}

/** One 48dp-minimum pill row of the list; [onClickLabel] is what a screen reader says the tap does. */
@Composable
private fun PickerRow(
    background: Color,
    onClickLabel: String,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT_DP.dp)
            .clip(AzphaltSurface.capsule)
            .background(background)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}
