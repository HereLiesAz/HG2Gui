@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.hereliesaz.hg2gui.ui.files

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import kotlinx.coroutines.launch

/*
 * The file manager: search, sort, multi-select batch actions, in-place rename, an automatic
 * media grid, and a real storage-by-type breakdown, all built on Azphalt's capsule primitive.
 * No icons: a folder is a whole rounded rectangle in its own hue; tapping one expands it while
 * its siblings squish into thin coloured rods beside it, its children living inside it as
 * smaller rectangles - two accordion levels deep, then a plain record of what's inside.
 */

private enum class SortMode(val label: String) { NAME("Name"), NEWEST("Newest") }
private enum class FMScreen { Browse, Search, Storage, PickMove, PickCopy }
private enum class CreateMode { FOLDER, FILE }

private fun sortEntries(list: List<VfsEntry>, mode: SortMode): List<VfsEntry> = when (mode) {
    SortMode.NAME -> list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    SortMode.NEWEST -> list.sortedWith(compareBy({ !it.isDirectory }, { -it.modifiedAt }))
}

@Composable
fun FilesScreen(
    fullscreen: Boolean,
    listDir: suspend (path: String) -> List<VfsEntry>,
    search: suspend (query: String) -> List<VfsSearchResult>,
    storageStats: suspend () -> StorageStats,
    onOpenFile: (path: String) -> Unit,
    onCreateFolder: suspend (parentPath: String, name: String) -> Unit,
    onCreateFile: suspend (parentPath: String, name: String) -> Unit,
    onDelete: suspend (path: String) -> Unit,
    onRename: suspend (path: String, newName: String) -> Unit,
    onMove: suspend (path: String, targetDirPath: String) -> Unit,
    onCopy: suspend (path: String, targetDirPath: String) -> Unit,
    onShare: (path: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var screen by remember { mutableStateOf(FMScreen.Browse) }
    var openChain by remember { mutableStateOf<List<VfsEntry>>(emptyList()) }
    var rootEntries by remember { mutableStateOf<List<VfsEntry>>(emptyList()) }
    var l0Entries by remember { mutableStateOf<List<VfsEntry>>(emptyList()) }
    var recordEntries by remember { mutableStateOf<List<VfsEntry>>(emptyList()) }
    var sortMode by remember { mutableStateOf(SortMode.NAME) }
    var refreshTick by remember { mutableStateOf(0) }

    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<VfsSearchResult>>(emptyList()) }

    var selectMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    var renameTarget by remember { mutableStateOf<VfsEntry?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf<CreateMode?>(null) }
    var createInput by remember { mutableStateOf("") }

    var storage by remember { mutableStateOf<StorageStats?>(null) }

    // The frame around the wrap-reveal that opened this screen already carries the "screen
    // arriving" beat - this is the header/footer chrome's own arrival on top of that: the top
    // bar drops in from above the top edge, the bottom bar pops up from below the bottom edge.
    val chromeIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) { chromeIn.animateTo(1f, tween(360, easing = CubicBezierEasing(0f, .9f, .1f, 1f))) }

    val scope = rememberCoroutineScope()
    fun refresh() { refreshTick++ }

    val currentTargetDir = openChain.lastOrNull()?.path ?: "/"

    LaunchedEffect(refreshTick) { rootEntries = sortEntries(listDir("/"), sortMode) }
    LaunchedEffect(openChain.getOrNull(0)?.path, sortMode, refreshTick) {
        l0Entries = openChain.getOrNull(0)?.let { sortEntries(listDir(it.path), sortMode) } ?: emptyList()
    }
    LaunchedEffect(openChain.getOrNull(1)?.path, sortMode, refreshTick) {
        recordEntries = openChain.getOrNull(1)?.let { sortEntries(listDir(it.path), sortMode) } ?: emptyList()
    }
    LaunchedEffect(searchQuery, refreshTick) {
        searchResults = if (searchQuery.isNotBlank()) search(searchQuery) else emptyList()
    }

    fun openEntry(depth: Int, entry: VfsEntry) {
        openChain = when (depth) {
            0 -> if (openChain.getOrNull(0)?.path == entry.path) emptyList() else listOf(entry)
            1 -> if (openChain.getOrNull(1)?.path == entry.path) openChain.take(1) else openChain.take(1) + entry
            else -> listOf(openChain[1], entry) // descending past the 2-level window
        }
    }

    fun tapEntry(depth: Int, entry: VfsEntry) {
        if (selectMode) {
            selected = if (entry.path in selected) selected - entry.path else selected + entry.path
        } else if (entry.isDirectory) {
            openEntry(depth, entry)
        } else {
            onOpenFile(entry.path)
        }
    }

    if (screen == FMScreen.PickMove || screen == FMScreen.PickCopy) {
        FolderPicker(
            title = if (screen == FMScreen.PickMove) "Move to…" else "Copy to…",
            listDir = listDir,
            onCancel = { screen = FMScreen.Browse },
            onConfirm = { target ->
                scope.launch {
                    val isMove = screen == FMScreen.PickMove
                    for (path in selected) {
                        if (isMove) onMove(path, target) else onCopy(path, target)
                    }
                    selected = emptySet()
                    selectMode = false
                    screen = FMScreen.Browse
                    refresh()
                }
            },
            modifier = modifier
        )
        return
    }

    if (screen == FMScreen.Storage) {
        LaunchedEffect(Unit) { storage = storageStats() }
        StorageScreen(
            stats = storage,
            onDelete = { path -> scope.launch { onDelete(path); storage = storageStats(); refresh() } },
            onBack = { screen = FMScreen.Browse },
            fullscreen = fullscreen,
            modifier = modifier
        )
        return
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .then(if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
    ) {
        // --- Header --------------------------------------------------------------------
        // Dropped in from above the top edge as one unit, rather than appearing in place.
        Column(Modifier.offset(y = (-90).dp * (1f - chromeIn.value))) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectMode) {
                Chip("‹ ${selected.size} SELECTED", onClick = { selectMode = false; selected = emptySet() })
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Chip("‹ CLOSE", onClick = onBack)
                    // Drops in alongside the rest of the header, but only once there is
                    // somewhere to go up to - the root level has no parent of its own.
                    if (openChain.isNotEmpty()) {
                        Chip("…", background = Azphalt.Yellow, foreground = Azphalt.Ink, onClick = { openChain = openChain.dropLast(1) })
                    }
                }
                val count = rootEntries.size + l0Entries.size + recordEntries.size
                Chip("$count THINGS HERE", filled = false, clickable = false)
            }
        }

        // --- Search / sort row ------------------------------------------------------------
        if (!selectMode) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier
                        .weight(if (searchActive) 1f else 0.001f, fill = searchActive)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Azphalt.Ink.copy(alpha = .10f))
                        .clickable(enabled = !searchActive) { searchActive = true }
                        .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (searchActive) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it; screen = FMScreen.Search },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Azphalt.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            ),
                            cursorBrush = SolidColor(Azphalt.Ink),
                            singleLine = true
                        )
                        Text(
                            "✕", color = Azphalt.Ink.copy(alpha = .6f), fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                searchActive = false; searchQuery = ""; screen = FMScreen.Browse
                            }
                        )
                    } else {
                        Text(
                            "SEARCH", color = Azphalt.Ink.copy(alpha = .55f),
                            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em
                        )
                    }
                }
                if (!searchActive) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Azphalt.Ink.copy(alpha = .10f))
                            .clickable { sortMode = if (sortMode == SortMode.NAME) SortMode.NEWEST else SortMode.NAME }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "${sortMode.label.uppercase()} ▾", color = Azphalt.Ink.copy(alpha = .55f),
                            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em
                        )
                    }
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Azphalt.Ink.copy(alpha = .10f))
                            .clickable { screen = FMScreen.Storage }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "STORAGE ›", color = Azphalt.Ink.copy(alpha = .55f),
                            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em
                        )
                    }
                }
            }
        }
        }

        // --- Content ------------------------------------------------------------------------
        if (screen == FMScreen.Search) {
            SearchResults(
                results = searchResults,
                onOpen = { result ->
                    if (result.entry.isDirectory) {
                        // A folder from search just becomes the new deepest-open level.
                        openChain = listOf(result.entry)
                        searchActive = false; searchQuery = ""; screen = FMScreen.Browse
                    } else {
                        onOpenFile(result.entry.path)
                    }
                }
            )
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "level0") {
                    ExpandableLevel(
                        entries = rootEntries,
                        openEntry = openChain.getOrNull(0),
                        onToggle = { openEntry(0, it) },
                        selectMode = selectMode,
                        selected = selected,
                        onTap = { tapEntry(0, it) },
                        onLongPress = { selectMode = true; selected = setOf(it.path) },
                        onRename = { renameTarget = it; renameInput = it.name },
                        onDelete = { scope.launch { onDelete(it.path); refresh() } },
                        onShare = { onShare(it.path) }
                    ) {
                        if (openChain.isNotEmpty()) {
                            ExpandableLevel(
                                entries = l0Entries,
                                openEntry = openChain.getOrNull(1),
                                onToggle = { openEntry(1, it) },
                                selectMode = selectMode,
                                selected = selected,
                                onTap = { tapEntry(1, it) },
                                onLongPress = { selectMode = true; selected = setOf(it.path) },
                                onRename = { renameTarget = it; renameInput = it.name },
                                onDelete = { scope.launch { onDelete(it.path); refresh() } },
                                onShare = { onShare(it.path) }
                            ) {
                                if (openChain.size == 2) {
                                    RecordList(
                                        entries = recordEntries,
                                        selectMode = selectMode,
                                        selected = selected,
                                        onTap = { tapEntry(2, it) },
                                        onLongPress = { selectMode = true; selected = setOf(it.path) },
                                        onRename = { renameTarget = it; renameInput = it.name },
                                        onDelete = { scope.launch { onDelete(it.path); refresh() } },
                                        onShare = { onShare(it.path) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (creating != null) {
            NamePrompt(
                label = if (creating == CreateMode.FOLDER) "NEW FOLDER" else "NEW FILE",
                name = createInput,
                onNameChange = { createInput = it },
                onConfirm = {
                    val name = createInput.trim()
                    if (name.isNotEmpty()) {
                        scope.launch {
                            when (creating) {
                                CreateMode.FOLDER -> onCreateFolder(currentTargetDir, name)
                                CreateMode.FILE -> onCreateFile(currentTargetDir, name)
                                null -> {}
                            }
                            refresh()
                        }
                    }
                    creating = null; createInput = ""
                },
                onCancel = { creating = null; createInput = "" }
            )
        }

        renameTarget?.let { target ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    "Same capsule as creating something — renaming isn't a different tool, just a filled-in one.",
                    color = Azphalt.Ink.copy(alpha = .5f), fontSize = 9.sp, lineHeight = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                NamePrompt(
                    label = "RENAME",
                    name = renameInput,
                    onNameChange = { renameInput = it },
                    onConfirm = {
                        val name = renameInput.trim()
                        if (name.isNotEmpty()) {
                            scope.launch { onRename(target.path, name); refresh() }
                        }
                        renameTarget = null
                    },
                    onCancel = { renameTarget = null }
                )
            }
        }

        // --- Bottom bar ---------------------------------------------------------------------
        // Popped up from below the bottom edge, mirroring the header's drop from above.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .offset(y = 90.dp * (1f - chromeIn.value)),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selectMode) {
                Chip("MOVE", onClick = { screen = FMScreen.PickMove })
                Chip("COPY", onClick = { screen = FMScreen.PickCopy })
                Chip("SHARE", onClick = { selected.forEach(onShare) })
                Spacer(Modifier.weight(1f))
                Chip("DELETE", background = Azphalt.hues[6], foreground = Azphalt.White, onClick = {
                    scope.launch {
                        selected.forEach { onDelete(it) }
                        selected = emptySet(); selectMode = false; refresh()
                    }
                })
            } else {
                Chip("+ NEW FOLDER", filled = false, onClick = { creating = CreateMode.FOLDER; createInput = "" })
                Chip("+ NEW FILE", filled = false, onClick = { creating = CreateMode.FILE; createInput = "" })
                Spacer(Modifier.weight(1f))
                Chip("SELECT", onClick = { selectMode = true })
            }
        }
    }
}

@Composable
private fun ExpandableLevel(
    entries: List<VfsEntry>,
    openEntry: VfsEntry?,
    onToggle: (VfsEntry) -> Unit,
    selectMode: Boolean,
    selected: Set<String>,
    onTap: (VfsEntry) -> Unit,
    onLongPress: (VfsEntry) -> Unit,
    onRename: (VfsEntry) -> Unit,
    onDelete: (VfsEntry) -> Unit,
    onShare: (VfsEntry) -> Unit,
    nestedContent: @Composable () -> Unit
) {
    val folders = entries.filter { it.isDirectory }
    val files = entries.filter { !it.isDirectory }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (openEntry != null) {
            val others = folders.filter { it.path != openEntry.path }
            if (others.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    others.forEach { f ->
                        key(f.path) {
                            Box(
                                Modifier
                                    .width(10.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(Azphalt.hues[Azphalt.hueOf(f.path)])
                                    .clickable { onToggle(f) }
                            )
                        }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    // The row's own hue never changes on selection - only the mark does, same
                    // as every other selectable row in this screen.
                    .background(Azphalt.hues[Azphalt.hueOf(openEntry.path)])
                    .clickable { onTap(openEntry) }
                    .padding(14.dp)
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectMode) SelectMark(openEntry.path in selected, dark = true)
                            Text(
                                openEntry.name.uppercase(), color = Azphalt.White,
                                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em
                            )
                        }
                        EntryMenu(openEntry, onRename, onDelete, onShare, tint = Azphalt.White)
                    }
                    Box(Modifier.padding(start = 14.dp, top = 10.dp)) { nestedContent() }
                }
            }
        } else if (folders.isEmpty() && files.isEmpty()) {
            EmptyLabel()
        } else if (folders.isNotEmpty()) {
            Text(
                "FOLDERS · ${folders.size}", color = Azphalt.Ink.copy(alpha = .45f),
                fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em
            )
            folders.forEach { f -> key(f.path) { FolderRow(f, selectMode, f.path in selected, onTap, onLongPress, onRename, onDelete, onShare) } }
        }

        if (files.isNotEmpty()) {
            if (folders.isNotEmpty() || openEntry != null) {
                Text(
                    "FILES · ${files.size}", color = Azphalt.Ink.copy(alpha = .45f),
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            FileRows(files, selectMode, selected, onTap, onLongPress, onRename, onDelete, onShare)
        }
    }
}

@Composable
private fun RecordList(
    entries: List<VfsEntry>,
    selectMode: Boolean,
    selected: Set<String>,
    onTap: (VfsEntry) -> Unit,
    onLongPress: (VfsEntry) -> Unit,
    onRename: (VfsEntry) -> Unit,
    onDelete: (VfsEntry) -> Unit,
    onShare: (VfsEntry) -> Unit
) {
    val folders = entries.filter { it.isDirectory }
    val files = entries.filter { !it.isDirectory }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (folders.isEmpty() && files.isEmpty()) EmptyLabel()
        folders.forEach { f -> key(f.path) { FolderRow(f, selectMode, f.path in selected, onTap, onLongPress, onRename, onDelete, onShare) } }
        if (files.isNotEmpty()) FileRows(files, selectMode, selected, onTap, onLongPress, onRename, onDelete, onShare)
    }
}

