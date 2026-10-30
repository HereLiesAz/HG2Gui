@file:OptIn(ExperimentalFoundationApi::class)

package com.hereliesaz.hg2gui.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.managers.StyledSpan
import com.hereliesaz.hg2gui.managers.TerminalHistoryEntry
import com.hereliesaz.hg2gui.terminal.ChainOperator
import com.hereliesaz.hg2gui.terminal.CompletionCandidate
import com.hereliesaz.hg2gui.terminal.CompletionRequest
import com.hereliesaz.hg2gui.terminal.CompletionSelectionPolicy
import com.hereliesaz.hg2gui.terminal.PlatformCompletionBridge
import com.hereliesaz.hg2gui.terminal.ShellAliases
import com.hereliesaz.hg2gui.terminal.ShellPresentation
import com.hereliesaz.hg2gui.terminal.applyCompletion
import com.hereliesaz.hg2gui.terminal.chainSegment
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import com.hereliesaz.hg2gui.ui.menu.PillMenu
import com.hereliesaz.hg2gui.ui.menu.contrastingText
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_BUFFER_ENTRIES = 200

// Lines past the animated head (OUTPUT_WIPE_STAGGER_CAP) render as one block; this bounds that block.
private const val MAX_RENDERED_TAIL_LINES = 1500

private fun SessionUiState.updateBufferEntry(entryId: Long, transform: (TerminalHistoryEntry) -> TerminalHistoryEntry) {
    buffer = buffer.map { entry -> if (entry.id == entryId) transform(entry) else entry }
}

private fun SessionUiState.pendingSegment(): String = buildString {
    if (tokens.isNotEmpty()) append(tokens.joinToString(" "))
    if (inputText.isNotBlank()) {
        if (isNotEmpty()) append(" ")
        append(inputText.trim())
    }
}.trim()

private fun SessionUiState.completionLine(): String = buildString {
    append(composedPrefix)
    if (tokens.isNotEmpty()) {
        if (isNotEmpty() && !last().isWhitespace()) append(' ')
        append(tokens.joinToString(" "))
    }
    if (inputText.isNotEmpty()) {
        if (isNotEmpty() && !last().isWhitespace()) append(' ')
        append(inputText)
    } else if (tokens.isNotEmpty() && (isEmpty() || !last().isWhitespace())) {
        append(' ')
    }
}

private fun expectedInputKind(tokens: List<String>): String? {
    if (tokens.isEmpty()) return null
    val flattened = tokens.flatMap { it.trim().split(Regex("\\s+")).filter(String::isNotBlank) }
    if (flattened.isEmpty()) return null
    val verb = flattened.first().substringAfterLast('/').lowercase()
    val tail = flattened.drop(1)
    val last = flattened.last().lowercase()

    fun labelFrom(text: String): String? {
        val t = text.lowercase()
        return when {
            "url" in t || "uri" in t -> "URL"
            "file" in t || "output" in t || "config" in t || "cert" in t || "key" in t -> "FILE PATH"
            "directory" in t || "dir" in t || "path" in t -> "PATH"
            "hostname" in t || "host" in t || "server" in t -> "HOST OR ADDRESS"
            "port" in t -> "PORT"
            "username" in t || "user" in t -> "USERNAME"
            "package" in t -> "PACKAGE NAME"
            "pattern" in t || "regex" in t || "regexp" in t -> "SEARCH PATTERN"
            "query" in t || "search" in t -> "SEARCH TERM"
            "repository" in t || "repo" in t -> "REPOSITORY"
            "branch" in t -> "BRANCH"
            "message" in t -> "MESSAGE"
            "name" in t -> "NAME"
            else -> null
        }
    }

    val explicitMetavariable = flattened.asReversed().firstNotNullOfOrNull { token ->
        Regex("[<\\[]?([A-Z][A-Z0-9_-]{1,})[>\\]]?").find(token)?.groupValues?.getOrNull(1)?.let(::labelFrom)
    }
    if (explicitMetavariable != null) return explicitMetavariable

    if (last.startsWith("-")) {
        labelFrom(last)?.let { return it }
    }

    when (verb) {
        "curl", "wget" -> {
            val hasTarget = tail.any { it.contains("://") || (!it.startsWith("-") && '.' in it) }
            if (!hasTarget) return "URL"
        }
        "ssh", "sftp", "telnet", "ping", "traceroute", "tracepath", "dig", "nslookup", "whois", "nmap", "nc", "netcat" -> {
            val operands = tail.filterNot { it.startsWith("-") }
            if (operands.isEmpty()) return "HOST OR ADDRESS"
        }
        "grep", "egrep", "fgrep", "rg", "ripgrep" -> {
            val operands = tail.filterNot { it.startsWith("-") }
            if (operands.isEmpty()) return "SEARCH PATTERN"
        }
        "cd", "cat", "less", "more", "head", "tail", "touch", "mkdir", "rmdir", "rm", "stat", "readlink", "realpath" -> {
            val operands = tail.filterNot { it.startsWith("-") }
            if (operands.isEmpty()) return "FILE OR PATH"
        }
        "cp", "mv" -> {
            val operands = tail.filterNot { it.startsWith("-") }
            if (operands.size < 2) return "SOURCE AND DESTINATION"
        }
        "apt", "apt-get", "pkg", "hg2pkg" -> {
            val operation = tail.firstOrNull()?.lowercase()
            if (operation in setOf("install", "in", "remove", "rm", "uninstall", "purge", "show", "info") && tail.size < 2) {
                return "PACKAGE NAME"
            }
            if (operation == "search" && tail.size < 2) return "SEARCH TERM"
        }
        "git" -> {
            when (tail.firstOrNull()?.lowercase()) {
                "clone" -> if (tail.size < 2) return "REPOSITORY URL"
                "checkout", "switch" -> if (tail.size < 2) return "BRANCH"
                "add" -> if (tail.size < 2) return "FILE OR PATH"
            }
        }
    }

    return if (last.startsWith("-")) "ARGUMENT" else null
}


private val TERMINAL_MAX_CONTENT_WIDTH = 720.dp
private const val NOTICE_MS = 2200L
private const val MAX_SESSION_NAME = 24
private const val TAIL_SCROLL_PASSES = 2
private const val CARD_WASH = .09f
private const val CARD_WASH_SELECTED = .15f

/** A transient message for the hint line (copy feedback, an unsupported key). A new instance restarts the timer. */
private class ScreenNotice(val text: String)

/**
 * Scrolls so the *end* of the last item is in view. `animateScrollToItem(last)` aligns the item's
 * top with the viewport top, which hides the tail of any entry taller than the buffer - exactly the
 * output the user is waiting for. The scroll is instant (not animated) because it runs once per
 * output chunk and an animation restarted per chunk is what used to yank the list around.
 */
