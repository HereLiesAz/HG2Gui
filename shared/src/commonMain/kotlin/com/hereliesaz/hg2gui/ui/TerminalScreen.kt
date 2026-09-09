package com.hereliesaz.hg2gui.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.hereliesaz.hg2gui.terminal.applyCompletion
import com.hereliesaz.hg2gui.terminal.chainSegment
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import com.hereliesaz.hg2gui.ui.menu.PillMenu
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_BUFFER_ENTRIES = 200

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
    val listState = remember(active.id) { LazyListState() }
    val inputFocusRequester = remember(active.id) { FocusRequester() }
    var selectedEntryId by remember(active.id) { mutableStateOf<Long?>(null) }
    var selectedShowRaw by remember(active.id) { mutableStateOf(false) }
    var requestedInputKind by remember(active.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedEntryId) {
        selectedShowRaw = false
    }

    LaunchedEffect(requestedInputKind) {
        if (requestedInputKind != null) inputFocusRequester.requestFocus()
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

    val newestEntry = active.buffer.lastOrNull()
    LaunchedEffect(
        active.buffer.size,
        newestEntry?.id,
        newestEntry?.output,
        newestEntry?.stderr,
        newestEntry?.styledOutput
    ) {
        if (active.buffer.isNotEmpty()) {
            listState.animateScrollToItem(active.buffer.lastIndex)
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
                session.running = true
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
                            { stderrChunk -> session.updateBufferEntry(entryId) { it.copy(stderr = stderrChunk) } },
                            { styled -> session.updateBufferEntry(entryId) { it.copy(styledOutput = styled) } }
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
                        }
                        session.running = false
                    }
                }
            }
        }
    }

    val yesNoPrompt = active.pendingPrompt?.takeIf { ShellAliases.looksLikeYesNo(it) }
    if (yesNoPrompt != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Confirm") },
            text = {
                Text(
                    yesNoPrompt.substringAfterLast('\n').ifBlank { yesNoPrompt },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        active.tokens = emptyList()
                        active.inputText = ""
                        requestedInputKind = null
                        active.answerPrompt("y")
                    }
                ) {
                    Text("YES")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        active.tokens = emptyList()
                        active.inputText = ""
                        requestedInputKind = null
                        active.answerPrompt("n")
                    }
                ) {
                    Text("NO")
                }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .then(if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
            .imePadding()
    ) {
        SessionTabs(
            sessions = sessions,
            activeId = activeSessionId,
            onOpenSettings = onOpenSettings,
            onOpenGuide = onOpenGuide,
            onOpenFiles = onOpenFiles,
            onFilesButtonPositioned = onFilesButtonPositioned,
            onPick = onSessionPick,
            onNew = onNewSession,
            onClose = onCloseSession
        )

        Text(
            active.cwd,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Azphalt.currentGround.onPage.copy(alpha = .45f),
                fontSize = 11.sp,
                letterSpacing = 0.em
            ),
            modifier = Modifier.padding(start = 20.dp, top = 10.dp)
        )

        active.transientStatus?.takeIf { active.running }?.let { LiveStatusStrip(it) }

        if (active.buffer.isNotEmpty()) {
            Eyebrow("00 — Buffer")
            Column(
                Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(active.buffer, key = { it.id }) { entry ->
                        BufferEntry(
                            entry = entry,
                            selected = selectedEntryId == entry.id,
                            showRaw = selectedEntryId == entry.id && selectedShowRaw,
                            onSelect = {
                                selectedEntryId = if (selectedEntryId == entry.id) null else entry.id
                            },
                            onCopyLine = onCopy,
                            onStop = { onInterrupt(active.id) }
                        )
                    }
                }

                val selectedEntry = active.buffer.firstOrNull { it.id == selectedEntryId }
                if (selectedEntry != null) {
                    BufferActionDock(
                        entry = selectedEntry,
                        showRaw = selectedShowRaw,
                        onCopy = onCopy,
                        onShare = onShare,
                        onRerun = { command ->
                            requestedInputKind = null
                            active.tokens = emptyList()
                            active.inputText = command
                        },
                        onToggleRaw = { selectedShowRaw = !selectedShowRaw }
                    )
                }
            }
        }

        Eyebrow("01 — Command tree")

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

        PillMenu(
            roots = effectiveTree,
            modifier = Modifier.weight(if (active.buffer.isEmpty()) 1f else 0.6f).padding(horizontal = 20.dp, vertical = 12.dp),
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

        val maskInput = pendingPrompt != null && ShellAliases.looksLikePassword(pendingPrompt)
        val choiceRequired = requestedInputKind != null &&
            CompletionSelectionPolicy.shouldReplaceFreeForm(active.inputText, active.completionCandidates)
        val visibleInputRequest = requestedInputKind?.takeIf {
            pendingPrompt == null && active.inputText.isBlank() && !choiceRequired
        }

        CommandLine(
            composedPrefix = active.composedPrefix,
            tokens = active.tokens,
            inputText = active.inputText,
            onInputTextChange = {
                active.inputText = it
                if (it.isNotBlank()) requestedInputKind = null
            },
            hint = when {
                pendingPrompt != null -> pendingPrompt.substringAfterLast('\n').ifBlank { "Waiting for input…" }
                choiceRequired -> "Choose ${requestedInputKind!!.lowercase()} above"
                visibleInputRequest != null -> "Type ${visibleInputRequest.lowercase()} below, then press run"
                active.transientStatus != null -> active.transientStatus!!
                active.running -> "Running…"
                active.tokens.isNotEmpty() || active.inputText.isNotBlank() -> "Ready — press run"
                active.tokens.isEmpty() -> "Pick a category"
                else -> "Pick a command"
            },
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
            onKeyClick = { key ->
                when (key) {
                    "↑" -> {
                        requestedInputKind = null
                        if (active.commandHistory.isNotEmpty()) {
                            val nextIdx = if (active.historyIndex == -1) active.commandHistory.size - 1 else (active.historyIndex - 1).coerceAtLeast(0)
                            active.historyIndex = nextIdx
                            active.inputText = active.commandHistory[nextIdx]
                            active.tokens = emptyList()
                        }
                    }
                    "↓" -> {
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
                    "esc" -> {
                        requestedInputKind = null
                        active.tokens = emptyList()
                        active.inputText = ""
                        active.clearCompletions()
                    }
                    "tab" -> {
                        val first = active.completionCandidates.firstOrNull()
                        if (first != null) {
                            active.inputText = applyCompletion(active.inputText, candidate = first).line
                            active.clearCompletions()
                        } else if (active.inputText.isNotEmpty() && !active.inputText.endsWith(" ")) {
                            active.inputText += " "
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun LiveStatusStrip(status: String) {
    val onPage = Azphalt.currentGround.onPage
    val percent = Regex("""\b(\d{1,3})%""").find(status)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 100)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Azphalt.Ink.copy(alpha = .09f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "LIVE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Azphalt.Yellow,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp
                )
            )
            Spacer(Modifier.width(10.dp))
            Text(
                status,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = onPage,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            )
        }
        if (percent != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink.copy(alpha = .14f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percent / 100f)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Azphalt.Yellow)
                )
            }
        }
    }
}

