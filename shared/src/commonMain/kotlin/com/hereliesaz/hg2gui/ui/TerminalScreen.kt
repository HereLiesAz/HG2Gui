package com.hereliesaz.hg2gui.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.managers.TerminalHistoryEntry
import com.hereliesaz.hg2gui.terminal.ShellAliases
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import com.hereliesaz.hg2gui.ui.menu.PillMenu
import kotlinx.coroutines.launch

// SH-5: each entry's own VT100 scrollback is already capped independently - this bounds the
// outer list of commands itself, which used to grow without limit for the life of a session.
private const val MAX_BUFFER_ENTRIES = 200

@Composable
fun TerminalScreen(
    tree: List<MenuNode>,
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
    onRun: suspend (
        sessionId: String,
        line: String,
        onOutput: (String) -> Unit,
        onNeedInput: suspend (prompt: String) -> String
    ) -> Unit
) {
    val active = sessions.first { it.id == activeSessionId }
    val scope = rememberCoroutineScope()
    val listState = remember(active.id) { LazyListState() }

    val executeCommand = {
        val session = active
        val pendingPrompt = session.pendingPrompt
        if (pendingPrompt != null) {
            // RUN doubles as SEND while a command is stalled waiting on us - the answer is
            // whatever's built up exactly like a normal command line would be, just handed to
            // the running process instead of starting a new one.
            val answer = buildString {
                if (session.tokens.isNotEmpty()) append(session.tokens.joinToString(" "))
                if (session.inputText.isNotBlank()) {
                    if (isNotEmpty()) append(" ")
                    append(session.inputText.trim())
                }
            }.trim()
            session.tokens = emptyList()
            session.inputText = ""
            session.answerPrompt(answer)
        } else {
            val fullLine = buildString {
                if (session.tokens.isNotEmpty()) append(session.tokens.joinToString(" "))
                if (session.inputText.isNotBlank()) {
                    if (isNotEmpty()) append(" ")
                    append(session.inputText.trim())
                }
            }.trim()

            if (fullLine.isNotEmpty() && !session.running) {
                session.running = true
                if (session.commandHistory.isEmpty() || session.commandHistory.last() != fullLine) {
                    session.commandHistory = (session.commandHistory + fullLine).takeLast(MAX_BUFFER_ENTRIES)
                }
                session.historyIndex = -1
                val lineToRun = fullLine
                // Aliases are expanded only for what actually reaches the shell - hintForRanCommand
                // needs the line the user actually typed, unexpanded, to know whether they already
                // used the shortcut.
                val execLine = ShellAliases.expand(lineToRun)
                session.tokens = emptyList()
                session.inputText = ""

                // Add initial entry
                val entryId = session.buffer.size
                session.buffer = session.buffer + TerminalHistoryEntry(command = lineToRun, isRunning = true)

                scope.launch {
                    try {
                        onRun(
                            session.id,
                            execLine,
                            { outputChunk ->
                                // Update the buffer with the streaming output
                                session.buffer = session.buffer.mapIndexed { index, entry ->
                                    if (index == entryId) {
                                        entry.copy(output = outputChunk)
                                    } else {
                                        entry
                                    }
                                }
                            },
                            { prompt -> session.awaitPromptAnswer(prompt) }
                        )
                    } catch (e: Exception) {
                        session.buffer = session.buffer.mapIndexed { index, entry ->
                            if (index == entryId) {
                                entry.copy(output = entry.output + "\nerror: ${e.message}")
                            } else {
                                entry
                            }
                        }
                    } finally {
                        session.buffer = session.buffer.mapIndexed { index, entry ->
                            if (index == entryId) entry.copy(isRunning = false) else entry
                        }
                        // SH-5: each entry's own VT100 scrollback is already capped, but nothing
                        // ever trimmed the *outer* list of commands itself - a long session just
                        // kept growing it forever. Only safe to trim here, once this entry is no
                        // longer being updated by its own entryId - trimming mid-run would shift
                        // every index the streaming/error/finally branches above still target.
                        if (session.buffer.size > MAX_BUFFER_ENTRIES) {
                            session.buffer = session.buffer.takeLast(MAX_BUFFER_ENTRIES)
                        }
                        session.running = false
                    }

                    if (session.buffer.isNotEmpty()) {
                        listState.animateScrollToItem(session.buffer.size - 1)
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .then(if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
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
                color = Azphalt.Ink.copy(alpha = .45f),
                fontSize = 11.sp,
                letterSpacing = 0.em
            ),
            modifier = Modifier.padding(start = 20.dp, top = 10.dp)
        )

        if (active.buffer.isNotEmpty()) {
            Eyebrow("00 — Buffer")
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(active.buffer) { entry ->
                    BufferEntry(
                        entry = entry,
                        onCopy = onCopy,
                        onShare = onShare,
                        onRerun = { command ->
                            active.tokens = emptyList()
                            active.inputText = command
                        }
                    )
                }
            }
        }

        Eyebrow("01 — Command tree")

        // A stalled command shaped like a yes/no question gets a dedicated Answer stack so the
        // reply is a tap, not typed text - same stack, same mechanism as everything else: YES
        // and NO are ordinary terminal leaves, so picking one auto-runs (here, auto-sends) via
        // the exact same isTerminal path a normal command completes through.
        val pendingPrompt = active.pendingPrompt
        val answerNode = if (pendingPrompt != null && ShellAliases.looksLikeYesNo(pendingPrompt)) {
            MenuNode(
                id = "answer",
                label = "Answer",
                emitsToken = false,
                children = listOf(
                    MenuNode(id = "answer-yes", label = "YES", value = "y"),
                    MenuNode(id = "answer-no", label = "NO", value = "n")
                )
            )
        } else null

        // The suggestion host, when it has anything to offer, rides along as just one more
        // root in the same stack every other command lives in - not a second PillMenu next to
        // it. Whichever of these lands last fans out from the row closest to the command line -
        // a pending answer takes that spot over a suggestion, since it's the more urgent one.
        val suggestionNode = suggestionNodeFor(active)
        val effectiveTree = tree + listOfNotNull(suggestionNode, answerNode)

        PillMenu(
            roots = effectiveTree,
            modifier = Modifier.weight(if (active.buffer.isEmpty()) 1f else 0.6f).padding(horizontal = 20.dp, vertical = 12.dp),
            onRun = { picked, isTerminal ->
                active.tokens = picked
                active.inputText = ""
                // A pick that just fully resolved every parameter a command needs runs right
                // away instead of waiting for a separate tap on RUN - or, if a prompt is
                // pending, sends the pick as that prompt's answer the same way.
                if (isTerminal) executeCommand()
            },
            onWizard = onWizard,
            onCrumbPositioned = onCrumbPositioned
        )

        // A password/passphrase prompt (ssh, sudo, su - anything ShellSession's own idle-gap
        // detector catches) masks the free-text answer field the same way any password field
        // would; a yes/no prompt never reaches here as text at all, it gets the Answer pill
        // stack above instead, so no need to exclude it explicitly.
        val maskInput = pendingPrompt != null && ShellAliases.looksLikePassword(pendingPrompt)

        CommandLine(
            tokens = active.tokens,
            inputText = active.inputText,
            onInputTextChange = { active.inputText = it },
            hint = when {
                pendingPrompt != null -> pendingPrompt.substringAfterLast('\n').ifBlank { "Waiting for input…" }
                active.running -> "Running…"
                active.tokens.isNotEmpty() || active.inputText.isNotBlank() -> "Ready — press run"
                active.tokens.isEmpty() -> "Pick a category"
                else -> "Pick a command"
            },
            runLabel = if (pendingPrompt != null) "SEND" else "RUN",
            enabled = pendingPrompt != null || (!active.running && (active.tokens.isNotEmpty() || active.inputText.isNotBlank())),
            masked = maskInput,
            onRun = executeCommand
        )

        ModifierKeys(
            onKeyClick = { key ->
                when (key) {
                    "↑" -> {
                        if (active.commandHistory.isNotEmpty()) {
                            val nextIdx = if (active.historyIndex == -1) active.commandHistory.size - 1 else (active.historyIndex - 1).coerceAtLeast(0)
                            active.historyIndex = nextIdx
                            active.inputText = active.commandHistory[nextIdx]
                            active.tokens = emptyList()
                        }
                    }
                    "↓" -> {
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
                        active.tokens = emptyList()
                        active.inputText = ""
                    }
                    "tab" -> {
                        if (active.inputText.isNotEmpty() && !active.inputText.endsWith(" ")) {
                            active.inputText += " "
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun BufferEntry(
    entry: TerminalHistoryEntry,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onRerun: (String) -> Unit
) {
    // Blocks: tap an entry to reveal COPY/RE-RUN/SHARE - the tap-to-reveal idiom already used
    // for the MCP pairing token. Re-run only ever populates the input line for review, never
    // fires the command itself - same "assemble, then let the user press Run" rule every
    // wizard-produced command already follows.
    var expanded by remember { mutableStateOf(false) }
    val isArt = remember(entry.output) { looksLikeAsciiArt(entry.output) }
    // Checked only when the output isn't already art - a block of `label: value` lines and a
    // dense symbol-art block are mutually exclusive readings of the same text.
    val isTable = remember(entry.output) { !isArt && looksLikeKeyValueTable(entry.output) }
    // Keyed on the command, not the output - entry.output mutates on every streamed chunk while
    // a command is still running, and re-keying on it reset this toggle out from under anyone
    // reading the raw text of a long-running command.
    var showRaw by remember(entry.command) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            // "Radii are 999px for anything pressable, 26px for a record tile" - DESIGN.md's own
            // literal figures for this exact element.
            .clip(RoundedCornerShape(26.dp))
            .background(Azphalt.Ink.copy(alpha = .09f))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Azphalt.Ink.copy(alpha = .5f),
                    fontWeight = FontWeight.Black
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                entry.command,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Azphalt.Ink,
                    fontWeight = FontWeight.Bold
                )
            )
            if (entry.isRunning) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(percent = 50)).background(Azphalt.Yellow))
            }
        }
        if (entry.output.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            if (isArt && !showRaw) {
                // Vector-style rendering: flat filled cells sized by character density, not
                // literal glyphs - a script's ASCII/box-drawing art reads as art, not text.
                AsciiArtCanvas(entry.output, Azphalt.Ink.copy(alpha = .8f))
            } else if (isTable && !showRaw) {
                // "Output is set, not echoed": a block of label: value lines is set on the page
                // as a two-column grid with hairline rules, not left as raw monospace text.
                KeyValueTable(entry.output, Azphalt.Ink)
            } else {
                Text(
                    entry.output,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Azphalt.Ink.copy(alpha = .8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
            }
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val copyText = entry.output.ifEmpty { entry.command }
                BlockActionPill("COPY") { onCopy(copyText) }
                BlockActionPill("RE-RUN") { onRerun(entry.command) }
                BlockActionPill("SHARE") { onShare(copyText) }
                if (isArt) {
                    BlockActionPill(if (showRaw) "ART" else "PLAIN TEXT") { showRaw = !showRaw }
                } else if (isTable) {
                    BlockActionPill(if (showRaw) "READING" else "PLAIN TEXT") { showRaw = !showRaw }
                }
            }
        }
    }
}

@Composable
private fun BlockActionPill(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Azphalt.Ink.copy(alpha = .14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Azphalt.Ink,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black
            )
        )
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
                                color = if (on) Azphalt.Yellow else Azphalt.Ink.copy(alpha = .55f),
                                fontSize = 8.sp
                            )
                        )
                        if (on && sessions.size > 1) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "×",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Azphalt.Yellow,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.clickable { onClose(s.id) }
                            )
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
                Text(
                    "+",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Azphalt.Ink.copy(alpha = .55f),
                        fontSize = 10.sp
                    )
                )
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
            Text(
                "FILES",
                style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
            )
        }
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Azphalt.Ink.copy(alpha = .14f))
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "⚙",
                style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.Ink, fontSize = 12.sp)
            )
        }
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Azphalt.Ink.copy(alpha = .14f))
                .clickable(onClick = onOpenGuide),
            contentAlignment = Alignment.Center
        ) { 
            Text(
                "?", 
                style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.Ink, fontSize = 12.sp)
            ) 
        }
    }
}