private suspend fun LazyListState.scrollToTail(lastIndex: Int) {
    scrollToItem(lastIndex)
    repeat(TAIL_SCROLL_PASSES) {
        val info = layoutInfo
        val last = info.visibleItemsInfo.lastOrNull { it.index == lastIndex } ?: return
        val overshoot = last.offset + last.size - (info.viewportEndOffset - info.afterContentPadding)
        if (overshoot <= 0) return
        scrollBy(overshoot.toFloat())
    }
}

@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod", "MagicNumber", "NestedBlockDepth")
@Composable
fun TerminalScreen(
    tree: List<MenuNode>,
    knownCommands: List<String> = emptyList(),
    sessions: List<SessionUiState>,
    activeSessionId: String,
    onSessionPick: (String) -> Unit,
    onNewSession: () -> Unit,
    onCloseSession: (String) -> Unit,
    fullscreen: Boolean,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenFiles: () -> Unit,
    onFilesButtonPositioned: (Rect) -> Unit = {},
    onWizard: (wizardId: String) -> Unit = {},
    onCrumbPositioned: (id: String, rect: Rect) -> Unit = { _, _ -> },
    onCopy: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
    onInterrupt: (sessionId: String) -> Unit = {},
    /** Called after every copy with a short message ("Copied output"); the screen also shows it itself. */
    onCopied: (message: String) -> Unit = {},
    /** Runs the short read-only probes (git status) that feed the branch/dirty chip; default shows none. */
    shellProbe: ShellProbe = NoShellProbe,
    onRun: suspend (
        sessionId: String,
        line: String,
        onOutput: (String) -> Unit,
        onNeedInput: suspend (prompt: String) -> String,
        onExit: (Int?) -> Unit,
        onStderr: (String) -> Unit,
        onStyledOutput: (List<List<StyledSpan>>) -> Unit
    ) -> Unit
) {
    val active = sessions.first { it.id == activeSessionId }
    val scope = rememberCoroutineScope()
    val listState = active.listState
    val inputFocusRequester = remember(active.id) { FocusRequester() }
    var selectedEntryId by remember(active.id) { mutableStateOf<Long?>(null) }
    var expandedEntryId by remember(active.id) { mutableStateOf<Long?>(null) }
    var selectedShowRaw by remember(active.id) { mutableStateOf(false) }
    var requestedInputKind by remember(active.id) { mutableStateOf<String?>(null) }
    var armedModifier by remember(active.id) { mutableStateOf<ComposerModifier?>(null) }
    var notice by remember { mutableStateOf<ScreenNotice?>(null) }
    var renaming by remember { mutableStateOf<SessionUiState?>(null) }
    val latestActiveId by rememberUpdatedState(activeSessionId)
    // With the keyboard up the chrome collapses (status line, spacing) so the output and the pill
    // stack keep their height - the workspace never trades them for a thin side column.
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(selectedEntryId) {
        selectedShowRaw = false
    }

    LaunchedEffect(requestedInputKind) {
        if (requestedInputKind != null) inputFocusRequester.requestFocus()
    }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MS)
            notice = null
        }
    }

    // Returning to a tab clears its "finished while you were away" badge.
    LaunchedEffect(active.id) {
        active.finishedWhileAway = false
    }

    LaunchedEffect(
        active.id,
        active.composedPrefix,
        active.tokens,
        active.inputText,
        active.cwd,
        active.running,
        active.pendingPrompt,
        active.shellPresentation
    ) {
        if (active.running || active.pendingPrompt != null) {
            active.clearCompletions()
            return@LaunchedEffect
        }
        val line = active.completionLine()
        if (line.isBlank()) {
            active.clearCompletions()
            return@LaunchedEffect
        }
        delay(120)
        val presentation = active.shellPresentation
        val requestKey = "${active.cwd}\u0000${presentation.shell}\u0000$line"
        val candidates = PlatformCompletionBridge.complete(
            CompletionRequest(
                line = line,
                cursor = line.length,
                cwd = active.cwd,
                shell = presentation.shell,
                provider = presentation.completionProvider
            )
        )
        active.updateCompletions(requestKey, candidates)
    }

    // After each command (and on a directory change, and when a tab is shown) re-derive the cwd /
    // git branch / dirty / exit status the status line and the completion call work from.
    val latestProbe by rememberUpdatedState(shellProbe)
    LaunchedEffect(active.id, active.cwd, active.running) {
        if (active.running) return@LaunchedEffect
        val lastExit = active.buffer.lastOrNull()?.takeIf { !it.isRunning }?.exitCode
        try {
            active.refreshShellPresentation(lastExit, latestProbe)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Advisory status only; a failing probe must not disturb the terminal.
        }
    }

    // Whether the output follows its newest line is decided only when a scroll ends: at the bottom
    // means "follow", anywhere else means the user has taken over. Programmatic tail scrolls end at
    // the bottom, so they keep following.
    LaunchedEffect(active.id) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) active.followTail = !listState.canScrollForward
        }
    }

    val newestEntry = active.buffer.lastOrNull()
    LaunchedEffect(
        active.buffer.size,
        newestEntry?.id,
        newestEntry?.output,
        newestEntry?.stderr,
        newestEntry?.styledOutput,
        selectedEntryId
    ) {
        if (active.buffer.isNotEmpty() && active.followTail) {
            listState.scrollToTail(active.buffer.lastIndex)
        }
    }

    val executeCommand = {
        val session = active
        val pendingPrompt = session.pendingPrompt
        if (pendingPrompt != null) {
            val answer = session.pendingSegment()
            session.tokens = emptyList()
            session.inputText = ""
            requestedInputKind = null
            session.answerPrompt(answer)
        } else {
            val fullLine = (session.composedPrefix + session.pendingSegment()).trim()

            if (fullLine.isNotEmpty() && !session.running) {
                requestedInputKind = null
                armedModifier = null
                session.running = true
                session.followTail = true
                session.transientStatus = null
                session.clearCompletions()
                if (session.commandHistory.isEmpty() || session.commandHistory.last() != fullLine) {
                    session.commandHistory = (session.commandHistory + fullLine).takeLast(MAX_BUFFER_ENTRIES)
                }
                session.historyIndex = -1
                val lineToRun = fullLine
                val execLine = ShellAliases.expand(lineToRun)
                session.tokens = emptyList()
                session.inputText = ""
                session.composedPrefix = ""

                val newEntry = TerminalHistoryEntry(command = lineToRun, isRunning = true)
                val entryId = newEntry.id
                session.buffer = session.buffer + newEntry

                scope.launch {
                    var exitCode: Int? = null
                    try {
                        onRun(
                            session.id,
                            execLine,
                            { outputChunk ->
                                session.transientStatus = ShellAliases.transientStatusLine(outputChunk)
                                session.updateBufferEntry(entryId) { it.copy(output = outputChunk) }
                            },
                            { prompt -> session.awaitPromptAnswer(prompt) },
                            { code -> exitCode = code },
                            // The engine calls these from its IO threads. updateBufferEntry is a
                            // read-modify-write of session.buffer, so hop to this (main) scope -
                            // otherwise it races the other updates and loses entries.
                            { stderrChunk -> scope.launch { session.updateBufferEntry(entryId) { it.copy(stderr = stderrChunk) } } },
                            { styled -> scope.launch { session.updateBufferEntry(entryId) { it.copy(styledOutput = styled) } } }
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        exitCode = -1
                        session.updateBufferEntry(entryId) { it.copy(output = it.output + "\nerror: ${e.message}") }
                    } finally {
                        session.transientStatus = null
                        session.updateBufferEntry(entryId) { it.copy(isRunning = false, exitCode = exitCode) }
                        if (session.buffer.size > MAX_BUFFER_ENTRIES) {
                            session.buffer = session.buffer.takeLast(MAX_BUFFER_ENTRIES)
                            session.wipeLedger.retainOnly(session.buffer.map { it.id }.toSet())
                        }
                        if (session.id != latestActiveId) session.finishedWhileAway = true
                        session.running = false
                    }
                }
            }
        }
    }

    fun historyPrevious() {
        requestedInputKind = null
        if (active.commandHistory.isNotEmpty()) {
            val nextIdx = if (active.historyIndex == -1) {
                active.commandHistory.size - 1
            } else {
                (active.historyIndex - 1).coerceAtLeast(0)
            }
            active.historyIndex = nextIdx
            active.inputText = active.commandHistory[nextIdx]
            active.tokens = emptyList()
        }
    }

    fun historyNext() {
        requestedInputKind = null
        if (active.historyIndex >= 0) {
            val nextIdx = active.historyIndex + 1
            if (nextIdx < active.commandHistory.size) {
                active.historyIndex = nextIdx
                active.inputText = active.commandHistory[nextIdx]
            } else {
                active.historyIndex = -1
                active.inputText = ""
            }
            active.tokens = emptyList()
        }
    }

    // CTRL / ALT were armed and the next character arrived: interpret it instead of inserting it.
    fun applyModifiedKey(modifier: ComposerModifier, key: Char) {
        when (val result = ComposerKeys.apply(modifier, key, active.inputText, active.commandHistory.lastOrNull())) {
            is ModifiedKey.Replace -> {
                requestedInputKind = null
                active.inputText = result.text
            }
            ModifiedKey.Interrupt -> if (active.running) {
                onInterrupt(active.id)
            } else {
                // Idle: abandon the whole line being composed, like ^C at a shell prompt.
                requestedInputKind = null
                active.tokens = emptyList()
                active.inputText = ""
                active.composedPrefix = ""
                active.historyIndex = -1
                active.clearCompletions()
            }
            ModifiedKey.Run -> executeCommand()
            ModifiedKey.HistoryPrevious -> historyPrevious()
            ModifiedKey.HistoryNext -> historyNext()
            ModifiedKey.EndOfInput -> if (active.pendingPrompt != null) {
                active.answerPrompt("")
            } else {
                notice = ScreenNotice("CTRL+D: nothing is waiting for input")
            }
            is ModifiedKey.Unsupported -> {
                notice = ScreenNotice(result.reason)
            }
        }
    }

    val handleInputChange: (String) -> Unit = { newText ->
        val armed = armedModifier
        val typed = if (armed != null) ComposerKeys.detectTypedChar(active.inputText, newText) else null
        armedModifier = null
        if (armed != null && typed != null) {
            applyModifiedKey(armed, typed)
        } else {
            active.inputText = newText
            if (newText.isNotBlank()) requestedInputKind = null
        }
    }

    val copyWithFeedback: (String, String) -> Unit = { text, message ->
        onCopy(text)
        onCopied(message)
        notice = ScreenNotice(message)
    }

    val yesNoPrompt = active.pendingPrompt?.takeIf { ShellAliases.looksLikeYesNo(it) }
    if (yesNoPrompt != null) {
        val parsed = remember(active.pendingPromptDetail, yesNoPrompt) {
            ConfirmPrompts.parse(active.pendingPromptDetail ?: yesNoPrompt)
        }
        val answer: (String) -> Unit = { reply ->
            active.tokens = emptyList()
            active.inputText = ""
            requestedInputKind = null
            active.answerPrompt(reply)
        }
        ConfirmDialog(
            title = parsed.title,
            message = parsed.body,
            confirmLabel = "YES",
            cancelLabel = "NO",
            onConfirm = { answer("y") },
            // Back, or a tap outside, is the safe answer.
            onDismiss = { answer("n") },
            tone = if (parsed.kind == ConfirmPromptKind.General) ConfirmTone.Normal else ConfirmTone.Elevated,
            badge = parsed.badge,
            monospaceMessage = true
        )
    }

    renaming?.let { target ->
        var draft by remember(target.id) { mutableStateOf(target.name) }
        val save = {
            val value = draft.trim().take(MAX_SESSION_NAME)
            if (value.isNotEmpty()) target.name = value
            renaming = null
        }
        ConfirmDialog(
            title = "Rename session",
            message = "",
            confirmLabel = "SAVE",
            onConfirm = save,
            onDismiss = { renaming = null },
            confirmTint = Azphalt.Yellow,
            extraContent = { RenameField(draft, { draft = it }, save) }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .screenInsets(fullscreen = fullscreen)
    ) {
        Column(Modifier.weight(1f).maxContentWidth(TERMINAL_MAX_CONTENT_WIDTH)) {
            SessionTabs(
                sessions = sessions,
                activeId = activeSessionId,
                compact = imeOpen,
                onOpenSettings = onOpenSettings,
                onOpenGuide = onOpenGuide,
                onOpenFiles = onOpenFiles,
                onFilesButtonPositioned = onFilesButtonPositioned,
                onPick = onSessionPick,
                onNew = onNewSession,
                onClose = onCloseSession,
                onRename = { renaming = it }
            )

            if (!imeOpen) {
                ShellStatusLine(cwd = active.cwd, presentation = active.shellPresentation)
            }

            active.transientStatus?.takeIf { active.running }?.let { LiveStatusStrip(it) }

            val pendingPrompt = active.pendingPrompt
            val answerNode = when {
                pendingPrompt == null || ShellAliases.looksLikeYesNo(pendingPrompt) -> null
                else -> ShellAliases.numberedMenuChoices(pendingPrompt)?.let { choices ->
                    MenuNode(
                        id = "answer",
                        label = "Answer",
                        emitsToken = false,
                        children = choices.map { (number, label) ->
                            MenuNode(id = "answer-$number", label = "$number) $label", value = number)
                        }
                    )
                } ?: ShellAliases.bracketedChoices(pendingPrompt)?.let { choices ->
                    MenuNode(
                        id = "answer",
                        label = "Answer",
                        emitsToken = false,
                        children = choices.map { choice -> MenuNode(id = "answer-$choice", label = choice, value = choice) }
                    )
                }
            }

            val completionNode = completionNodeFor(active)
            val suggestionNode = suggestionNodeFor(active, knownCommands)
            val chainNode = chainNodeFor(active)
            val effectiveTree = tree + listOfNotNull(completionNode, suggestionNode, chainNode, answerNode)

            FluidTerminalWorkspace(
                hasBuffer = active.buffer.isNotEmpty(),
                estimatedPillRows = effectiveTree.size,
                // An output card the user expanded takes the whole workspace; COLLAPSE restores.
                bufferExpanded = active.buffer.any { it.id == expandedEntryId },
                modifier = Modifier.weight(1f),
                bufferContent = {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(active.buffer, key = { it.id }) { entry ->
                                BufferEntry(
                                    entry = entry,
                                    isSelected = selectedEntryId == entry.id,
                                    showRaw = selectedEntryId == entry.id && selectedShowRaw,
                                    isExpanded = expandedEntryId == entry.id,
                                    hintActions = entry.id == newestEntry?.id && selectedEntryId == null,
                                    ledger = active.wipeLedger,
                                    actions = EntryActions(
                                        onSelect = { selectedEntryId = if (selectedEntryId == entry.id) null else entry.id },
                                        onCopyLine = { line -> copyWithFeedback(line, "Line copied") },
                                        onCopy = { text -> copyWithFeedback(text, "Output copied") },
                                        onShare = onShare,
                                        onRerun = { command ->
                                            // Loads the command for editing and clears any half-built
                                            // chain, like History's picker; RUN is still the user's call.
                                            requestedInputKind = null
                                            armedModifier = null
                                            expandedEntryId = null
                                            active.tokens = emptyList()
                                            active.composedPrefix = ""
                                            active.historyIndex = -1
                                            active.inputText = command
                                            notice = ScreenNotice("Command loaded - press RUN")
                                        },
                                        onToggleRaw = { selectedShowRaw = !selectedShowRaw },
                                        onToggleExpand = {
                                            expandedEntryId = if (expandedEntryId == entry.id) null else entry.id
                                        },
                                        onStop = { onInterrupt(active.id) }
                                    )
                                )
                            }
                        }
                        if (!active.followTail && active.buffer.isNotEmpty()) {
                            AzphaltChip(
                                label = "LATEST ↓",
                                onClick = {
                                    active.followTail = true
                                    scope.launch { listState.scrollToTail(active.buffer.lastIndex) }
                                },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp),
                                contentDescription = "Jump to latest output"
                            )
                        }
                    }
                },
                commandTreeContent = { workspaceModifier ->
                    PillMenu(
                        roots = effectiveTree,
                        modifier = workspaceModifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        onRun = { picked, isTerminal ->
                            val completion = completionCandidateFromPick(picked, active)
                            val chainOperator = chainOperatorFromPick(picked)
                            when {
                                completion != null -> {
                                    requestedInputKind = null
                                    val insertion = applyCompletion(active.inputText, candidate = completion)
                                    active.inputText = insertion.line
                                    active.clearCompletions()
                                }
                                chainOperator != null -> {
                                    requestedInputKind = null
                                    active.composedPrefix = chainSegment(active.composedPrefix, active.pendingSegment(), chainOperator)
                                    active.tokens = emptyList()
                                    active.inputText = ""
                                }
                                else -> {
                                    active.tokens = picked
                                    if (picked.isNotEmpty()) active.inputText = ""
                                    if (isTerminal && pendingPrompt != null) {
                                        requestedInputKind = null
                                        executeCommand()
                                    } else if (isTerminal) {
                                        requestedInputKind = expectedInputKind(picked)
                                    } else {
                                        requestedInputKind = null
                                    }
                                }
                            }
                        },
                        onWizard = onWizard,
                        onCrumbPositioned = onCrumbPositioned
                    )
                }
            )

            val maskInput = pendingPrompt != null && ShellAliases.looksLikePassword(pendingPrompt)
            val choiceRequired = requestedInputKind != null &&
                CompletionSelectionPolicy.shouldReplaceFreeForm(active.inputText, active.completionCandidates)
            val visibleInputRequest = requestedInputKind?.takeIf {
                pendingPrompt == null && active.inputText.isBlank() && !choiceRequired
            }
            val armed = armedModifier

            CommandLine(
                composedPrefix = active.composedPrefix,
                tokens = active.tokens,
                inputText = active.inputText,
                onInputTextChange = handleInputChange,
                hint = notice?.text ?: armed?.let { ComposerKeys.hint(it) } ?: when {
                    pendingPrompt != null -> pendingPrompt.substringAfterLast('\n').ifBlank { "Waiting for input…" }
                    choiceRequired -> "Choose ${requestedInputKind!!.lowercase()} above"
                    visibleInputRequest != null -> "Type ${visibleInputRequest.lowercase()} below, then press run"
                    active.transientStatus != null -> active.transientStatus!!
                    active.running -> "Running…"
                    active.tokens.isNotEmpty() || active.inputText.isNotBlank() -> "Ready — press run"
                    else -> "Pick a category"
                },
                emphasizeHint = pendingPrompt != null || notice != null || armed != null,
                compact = imeOpen,
                runLabel = if (pendingPrompt != null) "SEND" else "RUN",
                enabled = pendingPrompt != null ||
                    (!active.running && !choiceRequired && (active.tokens.isNotEmpty() || active.inputText.isNotBlank())),
                masked = maskInput,
                inputRequest = visibleInputRequest,
                inputSuppressed = choiceRequired,
                inputFocusRequester = inputFocusRequester,
                onRun = executeCommand
            )

            ModifierKeys(
                armed = armed,
                compact = imeOpen,
                onKeyClick = { key ->
                    when (key) {
                        "ctrl" -> {
                            armedModifier = if (armed == ComposerModifier.Ctrl) null else ComposerModifier.Ctrl
                        }
                        "alt" -> {
                            armedModifier = if (armed == ComposerModifier.Alt) null else ComposerModifier.Alt
                        }
                        "↑" -> {
                            armedModifier = null
                            historyPrevious()
                        }
                        "↓" -> {
                            armedModifier = null
                            historyNext()
                        }
                        // ESC cancels the current pick only (the chosen pills, a pending operand
                        // request, the completion list). Typed text and a chain being built stay;
                        // CTRL+U / CTRL+C are the ways to throw those away.
                        "esc" -> {
                            armedModifier = null
                            requestedInputKind = null
                            active.tokens = emptyList()
                            active.clearCompletions()
                        }
                        // TAB completes or accepts the history suggestion; with neither available it
                        // does nothing (it used to append a space, which is not what TAB means).
                        "tab" -> {
                            armedModifier = null
                            val first = active.completionCandidates.firstOrNull()
                            val suggestion = if (active.inputText.isNotBlank()) {
                                ShellAliases.autosuggest(active.inputText, active.commandHistory)
                            } else {
                                null
                            }
                            when {
                                first != null -> {
                                    active.inputText = applyCompletion(active.inputText, candidate = first).line
                                    active.clearCompletions()
                                }
                                suggestion != null -> {
                                    active.inputText += suggestion
                                }
                                else -> {
                                    notice = ScreenNotice("Nothing to complete")
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun LiveStatusStrip(status: String) {
    val ground = Azphalt.currentGround
    val onPage = ground.onPage
    val percent = Regex("""\b(\d{1,3})%""").find(status)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 100)
    val surface = remember(ground.page) { Azphalt.Ink.copy(alpha = CARD_WASH).compositeOver(ground.page) }
    val liveColor = remember(surface) { statusColorOn(StatusKind.Info, surface) }
    val type = LocalAzphaltType.current
    // The announced text is coarse on purpose: a live region that re-spoke every progress line
    // would be unusable, so it only changes when the percentage crosses a ten.
    val spoken = if (percent != null) "Live status, ${percent / 10 * 10} percent" else "Live status, running"
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(AzphaltSurface.recordTile)
            .background(Azphalt.Ink.copy(alpha = CARD_WASH))
            .padding(12.dp)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = spoken
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LIVE", style = type.eyebrow.copy(color = liveColor, fontWeight = FontWeight.Black))
            Spacer(Modifier.width(10.dp))
            Text(
                status,
                modifier = Modifier.weight(1f),
                style = type.body.copy(
                    color = onPage,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            )
        }
        if (percent != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(AzphaltSurface.capsule)
                    .background(Azphalt.Ink.copy(alpha = .14f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percent / 100f)
                        .clip(AzphaltSurface.capsule)
                        .background(progressFillOn(surface))
                )
            }
        }
    }
}

/** Everything a [BufferEntry] can ask its host to do. */
private class EntryActions(
    val onSelect: () -> Unit,
    val onCopyLine: (String) -> Unit,
    val onCopy: (String) -> Unit,
    val onShare: (String) -> Unit,
    val onRerun: (String) -> Unit,
    val onToggleRaw: () -> Unit,
    val onToggleExpand: () -> Unit,
    val onStop: () -> Unit
)

@Composable
private fun entryTextStyle(): TextStyle = LocalAzphaltType.current.body.copy(fontSize = 14.sp, lineHeight = 20.sp)

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun BufferEntry(
    entry: TerminalHistoryEntry,
    isSelected: Boolean,
    showRaw: Boolean,
    isExpanded: Boolean,
    hintActions: Boolean,
    ledger: OutputWipeLedger,
    actions: EntryActions
) {
    val kind = remember(entry.output) { classifyOutput(entry.output) }
    val onPage = Azphalt.currentGround.onPage
    val page = Azphalt.currentGround.page
    val wash = if (isSelected) CARD_WASH_SELECTED else CARD_WASH
    // The colour a card actually paints: status colours are checked against this, not the bare page.
    val surface = remember(page, wash) { Azphalt.Ink.copy(alpha = wash).compositeOver(page) }
    val muted = remember(surface) { statusColorOn(StatusKind.Muted, surface) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AzphaltSurface.recordTile)
            .background(Azphalt.Ink.copy(alpha = wash))
            .clickable(
                onClickLabel = if (isSelected) "Hide copy, re-run and share" else "Show copy, re-run and share",
                onClick = actions.onSelect
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$", style = entryTextStyle().copy(color = muted, fontWeight = FontWeight.Black))
            Spacer(Modifier.width(8.dp))
            Text(
                entry.command,
                modifier = Modifier.weight(1f, fill = false),
                style = entryTextStyle().copy(color = onPage, fontWeight = FontWeight.Bold)
            )
            StatusDot(entry, surface)
            if (entry.isRunning) {
                Spacer(Modifier.width(8.dp))
                AzphaltChip("STOP", actions.onStop, contentDescription = "Stop the running command")
            }
        }
        // Selecting (or expanding) acts on the output below, so the text can be selected with a
        // long press; tapping a single line copies it.
        SelectionContainer {
            Column {
                if (entry.output.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    ClassifiedOutput(kind, entry, onPage, surface, showRaw, ledger, actions.onCopyLine)
                }
                if (entry.stderr.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    StderrBlock(entry.stderr, surface)
                }
            }
        }
        if (isSelected && !entry.isRunning) {
            Spacer(Modifier.height(6.dp))
            EntryActionRow(entry, kind, showRaw, isExpanded, actions)
        } else if (hintActions && !entry.isRunning) {
            Spacer(Modifier.height(4.dp))
            Text(
                "TAP FOR COPY · RE-RUN · SHARE",
                style = LocalAzphaltType.current.eyebrow.copy(color = muted, letterSpacing = 0.1.em)
            )
        }
    }
}

/** COPY / RE-RUN / SHARE (and the reading toggle, EXPAND) for one card, shown inside the card itself. */
@Composable
private fun EntryActionRow(
    entry: TerminalHistoryEntry,
    kind: OutputKind,
    showRaw: Boolean,
    isExpanded: Boolean,
    actions: EntryActions
) {
    val copyText = entry.output.ifEmpty { entry.command }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AzphaltChip("COPY", { actions.onCopy(copyText) }, contentDescription = "Copy the output")
        AzphaltChip("RE-RUN", { actions.onRerun(entry.command) }, contentDescription = "Load this command into the command line")
        AzphaltChip("SHARE", { actions.onShare(copyText) }, contentDescription = "Share the output")
        ClassificationTogglePill(kind, showRaw, actions.onToggleRaw)
        AzphaltChip(
            label = "EXPAND",
            onClick = actions.onToggleExpand,
            selected = isExpanded,
            contentDescription = "Expand this output to fill the screen"
        )
    }
}

@Composable
private fun ClassifiedOutput(
    kind: OutputKind,
    entry: TerminalHistoryEntry,
    onPage: Color,
    surface: Color,
    showRaw: Boolean,
    ledger: OutputWipeLedger,
    onCopyLine: (String) -> Unit
) {
    when {
        kind == OutputKind.BINARY && !showRaw -> Text(
            "Binary output - not displayed as text.",
            style = entryTextStyle().copy(color = onPage.copy(alpha = .72f), fontStyle = FontStyle.Italic)
        )
        kind == OutputKind.ART && !showRaw -> AsciiArtCanvas(entry.output, onPage.copy(alpha = .8f))
        kind == OutputKind.TABLE && !showRaw -> KeyValueTable(entry.output, onPage)
        kind == OutputKind.WIDE_TABLE && !showRaw -> WideTableRecords(entry.output, onPage)
        else -> OutputLines(entry, surface, ledger, onCopyLine)
    }
}

@Composable
private fun ClassificationTogglePill(kind: OutputKind, showRaw: Boolean, onToggle: () -> Unit) {
    val label = when (kind) {
        OutputKind.BINARY -> "BINARY"
        OutputKind.ART -> "ART"
        OutputKind.TABLE -> "READING"
        OutputKind.WIDE_TABLE -> "REFLOW"
        OutputKind.PLAIN -> null
    } ?: return
    AzphaltChip(
        label = if (showRaw) label else "PLAIN TEXT",
        onClick = onToggle,
        contentDescription = if (showRaw) "Show as ${label.lowercase()}" else "Show as plain text"
    )
}

@Composable
private fun StatusDot(entry: TerminalHistoryEntry, surface: Color) {
    if (entry.isRunning) {
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(8.dp)
                .clip(AzphaltSurface.capsule)
                .background(Azphalt.Yellow)
                .semantics { contentDescription = "Running" }
        )
    } else if (entry.exitCode != null) {
        // Always state the outcome in words: a silent success and a silent failure must not look alike.
        val failed = entry.exitCode != 0
        val ink = remember(surface, failed) {
            statusColorOn(if (failed) StatusKind.Error else StatusKind.Muted, surface)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(8.dp).clip(AzphaltSurface.capsule).background(ink))
        Spacer(Modifier.width(6.dp))
        Text(
            if (failed) "exit ${entry.exitCode}" else "ok",
            style = LocalAzphaltType.current.pillCap.copy(color = ink, fontFamily = FontFamily.Monospace)
        )
    }
}

private val OUTPUT_WIPE_EASE = CubicBezierEasing(0f, .9f, .1f, 1f)
private const val OUTPUT_WIPE_MS = 240
private const val OUTPUT_WIPE_STAGGER_MS = 35L
private const val OUTPUT_WIPE_STAGGER_STEPS = 8
private const val OUTPUT_WIPE_STAGGER_CAP = 24
private const val ANSI_PLAIN_ALPHA = .8f

internal fun StyledSpan.inkColor(onPage: Color, background: Color = Azphalt.currentGround.page): Color =
    hue?.let { Azphalt.hues[it].readableOn(background) } ?: onPage.copy(alpha = ANSI_PLAIN_ALPHA)

/**
 * One output line as styled text. ANSI hues are pill colours, tuned to sit behind labels, not to be
 * text on the ground: each is pushed to a readable contrast against [background] (the ground by
 * default; cards pass their own painted surface).
 */
internal fun buildStyledLine(
    spans: List<StyledSpan>,
    onPage: Color,
    background: Color = Azphalt.currentGround.page
): AnnotatedString = buildAnnotatedString {
    spans.forEach { span ->
        withStyle(SpanStyle(color = span.inkColor(onPage, background), fontWeight = if (span.bold) FontWeight.Bold else null)) {
            append(span.text)
        }
    }
}

@Composable
private fun outputTextStyle(): TextStyle = LocalAzphaltType.current.body.copy(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 17.sp
)

@Composable
private fun OutputLines(
    entry: TerminalHistoryEntry,
    surface: Color,
    ledger: OutputWipeLedger,
    onCopyLine: (String) -> Unit
) {
    val lines = remember(entry.output, entry.styledOutput) {
        if (entry.styledOutput.isNotEmpty()) entry.styledOutput
        else entry.output.split("\n").map { listOf(StyledSpan(it)) }
    }
    val reduced = rememberReducedMotion()
    // Captured once per composition: lines already revealed before this card was (re)composed -
    // scrolled out and back in, or the screen left and returned to - appear at once. Only lines
    // that arrive while it is composed wipe in.
    val alreadyShown = remember(entry.id) { ledger.shownLines(entry.id) }
    val headCount = minOf(lines.size, OUTPUT_WIPE_STAGGER_CAP)
    SideEffect { ledger.record(entry.id, headCount) }
    val onPage = Azphalt.currentGround.onPage
    Column {
        lines.take(OUTPUT_WIPE_STAGGER_CAP).forEachIndexed { index, spans ->
            OutputWipeLine(
                relativeIndex = index - alreadyShown,
                animate = !reduced && index >= alreadyShown,
                spans = spans,
                surface = surface,
                onCopyLine = onCopyLine
            )
        }
        if (lines.size > OUTPUT_WIPE_STAGGER_CAP) {
            // Built once per output, not on every recomposition, and capped: a command that prints
            // tens of thousands of lines would otherwise lay out one enormous Text. The copy/share
            // actions still use the full entry.output.
            val tail = remember(lines, onPage, surface) {
                val rest = lines.drop(OUTPUT_WIPE_STAGGER_CAP)
                val shown = rest.takeLast(MAX_RENDERED_TAIL_LINES)
                buildAnnotatedString {
                    if (shown.size < rest.size) append("… ${rest.size - shown.size} earlier lines not shown (COPY has all of it)\n")
                    shown.forEachIndexed { index, spans ->
                        if (index > 0) append("\n")
                        append(buildStyledLine(spans, onPage, surface))
                    }
                }
            }
            Text(tail, style = outputTextStyle())
        }
    }
}

@Composable
private fun OutputWipeLine(
    relativeIndex: Int,
    animate: Boolean,
    spans: List<StyledSpan>,
    surface: Color,
    onCopyLine: (String) -> Unit
) {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) {
            // Capped: the Nth line of a long output must not wait N * stagger.
            val steps = relativeIndex.coerceIn(0, OUTPUT_WIPE_STAGGER_STEPS)
            delay(scaledDelayMs(steps * OUTPUT_WIPE_STAGGER_MS))
            progress.animateTo(1f, tween(OUTPUT_WIPE_MS, easing = OUTPUT_WIPE_EASE))
        }
    }
    val onPage = Azphalt.currentGround.onPage
    val line = remember(spans, onPage, surface) { buildStyledLine(spans, onPage, surface) }
    Text(
        line,
        modifier = Modifier
            .drawWithContent {
                clipRect(right = size.width * progress.value) { this@drawWithContent.drawContent() }
            }
            .graphicsLayer { translationX = -14.dp.toPx() * (1f - progress.value) }
            .clickable(onClickLabel = "Copy this line") { onCopyLine(spans.joinToString("") { it.text }) },
        style = outputTextStyle()
    )
}