@Composable
private fun BufferEntry(
    entry: TerminalHistoryEntry,
    selected: Boolean,
    showRaw: Boolean,
    onSelect: () -> Unit,
    onCopyLine: (String) -> Unit,
    onStop: () -> Unit
) {
    val kind = remember(entry.output) { classifyOutput(entry.output) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Azphalt.Ink.copy(alpha = if (selected) .15f else .09f))
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        val onPage = Azphalt.currentGround.onPage
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = onPage.copy(alpha = .5f),
                    fontWeight = FontWeight.Black
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                entry.command,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = onPage,
                    fontWeight = FontWeight.Bold
                )
            )
            StatusDot(entry)
            if (entry.isRunning) {
                Spacer(Modifier.weight(1f))
                BlockActionPill("STOP", onStop)
            }
        }
        if (entry.output.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            ClassifiedOutput(kind, entry, onPage, showRaw, onCopyLine)
        }
        if (entry.stderr.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            StderrBlock(entry.stderr)
        }
    }
}

@Composable
private fun BufferActionDock(
    entry: TerminalHistoryEntry,
    showRaw: Boolean,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onRerun: (String) -> Unit,
    onToggleRaw: () -> Unit
) {
    val copyText = entry.output.ifEmpty { entry.command }
    val kind = remember(entry.output) { classifyOutput(entry.output) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Azphalt.currentGround.pageBrush())
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BlockActionPill("COPY") { onCopy(copyText) }
        BlockActionPill("RE-RUN") { onRerun(entry.command) }
        BlockActionPill("SHARE") { onShare(copyText) }
        ClassificationTogglePill(kind, showRaw, onToggleRaw)
    }
}

