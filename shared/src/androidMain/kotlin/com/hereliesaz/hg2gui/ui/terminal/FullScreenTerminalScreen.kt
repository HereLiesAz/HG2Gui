package com.hereliesaz.hg2gui.ui.terminal

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.terminal.FullScreenPtySession
import com.hereliesaz.hg2gui.terminal.TuiTerminalAdapter
import com.hereliesaz.hg2gui.terminal.terminalRowText
import com.hereliesaz.hg2gui.ui.AzphaltChip
import com.hereliesaz.hg2gui.ui.ConfirmDialog
import com.hereliesaz.hg2gui.ui.LocalAzphaltUiScale
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.screenInsets
import com.termux.terminal.KeyHandler
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalRow
import com.termux.terminal.TextStyle as TmStyle
import kotlin.math.max
import kotlin.math.min

private val CELL_FONT_SIZE = 14.sp
private const val CURSOR_ALPHA = 0.5f

/** Below this window height (dp) - a landscape phone - the symbol keys share the first row. */
private const val COMPACT_HEIGHT_DP = 480

/** Keeps the screen-reader description of the grid to a size TalkBack can actually read out. */
private const val MAX_DESCRIBED_LINES = 60

private const val ESC_CODE_POINT = 0x1b

private data class ExtraKey(val label: String, val keyCode: Int, val description: String? = null)

/** A key that types a literal character (shell punctuation that soft keyboards bury). */
private data class SymbolKey(val label: String, val text: String, val description: String)

private val EXTRA_KEYS = listOf(
    ExtraKey("ESC", NativeKeyEvent.KEYCODE_ESCAPE, "Escape"),
    ExtraKey("TAB", NativeKeyEvent.KEYCODE_TAB, "Tab"),
    ExtraKey("←", NativeKeyEvent.KEYCODE_DPAD_LEFT, "Left arrow"),
    ExtraKey("↑", NativeKeyEvent.KEYCODE_DPAD_UP, "Up arrow"),
    ExtraKey("↓", NativeKeyEvent.KEYCODE_DPAD_DOWN, "Down arrow"),
    ExtraKey("→", NativeKeyEvent.KEYCODE_DPAD_RIGHT, "Right arrow"),
    ExtraKey("HOME", NativeKeyEvent.KEYCODE_MOVE_HOME),
    ExtraKey("END", NativeKeyEvent.KEYCODE_MOVE_END),
    ExtraKey("PGUP", NativeKeyEvent.KEYCODE_PAGE_UP, "Page up"),
    ExtraKey("PGDN", NativeKeyEvent.KEYCODE_PAGE_DOWN, "Page down"),
    ExtraKey("⌫", NativeKeyEvent.KEYCODE_DEL, "Backspace"),
    ExtraKey("⏎", NativeKeyEvent.KEYCODE_ENTER, "Enter")
)

private val SYMBOL_KEYS = listOf(
    SymbolKey("/", "/", "Slash"),
    SymbolKey("|", "|", "Pipe"),
    SymbolKey("~", "~", "Tilde"),
    SymbolKey("-", "-", "Dash"),
    SymbolKey("_", "_", "Underscore"),
    SymbolKey("\\", "\\", "Backslash")
)

/**
 * Full-screen PTY surface. When the live alternate-screen buffer can be modeled confidently as
 * a structured terminal UI, the same entry point dynamically projects it through
 * [AdaptiveTuiWrapperScreen]. Otherwise it renders the raw terminal grid.
 *
 * Both surfaces are screen roots: they pad themselves for the system bars, the display cutout and
 * the keyboard (see `screenInsets`), so the PTY grid is sized from the space that is actually
 * visible. Pass [fullscreen] = the user's immersive-mode preference so hidden bars are not padded.
 *
 * RAW is not a dead end: when the program is also wrappable, the RAW surface offers a "native view"
 * control that clears the override again.
 */