@Composable
private fun StderrBlock(text: String, surface: Color) {
    val warn = remember(surface) { statusColorOn(StatusKind.Warning, surface) }
    val type = LocalAzphaltType.current
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(warn.copy(alpha = .5f)))
        Spacer(Modifier.height(6.dp))
        // The word, not just the colour, says this is the error stream.
        Text("STDERR", style = type.eyebrow.copy(color = warn))
        Text(text, style = outputTextStyle().copy(color = warn))
    }
}

/**
 * A capsule for the session strip: a 32dp visible shape with a 48dp hit box ([minTouch]), optional
 * long press, and the semantics a tab or button needs. [isSelected] is non-null for tabs.
 */
@Suppress("LongParameterList")
@Composable
private fun StripCapsule(
    label: String,
    onClick: () -> Unit,
    fill: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    role: Role = Role.Button,
    isSelected: Boolean? = null,
    description: String? = null,
    state: String? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .semantics {
                if (description != null) contentDescription = description
                if (isSelected != null) selected = isSelected
                if (state != null) stateDescription = state
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = role,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onClick = onClick
            )
            .minTouch(minTouchSize()),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .heightIn(min = AzphaltDimens.ChipHeight)
                .clip(AzphaltSurface.capsule)
                .background(fill)
                .indication(interaction, LocalIndication.current)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = textColor,
                style = LocalAzphaltType.current.chip,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp)
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun SessionTabs(
    sessions: List<SessionUiState>,
    activeId: String,
    compact: Boolean,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenFiles: () -> Unit,
    onFilesButtonPositioned: (Rect) -> Unit,
    onPick: (String) -> Unit,
    onNew: () -> Unit,
    onClose: (String) -> Unit,
    onRename: (SessionUiState) -> Unit
) {
    val onPage = Azphalt.currentGround.onPage
    val wash = Azphalt.Ink.copy(alpha = .14f)
    val filesFill = Azphalt.hues[Azphalt.hueOf("/")]
    // Every item is a 32dp capsule in a 48dp hit box, so the strip is the same height whatever it
    // holds: no tab changes height when it becomes active or gains a close button.
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = if (compact) 4.dp else 12.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sessions.forEach { s ->
                key(s.id) {
                    val on = s.id == activeId
                    val badge = s.activity
                    StripCapsule(
                        label = if (badge.glyph.isEmpty()) s.name.uppercase() else "${s.name.uppercase()} ${badge.glyph}",
                        onClick = { onPick(s.id) },
                        fill = if (on) Azphalt.Ink else wash,
                        textColor = if (on) Azphalt.Yellow else onPage,
                        role = Role.Tab,
                        isSelected = on,
                        description = s.name,
                        state = badge.description,
                        onLongClick = { onRename(s) },
                        onLongClickLabel = "Rename session"
                    )
                    if (on && sessions.size > 1) {
                        StripCapsule(
                            label = "×",
                            onClick = { onClose(s.id) },
                            fill = wash,
                            textColor = onPage,
                            description = "Close ${s.name}"
                        )
                    }
                }
            }
            StripCapsule("+", onNew, wash, onPage, description = "New session")
        }
        StripCapsule(
            label = "FILES",
            onClick = onOpenFiles,
            fill = filesFill,
            textColor = filesFill.contrastingText(),
            modifier = Modifier.onGloballyPositioned { onFilesButtonPositioned(it.boundsInRoot()) }
        )
        StripCapsule("⚙", onOpenSettings, wash, onPage, description = "Settings")
        StripCapsule("?", onOpenGuide, wash, onPage, description = "Command guide")
    }
}

