@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.hereliesaz.hg2gui.ui.files

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.hereliesaz.hg2gui.ui.BackStepState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.ConfirmDialog
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import kotlinx.coroutines.launch

/*
 * The file manager: search, sort, filter (kind/hidden/recency), multi-select batch actions,
 * in-place rename, an automatic media grid, and a real storage-by-type breakdown, all built on
 * Azphalt's capsule primitive.
 * No icons: a folder is a whole rounded rectangle in its own hue; tapping one expands it while
 * its siblings squish into thin coloured rods beside it, its children living inside it as
 * smaller rectangles - nested arbitrarily deep, not flattened into rows, the same "one host, the
 * rest leave" choreography PillMenu's own capsule stack uses (ExpandableLevel recurses into
 * itself rather than capping out at a fixed depth). A selected row goes to ink with an inverted
 * (yellow) foreground - same "ink means selected/open" convention PillMenu's open pills use.
 */

private enum class SortMode(val label: String) { NAME("Name"), NEWEST("Newest") }
private enum class FMScreen { Browse, Search, Storage, PickMove, PickCopy }
private enum class CreateMode { FOLDER, FILE }

// Cycled with a tap, same as SortMode - each one a single always-visible chip rather than a
// picker sheet, since a folder listing is small enough on a phone that a menu would cost more
// taps than it saves.
private enum class KindFilter(val label: String) { ALL("All"), FOLDERS("Folders"), FILES("Files"), IMAGES("Images") }
private enum class RecencyFilter(val label: String) { ANY("Any time"), TODAY("Today"), WEEK("This week") }

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val WEEK_MS = 7 * DAY_MS

private fun sortEntries(list: List<VfsEntry>, mode: SortMode): List<VfsEntry> = when (mode) {
    SortMode.NAME -> list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    SortMode.NEWEST -> list.sortedWith(compareBy({ !it.isDirectory }, { -it.modifiedAt }))
}

/**
 * Hidden dotfiles are stripped first regardless of [kind]/[recency], same as every real Unix
 * file manager. [recency] only ever hides *files* - a folder's own mtime tracks its last touched
 * child, not something meaningful to filter a folder's own presence by, so folders always pass
 * through it untouched.
 */
private fun List<VfsEntry>.filtered(kind: KindFilter, showHidden: Boolean, recency: RecencyFilter, nowMillis: Long): List<VfsEntry> {
    var out: List<VfsEntry> = this
    if (!showHidden) out = out.filter { !it.name.startsWith(".") }
    out = when (kind) {
        KindFilter.ALL -> out
        KindFilter.FOLDERS -> out.filter { it.isDirectory }
        KindFilter.FILES -> out.filter { !it.isDirectory }
        KindFilter.IMAGES -> out.filter { it.isImage }
    }
    val recencyFloor = when (recency) {
        RecencyFilter.ANY -> return out
        RecencyFilter.TODAY -> DAY_MS
        RecencyFilter.WEEK -> WEEK_MS
    }
    return out.filter { it.isDirectory || nowMillis - it.modifiedAt <= recencyFloor }
}