@Composable
private fun ClassifiedOutput(
    kind: OutputKind,
    entry: TerminalHistoryEntry,
    onPage: Color,
    showRaw: Boolean,
    onCopyLine: (String) -> Unit
) {
    when {
        kind == OutputKind.BINARY && !showRaw -> Text(
            "Binary output - not displayed as text.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = onPage.copy(alpha = .55f),
                fontStyle = FontStyle.Italic
            )
        )
        kind == OutputKind.ART && !showRaw -> AsciiArtCanvas(entry.output, onPage.copy(alpha = .8f))
        kind == OutputKind.TABLE && !showRaw -> KeyValueTable(entry.output, onPage)
        kind == OutputKind.WIDE_TABLE && !showRaw -> WideTableRecords(entry.output, onPage)
        else -> OutputLines(entry, onCopyLine)
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
    BlockActionPill(if (showRaw) label else "PLAIN TEXT", onToggle)
}

@Composable
private fun StatusDot(entry: TerminalHistoryEntry) {
    if (entry.isRunning) {
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(percent = 50)).background(Azphalt.Yellow))
    } else if (entry.exitCode != null && entry.exitCode != 0) {
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(percent = 50)).background(Azphalt.hues[6]))
    }
}

private val OUTPUT_WIPE_EASE = CubicBezierEasing(0f, .9f, .1f, 1f)
private const val OUTPUT_WIPE_MS = 320
private const val OUTPUT_WIPE_STAGGER_MS = 90L
private const val OUTPUT_WIPE_STAGGER_CAP = 24

internal fun StyledSpan.inkColor(onPage: Color): Color = hue?.let { Azphalt.hues[it] } ?: onPage.copy(alpha = .8f)

internal fun buildStyledLine(spans: List<StyledSpan>, onPage: Color): AnnotatedString = buildAnnotatedString {
    spans.forEach { span ->
        withStyle(SpanStyle(color = span.inkColor(onPage), fontWeight = if (span.bold) FontWeight.Bold else null)) {
            append(span.text)
        }
    }
}

