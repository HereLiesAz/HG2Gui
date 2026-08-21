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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
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
import com.hereliesaz.hg2gui.terminal.ShellAliases
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import com.hereliesaz.hg2gui.ui.menu.PillMenu
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// SH-5: each entry's own VT100 scrollback is already capped independently - this bounds the
// outer list of commands itself, which used to grow without limit for the life of a session.
private const val MAX_BUFFER_ENTRIES = 200

// Every in-flight update to one running entry - a streamed output/stderr/styled chunk, the exit
// code once it lands - is the same "find it by id, replace it" shape; this is that shape, once.
// Matched by entry.id rather than buffer position: only one command runs per session at a time
// today, so a position-based match was never actually wrong in practice, but matching by identity
// instead means these updates keep finding the right entry even if that stops being true, and
// don't rely on the buffer's own trim (see MAX_BUFFER_ENTRIES below) never running mid-update.
private fun SessionUiState.updateBufferEntry(entryId: Long, transform: (TerminalHistoryEntry) -> TerminalHistoryEntry) {
    buffer = buffer.map { entry -> if (entry.id == entryId) transform(entry) else entry }
}

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
        onNeedInput: suspend (prompt: String) -> String,
        onExit: (Int?) -> Unit,
        onStderr: (String) -> Unit,
        onStyledOutput: (List<List<StyledSpan>>) -> Unit
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
                val newEntry = TerminalHistoryEntry(command = lineToRun, isRunning = true)
                val entryId = newEntry.id
                session.buffer = session.buffer + newEntry

                scope.launch {
                    // D2: null until the command actually exits (or forever, for the
                    // bootstrap/Builtins branches - neither is a real shell command with an exit
                    // status of its own) - captured here rather than read straight off onRun's
                    // own return value, since a CancellationException below skips past that.
                    var exitCode: Int? = null
                    try {
                        onRun(
                            session.id,
                            execLine,
                            { outputChunk -> session.updateBufferEntry(entryId) { it.copy(output = outputChunk) } },
                            { prompt -> session.awaitPromptAnswer(prompt) },
                            { code -> exitCode = code },
                            { stderrChunk -> session.updateBufferEntry(entryId) { it.copy(stderr = stderrChunk) } },
                            { styled -> session.updateBufferEntry(entryId) { it.copy(styledOutput = styled) } }
                        )
                    } catch (e: CancellationException) {
                        // Composition teardown (e.g. navigating away to Settings/Guide/Files
                        // while a command is still running) cancels this scope, which surfaces
                        // here as a CancellationException - not a real command failure. Nothing
                        // was wrong, the screen just went away, so don't write a fake "error:"
                        // line into a buffer nobody but the next visit will see; rethrow so
                        // structured concurrency still sees the cancellation.
                        throw e
                    } catch (e: Exception) {
                        // A thrown exception never reaches onExit, so exitCode (above) would
                        // otherwise stay null forever - identical to a clean success as far as
                        // StatusDot (which only special-cases isRunning and a non-null non-zero
                        // exitCode) is concerned, leaving a real failure showing no failure signal
                        // at all beyond text buried in the (possibly collapsed) output block.
                        exitCode = -1
                        session.updateBufferEntry(entryId) { it.copy(output = it.output + "\nerror: ${e.message}") }
                    } finally {
                        session.updateBufferEntry(entryId) { it.copy(isRunning = false, exitCode = exitCode) }
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
            // The command line and modifier keys are pinned at the bottom, below the weighted
            // PillMenu/buffer above them - with no IME inset, the keyboard just overlaid the
            // screen on top of them instead of the layout making room. imePadding() shrinks this
            // Column's own height by the keyboard's, so the weighted content above eats the
            // difference and the input row stays above the keyboard rather than under it.
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
                items(active.buffer, key = { it.id }) { entry ->
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

        // A stalled command shaped like a question with a known, enumerable set of answers gets
        // a dedicated Answer stack so the reply is a tap, not typed text - each option is an
        // ordinary terminal leaf, so picking one auto-runs (here, auto-sends) via the exact same
        // isTerminal path a normal command completes through. Checked in order of how much
        // structure each shape actually offers: a numbered `select`-style menu names every
        // option, so it wins over a same-prompt bracketed list; a plain y/n question gets its
        // own clearer YES/NO pair before falling back to a generic bracketed/lettered list
        // (dpkg conffile prompts, git's interactive add, ...).
        val pendingPrompt = active.pendingPrompt
        val answerNode = when {
            pendingPrompt == null -> null
            else -> ShellAliases.numberedMenuChoices(pendingPrompt)?.let { choices ->
                MenuNode(
                    id = "answer",
                    label = "Answer",
                    emitsToken = false,
                    children = choices.map { (number, label) ->
                        MenuNode(id = "answer-$number", label = "$number) $label", value = number)
                    }
                )
            } ?: if (ShellAliases.looksLikeYesNo(pendingPrompt)) {
                MenuNode(
                    id = "answer",
                    label = "Answer",
                    emitsToken = false,
                    children = listOf(
                        MenuNode(id = "answer-yes", label = "YES", value = "y"),
                        MenuNode(id = "answer-no", label = "NO", value = "n")
                    )
                )
            } else {
                ShellAliases.bracketedChoices(pendingPrompt)?.let { choices ->
                    MenuNode(
                        id = "answer",
                        label = "Answer",
                        emitsToken = false,
                        children = choices.map { choice -> MenuNode(id = "answer-$choice", label = choice, value = choice) }
                    )
                }
            }
        }

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
    // Keyed on entry.id, not left unkeyed - items(active.buffer) is now itself keyed on entry.id
    // (see its own call site), but that alone only protects which *entry* this composable sees;
    // without also keying this remember, a trim that shifts a surviving entry into a
    // previously-different slot's composable instance would still hand it that instance's own
    // already-remembered `expanded` value from whatever command used to occupy it.
    var expanded by remember(entry.id) { mutableStateOf(false) }
    // entry.output never carries raw ANSI/VT100 escapes to strip here: real shell output is
    // always pre-flattened through ShellSession's headless TerminalEmulator before it reaches the
    // buffer, and the bootstrap/Builtins branches only ever emit app-authored plain text.
    val kind = remember(entry.output) { classifyOutput(entry.output) }
    // Keyed on entry.id: entry.output mutates on every streamed chunk while a command is still
    // running (ruling out entry.output as a key), and entry.command alone let two runs of the
    // exact same command text leak this toggle's state between them - two distinct entries sharing
    // one key is exactly what entry.id exists to prevent.
    var showRaw by remember(entry.id) { mutableStateOf(false) }
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
        }
        if (entry.output.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            ClassifiedOutput(kind, entry, onPage, showRaw)
        }
        if (entry.stderr.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            StderrBlock(entry.stderr)
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val copyText = entry.output.ifEmpty { entry.command }
                BlockActionPill("COPY") { onCopy(copyText) }
                BlockActionPill("RE-RUN") { onRerun(entry.command) }
                BlockActionPill("SHARE") { onShare(copyText) }
                ClassificationTogglePill(kind, showRaw) { showRaw = !showRaw }
            }
        }
    }
}

