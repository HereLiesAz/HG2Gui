package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
    sessions: List<String> = listOf("main", "tuixt", "rss"),
    cwd: String,
    fullscreen: Boolean,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onRun: suspend (String, (String) -> Unit) -> Unit
) {
    var active by remember { mutableStateOf(sessions.first()) }
    var tokens by remember { mutableStateOf(listOf<String>()) }
    var inputText by remember { mutableStateOf("") }
    
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableStateOf(-1) }
    
    var buffer by remember { mutableStateOf(listOf<TerminalHistoryEntry>()) }
    
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val executeCommand = {
        val fullLine = buildString {
            if (tokens.isNotEmpty()) append(tokens.joinToString(" "))
            if (inputText.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(inputText.trim())
            }
        }.trim()

        if (fullLine.isNotEmpty() && !running) {
            running = true
            if (commandHistory.isEmpty() || commandHistory.last() != fullLine) {
                commandHistory = commandHistory + fullLine
            }
            historyIndex = -1
            val lineToRun = fullLine
            tokens = emptyList()
            inputText = ""
            
            // Add initial entry
            val entryId = buffer.size
            buffer = buffer + TerminalHistoryEntry(command = lineToRun, isRunning = true)
            
            scope.launch {
                try {
                    onRun(lineToRun) { outputChunk ->
                        // Update the buffer with the streaming output
                        buffer = buffer.mapIndexed { index, entry ->
                            if (index == entryId) {
                                entry.copy(output = outputChunk)
                            } else {
                                entry
                            }
                        }
                    }
                } catch (e: Exception) {
                    buffer = buffer.mapIndexed { index, entry ->
                        if (index == entryId) {
                            entry.copy(output = entry.output + "\nerror: ${e.message}")
                        } else {
                            entry
                        }
                    }
                } finally {
                    buffer = buffer.mapIndexed { index, entry ->
                        if (index == entryId) entry.copy(isRunning = false) else entry
                    }
                    running = false
                }
                
                if (buffer.isNotEmpty()) {
                    listState.animateScrollToItem(buffer.size - 1)
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

        SessionTabs(sessions, active, onOpenSettings, onOpenGuide) { active = it }

        Text(
            cwd,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Azphalt.Ink.copy(alpha = .45f),
                fontSize = 11.sp,
                letterSpacing = 0.em
            ),
            modifier = Modifier.padding(start = 20.dp, top = 10.dp)
        )

        if (buffer.isNotEmpty()) {
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
                items(buffer) { entry ->
                    BufferEntry(entry)
                }
            }
        }

        Eyebrow("01 — Command tree")

        PillMenu(
            roots = tree,
            modifier = Modifier.weight(if (buffer.isEmpty()) 1f else 0.6f).padding(horizontal = 20.dp, vertical = 12.dp),
            onRun = {
                tokens = it
                inputText = ""
            }
        )

        CommandLine(
            tokens = tokens,
            inputText = inputText,
            onInputTextChange = { inputText = it },
            hint = when {
                running -> "Running…"
                tokens.isNotEmpty() || inputText.isNotBlank() -> "Ready — press run"
                tokens.isEmpty() -> "Pick a category"
                else -> "Pick a command"
            },
            enabled = !running && (tokens.isNotEmpty() || inputText.isNotBlank()),
            onRun = executeCommand
        )

        ModifierKeys(
            onKeyClick = { key ->
                when (key) {
                    "↑" -> {
                        if (commandHistory.isNotEmpty()) {
                            val nextIdx = if (historyIndex == -1) commandHistory.size - 1 else (historyIndex - 1).coerceAtLeast(0)
                            historyIndex = nextIdx
                            inputText = commandHistory[nextIdx]
                            tokens = emptyList()
                        }
                    }
                    "↓" -> {
                        if (historyIndex >= 0) {
                            val nextIdx = historyIndex + 1
                            if (nextIdx < commandHistory.size) {
                                historyIndex = nextIdx
                                inputText = commandHistory[nextIdx]
                            } else {
                                historyIndex = -1
                                inputText = ""
                            }
                            tokens = emptyList()
                        }
                    }
                    "esc" -> {
                        tokens = emptyList()
                        inputText = ""
                    }
                    "tab" -> {
                        if (inputText.isNotEmpty() && !inputText.endsWith(" ")) {
                            inputText += " "
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
    sessions: List<String>,
    active: String,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onPick: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sessions.forEach { s ->
            val on = s == active
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (on) Azphalt.Ink else Azphalt.Ink.copy(alpha = .14f))
                    .clickable { onPick(s) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    s.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (on) Azphalt.Yellow else Azphalt.Ink.copy(alpha = .55f),
                        fontSize = 8.sp
                    )
                )
            }
        }
        Spacer(Modifier.weight(1f))
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