/** A small non-interactive tag on the status line (branch, identity, exit status). */
@Composable
private fun StatusTag(text: String, description: String, color: Color) {
    Text(
        text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = LocalAzphaltType.current.pillCap.copy(color = color),
        modifier = Modifier
            .widthIn(max = 160.dp)
            .clip(AzphaltSurface.capsule)
            .background(Azphalt.Ink.copy(alpha = .14f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .semantics { contentDescription = description }
    )
}

/**
 * The working directory (shortened in the middle so the folder you are in stays visible) plus what a
 * prompt framework would show: git branch and whether it is dirty, user@host when known, and a
 * non-zero exit status. All of it comes from [ShellPresentation], refreshed after each command.
 */
@Composable
private fun ShellStatusLine(cwd: String, presentation: ShellPresentation) {
    val ground = Azphalt.currentGround
    val tagSurface = remember(ground.page) { Azphalt.Ink.copy(alpha = .14f).compositeOver(ground.page) }
    val muted = statusColor(StatusKind.Muted)
    val errorColor = remember(tagSurface) { statusColorOn(StatusKind.Error, tagSurface) }
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            cwd,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            style = LocalAzphaltType.current.eyebrow.copy(color = muted, letterSpacing = 0.em),
            modifier = Modifier.weight(1f, fill = false)
        )
        presentation.gitBranch?.let { branch ->
            val dirty = presentation.gitDirty
            StatusTag(
                text = "GIT $branch${if (dirty) " ●" else ""}",
                description = "Git branch $branch, ${if (dirty) "uncommitted changes" else "no changes"}",
                color = ground.onPage
            )
        }
        val identity = listOfNotNull(presentation.user, presentation.host).takeIf { it.size == 2 }?.joinToString("@")
        identity?.let { StatusTag(text = it, description = "Signed in as $it", color = ground.onPage) }
        presentation.lastExitCode?.takeIf { it != 0 }?.let { code ->
            StatusTag(text = "EXIT $code", description = "Last command exited with status $code", color = errorColor)
        }
    }
}

