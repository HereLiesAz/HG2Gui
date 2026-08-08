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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.managers.TerminalHistoryEntry
import com.hereliesaz.hg2gui.terminal.ShellAliases
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import com.hereliesaz.hg2gui.ui.menu.PillMenu
import kotlinx.coroutines.launch

private val PageYellow = Brush.linearGradient(
    0f to Color(0xFFE8C81E), 0.5f to Color(0xFFD9B615), 1f to Color(0xFFE8C81E)
)

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
    onRun: suspend (sessionId: String, line: String, onOutput: (String) -> Unit) -> Unit
) {
    val active = sessions.first { it.id == activeSessionId }
    val scope = rememberCoroutineScope()
    val listState = remember(active.id) { LazyListState() }

    val executeCommand = {
        val session = active
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
                session.commandHistory = session.commandHistory + fullLine
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
                    onRun(session.id, execLine) { outputChunk ->
                        // Update the buffer with the streaming output
                        session.buffer = session.buffer.mapIndexed { index, entry ->
                            if (index == entryId) {
                                entry.copy(output = outputChunk)
                            } else {
                                entry
                            }
                        }
                    }
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
                    session.running = false
                }

                if (session.buffer.isNotEmpty()) {
                    listState.animateScrollToItem(session.buffer.size - 1)
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(PageYellow)
            .then(if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
    ) {

        SessionTabs(
            sessions = sessions,
            activeId = activeSessionId,
            onOpenSettings = onOpenSettings,
            onOpenGuide = onOpenGuide,
            onOpenFiles = onOpenFiles,
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
                    BufferEntry(entry)
                }
            }
        }

        Eyebrow("01 — Command tree")

        // The suggestion host, when it has anything to offer, rides along as just one more
        // root in the same stack every other command lives in - not a second PillMenu next to
        // it. It lands last, so it fans out from the row closest to the command line.
        val suggestionNode = suggestionNodeFor(active)
        val effectiveTree = if (suggestionNode != null) tree + suggestionNode else tree

        PillMenu(
            roots = effectiveTree,
            modifier = Modifier.weight(if (active.buffer.isEmpty()) 1f else 0.6f).padding(horizontal = 20.dp, vertical = 12.dp),
            onRun = { picked, isTerminal ->
                active.tokens = picked
                active.inputText = ""
                // A pick that just fully resolved every parameter a command needs runs right
                // away instead of waiting for a separate tap on RUN.
                if (isTerminal) executeCommand()
            }
        )

        CommandLine(
            tokens = active.tokens,
            inputText = active.inputText,
            onInputTextChange = { active.inputText = it },
            hint = when {
                active.running -> "Running…"
                active.tokens.isNotEmpty() || active.inputText.isNotBlank() -> "Ready — press run"
                active.tokens.isEmpty() -> "Pick a category"
                else -> "Pick a command"
            },
            enabled = !active.running && (active.tokens.isNotEmpty() || active.inputText.isNotBlank()),
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
private fun BufferEntry(entry: TerminalHistoryEntry) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Azphalt.Ink.copy(alpha = .05f))
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
}

@Composable
private fun SessionTabs(
    sessions: List<SessionUiState>,
    activeId: String,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenFiles: () -> Unit,
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
                .size(22.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Azphalt.Ink.copy(alpha = .14f))
                .clickable(onClick = onOpenFiles),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "F",
                style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.Ink, fontSize = 11.sp, fontWeight = FontWeight.Black)
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
    onRun: () -> Unit
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
                    "RUN", 
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
    return if (children.isEmpty()) null else MenuNode(id = "suggest", label = "Suggest", children = children)
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