// Split out of BufferEntry so its own dispatch doesn't count against that composable's
// complexity budget - see [OutputKind]'s own doc comment for why this is one classification
// rather than a chain of independent booleans.
@Composable
private fun ClassifiedOutput(kind: OutputKind, entry: TerminalHistoryEntry, onPage: Color, showRaw: Boolean) {
    when {
        kind == OutputKind.BINARY && !showRaw -> {
            // D6: "refused with an explanation rather than rendered" - garbled bytes typeset as
            // if they were prose or a table is worse than admitting there's nothing readable
            // here; showRaw is still one tap away for anyone who wants it anyway.
            Text(
                "Binary output - not displayed as text.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = onPage.copy(alpha = .55f),
                    fontStyle = FontStyle.Italic
                )
            )
        }
        kind == OutputKind.ART && !showRaw -> {
            // Vector-style rendering: flat filled cells sized by character density, not literal
            // glyphs - a script's ASCII/box-drawing art reads as art, not text.
            AsciiArtCanvas(entry.output, onPage.copy(alpha = .8f))
        }
        kind == OutputKind.TABLE && !showRaw -> {
            // "Output is set, not echoed": a block of label: value lines is set on the page as a
            // two-column grid with hairline rules, not left as raw monospace text.
            KeyValueTable(entry.output, onPage)
        }
        kind == OutputKind.WIDE_TABLE && !showRaw -> {
            // D6: a record per row, fields as labelled pairs - "suits this design better than a
            // table anyway" per the audit's own recommendation, instead of either wrapping a wide
            // row into unreadable ribbons or clipping it at the screen edge.
            WideTableRecords(entry.output, onPage)
        }
        else -> {
            // "Output sets, not echoes" (RATTLE 5G / OUTPUT SETS): raw output reveals line by
            // line via a clip-wipe + slight slide, the same idiom GuideReaderScreen's WipeItem
            // uses for its own reveals, rather than the whole block appearing instantly.
            OutputLines(entry)
        }
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
        // D2: success and failure used to render identically - no exit code reached the UI at
        // all. "04 - Semantics" is explicit that a non-zero exit is red, the same hue Run/Primary
        // already uses, rather than a separate error colour.
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(percent = 50)).background(Azphalt.hues[6]))
    }
}