@Composable
fun FullScreenTerminalScreen(
    holder: FullScreenPtySession,
    onExit: () -> Unit,
    forceRaw: Boolean = false,
    fullscreen: Boolean = false
) {
    var rawOverride by remember(holder) { mutableStateOf(forceRaw) }
    // Registered before choosing a surface: with the effect below the old early return, a session
    // that exited while the adaptive wrapper was showing never reported its exit.
    val exitCode = holder.exitCode
    LaunchedEffect(exitCode) { if (exitCode != null) onExit() }

    @Suppress("UNUSED_VARIABLE")
    val generation = holder.generation
    // Evaluated in RAW too (when overridden): it decides whether "native view" can be offered.
    val wrappable = TuiTerminalAdapter.snapshot(holder)?.isWrappable == true
    if (wrappable && !rawOverride) {
        AdaptiveTuiWrapperScreen(
            holder = holder,
            onRawTerminal = { rawOverride = true },
            onExit = onExit,
            fullscreen = fullscreen
        )
    } else {
        RawTerminalSurface(
            holder = holder,
            fullscreen = fullscreen,
            onNativeView = if (wrappable) ({ rawOverride = false }) else null,
            onExit = onExit
        )
    }
}

/**
 * The "Closing now ends the running program." confirmation, shared by the RAW and wrapped surfaces
 * and worded exactly like the Back-gesture confirmation in `TerminalActivity`, so the visible kill
 * buttons are never a quieter way to lose an unsaved editor or REPL session than Back.
 */
@Composable
internal fun CloseProgramConfirm(holder: FullScreenPtySession, onDismiss: () -> Unit, onExit: () -> Unit) {
    ConfirmDialog(
        title = "CLOSE ${holder.commandLine.take(MAX_TITLE_COMMAND_CHARS).uppercase()}?",
        message = "Closing now ends the running program.",
        confirmLabel = "CLOSE",
        onConfirm = {
            onDismiss()
            holder.kill()
            onExit()
        },
        onDismiss = onDismiss
    )
}

private const val MAX_TITLE_COMMAND_CHARS = 40