@Composable
private fun EmptyLabel() {
    Text(
        "NOTHING HERE", color = Azphalt.Ink.copy(alpha = .4f),
        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.14.em
    )
}

@Composable
private fun FolderRow(
    entry: VfsEntry,
    selectMode: Boolean,
    isSelected: Boolean,
    onTap: (VfsEntry) -> Unit,
    onLongPress: (VfsEntry) -> Unit,
    onRename: (VfsEntry) -> Unit,
    onDelete: (VfsEntry) -> Unit,
    onShare: (VfsEntry) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            // The row's own hue never changes on selection - only the mark does.
            .background(Azphalt.hues[Azphalt.hueOf(entry.path)])
            .combinedClickable(onClick = { onTap(entry) }, onLongClick = { onLongPress(entry) })
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (selectMode) SelectMark(isSelected, dark = true)
            Text(
                (entry.name + "/").uppercase(),
                color = Azphalt.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em, maxLines = 1
            )
        }
        if (!selectMode) EntryMenu(entry, onRename, onDelete, onShare, tint = Azphalt.White)
    }
}

@Composable
private fun FileRows(
    files: List<VfsEntry>,
    selectMode: Boolean,
    selected: Set<String>,
    onTap: (VfsEntry) -> Unit,
    onLongPress: (VfsEntry) -> Unit,
    onRename: (VfsEntry) -> Unit,
    onDelete: (VfsEntry) -> Unit,
    onShare: (VfsEntry) -> Unit
) {
    val images = files.filter { it.isImage }
    val others = files.filterNot { it.isImage }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // A folder made mostly of images renders itself as a thumbnail grid, in place,
        // automatically - never a manual list/grid toggle.
        if (images.size >= 3) {
            Text(
                "${images.size} PHOTOS", color = Azphalt.Ink.copy(alpha = .5f),
                fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().height(((images.size / 3 + 1) * 90).dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(images, key = { it.path }) { img ->
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(7.dp))
                            // The tile's own hue never changes on selection - only the mark does.
                            .background(Azphalt.hues[Azphalt.hueOf(img.path)])
                            .clickable { onTap(img) }
                    ) {
                        Text(
                            img.name, color = Azphalt.White.copy(alpha = .8f), fontSize = 7.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1,
                            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                        )
                        if (selectMode) {
                            Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                SelectMark(img.path in selected, dark = true)
                            }
                        }
                    }
                }
            }
        }
        others.forEach { f ->
            key(f.path) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(percent = 50))
                        // The row's own wash never changes on selection - only the mark does.
                        .background(Azphalt.Ink.copy(alpha = .09f))
                        .combinedClickable(onClick = { onTap(f) }, onLongClick = { onLongPress(f) })
                        .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selectMode) SelectMark(f.path in selected, dark = false)
                        // Filenames are literal, not labels - the one place real case survives
                        // outside body copy, same as every other identifier here that names an
                        // actual leaf item rather than a folder/chrome label.
                        Text(f.name, color = Azphalt.Ink, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em, maxLines = 1)
                        Text(formatFileSize(f.sizeBytes), color = Azphalt.Ink.copy(alpha = .55f), fontSize = 9.sp)
                    }
                    if (!selectMode) EntryMenu(f, onRename, onDelete, onShare, tint = Azphalt.Ink)
                }
            }
        }
    }
}