@Composable
private fun RenameField(value: String, onValueChange: (String) -> Unit, onDone: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(AzphaltSurface.capsule)
            .background(Azphalt.White.copy(alpha = .12f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .focusRequester(focus)
            .semantics { contentDescription = "Session name" },
        textStyle = LocalAzphaltType.current.body.copy(color = Azphalt.White, fontSize = 15.sp, lineHeight = 20.sp),
        cursorBrush = SolidColor(Azphalt.Yellow),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() })
    )
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun CommandLine(
    composedPrefix: String = "",
    tokens: List<String>,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    hint: String,
    enabled: Boolean,
    onRun: () -> Unit,
    runLabel: String = "RUN",
    masked: Boolean = false,
    inputRequest: String? = null,
    inputSuppressed: Boolean = false,
    inputFocusRequester: FocusRequester,
    emphasizeHint: Boolean = false,
    compact: Boolean = false
) {
    val type = LocalAzphaltType.current
    val onPage = Azphalt.currentGround.onPage
    Column(Modifier.padding(horizontal = 20.dp).padding(top = if (compact) 6.dp else 16.dp)) {
        Text(
            hint.uppercase(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = type.pillCap.copy(
                color = if (emphasizeHint) onPage else statusColor(StatusKind.Muted),
                fontWeight = if (emphasizeHint) FontWeight.Black else null
            ),
            // The hint line is where "Running…", a program's question and key feedback surface.
            modifier = Modifier.liveRegionPolite()
        )
        if (inputRequest != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "TYPE ${inputRequest.uppercase()} ↓",
                style = type.chip.copy(color = onPage, fontWeight = FontWeight.Black, letterSpacing = 0.08.em)
            )
        }
        Spacer(Modifier.height(if (compact) 4.dp else 9.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(AzphaltSurface.capsule)
                    .background(if (inputRequest != null || inputSuppressed) Azphalt.Ink.copy(alpha = .96f) else Azphalt.Ink)
                    .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("$", style = type.capsule.copy(color = Azphalt.Yellow))
                if (composedPrefix.isNotEmpty()) {
                    Text(
                        composedPrefix.trimEnd(),
                        style = type.body.copy(
                            color = Azphalt.Yellow.copy(alpha = .75f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                tokens.forEach { t ->
                    Box(
                        Modifier.clip(AzphaltSurface.capsule).background(Azphalt.Yellow).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(t.uppercase(), style = type.pillCap.copy(color = Azphalt.Ink))
                    }
                }
                if (inputSuppressed) {
                    Text(
                        "CHOOSE ABOVE",
                        modifier = Modifier.weight(1f),
                        style = type.body.copy(
                            color = Azphalt.Yellow.copy(alpha = .85f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                } else {
                    val fieldLabel = if (masked) "Password" else "Command line"
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = fieldLabel }
                            .focusRequester(inputFocusRequester),
                        textStyle = type.body.copy(
                            color = Azphalt.Yellow,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(Azphalt.Yellow),
                        singleLine = true,
                        visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
                        // Autocorrect/suggestions mangle shell input (and would learn a password into the
                        // keyboard's dictionary); a masked prompt also asks for the password keyboard.
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = if (masked) KeyboardType.Password else KeyboardType.Text,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { onRun() })
                    )
                }
            }
            RunButton(runLabel, enabled, onRun)
        }
    }
}

/** RUN / SEND: a 32dp capsule with the 48dp hit box, so a thumb that misses it does not land in the input. */
@Composable
private fun RunButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val fill = if (enabled) Azphalt.hues[6] else Azphalt.Ink.copy(alpha = .14f)
    val textColor = if (enabled) fill.contrastingText() else Azphalt.currentGround.onPage.copy(alpha = .6f)
    Box(
        Modifier
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .minTouch(minTouchSize()),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .heightIn(min = AzphaltDimens.ChipHeight)
                .clip(AzphaltSurface.capsule)
                .background(fill)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, style = LocalAzphaltType.current.chip.copy(color = textColor))
            Box(Modifier.size(14.dp).clip(AzphaltSurface.capsule).background(Azphalt.caps[6]))
        }
    }
}

private const val COMPLETION_VALUE_PREFIX = "\u0000completion:"

private fun completionCandidateFromPick(picked: List<String>, session: SessionUiState): CompletionCandidate? {
    val encoded = picked.singleOrNull()?.takeIf { it.startsWith(COMPLETION_VALUE_PREFIX) } ?: return null
    val index = encoded.removePrefix(COMPLETION_VALUE_PREFIX).toIntOrNull() ?: return null
    return session.completionCandidates.getOrNull(index)
}

private fun completionNodeFor(session: SessionUiState): MenuNode? {
    if (session.running || session.pendingPrompt != null || session.completionCandidates.isEmpty()) return null
    return MenuNode(
        id = "complete",
        label = "Complete",
        emitsToken = false,
        children = session.completionCandidates.take(50).mapIndexed { index, candidate ->
            MenuNode(
                id = "complete-$index",
                label = candidate.label,
                cap = candidate.kind.name.lowercase(),
                value = COMPLETION_VALUE_PREFIX + index
            )
        }
    )
}

private fun suggestionNodeFor(session: SessionUiState, knownCommands: List<String>): MenuNode? {
    val children = buildList {
        if (session.inputText.isNotBlank()) {
            val historyMatch = ShellAliases.autosuggest(session.inputText, session.commandHistory)
            if (historyMatch != null) {
                add(MenuNode(id = "suggest-tab", label = session.inputText + historyMatch, cap = "TAB"))
            } else if (!session.inputText.contains(' ')) {
                ShellAliases.commandNameCompletions(session.inputText, knownCommands).forEach { name ->
                    add(MenuNode(id = "suggest-cmd-$name", label = name, cap = "TAB"))
                }
            }
        }

        val idle = !session.running && session.inputText.isBlank() && session.tokens.isEmpty()
        if (idle) {
            session.commandHistory.lastOrNull()
                ?.let { ShellAliases.hintForRanCommand(it) }
                ?.let { (key, _) -> add(MenuNode(id = "suggest-alias", label = key, cap = "ALIAS")) }

            session.buffer.lastOrNull()?.let { lastEntry ->
                if (ShellAliases.looksLikeNotFound(lastEntry.output)) {
                    val failedWord = lastEntry.command.substringBefore(' ')
                    val known = session.commandHistory.map { it.substringBefore(' ') }.distinct()
                    ShellAliases.didYouMean(failedWord, known)?.let { fix ->
                        val corrected = fix + lastEntry.command.removePrefix(failedWord)
                        add(MenuNode(id = "suggest-fix", label = corrected, cap = "FIX"))
                    }
                }
            }
        }
    }
    return if (children.isEmpty()) null else MenuNode(
        id = "suggest",
        label = "Suggest",
        children = children,
        emitsToken = false
    )
}

private const val CHAIN_VALUE_PREFIX = "\u0000chain:"

private fun chainOperatorFromPick(picked: List<String>): ChainOperator? {
    val value = picked.singleOrNull()?.takeIf { it.startsWith(CHAIN_VALUE_PREFIX) } ?: return null
    return ChainOperator.entries.firstOrNull { it.name == value.removePrefix(CHAIN_VALUE_PREFIX) }
}

private fun chainNodeFor(session: SessionUiState): MenuNode? {
    val idle = !session.running && session.pendingPrompt == null
    val hasSegment = session.tokens.isNotEmpty() || session.inputText.isNotBlank()
    if (!idle || !hasSegment) return null
    return MenuNode(
        id = "chain",
        label = "Chain",
        emitsToken = false,
        children = ChainOperator.entries.map { op ->
            MenuNode(id = "chain-${op.name}", label = op.symbol, value = CHAIN_VALUE_PREFIX + op.name, cap = op.cap)
        }
    )
}


/** Screen-reader wording for a key on the modifier row; the visible label is just the key name. */
private fun keyDescription(key: String): String = when (key) {
    "ctrl" -> "Control. Arms the next typed letter as a control key"
    "alt" -> "Alt. Arms the next typed character as an alt key"
    "esc" -> "Escape. Cancels the current pick"
    "tab" -> "Tab. Completes the command"
    "↑" -> "Previous command"
    "↓" -> "Next command"
    else -> key
}

@Composable
private fun ModifierKeys(
    armed: ComposerModifier?,
    compact: Boolean,
    keys: List<String> = listOf("ctrl", "alt", "esc", "tab", "↑", "↓"),
    onKeyClick: (String) -> Unit = {}
) {
    val onPage = Azphalt.currentGround.onPage
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = if (compact) 0.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { k ->
            val isToggle = k == "ctrl" || k == "alt"
            val isArmed = (k == "ctrl" && armed == ComposerModifier.Ctrl) || (k == "alt" && armed == ComposerModifier.Alt)
            val spoken = keyDescription(k)
            Box(
                Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = spoken
                        if (isToggle) {
                            selected = isArmed
                            stateDescription = if (isArmed) "armed for the next key" else "off"
                        }
                    }
                    .clickable(role = Role.Button) { onKeyClick(k) }
                    .minTouch(minTouchSize()),
                contentAlignment = Alignment.Center
            ) {
                // An armed key is filled solid ink with yellow text; an idle one is a light wash.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(AzphaltDimens.ChipHeight)
                        .clip(AzphaltSurface.capsule)
                        .background(if (isArmed) Azphalt.Ink else Azphalt.Ink.copy(alpha = .14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        k.uppercase(),
                        maxLines = 1,
                        softWrap = false,
                        style = LocalAzphaltType.current.chip.copy(color = if (isArmed) Azphalt.Yellow else onPage)
                    )
                }
            }
        }
    }
}

/**
 * The small uppercase section label used by the secondary screens. The one shared copy: screens
 * should use this rather than declaring their own.
 */
@Composable
internal fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        color = Azphalt.currentGround.onPage,
        style = LocalAzphaltType.current.eyebrow,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 9.dp)
    )
}