@Composable
private fun CommandLine(
    tokens: List<String>,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    hint: String,
    enabled: Boolean,
    onRun: () -> Unit,
    runLabel: String = "RUN",
    masked: Boolean = false
) {
    Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
        Text(
            hint.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = Azphalt.Ink.copy(alpha = .45f),
                fontSize = 9.sp
            )
        )
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "$", 
                    style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.Yellow)
                )
                tokens.forEach { t ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Azphalt.Yellow)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            t.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Azphalt.Ink,
                                fontSize = 8.sp,
                                letterSpacing = 0.06.em
                            )
                        )
                    }
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier.weight(1f),
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
            Row(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (enabled) Azphalt.hues[6] else Azphalt.hues[6].copy(alpha = .4f))
                    .clickable(enabled = enabled, onClick = onRun)
                    .padding(start = 16.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    runLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Azphalt.White,
                        fontSize = 9.sp
                    )
                )
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(percent = 50)).background(Azphalt.caps[6]))
            }
        }
    }
}

/*
 * The Kotlin-native stand-ins for what a live zsh line editor would offer - autosuggestion,
 * "did you mean", alias hints - built as an ordinary MenuNode host+children, so they render
 * through the exact same PillMenu stack every other command uses, instead of a bespoke row of
 * pills. Each leaf's label is the literal input text a tap should adopt, matching the
 * convention the real command tree already uses for its own pills. There's no live PTY for a
 * real shell line editor to attach to - ShellSession only ever sends one complete line at a
 * time and reads a complete result back - so this is the delivery mechanism instead.
 */
private fun suggestionNodeFor(session: SessionUiState): MenuNode? {
    val children = buildList {
        if (session.inputText.isNotBlank()) {
            ShellAliases.autosuggest(session.inputText, session.commandHistory)?.let { rest ->
                add(MenuNode(id = "suggest-tab", label = session.inputText + rest, cap = "TAB"))
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
                Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink.copy(alpha = .14f))
                    .clickable { onKeyClick(k) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    k.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Azphalt.Ink,
                        fontSize = 8.sp
                    )
                )
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