// Line-by-line duration/stagger for the output "set" beat - the motion sheet's own "Output sets"
// tile calls for 360ms a line, 90ms apart (docs/HG2Gui Motion Sheet.dc.html); 320ms here runs
// somewhat faster than that exact figure, using the one house easing curve everything else in
// the app already animates with.
private val OUTPUT_WIPE_EASE = CubicBezierEasing(0f, .9f, .1f, 1f)
private const val OUTPUT_WIPE_MS = 320
private const val OUTPUT_WIPE_STAGGER_MS = 90L

// PillMenu's own per-row stagger (UNFOLD_STAGGER_MS) has no such cap - a category with enough
// rows delays its last one proportionally, uncapped. This is the fix that one doesn't have: only
// the first STAGGER_CAP lines get the staggered wipe treatment, the rest of a very long output
// just appears immediately rather than making the reader wait out dozens of staggered reveals.
private const val OUTPUT_WIPE_STAGGER_CAP = 24

// D1: an unstyled span's color - exactly what every line rendered as before ANSI-aware styling
// existed, kept as the fallback for plain text and for any run whose color didn't map to a
// named Azphalt hue (StyledTranscript.kt's own ansiHueOf).
private fun StyledSpan.inkColor(onPage: Color): Color = hue?.let { Azphalt.hues[it] } ?: onPage.copy(alpha = .8f)

private fun buildStyledLine(spans: List<StyledSpan>, onPage: Color): AnnotatedString = buildAnnotatedString {
    spans.forEach { span ->
        withStyle(SpanStyle(color = span.inkColor(onPage), fontWeight = if (span.bold) FontWeight.Bold else null)) {
            append(span.text)
        }
    }
}

