@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.hereliesaz.hg2gui.ui.files

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.ConfirmDialog
import com.hereliesaz.hg2gui.ui.buildStyledLine
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    // F3: a tapped file reveals its own contents in place, inside its own row - this is the read,
    // separate from onOpenFile below (which still opens the real editor, for actually changing it).
    previewFile: suspend (path: String) -> FilePreview,
    onOpenFile: (path: String) -> Unit,
    // VFS-13: every one of these used to discard its own success/failure - create-with-an-
    // existing-name reported fake success (the underlying call is a no-op that still returns
    // true), and every other failure (permission denied, sandbox containment) simply vanished.
    // Returning Boolean lets this screen tell the user when a tap didn't do what it looked like.
    onCreateFolder: suspend (parentPath: String, name: String) -> Boolean,
    onCreateFile: suspend (parentPath: String, name: String) -> Boolean,
    onDelete: suspend (path: String) -> Boolean,
    // TRASH-1: onDelete above no longer means "gone" - it moves into the trash, and this is how
    // the Storage screen's TRASH tab reads and reverses that. Bundled (see TrashActions) rather
    // than four more parameters on a signature already past detekt's own threshold before them.
    trash: TrashActions,
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
    // Refreshed alongside storage - both describe "what's using space right now," and a
    // delete/restore/purge is exactly the kind of mutation refreshTick already exists to catch.
    var trashItems by remember { mutableStateOf<List<VfsTrashEntry>>(emptyList()) }
    var opError by remember { mutableStateOf<String?>(null) }

    // F3: the one file currently expanded in place - re-tapping it, or tapping a different file,
    // closes/replaces it, the same one-open-at-a-time rule openChain already applies to folders.
    // Cached by path so re-opening a file already previewed this session doesn't re-read/re-
    // highlight it, but a rename/edit elsewhere never invalidates a stale entry here - acceptable,
    // since the cache only ever lives as long as this composition.
    var previewPath by remember { mutableStateOf<String?>(null) }
    var previewCache by remember { mutableStateOf<Map<String, FilePreview>>(emptyMap()) }
    LaunchedEffect(previewPath) {
        val path = previewPath
        if (path != null && path !in previewCache) {
            previewCache = previewCache + (path to previewFile(path))
        }
    }

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

    // F4: item hue is share-of-total-storage, so the total has to be known in the browse view
    // too, not just on the dedicated Storage screen - refreshed on every mutating op the same way
    // levelCache is, so a delete/move doesn't leave every remaining item's hue stale.
    LaunchedEffect(refreshTick) { storage = storageStats(); trashItems = trash.items() }

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

    // Rename/delete/move on a folder currently sitting in openChain used to leave that entry -
    // and everything drilled in beneath it - pointing at a path that's since changed or vanished;
    // the panel kept re-fetching the stale path, showed "NOTHING HERE" under a header still
    // reading the old name, and currentTargetDir kept resolving to a directory that no longer
    // existed. Deleting/moving the source out from under an open level closes it (and whatever
    // was drilled in deeper, since a path nested under a gone folder is meaningless); renaming it
    // updates that one level in place and closes anything drilled in deeper - LaunchedEffect above
    // re-fetches every surviving level fresh the moment openChain itself changes, so there's
    // nothing else to keep in sync by hand.
    fun closeChainIfAffected(paths: Set<String>) {
        val idx = openChain.indexOfFirst { it.path in paths }
        if (idx >= 0) openChain = openChain.take(idx)
    }

    fun renameChainIfAffected(oldPath: String, newName: String) {
        val idx = openChain.indexOfFirst { it.path == oldPath }
        if (idx < 0) return
        val renamed = openChain[idx].copy(name = newName, path = vfsChildPath(vfsParentPath(oldPath), newName))
        openChain = openChain.take(idx) + renamed
    }

    fun moveOrCopyErrorMessage(failed: Int, total: Int, isMove: Boolean): String? =
        if (failed > 0) "$failed of $total didn't ${if (isMove) "move" else "copy"}." else null

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
        } else if (entry.isImage) {
            // Images already get their own thumbnail grid (FileRows) - a tap opens the real
            // editor/viewer directly rather than an inline text preview that doesn't apply to them.
            onOpenFile(entry.path)
        } else {
            previewPath = if (previewPath == entry.path) null else entry.path
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
                // Checked ahead of the generic `screen != Browse` branch below: typing a query
                // flips `screen` to Search as a side effect (see onValueChange above), but the
                // content area is gated on `searchActive` alone - resetting only `screen` here
                // left the visible search results on screen with nothing to show for the back
                // press, requiring a second one to actually leave search.
                searchActive -> { searchActive = false; searchQuery = ""; screen = FMScreen.Browse }
                screen != FMScreen.Browse -> { screen = FMScreen.Browse }
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
                    val failed = paths.filterNot { path -> if (isMove) onMove(path, target) else onCopy(path, target) }.toSet()
                    opError = moveOrCopyErrorMessage(failed.size, paths.size, isMove)
                    // Only a move actually removes the source - a copy leaves the open folder
                    // right where it was.
                    if (isMove) closeChainIfAffected(paths - failed)
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
                        val failed = selected.filterNot { path -> if (isMove) onMove(path, target) else onCopy(path, target) }.toSet()
                        opError = moveOrCopyErrorMessage(failed.size, selected.size, isMove)
                        if (isMove) closeChainIfAffected(selected - failed)
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
        // Loaded unconditionally above (LaunchedEffect(refreshTick)) now that the browse view
        // needs the same total for its own per-file hue.
        StorageScreen(
            stats = storage,
            onDelete = { path ->
                scope.launch {
                    if (!onDelete(path)) opError = "Couldn't delete that."
                    storage = storageStats(); trashItems = trash.items(); refresh()
                }
            },
            trash = TrashPanelState(
                items = trashItems,
                onRestore = { entry ->
                    scope.launch {
                        if (!trash.restore(entry)) opError = "Couldn't restore ${entry.name}."
                        storage = storageStats(); trashItems = trash.items(); refresh()
                    }
                },
                onPurgeTrash = { entry ->
                    scope.launch {
                        if (!trash.purge(entry)) opError = "Couldn't delete ${entry.name}."
                        trashItems = trash.items()
                    }
                },
                onEmptyTrash = {
                    scope.launch {
                        trash.empty()
                        trashItems = trash.items()
                    }
                }
            ),
            onBack = { screen = FMScreen.Browse },
            fullscreen = fullscreen,
            errorMessage = opError,
            onErrorDismiss = { opError = null },
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
                        ctx = LevelContext(
                            entriesAt = ::entriesAt,
                            selection = SelectionState(selectMode, selected),
                            actions = LevelActions(
                                onTap = { d, e -> tapEntry(d, e) },
                                // Long-pressing while already selecting adds to the set, same as
                                // tapping a row already does in select mode - replacing it wiped
                                // everything else picked so far the moment a second long-press
                                // landed on a new row.
                                onLongPress = { selectMode = true; selected = selected + it.path },
                                onRename = { renameTarget = it; renameInput = it.name },
                                onDelete = { deleteTarget = it },
                                onShare = { onShare(it.path) }
                            ),
                            totalBytes = storage?.totalBytes,
                            preview = PreviewContext(previewPath, previewCache, onOpenFile)
                        )
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
                                    val ok = onRename(target.path, name)
                                    opError = if (!ok) "Couldn't rename that." else null
                                    if (ok) renameChainIfAffected(target.path, name)
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
                        val ok = onRename(target.path, newName)
                        opError = if (!ok) "Couldn't rename that." else null
                        if (ok) renameChainIfAffected(target.path, newName)
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
                // TRASH-1: no longer final - moved into Storage > Trash, recoverable there.
                message = if (entry.isDirectory) {
                    "This moves ${entry.name} and everything inside it to the trash."
                } else {
                    "This moves ${entry.name} to the trash."
                },
                confirmLabel = "DELETE",
                onConfirm = {
                    scope.launch {
                        val ok = onDelete(entry.path)
                        opError = if (!ok) "Couldn't delete ${entry.name}." else null
                        if (ok) closeChainIfAffected(setOf(entry.path))
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
                // TRASH-1: no longer final - moved into Storage > Trash, recoverable there.
                message = "This moves everything selected, including the contents of any selected " +
                    "folders, to the trash.",
                confirmLabel = "DELETE",
                onConfirm = {
                    scope.launch {
                        val failed = selected.filterNot { onDelete(it) }.toSet()
                        opError = if (failed.isNotEmpty()) "${failed.size} of ${selected.size} didn't delete." else null
                        closeChainIfAffected(selected - failed)
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
 * A folder is a tile in its own hue. Tapping one opens it in place: its sibling folders drop,
 * race, and climb into a thin coloured rod column beside it (see [FolderBand]), and its own
 * contents render below it - which, if one of *those* is itself opened, means this composable
 * calling itself again for [depth] + 1, exactly the way an opened folder's children are still
 * nested inside the parent it's always belonged to. Nothing caps how deep that goes; it bottoms
 * out on its own the moment a level has nothing open in it.
 */
// F2: the open panel's own inset and its recursed child's start/top inset each compound with
// every level down - a fixed 14dp/10dp makes depth five effectively unusable on a phone-width
// screen. Thin both toward a floor as depth grows instead.
private const val NESTING_INSET_STEP_DP = 3
private const val NESTING_TOP_INSET_STEP_DP = 2
private val NESTING_INSET_MIN = 4.dp
private val NESTING_INSET_BASE = 14.dp
private val NESTING_TOP_INSET_BASE = 10.dp

private fun nestingInset(depth: Int): Dp =
    (NESTING_INSET_BASE - (depth * NESTING_INSET_STEP_DP).dp).coerceAtLeast(NESTING_INSET_MIN)

private fun nestingTopInset(depth: Int): Dp =
    (NESTING_TOP_INSET_BASE - (depth * NESTING_TOP_INSET_STEP_DP).dp).coerceAtLeast(NESTING_INSET_MIN)

// F4: a file's hue is its share of total storage, cool for small and red for the outliers - the
// first seven entries of Azphalt.hues run violet -> cyan -> teal -> green -> amber -> orange ->
// red, exactly that ramp, before the list moves on to the grounds/category-recolor extension
// hues that don't belong in a size gradient. Folders keep the identity hash (Azphalt.hueOf
// directly) - a folder's hue changing as its contents change would be disorienting.
private const val SIZE_HUE_RAMP_LENGTH = 7
private val SIZE_HUE_RAMP = Azphalt.hues.subList(0, SIZE_HUE_RAMP_LENGTH)

private fun hueForShare(fraction: Float): Color {
    val steps = SIZE_HUE_RAMP.size - 1
    val scaled = fraction.coerceIn(0f, 1f) * steps
    val index = scaled.toInt().coerceIn(0, steps - 1)
    return lerp(SIZE_HUE_RAMP[index], SIZE_HUE_RAMP[index + 1], scaled - index)
}

private fun fileHue(entry: VfsEntry, totalBytes: Long?): Color =
    if (totalBytes != null && totalBytes > 0) {
        hueForShare(entry.sizeBytes.toFloat() / totalBytes.toFloat())
    } else {
        Azphalt.hues[Azphalt.hueOf(entry.path)]
    }

// F1/F6: tap a folder and its siblings drop, race, and climb into a thin column at the trailing
// edge while the tapped one grows to fill the width - a directed Rect choreography (shared with
// FolderPicker's own MorphTile via TileMorph.kt) in place of the single animateContentSize this
// screen used to lean on for the whole level. Folders arrive as a tile grid, capped at two rows
// with the remainder behind a "+N" tile, the same way a PillMenu category caps what it shows at
// once. The open tile itself only ever renders a header bar - the folder's own recursive contents
// (arbitrarily deep, arbitrarily tall) still reveal below it via ExpandableLevel's own recursion,
// unchanged, since no fixed-size Rect can represent that up front.
private const val FOLDER_GRID_COLUMNS = 3
private const val FOLDER_GRID_ROW_CAP = 2
private const val FOLDER_TILE_CAP = FOLDER_GRID_COLUMNS * FOLDER_GRID_ROW_CAP
private val OPEN_HEADER_HEIGHT = 56.dp
private const val TILE_DROP_FRACTION = 0.35f
private const val TILE_RACE_FRACTION = 0.7f
private val TILE_MOTION_EASING = CubicBezierEasing(0f, .9f, .1f, 1f)
private const val DROP_OVERSHOOT_FRACTION = 0.5f

private data class FolderEntryActions(
    val onTap: (VfsEntry) -> Unit,
    val onLongPress: (VfsEntry) -> Unit,
    val onRename: (VfsEntry) -> Unit,
    val onDelete: (VfsEntry) -> Unit,
    val onShare: (VfsEntry) -> Unit
)

private data class SelectionState(val selectMode: Boolean, val selected: Set<String>)
private data class FolderTileSelection(val selectMode: Boolean, val isSelected: Boolean)

// ExpandableLevel recurses through however many levels are currently open - everything here
// except [depth]/[openChain] themselves stays identical from one level to the next, so it's
// threaded straight through recursive calls rather than re-listed at every one.
private class LevelActions(
    val onTap: (Int, VfsEntry) -> Unit,
    val onLongPress: (VfsEntry) -> Unit,
    val onRename: (VfsEntry) -> Unit,
    val onDelete: (VfsEntry) -> Unit,
    val onShare: (VfsEntry) -> Unit
)

private class LevelContext(
    val entriesAt: (Int) -> List<VfsEntry>?,
    val selection: SelectionState,
    val actions: LevelActions,
    val totalBytes: Long?,
    val preview: PreviewContext
)

// F3: the one file currently expanded in place (across the whole tree, same "one at a time" rule
// openChain applies to folders), its already-loaded/loading previews, and the escape hatch into
// the real editor for actually changing a file rather than just reading it.
private class PreviewContext(
    val openPath: String?,
    val cache: Map<String, FilePreview>,
    val onOpenFile: (String) -> Unit
)

private fun LevelContext.entryActionsAt(depth: Int) = FolderEntryActions(
    onTap = { actions.onTap(depth, it) },
    onLongPress = actions.onLongPress,
    onRename = actions.onRename,
    onDelete = actions.onDelete,
    onShare = actions.onShare
)

private class FolderBandGeometry(
    val density: Density,
    val canvasWidthPx: Float,
    val tileSizePx: Float,
    val gapPx: Float,
    val headerHeightPx: Float,
    val rodLengthPx: Float
)

private fun folderBandGeometry(canvasWidthPx: Float, density: Density): FolderBandGeometry {
    val tileSizePx = if (canvasWidthPx > 0f) gridTileSizePx(FOLDER_GRID_COLUMNS, canvasWidthPx, density) else 0f
    return FolderBandGeometry(
        density = density,
        canvasWidthPx = canvasWidthPx,
        tileSizePx = tileSizePx,
        gapPx = with(density) { GRID_GAP.toPx() },
        headerHeightPx = with(density) { OPEN_HEADER_HEIGHT.toPx() },
        rodLengthPx = tileSizePx * ROD_LENGTH_FRACTION
    )
}

private fun folderBandHeightPx(g: FolderBandGeometry, openEntry: VfsEntry?, visibleCount: Int, overflow: Int): Float = when {
    g.canvasWidthPx <= 0f -> 0f
    openEntry != null -> {
        val othersCount = (visibleCount - 1).coerceAtLeast(0)
        g.headerHeightPx + if (othersCount > 0) g.gapPx + othersCount * g.rodLengthPx + (othersCount - 1) * g.gapPx else 0f
    }
    else -> {
        val slotCount = visibleCount + if (overflow > 0) 1 else 0
        val rows = (slotCount - 1) / FOLDER_GRID_COLUMNS + 1
        rows * g.tileSizePx + (rows - 1) * g.gapPx
    }
}

private fun folderTileTarget(g: FolderBandGeometry, visible: List<VfsEntry>, openEntry: VfsEntry?, i: Int, f: VfsEntry): TileRect {
    val isOpen = openEntry?.path == f.path
    return when {
        isOpen -> TileRect(0f, 0f, g.canvasWidthPx, g.headerHeightPx)
        openEntry != null -> {
            val others = visible.filter { it.path != openEntry.path }
            rodTileRect(others.indexOf(f), g.canvasWidthPx, g.rodLengthPx, g.density, startY = g.headerHeightPx + g.gapPx)
        }
        else -> gridTileRect(i, FOLDER_GRID_COLUMNS, g.canvasWidthPx, g.density)
    }
}

@Composable
private fun FolderBand(
    folders: List<VfsEntry>,
    openEntry: VfsEntry?,
    selection: SelectionState,
    actions: FolderEntryActions
) {
    if (folders.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var canvasWidthPx by remember { mutableStateOf(0f) }
    val capped = !expanded && openEntry == null && folders.size > FOLDER_TILE_CAP
    val visible = if (capped) folders.take(FOLDER_TILE_CAP - 1) else folders
    val overflow = folders.size - visible.size
    val g = folderBandGeometry(canvasWidthPx, density)
    val bandHeightPx = folderBandHeightPx(g, openEntry, visible.size, overflow)

    Box(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { canvasWidthPx = it.size.width.toFloat() }
            .then(with(density) { Modifier.height(bandHeightPx.toDp()) })
    ) {
        if (canvasWidthPx > 0f) {
            visible.forEachIndexed { i, f ->
                key(f.path) {
                    FolderTile(
                        entry = f,
                        hue = Azphalt.hues[Azphalt.hueOf(f.path)],
                        geometry = FolderTileGeometry(
                            rect = folderTileTarget(g, visible, openEntry, i, f),
                            isOpen = openEntry?.path == f.path,
                            referenceSizePx = g.tileSizePx
                        ),
                        selection = FolderTileSelection(selection.selectMode, f.path in selection.selected),
                        actions = actions
                    )
                }
            }
            if (openEntry == null && overflow > 0) {
                key("__more__") {
                    OverflowTile(
                        rect = gridTileRect(visible.size, FOLDER_GRID_COLUMNS, canvasWidthPx, density),
                        count = overflow,
                        onClick = { expanded = true }
                    )
                }
            }
        }
    }
}

private const val ROD_LENGTH_FRACTION = 0.7f

private class TileMotion(
    val x: Animatable<Float, AnimationVector1D>,
    val y: Animatable<Float, AnimationVector1D>,
    val w: Animatable<Float, AnimationVector1D>,
    val h: Animatable<Float, AnimationVector1D>,
    val scale: Animatable<Float, AnimationVector1D>
)

/** Animates [motion]'s x/y/w/h/scale from wherever they currently sit toward [to] - a drop, then
 *  a race across, then a climb up the far side, instead of a straight interpolation, so a tile
 *  visibly travels rather than just resizing in place. Landing in a square grid slot (closing back
 *  from a rod or from being open) gets a brief overshoot - "siblings ballooning and popping into
 *  place" on the way back in. */
private suspend fun animateDirected(motion: TileMotion, to: TileRect, referenceSizePx: Float) {
    val x = motion.x
    val y = motion.y
    val w = motion.w
    val h = motion.h
    val scale = motion.scale
    val dropY = maxOf(y.value, to.y) + referenceSizePx * DROP_OVERSHOOT_FRACTION
    val dropMs = (TILE_MORPH_MS * TILE_DROP_FRACTION).toInt()
    val raceMs = (TILE_MORPH_MS * (TILE_RACE_FRACTION - TILE_DROP_FRACTION)).toInt()
    val climbMs = TILE_MORPH_MS - dropMs - raceMs
    coroutineScope {
        launch {
            y.animateTo(dropY, tween(dropMs, easing = TILE_MOTION_EASING))
            delay(raceMs.toLong())
            y.animateTo(to.y, tween(climbMs, easing = TILE_MOTION_EASING))
        }
        launch {
            delay(dropMs.toLong())
            x.animateTo(to.x, tween(raceMs, easing = TILE_MOTION_EASING))
        }
        launch { w.animateTo(to.w, tween(TILE_MORPH_MS, easing = TILE_MOTION_EASING)) }
        launch { h.animateTo(to.h, tween(TILE_MORPH_MS, easing = TILE_MOTION_EASING)) }
        if (to.w == to.h) {
            launch {
                delay((TILE_MORPH_MS - POP_MS).toLong().coerceAtLeast(0L))
                scale.animateTo(POP_OVERSHOOT, tween(POP_MS / 2, easing = TILE_MOTION_EASING))
                scale.animateTo(1f, tween(POP_MS / 2, easing = TILE_MOTION_EASING))
            }
        } else if (scale.value != 1f) {
            launch { scale.snapTo(1f) }
        }
    }
}

private const val POP_MS = 180
private const val POP_OVERSHOOT = 1.12f

private class FolderTileGeometry(val rect: TileRect, val isOpen: Boolean, val referenceSizePx: Float)

@Composable
private fun FolderTile(
    entry: VfsEntry,
    hue: Color,
    geometry: FolderTileGeometry,
    selection: FolderTileSelection,
    actions: FolderEntryActions
) {
    val (rect, isOpen, referenceSizePx) = geometry
    val density = LocalDensity.current
    val motion = remember {
        TileMotion(
            x = Animatable(rect.x), y = Animatable(rect.y),
            w = Animatable(rect.w), h = Animatable(rect.h),
            scale = Animatable(1f)
        )
    }

    LaunchedEffect(rect) { animateDirected(motion, rect, referenceSizePx.coerceAtLeast(1f)) }

    with(density) {
        Box(
            Modifier
                .offset { IntOffset(motion.x.value.toInt(), motion.y.value.toInt()) }
                .width(motion.w.value.toDp())
                .height(motion.h.value.toDp())
                .graphicsLayer { scaleX = motion.scale.value; scaleY = motion.scale.value }
                .clip(folderTileShape(isOpen, motion.w.value.toDp()))
                .background(if (isOpen && selection.selectMode && selection.isSelected) Azphalt.Ink else hue)
                .combinedClickable(
                    onClick = { actions.onTap(entry) },
                    onLongClick = { actions.onLongPress(entry) }
                )
                .padding(if (isOpen) 14.dp else 6.dp),
            contentAlignment = if (isOpen) Alignment.CenterStart else Alignment.BottomStart
        ) {
            FolderTileContent(entry, isOpen, motion.w.value.toDp(), selection, actions)
        }
    }
}

private fun folderTileShape(isOpen: Boolean, width: Dp) = when {
    isOpen -> AzphaltSurface.recordTile
    width < 20.dp -> AzphaltSurface.capsule
    else -> AzphaltSurface.note
}

private operator fun FolderTileGeometry.component1() = rect
private operator fun FolderTileGeometry.component2() = isOpen
private operator fun FolderTileGeometry.component3() = referenceSizePx

@Composable
private fun FolderTileContent(
    entry: VfsEntry,
    isOpen: Boolean,
    width: Dp,
    selection: FolderTileSelection,
    actions: FolderEntryActions
) {
    if (isOpen) {
        val isSelectedTint = selection.selectMode && selection.isSelected
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selection.selectMode) SelectMark(selection.isSelected, dark = true)
                Text(
                    entry.name.uppercase(), color = if (isSelectedTint) Azphalt.Yellow else Azphalt.White,
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em, maxLines = 1
                )
            }
            if (!selection.selectMode) EntryMenu(entry, actions.onRename, actions.onDelete, actions.onShare, tint = Azphalt.White)
        }
    } else if (width > 24.dp) {
        Text(
            entry.name.uppercase(), color = Azphalt.White, fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em, maxLines = 2
        )
    }
}

@Composable
private fun OverflowTile(rect: TileRect, count: Int, onClick: () -> Unit) {
    val density = LocalDensity.current
    with(density) {
        Box(
            Modifier
                .offset { IntOffset(rect.x.toInt(), rect.y.toInt()) }
                .width(rect.w.toDp())
                .height(rect.h.toDp())
                .clip(AzphaltSurface.note)
                .background(Azphalt.Ink.copy(alpha = .14f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "+$count", color = Azphalt.currentGround.onPage, fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em
            )
        }
    }
}

@Composable
private fun ExpandableLevel(depth: Int, openChain: List<VfsEntry>, ctx: LevelContext) {
    val entries = ctx.entriesAt(depth)
    val openEntry = openChain.getOrNull(depth)
    val folders = entries?.filter { it.isDirectory } ?: emptyList()
    val files = entries?.filterNot { it.isDirectory } ?: emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (folders.isNotEmpty()) {
            if (openEntry == null) {
                Text(
                    // .55f, the app-wide "text-muted, eyebrows and captions" tier (style guide "03 -
                    // Transparency") - the 45% tier is for micro labels inside a pill, which this
                    // section caption isn't.
                    "FOLDERS · ${folders.size}", color = Azphalt.Ink.copy(alpha = .55f),
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em
                )
            }
            FolderBand(folders = folders, openEntry = openEntry, selection = ctx.selection, actions = ctx.entryActionsAt(depth))
        }
        if (openEntry != null) {
            Box(Modifier.padding(start = nestingInset(depth), top = nestingTopInset(depth))) {
                ExpandableLevel(depth = depth + 1, openChain = openChain, ctx = ctx)
            }
        } else if (entries == null) {
            // VFS-14: this depth's own listing hasn't come back yet - a folder just tapped open,
            // or a filter/sort change still in flight - not "there's genuinely nothing here."
            LoadingLabel()
        } else if (folders.isEmpty() && files.isEmpty()) {
            EmptyLabel()
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
            FileRows(files, ctx.selection, ctx.entryActionsAt(depth), ctx.totalBytes, ctx.preview)
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
private fun FileRows(
    files: List<VfsEntry>,
    selection: SelectionState,
    actions: FolderEntryActions,
    totalBytes: Long?,
    preview: PreviewContext
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
                items(images, key = { it.path }) { img -> ImageTile(img, selection, actions, totalBytes) }
            }
        }
        others.forEach { f ->
            key(f.path) { FileRow(f, selection, actions, preview) }
        }
    }
}

@Composable
private fun ImageTile(img: VfsEntry, selection: SelectionState, actions: FolderEntryActions, totalBytes: Long?) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(AzphaltSurface.note)
            // The tile's own hue never changes on selection - only the mark does.
            .background(fileHue(img, totalBytes))
            .combinedClickable(onClick = { actions.onTap(img) }, onLongClick = { actions.onLongPress(img) })
    ) {
        Text(
            img.name, color = Azphalt.White.copy(alpha = .8f), fontSize = 7.sp,
            fontWeight = FontWeight.Bold, maxLines = 1,
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
        )
        if (selection.selectMode) {
            Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                SelectMark(img.path in selection.selected, dark = true)
            }
        }
    }
}

@Composable
private fun FileRow(f: VfsEntry, selection: SelectionState, actions: FolderEntryActions, preview: PreviewContext) {
    val selectedTint = selection.selectMode && f.path in selection.selected
    // F3: open widens the shape from a pill into the same "note" surface every other expanding
    // panel on this screen uses - a fully-rounded pill reads oddly once it's tall enough to hold
    // a multi-line preview.
    val isOpen = preview.openPath == f.path
    Column(
        Modifier
            .fillMaxWidth()
            .clip(if (isOpen) AzphaltSurface.note else RoundedCornerShape(percent = 50))
            // A selected row goes to ink with an inverted (yellow) foreground - same
            // "ink means selected/open" convention PillMenu's open pills use.
            .background(if (selectedTint) Azphalt.Ink else Azphalt.Ink.copy(alpha = .09f))
            .animateContentSize(tween(Azphalt.SLIDE_MS, easing = LinearEasing))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { actions.onTap(f) }, onLongClick = { actions.onLongPress(f) })
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selection.selectMode) SelectMark(f.path in selection.selected, dark = false)
                // Filenames are literal, not labels - the one place real case survives outside
                // body copy, same as every other identifier here that names an actual leaf item
                // rather than a folder/chrome label.
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
            if (!selection.selectMode) EntryMenu(f, actions.onRename, actions.onDelete, actions.onShare, tint = Azphalt.Ink)
        }
        if (isOpen) FilePreviewPane(f, preview.cache[f.path]) { preview.onOpenFile(f.path) }
    }
}