@Composable
fun FilesScreen(
    fullscreen: Boolean,
    nowMillis: Long,
    listDir: suspend (path: String) -> List<VfsEntry>,
    search: suspend (query: String) -> List<VfsSearchResult>,
    storageStats: suspend () -> StorageStats,
    onOpenFile: (path: String) -> Unit,
    // VFS-13: every one of these used to discard its own success/failure - create-with-an-
    // existing-name reported fake success (the underlying call is a no-op that still returns
    // true), and every other failure (permission denied, sandbox containment) simply vanished.
    // Returning Boolean lets this screen tell the user when a tap didn't do what it looked like.
    onCreateFolder: suspend (parentPath: String, name: String) -> Boolean,
    onCreateFile: suspend (parentPath: String, name: String) -> Boolean,
    onDelete: suspend (path: String) -> Boolean,
    onRename: suspend (path: String, newName: String) -> Boolean,
    onMove: suspend (path: String, targetDirPath: String) -> Boolean,
    onCopy: suspend (path: String, targetDirPath: String) -> Boolean,
    onShare: (path: String) -> Unit,
    // VFS-4: a batch share used to just forEach the single-file callback, firing N independent
    // ACTION_SEND choosers in a row instead of one ACTION_SEND_MULTIPLE - only the last one was
    // ever actually reachable.
    onShareMultiple: (paths: Set<String>) -> Unit,
    onBack: () -> Unit,
    // UI-1: reports whether this screen has an internal level (Storage/Search/PickMove/PickCopy,
    // select mode, a rename/create prompt, a drilled-in folder chain) that system back/the edge
    // gesture should step up through one level at a time, instead of always closing this whole
    // screen the way [onBack] itself does. See BackStepState's own doc for the full mechanism.
    backStep: BackStepState,
    modifier: Modifier = Modifier
) {
    var screen by remember { mutableStateOf(FMScreen.Browse) }
    // Unbounded - a folder can be opened inside an opened folder inside an opened folder, no
    // fixed cap, the same way a PillMenu trail can drill as deep as the tree it's walking goes.
    var openChain by remember { mutableStateOf<List<VfsEntry>>(emptyList()) }
    // One listing per currently-relevant path: "/" plus every entry currently open in
    // [openChain]. Keyed by path rather than depth so a stale listing from a chain that's since
    // been trimmed just falls out of use rather than needing to be explicitly discarded.
    var levelCache by remember { mutableStateOf<Map<String, List<VfsEntry>>>(emptyMap()) }
    var sortMode by remember { mutableStateOf(SortMode.NAME) }
    var kindFilter by remember { mutableStateOf(KindFilter.ALL) }
    var recencyFilter by remember { mutableStateOf(RecencyFilter.ANY) }
    var showHidden by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }

    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<VfsSearchResult>>(emptyList()) }
    // Most-recent-first, capped, no duplicates - just enough to let a tap re-run a search that
    // was actually submitted, not every half-typed query.
    val recentSearches = remember { mutableStateListOf<String>() }

    var selectMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    var renameTarget by remember { mutableStateOf<VfsEntry?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf<CreateMode?>(null) }
    var createInput by remember { mutableStateOf("") }

    var storage by remember { mutableStateOf<StorageStats?>(null) }
    var opError by remember { mutableStateOf<String?>(null) }

    // VFS-2/VFS-3: every one of these gates a call into onDelete/onRename/onMove/onCopy behind a
    // ConfirmDialog rather than firing on the tap itself - deleteTarget and batchDeleteConfirm for
    // the two delete entry points, pendingRenameOverwrite/pendingBatchOverwrite for the silent-
    // clobber case where a rename/move/copy resolves onto a name that's already there.
    var deleteTarget by remember { mutableStateOf<VfsEntry?>(null) }
    var batchDeleteConfirm by remember { mutableStateOf(false) }
    var pendingRenameOverwrite by remember { mutableStateOf<Pair<VfsEntry, String>?>(null) }
    var pendingBatchOverwrite by remember { mutableStateOf<Triple<Set<String>, String, Boolean>?>(null) }

    // The frame around the wrap-reveal that opened this screen already carries the "screen
    // arriving" beat - this is the header/footer chrome's own arrival on top of that: the top
    // bar drops in from above the top edge, the bottom bar pops up from below the bottom edge.
    val chromeIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) { chromeIn.animateTo(1f, tween(360, easing = CubicBezierEasing(0f, .9f, .1f, 1f))) }

    val scope = rememberCoroutineScope()
    fun refresh() { refreshTick++ }

    val currentTargetDir = openChain.lastOrNull()?.path ?: "/"

    fun List<VfsEntry>.filteredAndSorted() = sortEntries(filtered(kindFilter, showHidden, recencyFilter, nowMillis), sortMode)

    // Reloads root plus every currently-open ancestor's own listing whenever the chain changes
    // depth (a tap drilled in or backed out) or the filter/sort/refresh state changes - the
    // direct generalization of the old three fixed LaunchedEffects (root/level-0/record) to
    // however many levels happen to be open right now.
    LaunchedEffect(openChain, sortMode, kindFilter, recencyFilter, showHidden, refreshTick) {
        val paths = listOf("/") + openChain.map { it.path }
        levelCache = paths.associateWith { listDir(it).filteredAndSorted() }
    }

    // VFS-14: null means "this depth's listing hasn't come back from [listDir] yet," distinct
    // from a present-but-empty list ("it came back and there's genuinely nothing here") - the
    // same COUNTING…/null-until-loaded distinction StorageScreen already uses for its own stats.
    // Collapsing both to emptyList() (the old behaviour) is what made every folder tap flash
    // "NOTHING HERE" for a frame before the real listing replaced it.
    fun entriesAt(depth: Int): List<VfsEntry>? {
        val path = if (depth == 0) "/" else openChain.getOrNull(depth - 1)?.path ?: return emptyList()
        return levelCache[path]
    }

    LaunchedEffect(searchQuery, showHidden, refreshTick) {
        val results = if (searchQuery.isNotBlank()) search(searchQuery) else emptyList()
        // Hidden means "living inside a dotted path," the same rule the browse view applies to
        // every ancestor - checking only the leaf's own name let a result surface here that the
        // browse view would never show, since a file itself can be plainly named while every
        // folder above it is dotted.
        searchResults = if (showHidden) {
            results
        } else {
            results.filter { r ->
                !r.entry.name.startsWith(".") &&
                    r.parentPath.split('/').none { segment -> segment.startsWith(".") }
            }
        }
    }

    fun openEntry(depth: Int, entry: VfsEntry) {
        // Tapping the folder already open at this depth closes it back to its own level;
        // tapping any other folder here replaces whatever was open at this depth (and
        // everything deeper, which only ever made sense nested inside it) with the new pick.
        openChain = if (openChain.getOrNull(depth)?.path == entry.path) {
            openChain.take(depth)
        } else {
            openChain.take(depth) + entry
        }
    }

    fun recordSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        recentSearches.remove(trimmed)
        recentSearches.add(0, trimmed)
        while (recentSearches.size > 6) recentSearches.removeAt(recentSearches.lastIndex)
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

    // UI-1: must run before the PickMove/PickCopy/Storage early-returns below so a system-back
    // press while any of those (or select mode, a rename/create prompt, or a drilled-in folder)
    // is showing steps up exactly one level instead of skipping straight past all of them to
    // onBack. Checked in the same "deepest first" order the header's own back-affordances use.
    SideEffect {
        backStep.canStepBack = creating != null || renameTarget != null ||
            screen != FMScreen.Browse || searchActive || selectMode || openChain.isNotEmpty()
        backStep.stepBack = {
            when {
                creating != null -> { creating = null; createInput = "" }
                renameTarget != null -> { renameTarget = null }
                screen != FMScreen.Browse -> { screen = FMScreen.Browse }
                searchActive -> { searchActive = false; searchQuery = ""; screen = FMScreen.Browse }
                selectMode -> { selectMode = false; selected = emptySet() }
                openChain.isNotEmpty() -> { openChain = openChain.dropLast(1) }
            }
        }
    }

    // Rendered ahead of the PickMove/PickCopy early-return below so it still shows once a
    // collision flips [screen] back to Browse - VfsManager's own move/copy always overwrite
    // silently (VFS-3), so this is the only gate standing between a same-named destination file
    // and losing it.
    pendingBatchOverwrite?.let { (paths, target, isMove) ->
        ConfirmDialog(
            title = if (isMove) "OVERWRITE ON MOVE?" else "OVERWRITE ON COPY?",
            message = "Something already named the same as one of these lives in the destination - " +
                (if (isMove) "moving" else "copying") + " here replaces it. This can't be undone.",
            confirmLabel = if (isMove) "MOVE" else "COPY",
            onConfirm = {
                scope.launch {
                    val failed = paths.count { path -> !(if (isMove) onMove(path, target) else onCopy(path, target)) }
                    opError = if (failed > 0) "$failed of ${paths.size} didn't ${if (isMove) "move" else "copy"}." else null
                    selected = emptySet()
                    selectMode = false
                    refresh()
                }
                pendingBatchOverwrite = null
            },
            onDismiss = { pendingBatchOverwrite = null }
        )
    }

    if (screen == FMScreen.PickMove || screen == FMScreen.PickCopy) {
        FolderPicker(
            title = if (screen == FMScreen.PickMove) "Move to…" else "Copy to…",
            listDir = listDir,
            onCancel = { screen = FMScreen.Browse },
            onConfirm = { target ->
                scope.launch {
                    val isMove = screen == FMScreen.PickMove
                    val destNames = listDir(target).map { it.name }.toSet()
                    val collides = selected.any { it.trimEnd('/').substringAfterLast('/') in destNames }
                    if (collides) {
                        screen = FMScreen.Browse
                        pendingBatchOverwrite = Triple(selected, target, isMove)
                    } else {
                        val failed = selected.count { path -> !(if (isMove) onMove(path, target) else onCopy(path, target)) }
                        opError = if (failed > 0) "$failed of ${selected.size} didn't ${if (isMove) "move" else "copy"}." else null
                        selected = emptySet()
                        selectMode = false
                        screen = FMScreen.Browse
                        refresh()
                    }
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
            onDelete = { path ->
                scope.launch {
                    if (!onDelete(path)) opError = "Couldn't delete that."
                    storage = storageStats(); refresh()
                }
            },
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
                // "Here" is wherever the chain has actually drilled to - the deepest open level's
                // own listing, not a sum across every depth (a folder's contents don't also sit
                // beside it at the level above).
                Chip("${entriesAt(openChain.size)?.size ?: 0} THINGS HERE", filled = false, clickable = false)
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
                            onValueChange = { searchQuery = it; screen = if (it.isBlank()) FMScreen.Browse else FMScreen.Search },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Azphalt.currentGround.onPage, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            ),
                            cursorBrush = SolidColor(Azphalt.currentGround.onPage),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { recordSearch(searchQuery) })
                        )
                        Text(
                            "✕", color = Azphalt.currentGround.onPage.copy(alpha = .6f), fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                searchActive = false; searchQuery = ""; screen = FMScreen.Browse
                            }
                        )
                    } else {
                        Text(
                            "SEARCH", color = Azphalt.currentGround.onPage.copy(alpha = .55f),
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
                            "${sortMode.label.uppercase()} ▾", color = Azphalt.currentGround.onPage.copy(alpha = .55f),
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
                            "STORAGE ›", color = Azphalt.currentGround.onPage.copy(alpha = .55f),
                            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em
                        )
                    }
                }
            }
        }

        // --- Filter row --------------------------------------------------------------------
        // Its own row rather than folded into the search/sort one above - a folder listing on a
        // phone is narrow enough that three more chips there would start wrapping or crowding
        // the search well. Horizontally scrollable so a later filter never has to fight the
        // ones already here for room.
        if (!selectMode && !searchActive) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    kindFilter.label.uppercase(), active = kindFilter != KindFilter.ALL,
                    onClick = {
                        val values = KindFilter.entries
                        kindFilter = values[(kindFilter.ordinal + 1) % values.size]
                    }
                )
                FilterChip(
                    recencyFilter.label.uppercase(), active = recencyFilter != RecencyFilter.ANY,
                    onClick = {
                        val values = RecencyFilter.entries
                        recencyFilter = values[(recencyFilter.ordinal + 1) % values.size]
                    }
                )
                FilterChip("HIDDEN", active = showHidden, onClick = { showHidden = !showHidden })
            }
        }
        }

        // VFS-13: the one place a failed file operation becomes visible instead of vanishing
        // silently - tap to dismiss, same as any other transient chip in this screen.
        opError?.let { message ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.hues[6])
                    .clickable { opError = null }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(message, color = Azphalt.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        // --- Content ------------------------------------------------------------------------
        if (searchActive) {
            if (searchQuery.isBlank()) {
                RecentSearches(recentSearches) { q ->
                    searchQuery = q
                    screen = FMScreen.Search
                    recordSearch(q)
                }
            } else {
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
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "level0") {
                    ExpandableLevel(
                        depth = 0,
                        openChain = openChain,
                        entriesAt = ::entriesAt,
                        onToggle = { d, e -> openEntry(d, e) },
                        selectMode = selectMode,
                        selected = selected,
                        onTap = { d, e -> tapEntry(d, e) },
                        // Long-pressing while already selecting adds to the set, same as tapping
                        // a row already does in select mode - replacing it wiped everything else
                        // picked so far the moment a second long-press landed on a new row.
                        onLongPress = { selectMode = true; selected = selected + it.path },
                        onRename = { renameTarget = it; renameInput = it.name },
                        onDelete = { deleteTarget = it },
                        onShare = { onShare(it.path) }
                    )
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
                            val ok = when (creating) {
                                CreateMode.FOLDER -> onCreateFolder(currentTargetDir, name)
                                CreateMode.FILE -> onCreateFile(currentTargetDir, name)
                                null -> true
                            }
                            opError = if (!ok) "$name already exists here." else null
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
                        if (name.isNotEmpty() && name != target.name) {
                            scope.launch {
                                val siblingNames = listDir(vfsParentPath(target.path)).map { it.name }.toSet()
                                if (name in siblingNames) {
                                    pendingRenameOverwrite = target to name
                                } else {
                                    opError = if (!onRename(target.path, name)) "Couldn't rename that." else null
                                    refresh()
                                }
                            }
                        }
                        renameTarget = null
                    },
                    onCancel = { renameTarget = null }
                )
            }
        }

        pendingRenameOverwrite?.let { (target, newName) ->
            ConfirmDialog(
                title = "OVERWRITE $newName?",
                message = "$newName already exists here - renaming ${target.name} onto it replaces " +
                    "whatever's there now. This can't be undone.",
                confirmLabel = "OVERWRITE",
                onConfirm = {
                    scope.launch {
                        opError = if (!onRename(target.path, newName)) "Couldn't rename that." else null
                        refresh()
                    }
                    pendingRenameOverwrite = null
                },
                onDismiss = { pendingRenameOverwrite = null }
            )
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
                Chip("SHARE", onClick = { onShareMultiple(selected) })
                Spacer(Modifier.weight(1f))
                Chip("DELETE", background = Azphalt.hues[6], foreground = Azphalt.White, onClick = {
                    batchDeleteConfirm = true
                })
            } else {
                Chip("+ NEW FOLDER", filled = false, onClick = { creating = CreateMode.FOLDER; createInput = "" })
                Chip("+ NEW FILE", filled = false, onClick = { creating = CreateMode.FILE; createInput = "" })
                Spacer(Modifier.weight(1f))
                Chip("SELECT", onClick = { selectMode = true })
            }
        }

        deleteTarget?.let { entry ->
            ConfirmDialog(
                title = "DELETE ${entry.name}?",
                message = if (entry.isDirectory) {
                    "This deletes ${entry.name} and everything inside it. This can't be undone."
                } else {
                    "This deletes ${entry.name}. This can't be undone."
                },
                confirmLabel = "DELETE",
                onConfirm = {
                    scope.launch {
                        opError = if (!onDelete(entry.path)) "Couldn't delete ${entry.name}." else null
                        refresh()
                    }
                    deleteTarget = null
                },
                onDismiss = { deleteTarget = null }
            )
        }

        if (batchDeleteConfirm) {
            ConfirmDialog(
                title = "DELETE ${selected.size} THINGS?",
                message = "This deletes everything selected, including the contents of any selected " +
                    "folders. This can't be undone.",
                confirmLabel = "DELETE",
                onConfirm = {
                    scope.launch {
                        val failed = selected.count { !onDelete(it) }
                        opError = if (failed > 0) "$failed of ${selected.size} didn't delete." else null
                        selected = emptySet(); selectMode = false; refresh()
                    }
                    batchDeleteConfirm = false
                },
                onDismiss = { batchDeleteConfirm = false }
            )
        }
    }
}