@Composable
private fun OutputLines(entry: TerminalHistoryEntry) {
    // D4: this used to short-circuit to a flat, unanimated Text while entry.isRunning was true,
    // and only mount the per-line wipe below once the command finished - so a slow command (an
    // install, a build) sat there looking frozen for its entire run, then dumped its whole
    // transcript in one staggered burst at the very end. Using the same per-line composable
    // structure whether running or not means each NEW line gets its own wipe the moment it
    // actually streams in - each OutputWipeLine's `spans` param can keep growing after its own
    // LaunchedEffect(Unit) has already fired (a line still being written, with no trailing
    // newline yet), which just reads as text extending live rather than re-wiping.
    //
    // D1: entry.styledOutput (one entry per real terminal row, ANSI-aware) takes over rendering
    // whenever it's non-empty - the bootstrap/Builtins branches, and any real command with no
    // ANSI escapes in its output at all, leave it empty and this falls back to a single unstyled
    // span per plain-text line, exactly what rendered before styling existed.
    val lines = remember(entry.output, entry.styledOutput) {
        if (entry.styledOutput.isNotEmpty()) {
            entry.styledOutput
        } else {
            entry.output.split("\n").map { listOf(StyledSpan(it)) }
        }
    }
    Column {
        // Only the animated prefix gets one composable per line - the stagger cap already bounds
        // how many lines wipe on individually, but the *rest* of a very long output (a recursive
        // listing, a package inventory, thousands of lines) used to still get one Text node each,
        // costing real composition/layout work for content nobody's watching wipe on anyway. The
        // remaining tail renders as a single joined block instead.
        lines.take(OUTPUT_WIPE_STAGGER_CAP).forEachIndexed { index, spans ->
            OutputWipeLine(seq = index, spans = spans)
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
private fun OutputWipeLine(seq: Int, spans: List<StyledSpan>) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // D4: seq is this line's absolute index into the whole transcript, which keeps growing
        // for the life of a long-running command - a plain `seq * STAGGER_MS` delay would make a
        // line arriving live at, say, index 400 wait another 36s past its own actual arrival
        // before it's even allowed to start wiping in. Wrapping the multiplier by the same cap
        // that already bounds how many lines animate individually keeps every line's own delay
        // small and bounded, whether it's part of a big finished-block burst or arriving alone
        // seconds into a stream.
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
            .graphicsLayer { translationX = -14.dp.toPx() * (1f - progress.value) },
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    )
}

// D3: stderr's own treatment - a hairline amber rule above amber-tinted monospace text, so "here
// is your answer" (stdout, above) and "something the program said on the side" (stderr) read as
// visually distinct the moment either has anything in it. StatusDot itself has only two tiers
// (running/failure) and never uses amber - this is stderr's own distinct color, not a shade of a
// StatusDot state. No wipe animation of its own - OutputLines already carries that beat for the
// primary transcript, and stderr is usually the smaller, secondary block sitting under it.
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
    // UI-7: the visible pill (padding(vertical = 11.dp) around 11sp type) renders well under the
    // 48dp minimum touch target. Rather than growing the pill itself - which would blow up its
    // compact look next to its siblings in the same Row - the clickable region is a separate,
    // invisible 48dp-tall Box the small pill is centered inside, the same pattern
    // GuideReaderScreen's own Chip helper uses for the identical shape.
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
                            // UI-7: a bare glyph-sized clickable here would hit the same well-
                            // under-48dp problem the guide Chip had. The full 48dp minimum isn't
                            // used - this sits inside a horizontally-scrolling multi-tab strip,
                            // and a 48dp close target per tab would make more than two or three
                            // tabs unreachable on a phone-width screen - but the hitbox is grown
                            // well past the bare glyph via padding, a deliberate middle ground
                            // between the two, not an oversight.
                            Box(
                                Modifier
                                    .clickable { onClose(s.id) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "×",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Azphalt.Yellow,
                                        fontSize = 10.sp
                                    )
                                )
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
                Text(
                    "+",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = onPage.copy(alpha = .55f),
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
        // UI-7: the visible circle stays its small designed 22dp size; the clickable region is a
        // separate, invisible 48dp-minimum Box the circle is centered inside, matching the pattern
        // GuideReaderScreen's own Chip() uses for this exact problem. Settings and Guide sit only
        // 6dp apart, so their 48dp hit areas necessarily overlap - that's an intentional generous
        // tap zone, not a bug, since each hit area is still centered on its own glyph.
        Box(
            Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⚙",
                    style = MaterialTheme.typography.titleMedium.copy(color = onPage, fontSize = 12.sp)
                )
            }
        }
        Box(
            Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(onClick = onOpenGuide),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "?",
                    style = MaterialTheme.typography.titleMedium.copy(color = onPage, fontSize = 12.sp)
                )
            }
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
                color = Azphalt.currentGround.onPage.copy(alpha = .45f),
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
                    // A capsule is never tinted, faded, or given alpha (style guide "03 -
                    // Transparency") - idle uses the same ink-14% wash every other disabled
                    // capsule in the app does, not a faded copy of its own hue.
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
            // UI-7: the visible pill keeps its small designed 26dp height; the clickable region
            // is a separate, invisible 48dp-minimum-height Box the pill is centered inside, same
            // pattern as GuideReaderScreen's Chip(). Still `.weight(1f)` so the six keys divide
            // the row's width evenly - only the height grows past the visible pill.
            Box(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { onKeyClick(k) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Azphalt.Ink.copy(alpha = .14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        k.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Azphalt.currentGround.onPage,
                            fontSize = 8.sp
                        )
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