@Composable
private fun RawTerminalSurface(
    holder: FullScreenPtySession,
    fullscreen: Boolean,
    onNativeView: (() -> Unit)?,
    onExit: () -> Unit
) {
    val measurer = rememberTextMeasurer()
    val focusRequester = remember { FocusRequester() }
    var ctrlArmed by remember { mutableStateOf(false) }
    var altArmed by remember { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }
    var columns by remember { mutableStateOf(0) }
    var rows by remember { mutableStateOf(0) }

    // Keyed on everything that changes a glyph's pixel size: the Text-size setting and the system
    // font scale are both applied in place (the Activity handles those config changes itself), so
    // an unkeyed measurement would keep positioning the grid with the old cell.
    val density = LocalDensity.current
    val uiScale = LocalAzphaltUiScale.current
    val cellMetrics = remember(measurer, density.density, density.fontScale, uiScale) {
        measurer.measure("X", ComposeTextStyle(fontFamily = FontFamily.Monospace, fontSize = CELL_FONT_SIZE))
    }
    val cellWidthPx = cellMetrics.size.width.coerceAtLeast(1)
    val cellHeightPx = cellMetrics.size.height.coerceAtLeast(1)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // The chrome (top bar, key rows) sits on the page ground so the system-bar icons - which follow
    // the ground - stay legible; only the terminal itself is Ink. Insets are applied to this root,
    // so the grid below is measured *after* the bars and the keyboard have taken their share and
    // the PTY is never told it has rows that are hidden behind them.
    Column(
        Modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.page)
            .screenInsets(fullscreen)
    ) {
        TerminalTopBar(holder, onNativeView, onRequestClose = { confirmClose = true })
        Column(Modifier.weight(1f).fillMaxWidth().background(Azphalt.Ink)) {
            StructuredShellStatus(holder)
            TerminalGrid(
                holder = holder,
                measurer = measurer,
                focusRequester = focusRequester,
                ctrlArmed = ctrlArmed,
                altArmed = altArmed,
                onModifiersConsumed = {
                    ctrlArmed = false
                    altArmed = false
                },
                cellWidthPx = cellWidthPx,
                cellHeightPx = cellHeightPx,
                columns = columns,
                rows = rows,
                onGridSized = { c, r ->
                    columns = c
                    rows = r
                    holder.updateSize(c, r, cellWidthPx, cellHeightPx)
                },
                onNativeView = onNativeView,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
        TerminalExtraKeys(
            holder = holder,
            ctrlArmed = ctrlArmed,
            altArmed = altArmed,
            onCtrlToggle = { ctrlArmed = !ctrlArmed },
            onAltToggle = { altArmed = !altArmed },
            onModifiersConsumed = {
                ctrlArmed = false
                altArmed = false
            }
        )
    }

    if (confirmClose) {
        CloseProgramConfirm(holder, onDismiss = { confirmClose = false }, onExit = onExit)
    }
}

/**
 * CLOSE sits at the far left and INTR at the right, with the command name between them, so the
 * destructive key is never the neighbour of the one used mid-program. Every control has a 48dp hit
 * box (see `AzphaltChip`).
 */
@Composable
private fun TerminalTopBar(
    holder: FullScreenPtySession,
    onNativeView: (() -> Unit)?,
    onRequestClose: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AzphaltChip(
            label = "CLOSE",
            onClick = onRequestClose,
            tint = Azphalt.hues[CLOSE_HUE],
            contentDescription = "Close program"
        )
        Text(
            holder.commandLine,
            color = Azphalt.currentGround.onPage,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        if (onNativeView != null) {
            AzphaltChip(
                label = "NATIVE",
                onClick = onNativeView,
                contentDescription = "Switch to native view"
            )
        }
        AzphaltChip(
            label = "INTR",
            onClick = { holder.sendInterrupt() },
            contentDescription = "Send interrupt"
        )
    }
}

/** Index of the red hue in [Azphalt.hues] (the app's destructive/primary colour). */
private const val CLOSE_HUE = 6

@Suppress("LongParameterList")
@Composable
private fun TerminalGrid(
    holder: FullScreenPtySession,
    measurer: TextMeasurer,
    focusRequester: FocusRequester,
    ctrlArmed: Boolean,
    altArmed: Boolean,
    onModifiersConsumed: () -> Unit,
    cellWidthPx: Int,
    cellHeightPx: Int,
    columns: Int,
    rows: Int,
    onGridSized: (Int, Int) -> Unit,
    onNativeView: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var gridPx by remember { mutableStateOf(IntSize.Zero) }
    val currentOnGridSized by rememberUpdatedState(onGridSized)
    // The grid is re-derived when its pixel size changes (rotation, keyboard open/close) *and*
    // when the cell size does (Text-size or font-scale change) - the latter changes no layout size.
    LaunchedEffect(gridPx, cellWidthPx, cellHeightPx) {
        if (gridPx == IntSize.Zero) return@LaunchedEffect
        val newColumns = max(1, gridPx.width / cellWidthPx)
        val newRows = max(1, gridPx.height / cellHeightPx)
        if (newColumns != columns || newRows != rows) currentOnGridSized(newColumns, newRows)
    }
    Box(
        modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Show keyboard"
            ) { focusRequester.requestFocus() }
            .onSizeChanged { size -> gridPx = size }
    ) {
        @Suppress("UNUSED_VARIABLE")
        val generation = holder.generation
        // The grid is drawn, not composed, so a screen reader would find nothing here. The
        // description (rebuilt only when accessibility asks for it) is the visible text; the
        // actions are the two things a non-visual user needs most.
        Canvas(
            Modifier.fillMaxSize().semantics {
                contentDescription = terminalScreenDescription(visibleTerminalLines(holder))
                customActions = buildList {
                    if (onNativeView != null) {
                        add(CustomAccessibilityAction("Switch to native view") { onNativeView(); true })
                    }
                    add(CustomAccessibilityAction("Send interrupt") { holder.sendInterrupt(); true })
                }
            }
        ) {
            drawTerminal(holder, GridMetrics(columns, rows, cellWidthPx.toFloat(), cellHeightPx.toFloat()), measurer)
        }

        BasicTextField(
            value = input,
            onValueChange = { new ->
                val added = new.text.removePrefix(input.text)
                sendTypedText(holder, added, ctrlArmed, altArmed, onModifiersConsumed)
                input = TextFieldValue("")
            },
            textStyle = ComposeTextStyle(color = Color.Transparent),
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester)
                .semantics { contentDescription = "Terminal keyboard input" }
                .onPreviewKeyEvent { event ->
                    handleSpecialKey(holder, event, holder.cursorApplicationMode, holder.keypadApplicationMode)
                }
        )
    }
}

