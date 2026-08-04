package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import com.hereliesaz.hg2gui.ui.menu.PillMenu
import kotlinx.coroutines.launch

/*
 * The HG2Gui terminal screen. Not a launcher: sessions, a working directory, modifier keys,
 * and the suggestion tree as the input method. The keyboard is optional, not primary.
 */

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
    onRun: suspend (String) -> String
) {
    var active by remember { mutableStateOf(sessions.first()) }
    var tokens by remember { mutableStateOf(listOf<String>()) }
    var inputText by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableStateOf(-1) }
    var output by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
            if (history.isEmpty() || history.last() != fullLine) {
                history = history + fullLine
            }
            historyIndex = -1
            val lineToRun = fullLine
            tokens = emptyList()
            inputText = ""
            scope.launch {
                output = try {
                    onRun(lineToRun).ifBlank { "(no output)" }
                } catch (e: Exception) {
                    "error: ${e.message}"
                } finally {
                    running = false
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(PageYellow)
            // Fullscreen mode hides the system bars (see TerminalActivity), so content is free
            // to draw under them. Otherwise the app draws edge-to-edge but the bars stay
            // visible — without this, the bottom-anchored command line and quick keys render
            // straight under the navigation bar, unreachable.
            .then(if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
    ) {

        SessionTabs(sessions, active, onOpenSettings) { active = it }

        Text(
            cwd,
            color = Azphalt.Ink.copy(alpha = .45f),
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 20.dp, top = 10.dp)
        )

        Eyebrow("01 — Command tree")

        // The pill tree is the input method, so it gets the middle of the screen; the command
        // line and quick keys anchor to the bottom, under it, where a thumb actually reaches.
        PillMenu(
            roots = tree,
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 12.dp),
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
                output != null -> "Ran · tap the host pill to go back"
                tokens.isNotEmpty() || inputText.isNotBlank() -> "Ready — press run"
                tokens.isEmpty() -> "Pick a category"
                else -> "Pick a command"
            },
            enabled = !running && (tokens.isNotEmpty() || inputText.isNotBlank()),
            onRun = executeCommand
        )

        output?.let { OutputTile(it) { output = null } }

        ModifierKeys(
            onKeyClick = { key ->
                when (key) {
                    "↑" -> {
                        if (history.isNotEmpty()) {
                            val nextIdx = if (historyIndex == -1) history.size - 1 else (historyIndex - 1).coerceAtLeast(0)
                            historyIndex = nextIdx
                            inputText = history[nextIdx]
                            tokens = emptyList()
                        }
                    }
                    "↓" -> {
                        if (historyIndex >= 0) {
                            val nextIdx = historyIndex + 1
                            if (nextIdx < history.size) {
                                historyIndex = nextIdx
                                inputText = history[nextIdx]
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
                        output = null
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
private fun SessionTabs(
    sessions: List<String>,
    active: String,
    onOpenSettings: () -> Unit,
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
                    color = if (on) Azphalt.Yellow else Azphalt.Ink.copy(alpha = .55f),
                    fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
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
        ) { Text("⚙", color = Azphalt.Ink, fontSize = 12.sp, fontWeight = FontWeight.Black) }
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Azphalt.Ink.copy(alpha = .14f)),
            contentAlignment = Alignment.Center
        ) { Text("+", color = Azphalt.Ink, fontSize = 12.sp, fontWeight = FontWeight.Black) }
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
            color = Azphalt.Ink.copy(alpha = .45f),
            fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em
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
                Text("$", color = Azphalt.Yellow, fontSize = 15.sp, fontWeight = FontWeight.Black)
                tokens.forEach { t ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Azphalt.Yellow)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            t.uppercase(), color = Azphalt.Ink,
                            fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em
                        )
                    }
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = Azphalt.Yellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
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
                Text("RUN", color = Azphalt.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em)
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(percent = 50)).background(Azphalt.caps[6]))
            }
        }
    }
}

/**
 * One command, one record. Output is capped in height and scrolls internally so a long `ls`
 * cannot push the command tree off the screen — the tree is the input method and must stay
 * reachable.
 */
@Composable
private fun OutputTile(text: String, onDismiss: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 20.dp, vertical = 9.dp)
            .fillMaxWidth()
            .heightIn(max = 190.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Azphalt.Ink.copy(alpha = .09f))
            .clickable(onClick = onDismiss)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(text, color = Azphalt.Ink.copy(alpha = .78f), fontSize = 12.sp, lineHeight = 19.sp)
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
                    k.uppercase(), color = Azphalt.Ink,
                    fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
        }
    }
}

@Composable
internal fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        color = Azphalt.Ink.copy(alpha = .55f),
        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.28.em,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 9.dp)
    )
}