@Composable
private fun OutputLines(entry: TerminalHistoryEntry, onCopyLine: (String) -> Unit) {
    val lines = remember(entry.output, entry.styledOutput) {
        if (entry.styledOutput.isNotEmpty()) entry.styledOutput
        else entry.output.split("\n").map { listOf(StyledSpan(it)) }
    }
    Column {
        lines.take(OUTPUT_WIPE_STAGGER_CAP).forEachIndexed { index, spans ->
            OutputWipeLine(seq = index, spans = spans, onCopyLine = onCopyLine)
        }
        if (lines.size > OUTPUT_WIPE_STAGGER_CAP) {
            val onPage = Azphalt.currentGround.onPage
            Text(
                buildAnnotatedString {
                    lines.drop(OUTPUT_WIPE_STAGGER_CAP).forEachIndexed { index, spans ->
                        if (index > 0) append("\n")
                        append(buildStyledLine(spans, onPage))
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            )
        }
    }
}

@Composable
private fun OutputWipeLine(seq: Int, spans: List<StyledSpan>, onCopyLine: (String) -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((seq % OUTPUT_WIPE_STAGGER_CAP) * OUTPUT_WIPE_STAGGER_MS)
        progress.animateTo(1f, tween(OUTPUT_WIPE_MS, easing = OUTPUT_WIPE_EASE))
    }
    val onPage = Azphalt.currentGround.onPage
    Text(
        buildStyledLine(spans, onPage),
        modifier = Modifier
            .drawWithContent {
                clipRect(right = size.width * progress.value) { this@drawWithContent.drawContent() }
            }
            .graphicsLayer { translationX = -14.dp.toPx() * (1f - progress.value) }
            .clickable { onCopyLine(spans.joinToString("") { it.text }) },
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    )
}

@Composable
private fun StderrBlock(text: String) {
    val warn = Azphalt.hues[4]
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(warn.copy(alpha = .35f)))
        Spacer(Modifier.height(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = warn,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun BlockActionPill(label: String, onClick: () -> Unit) {
    Box(
        Modifier.defaultMinSize(minHeight = 48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(Azphalt.Ink.copy(alpha = .14f))
                .padding(horizontal = 18.dp, vertical = 11.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Azphalt.currentGround.onPage.copy(alpha = .55f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.09.em
                )
            )
        }
    }
}

@Composable
private fun SessionTabs(
    sessions: List<SessionUiState>,
    activeId: String,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenFiles: () -> Unit,
    onFilesButtonPositioned: (Rect) -> Unit,
    onPick: (String) -> Unit,
    onNew: () -> Unit,
    onClose: (String) -> Unit
) {
    val onPage = Azphalt.currentGround.onPage
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sessions.forEach { s ->
                val on = s.id == activeId
                Box(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (on) Azphalt.Ink else Azphalt.Ink.copy(alpha = .14f))
                        .clickable { onPick(s.id) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            s.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (on) Azphalt.Yellow else onPage.copy(alpha = .55f),
                                fontSize = 8.sp
                            )
                        )
                        if (on && sessions.size > 1) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier.clickable { onClose(s.id) }.padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("×", style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.Yellow, fontSize = 10.sp))
                            }
                        }
                    }
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink.copy(alpha = .14f))
                    .clickable(onClick = onNew)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium.copy(color = onPage.copy(alpha = .55f), fontSize = 10.sp))
            }
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(Azphalt.hues[Azphalt.hueOf("/")])
                .onGloballyPositioned { onFilesButtonPositioned(it.boundsInRoot()) }
                .clickable(onClick = onOpenFiles)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text("FILES", style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.White, fontSize = 8.sp, fontWeight = FontWeight.Black))
        }
        Box(
            Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(percent = 50)).background(Azphalt.Ink.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Text("⚙", style = MaterialTheme.typography.titleMedium.copy(color = onPage, fontSize = 12.sp))
            }
        }
        Box(
            Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).clickable(onClick = onOpenGuide),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(percent = 50)).background(Azphalt.Ink.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Text("?", style = MaterialTheme.typography.titleMedium.copy(color = onPage, fontSize = 12.sp))
            }
        }
    }
}

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
    inputFocusRequester: FocusRequester
) {
    Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
        Text(
            hint.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = Azphalt.currentGround.onPage.copy(alpha = .45f),
                fontSize = 9.sp
            )
        )
        if (inputRequest != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "TYPE ${inputRequest.uppercase()} ↓",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Azphalt.currentGround.onPage,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 0.08.em
                )
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (inputRequest != null || inputSuppressed) Azphalt.Ink.copy(alpha = .96f) else Azphalt.Ink)
                    .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("$", style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.Yellow))
                if (composedPrefix.isNotEmpty()) {
                    Text(
                        composedPrefix.trimEnd(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Azphalt.Yellow.copy(alpha = .6f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                tokens.forEach { t ->
                    Box(
                        Modifier.clip(RoundedCornerShape(percent = 50)).background(Azphalt.Yellow).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            t.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.Ink, fontSize = 8.sp, letterSpacing = 0.06.em)
                        )
                    }
                }
                if (inputSuppressed) {
                    Text(
                        "CHOOSE ABOVE",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Azphalt.Yellow.copy(alpha = .7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                } else {
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        modifier = Modifier.weight(1f).focusRequester(inputFocusRequester),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Azphalt.Yellow,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(Azphalt.Yellow),
                        singleLine = true,
                        visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onRun() })
                    )
                }
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (enabled) Azphalt.hues[6] else Azphalt.Ink.copy(alpha = .14f))
                    .clickable(enabled = enabled, onClick = onRun)
                    .padding(start = 16.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    runLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (enabled) Azphalt.White else Azphalt.currentGround.onPage.copy(alpha = .55f),
                        fontSize = 9.sp
                    )
                )
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(percent = 50)).background(Azphalt.caps[6]))
            }
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

@Composable
private fun ModifierKeys(
    keys: List<String> = listOf("ctrl", "alt", "esc", "tab", "↑", "↓"),
    onKeyClick: (String) -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { k ->
            Box(
                Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).clickable { onKeyClick(k) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(percent = 50)).background(Azphalt.Ink.copy(alpha = .14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        k.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.currentGround.onPage, fontSize = 8.sp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 9.dp)
    )
}