/**
 * The extra keys a soft keyboard lacks: modifiers (CTRL, ALT - sticky, armed for the next key),
 * navigation keys and shell punctuation. In a landscape phone the punctuation joins the first row
 * so the key area stays one row high.
 */
@Composable
private fun TerminalExtraKeys(
    holder: FullScreenPtySession,
    ctrlArmed: Boolean,
    altArmed: Boolean,
    onCtrlToggle: () -> Unit,
    onAltToggle: () -> Unit,
    onModifiersConsumed: () -> Unit
) {
    val compact = LocalConfiguration.current.screenHeightDp < COMPACT_HEIGHT_DP
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(KEY_SPACING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AzphaltChip("CTRL", onCtrlToggle, selected = ctrlArmed, contentDescription = "Control modifier")
            AzphaltChip("ALT", onAltToggle, selected = altArmed, contentDescription = "Alt modifier")
            for (key in EXTRA_KEYS) {
                AzphaltChip(
                    label = key.label,
                    onClick = {
                        sendExtraKey(holder, key, ctrlArmed, altArmed)
                        if (ctrlArmed || altArmed) onModifiersConsumed()
                    },
                    contentDescription = key.description
                )
            }
            if (compact) SymbolKeys(holder, ctrlArmed, altArmed, onModifiersConsumed)
        }
        if (!compact) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(KEY_SPACING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SymbolKeys(holder, ctrlArmed, altArmed, onModifiersConsumed)
            }
        }
    }
}

private val KEY_SPACING = 2.dp

@Composable
private fun SymbolKeys(
    holder: FullScreenPtySession,
    ctrlArmed: Boolean,
    altArmed: Boolean,
    onModifiersConsumed: () -> Unit
) {
    for (key in SYMBOL_KEYS) {
        AzphaltChip(
            label = key.label,
            onClick = { sendTypedText(holder, key.text, ctrlArmed, altArmed, onModifiersConsumed) },
            contentDescription = key.description
        )
    }
}

private fun sendExtraKey(holder: FullScreenPtySession, key: ExtraKey, ctrl: Boolean, alt: Boolean) {
    var mods = 0
    if (ctrl) mods = mods or KeyHandler.KEYMOD_CTRL
    if (alt) mods = mods or KeyHandler.KEYMOD_ALT
    // A modified code the terminfo has no entry for (Ctrl+Enter, say) falls back to the plain key
    // rather than swallowing the tap.
    val code = KeyHandler.getCode(key.keyCode, mods, holder.cursorApplicationMode, holder.keypadApplicationMode)
        ?: KeyHandler.getCode(key.keyCode, 0, holder.cursorApplicationMode, holder.keypadApplicationMode)
    if (code != null) holder.sendText(code)
}

private fun sendTypedText(
    holder: FullScreenPtySession,
    added: String,
    ctrlArmed: Boolean,
    altArmed: Boolean,
    onModifiersConsumed: () -> Unit
) {
    var i = 0
    while (i < added.length) {
        val codePoint = added.codePointAt(i)
        for (point in terminalCodePoints(codePoint, ctrlArmed, altArmed)) holder.sendCodePoint(point)
        i += Character.charCount(codePoint)
    }
    if (added.isNotEmpty() && (ctrlArmed || altArmed)) onModifiersConsumed()
}