/**
 * A folder is a whole rounded rectangle in its own hue. Tapping one opens it in place: its
 * sibling folders squish into thin coloured rods beside it, and its own contents render inside
 * it - which, if one of *those* is itself opened, means this composable calling itself again for
 * [depth] + 1, exactly the way an opened rectangle's children are still nested inside the parent
 * rectangle they've always belonged to. Nothing caps how deep that goes; it bottoms out on its
 * own the moment a level has nothing open in it. [animateContentSize] reuses PillMenu's own
 * [Azphalt.SLIDE_MS]/linear timing for the size change this produces (a squish, an expand) rather
 * than the hand-rolled per-pill Animatable choreography PillMenu itself uses for a fixed, static
 * pill tree - this tree is arbitrary-depth and lazily loaded, so animating the aggregate size
 * change is the direct equivalent for data that isn't known until a folder is actually opened.
 */
@Composable
private fun ExpandableLevel(
    depth: Int,
    openChain: List<VfsEntry>,
    entriesAt: (Int) -> List<VfsEntry>?,
    onToggle: (Int, VfsEntry) -> Unit,
    selectMode: Boolean,
    selected: Set<String>,
    onTap: (Int, VfsEntry) -> Unit,
    onLongPress: (VfsEntry) -> Unit,
    onRename: (VfsEntry) -> Unit,
    onDelete: (VfsEntry) -> Unit,
    onShare: (VfsEntry) -> Unit
) {
    val entries = entriesAt(depth)
    val openEntry = openChain.getOrNull(depth)
    val folders = entries?.filter { it.isDirectory } ?: emptyList()
    val files = entries?.filterNot { it.isDirectory } ?: emptyList()

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.animateContentSize(tween(Azphalt.SLIDE_MS, easing = LinearEasing))
    ) {
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
                                    .clickable { onToggle(depth, f) }
                            )
                        }
                    }
                }
            }
            val isSelected = selectMode && openEntry.path in selected
            Box(
                Modifier
                    .fillMaxWidth()
                    // The style guide's canonical "record tile" radius - the one other ad hoc
                    // radius this screen used to carry (18dp) reconciled to it.
                    .clip(RoundedCornerShape(26.dp))
                    // A selected row goes to ink with an inverted (yellow) foreground - same
                    // "ink means selected/open" convention PillMenu's open pills use.
                    .background(if (isSelected) Azphalt.Ink else Azphalt.hues[Azphalt.hueOf(openEntry.path)])
                    .clickable { onTap(depth, openEntry) }
                    .padding(14.dp)
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectMode) SelectMark(openEntry.path in selected, dark = true)
                            Text(
                                openEntry.name.uppercase(), color = if (isSelected) Azphalt.Yellow else Azphalt.White,
                                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em
                            )
                        }
                        // Guarded the same as every sibling row's own menu (FolderRow, FileRows) -
                        // this was the one place still left live during select mode, so its ×
                        // could delete the very folder whose contents you were selecting inside.
                        if (!selectMode) EntryMenu(openEntry, onRename, onDelete, onShare, tint = Azphalt.White)
                    }
                    Box(Modifier.padding(start = 14.dp, top = 10.dp)) {
                        ExpandableLevel(
                            depth = depth + 1,
                            openChain = openChain,
                            entriesAt = entriesAt,
                            onToggle = onToggle,
                            selectMode = selectMode,
                            selected = selected,
                            onTap = onTap,
                            onLongPress = onLongPress,
                            onRename = onRename,
                            onDelete = onDelete,
                            onShare = onShare
                        )
                    }
                }
            }
        } else if (entries == null) {
            // VFS-14: this depth's own listing hasn't come back yet - a folder just tapped open,
            // or a filter/sort change still in flight - not "there's genuinely nothing here."
            LoadingLabel()
        } else if (folders.isEmpty() && files.isEmpty()) {
            EmptyLabel()
        } else if (folders.isNotEmpty()) {
            Text(
                // .55f, the app-wide "text-muted, eyebrows and captions" tier (style guide "03 -
                // Transparency") - the 45% tier is for micro labels inside a pill, which this
                // section caption isn't.
                "FOLDERS · ${folders.size}", color = Azphalt.Ink.copy(alpha = .55f),
                fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em
            )
            folders.forEach { f ->
                key(f.path) {
                    FolderRow(f, selectMode, f.path in selected, { onTap(depth, it) }, onLongPress, onRename, onDelete, onShare)
                }
            }
        }

        if (files.isNotEmpty()) {
            if (folders.isNotEmpty() || openEntry != null) {
                Text(
                    // .55f, the app-wide "text-muted, eyebrows and captions" tier - see the
                    // FOLDERS caption above for the same reasoning.
                    "FILES · ${files.size}", color = Azphalt.Ink.copy(alpha = .55f),
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            FileRows(files, selectMode, selected, { onTap(depth, it) }, onLongPress, onRename, onDelete, onShare)
        }
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
private fun LoadingLabel() {
    Text(
        "LOADING…", color = Azphalt.Ink.copy(alpha = .4f),
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
    val selectedTint = selectMode && isSelected
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            // A selected row goes to ink with an inverted (yellow) foreground - same
            // "ink means selected/open" convention PillMenu's open pills use.
            .background(if (selectedTint) Azphalt.Ink else Azphalt.hues[Azphalt.hueOf(entry.path)])
            .combinedClickable(onClick = { onTap(entry) }, onLongClick = { onLongPress(entry) })
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (selectMode) SelectMark(isSelected, dark = true)
            Text(
                (entry.name + "/").uppercase(),
                color = if (selectedTint) Azphalt.Yellow else Azphalt.White,
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em, maxLines = 1
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
                val selectedTint = selectMode && f.path in selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(percent = 50))
                        // A selected row goes to ink with an inverted (yellow) foreground - same
                        // "ink means selected/open" convention PillMenu's open pills use.
                        .background(if (selectedTint) Azphalt.Ink else Azphalt.Ink.copy(alpha = .09f))
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
                        Text(
                            f.name, color = if (selectedTint) Azphalt.Yellow else Azphalt.Ink,
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em, maxLines = 1
                        )
                        Text(
                            formatFileSize(f.sizeBytes),
                            color = if (selectedTint) Azphalt.Yellow.copy(alpha = .75f) else Azphalt.Ink.copy(alpha = .55f),
                            fontSize = 9.sp
                        )
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
private fun ColumnScope.RecentSearches(recent: List<String>, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp, vertical = 12.dp)) {
        if (recent.isEmpty()) {
            Text(
                "TYPE TO SEARCH", color = Azphalt.Ink.copy(alpha = .4f),
                fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em
            )
        } else {
            Text(
                // .55f, the app-wide "text-muted, eyebrows and captions" tier.
                "RECENT SEARCHES", color = Azphalt.Ink.copy(alpha = .55f),
                fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recent.forEach { q ->
                    key(q) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Azphalt.Ink.copy(alpha = .10f))
                                .clickable { onSelect(q) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(q, color = Azphalt.Ink.copy(alpha = .7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (active) Azphalt.Ink else Azphalt.Ink.copy(alpha = .10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label, color = if (active) Azphalt.Yellow else Azphalt.Ink.copy(alpha = .55f),
            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em
        )
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