private val PREVIEW_MAX_HEIGHT = 260.dp

/** F3: "the design has a tapped file reveal its contents inside its own rectangle - scrollable,
 *  with markdown rendered rather than shown as source, and syntax highlighting from whichever
 *  linters the user has chosen to install." [preview] is null while still loading; a null
 *  [FilePreview.text] means the read either failed or found something that isn't text - either
 *  way this says what happened instead of appearing inert. */
@Composable
private fun FilePreviewPane(entry: VfsEntry, preview: FilePreview?, onOpenInEditor: () -> Unit) {
    val onPage = Azphalt.currentGround.onPage
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = PREVIEW_MAX_HEIGHT)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
    ) {
        when {
            preview == null -> LoadingLabel()
            preview.text == null -> {
                val kind = entry.name.substringAfterLast('.', "THIS").uppercase()
                Text(
                    if (preview.truncated) "TOO LARGE TO PREVIEW" else "CAN'T PREVIEW $kind",
                    color = Azphalt.Ink.copy(alpha = .5f), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.12.em
                )
            }
            preview.isMarkdown -> MarkdownPreview(preview.text, onPage, Azphalt.Ink)
            preview.styled.isNotEmpty() -> preview.styled.forEach { line ->
                Text(buildStyledLine(line, Azphalt.Ink), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            else -> Text(preview.text, color = Azphalt.Ink, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Text(
            "OPEN IN EDITOR", color = Azphalt.Ink.copy(alpha = .7f), fontSize = 8.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp).clickable(onClick = onOpenInEditor)
        )
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
    // UI-7: these Text labels had no padding at all - well under the 48dp minimum touch target,
    // and with only 10dp between them, an imprecise tap intended for RENAME/SHARE could land on
    // the destructive × instead. Each gets its own invisible 48x48dp centered tap zone, the same
    // minWidth+minHeight pairing SessionTabs/ModifierKeys already use for a single-glyph target -
    // that alone also restores real separation between them without changing the visible spacing.
    val tapZone = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(tapZone.clickable { onRename(entry) }, contentAlignment = Alignment.Center) {
            Text("RENAME", color = tint.copy(alpha = .7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        if (!entry.isDirectory) {
            Box(tapZone.clickable { onShare(entry) }, contentAlignment = Alignment.Center) {
                Text("SHARE", color = tint.copy(alpha = .7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(tapZone.clickable { onDelete(entry) }, contentAlignment = Alignment.Center) {
            Text("×", color = tint.copy(alpha = .85f), fontSize = 13.sp)
        }
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