/** The control byte for Ctrl+[codePoint] (`Ctrl+C` = 3, `Ctrl+[` = ESC, ...), or null if it has none. */
internal fun controlCodePoint(codePoint: Int): Int? = when (codePoint) {
    in 'a'.code..'z'.code -> codePoint - 'a'.code + 1
    in 'A'.code..'Z'.code -> codePoint - 'A'.code + 1
    '@'.code, ' '.code -> 0
    '['.code -> ESC_CODE_POINT
    '\\'.code -> 0x1c
    ']'.code -> 0x1d
    '^'.code -> 0x1e
    '_'.code -> 0x1f
    '?'.code -> 0x7f
    else -> null
}

/**
 * What to write to the PTY for one typed character with the sticky modifiers applied: Ctrl maps it
 * to its control byte when it has one (otherwise the character goes through unchanged), Alt prefixes
 * ESC - the convention every terminal program expects for Meta.
 */
internal fun terminalCodePoints(codePoint: Int, ctrl: Boolean, alt: Boolean): IntArray {
    val base = if (ctrl) controlCodePoint(codePoint) ?: codePoint else codePoint
    return if (alt) intArrayOf(ESC_CODE_POINT, base) else intArrayOf(base)
}

/** The non-blank visible rows of the emulator screen, top to bottom, with trailing blanks trimmed. */
private fun visibleTerminalLines(holder: FullScreenPtySession): List<String> {
    val emulator = holder.session.emulator ?: return emptyList()
    val screen = emulator.screen
    return (0 until screen.mScreenRows).mapNotNull { row ->
        val line = screen.mLines.getOrNull(screen.externalToInternalRow(row))
        terminalRowText(line, 0, screen.mColumns).trimEnd().takeIf { it.isNotEmpty() }
    }
}

/** Spoken description of the drawn grid: the visible lines, capped to the last [MAX_DESCRIBED_LINES]. */
internal fun terminalScreenDescription(lines: List<String>): String =
    if (lines.isEmpty()) {
        "Terminal. The screen is empty."
    } else {
        "Terminal. " + lines.takeLast(MAX_DESCRIBED_LINES).joinToString("\n")
    }

private fun handleSpecialKey(holder: FullScreenPtySession, event: KeyEvent, cursorApp: Boolean, keypadApp: Boolean): Boolean {
    val native = event.nativeKeyEvent
    val code = if (native.action == NativeKeyEvent.ACTION_DOWN) specialKeyCode(native, cursorApp, keypadApp) else null
    if (code == null) return false
    holder.sendText(code)
    return true
}

private fun specialKeyCode(native: NativeKeyEvent, cursorApp: Boolean, keypadApp: Boolean): String? {
    // Hardware Ctrl+letter: KeyHandler has no mapping for it and the hidden text field turns it
    // into a clipboard shortcut rather than a character, so send the control byte directly
    // (Alt adds the usual ESC prefix).
    if (native.isCtrlPressed && native.keyCode in NativeKeyEvent.KEYCODE_A..NativeKeyEvent.KEYCODE_Z) {
        val control = (native.keyCode - NativeKeyEvent.KEYCODE_A + 1).toChar().toString()
        return if (native.isAltPressed) "\u001b$control" else control
    }
    var mods = 0
    if (native.isShiftPressed) mods = mods or KeyHandler.KEYMOD_SHIFT
    if (native.isCtrlPressed) mods = mods or KeyHandler.KEYMOD_CTRL
    if (native.isAltPressed) mods = mods or KeyHandler.KEYMOD_ALT
    return KeyHandler.getCode(native.keyCode, mods, cursorApp, keypadApp)
}

private class GridMetrics(val columns: Int, val rows: Int, val cellWidth: Float, val cellHeight: Float)

private class RenderContext(
    val metrics: GridMetrics,
    val screenRows: Int,
    val screenCols: Int,
    val colors: IntArray,
    val defaultBg: Int,
    val measurer: TextMeasurer
)