@Composable
private fun SelectMark(selected: Boolean, dark: Boolean) {
    Box(
        Modifier
            .size(16.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) Azphalt.Yellow else (if (dark) Azphalt.White.copy(alpha = .25f) else Azphalt.Ink.copy(alpha = .2f))),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Text("✓", color = Azphalt.Ink, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun EntryMenu(entry: VfsEntry, onRename: (VfsEntry) -> Unit, onDelete: (VfsEntry) -> Unit, onShare: (VfsEntry) -> Unit, tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("RENAME", color = tint.copy(alpha = .7f), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onRename(entry) })
        if (!entry.isDirectory) {
            Text("SHARE", color = tint.copy(alpha = .7f), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onShare(entry) })
        }
        Text("×", color = tint.copy(alpha = .85f), fontSize = 13.sp, modifier = Modifier.clickable { onDelete(entry) })
    }
}

@Composable
private fun ColumnScope.SearchResults(results: List<VfsSearchResult>, onOpen: (VfsSearchResult) -> Unit) {
    if (results.isEmpty()) {
        Text(
            "NOTHING", color = Azphalt.Ink.copy(alpha = .4f),
            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp)
        )
        return
    }
    LazyColumn(
        Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(results, key = { it.entry.path }) { r ->
            Column(
                Modifier.fillMaxWidth().clickable { onOpen(r) }.padding(vertical = 10.dp)
            ) {
                Text(
                    r.entry.name + if (r.entry.isDirectory) "/" else "",
                    color = Azphalt.Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(r.parentPath, color = Azphalt.Ink.copy(alpha = .5f), fontSize = 10.sp)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Azphalt.Ink.copy(alpha = .12f)))
        }
    }
}

@Composable
private fun NamePrompt(label: String, name: String, onNameChange: (String) -> Unit, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Azphalt.Ink)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, color = Azphalt.Yellow.copy(alpha = .7f), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em)
        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(color = Azphalt.Yellow, fontSize = 12.sp),
            cursorBrush = SolidColor(Azphalt.Yellow),
            singleLine = true
        )
        Text("OK", color = Azphalt.Yellow, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clickable(onClick = onConfirm).padding(horizontal = 8.dp, vertical = 6.dp))
        Text("X", color = Azphalt.Yellow.copy(alpha = .6f), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clickable(onClick = onCancel).padding(horizontal = 8.dp, vertical = 6.dp))
    }
}

@Composable
private fun Chip(
    label: String,
    modifier: Modifier = Modifier,
    background: Color = Azphalt.Ink,
    foreground: Color = Azphalt.Yellow,
    filled: Boolean = true,
    clickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    val bg = if (filled) background else Azphalt.Ink.copy(alpha = .14f)
    val fg = if (filled) foreground else Azphalt.Ink.copy(alpha = .55f)
    Box(
        modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(bg)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = fg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em)
    }
}