private fun DrawScope.drawTerminal(holder: FullScreenPtySession, metrics: GridMetrics, measurer: TextMeasurer) {
    val emulator = holder.session.emulator ?: return
    val screen = emulator.screen
    val colors = emulator.mColors.mCurrentColors
    val defaultBg = resolveColor(colors, TmStyle.COLOR_INDEX_BACKGROUND)
    val ctx = RenderContext(
        metrics = metrics,
        screenRows = min(metrics.rows, screen.mScreenRows),
        screenCols = min(metrics.columns, screen.mColumns),
        colors = colors,
        defaultBg = defaultBg,
        measurer = measurer
    )

    drawRect(Color(defaultBg), size = Size(metrics.columns * metrics.cellWidth, metrics.rows * metrics.cellHeight))
    for (row in 0 until ctx.screenRows) {
        drawRow(screen.mLines.getOrNull(screen.externalToInternalRow(row)), row, ctx)
    }
    drawCursor(holder, emulator, ctx)
}

private fun DrawScope.drawRow(line: TerminalRow?, row: Int, ctx: RenderContext) {
    var col = 0
    while (col < ctx.screenCols) {
        val style = line?.getStyle(col) ?: TmStyle.NORMAL
        var runEnd = col + 1
        while (runEnd < ctx.screenCols && (line?.getStyle(runEnd) ?: TmStyle.NORMAL) == style) runEnd++
        drawRun(line, RowSpan(row, col, runEnd), style, ctx)
        col = runEnd
    }
}

private class RowSpan(val row: Int, val col: Int, val end: Int)

private fun DrawScope.drawRun(line: TerminalRow?, span: RowSpan, style: Long, ctx: RenderContext) {
    val text = rowRunText(line, span.col, span.end)
    val effect = TmStyle.decodeEffect(style)
    val inverse = effect and TmStyle.CHARACTER_ATTRIBUTE_INVERSE != 0
    var fg = resolveColor(ctx.colors, TmStyle.decodeForeColor(style))
    var bg = resolveColor(ctx.colors, TmStyle.decodeBackColor(style))
    if (inverse) { val swap = fg; fg = bg; bg = swap }

    val x = span.col * ctx.metrics.cellWidth
    val y = span.row * ctx.metrics.cellHeight
    val runWidth = (span.end - span.col) * ctx.metrics.cellWidth
    if (bg != ctx.defaultBg) {
        drawRect(Color(bg), topLeft = Offset(x, y), size = Size(runWidth, ctx.metrics.cellHeight))
    }

    val bold = effect and TmStyle.CHARACTER_ATTRIBUTE_BOLD != 0
    val layout = ctx.measurer.measure(
        text,
        ComposeTextStyle(
            color = Color(fg),
            fontFamily = FontFamily.Monospace,
            fontSize = CELL_FONT_SIZE,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    )
    drawText(layout, topLeft = Offset(x, y))
    if (effect and TmStyle.CHARACTER_ATTRIBUTE_UNDERLINE != 0) {
        drawLine(Color(fg), Offset(x, y + ctx.metrics.cellHeight - 1), Offset(x + runWidth, y + ctx.metrics.cellHeight - 1))
    }
}

private fun DrawScope.drawCursor(holder: FullScreenPtySession, emulator: TerminalEmulator, ctx: RenderContext) {
    val cursorRow = emulator.getCursorRow()
    val cursorCol = emulator.getCursorCol()
    if (holder.cursorVisible && cursorRow in 0 until ctx.screenRows && cursorCol in 0 until ctx.screenCols) {
        drawRect(
            Color(resolveColor(ctx.colors, TmStyle.COLOR_INDEX_CURSOR)).copy(alpha = CURSOR_ALPHA),
            topLeft = Offset(cursorCol * ctx.metrics.cellWidth, cursorRow * ctx.metrics.cellHeight),
            size = Size(ctx.metrics.cellWidth, ctx.metrics.cellHeight)
        )
    }
}

private fun rowRunText(line: TerminalRow?, from: Int, to: Int): String = terminalRowText(line, from, to)

private fun resolveColor(colors: IntArray, styleColor: Int): Int =
    if (styleColor and 0xff000000.toInt() == 0xff000000.toInt()) styleColor else colors[styleColor]
