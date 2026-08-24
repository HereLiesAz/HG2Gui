package com.hereliesaz.hg2gui.ui.terminal

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.terminal.FullScreenPtySession
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.termux.terminal.KeyHandler
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalRow
import com.termux.terminal.TextStyle as TmStyle
import kotlin.math.max
import kotlin.math.min

private val CELL_FONT_SIZE = 14.sp
private const val CURSOR_ALPHA = 0.5f

private data class ExtraKey(val label: String, val keyCode: Int)

private val EXTRA_KEYS = listOf(
    ExtraKey("ESC", NativeKeyEvent.KEYCODE_ESCAPE),
    ExtraKey("TAB", NativeKeyEvent.KEYCODE_TAB),
    ExtraKey("←", NativeKeyEvent.KEYCODE_DPAD_LEFT),
    ExtraKey("↑", NativeKeyEvent.KEYCODE_DPAD_UP),
    ExtraKey("↓", NativeKeyEvent.KEYCODE_DPAD_DOWN),
    ExtraKey("→", NativeKeyEvent.KEYCODE_DPAD_RIGHT),
    ExtraKey("HOME", NativeKeyEvent.KEYCODE_MOVE_HOME),
    ExtraKey("END", NativeKeyEvent.KEYCODE_MOVE_END),
    ExtraKey("PGUP", NativeKeyEvent.KEYCODE_PAGE_UP),
    ExtraKey("PGDN", NativeKeyEvent.KEYCODE_PAGE_DOWN),
    ExtraKey("⌫", NativeKeyEvent.KEYCODE_DEL),
    ExtraKey("⏎", NativeKeyEvent.KEYCODE_ENTER)
)

/**
 * S1's full-screen surface: renders a [FullScreenPtySession]'s live screen buffer and forwards
 * raw input to it, instead of the app's normal flattened one-shot transcript. See
 * FullScreenPtySession's own doc comment for what it wraps and why.
 *
 * Known MVP limitations, honestly: the cell renderer treats each `Char` in a row's backing array
 * as one column (no wide/CJK character or combining-mark support), re-measures every styled run
 * on every redraw rather than caching glyph layout, and hardware-keyboard modifier combinations
 * beyond what [KeyHandler] already recognizes (shift/ctrl/alt on the arrow/function/nav keys) are
 * not specially handled. Good enough to make vim/htop/less/tmux usable at all, which today they
 * are not.
 */
@Composable
fun FullScreenTerminalScreen(holder: FullScreenPtySession, onExit: () -> Unit) {
    val measurer = rememberTextMeasurer()
    val focusRequester = remember { FocusRequester() }
    var ctrlArmed by remember { mutableStateOf(false) }
    var columns by remember { mutableStateOf(0) }
    var rows by remember { mutableStateOf(0) }

    val cellMetrics = remember {
        measurer.measure("X", ComposeTextStyle(fontFamily = FontFamily.Monospace, fontSize = CELL_FONT_SIZE))
    }
    val cellWidthPx = cellMetrics.size.width.coerceAtLeast(1)
    val cellHeightPx = cellMetrics.size.height.coerceAtLeast(1)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val exitCode = holder.exitCode
    LaunchedEffect(exitCode) { if (exitCode != null) onExit() }

    Column(Modifier.fillMaxSize().background(Azphalt.Ink)) {
        TerminalTopBar(holder, ctrlArmed, onCtrlToggle = { ctrlArmed = !ctrlArmed }, onExit = onExit)
        TerminalGrid(
            holder = holder,
            measurer = measurer,
            focusRequester = focusRequester,
            ctrlArmed = ctrlArmed,
            onCtrlConsumed = { ctrlArmed = false },
            cellWidthPx = cellWidthPx,
            cellHeightPx = cellHeightPx,
            columns = columns,
            rows = rows,
            onGridSized = { c, r ->
                columns = c
                rows = r
                holder.updateSize(c, r, cellWidthPx, cellHeightPx)
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        TerminalExtraKeysRow(holder)
    }
}

@Composable
private fun TerminalTopBar(
    holder: FullScreenPtySession,
    ctrlArmed: Boolean,
    onCtrlToggle: () -> Unit,
    onExit: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(holder.commandLine, color = Azphalt.White, modifier = Modifier.padding(end = 8.dp))
        Text(
            if (ctrlArmed) "CTRL•" else "CTRL",
            color = if (ctrlArmed) Azphalt.Yellow else Azphalt.White,
            modifier = Modifier.padding(horizontal = 8.dp).clickable { onCtrlToggle() }
        )
        Text(
            "INTR",
            color = Azphalt.White,
            modifier = Modifier.padding(horizontal = 8.dp).clickable { holder.sendInterrupt() }
        )
        Text(
            "✕",
            color = Azphalt.White,
            modifier = Modifier.padding(start = 8.dp).clickable {
                holder.kill()
                onExit()
            }
        )
    }
}

@Suppress("LongParameterList") // A live pty grid's own layout state - every param is genuinely
// distinct state the canvas/input pair below both need, not incidental plumbing.
@Composable
private fun TerminalGrid(
    holder: FullScreenPtySession,
    measurer: TextMeasurer,
    focusRequester: FocusRequester,
    ctrlArmed: Boolean,
    onCtrlConsumed: () -> Unit,
    cellWidthPx: Int,
    cellHeightPx: Int,
    columns: Int,
    rows: Int,
    onGridSized: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    Box(
        modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusRequester.requestFocus() }
            .onSizeChanged { size ->
                val newColumns = max(1, size.width / cellWidthPx)
                val newRows = max(1, size.height / cellHeightPx)
                if (newColumns != columns || newRows != rows) onGridSized(newColumns, newRows)
            }
    ) {
        // A read of holder.generation here (not inside the DrawScope lambda below) is what makes
        // this recompose - and therefore redraw - every time the pty produces output; see
        // FullScreenPtySession's own doc comment on the field.
        @Suppress("UNUSED_VARIABLE")
        val generation = holder.generation
        Canvas(Modifier.fillMaxSize()) {
            drawTerminal(holder, GridMetrics(columns, rows, cellWidthPx.toFloat(), cellHeightPx.toFloat()), measurer)
        }

        BasicTextField(
            value = input,
            onValueChange = { new ->
                val added = new.text.removePrefix(input.text)
                sendTypedText(holder, added, ctrlArmed, onCtrlConsumed)
                input = TextFieldValue("")
            },
            textStyle = ComposeTextStyle(color = Color.Transparent),
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    handleSpecialKey(holder, event, holder.cursorApplicationMode, holder.keypadApplicationMode)
                }
        )
    }
}

@Composable
private fun TerminalExtraKeysRow(holder: FullScreenPtySession) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        for (key in EXTRA_KEYS) {
            Text(
                key.label,
                color = Azphalt.White,
                modifier = Modifier.padding(horizontal = 10.dp).clickable {
                    val code = KeyHandler.getCode(
                        key.keyCode, 0, holder.cursorApplicationMode, holder.keypadApplicationMode
                    )
                    if (code != null) holder.sendText(code)
                }
            )
        }
    }
}

private fun sendTypedText(holder: FullScreenPtySession, added: String, ctrlArmed: Boolean, onCtrlConsumed: () -> Unit) {
    var i = 0
    while (i < added.length) {
        val codePoint = added.codePointAt(i)
        sendCodePoint(holder, codePoint, ctrlArmed)
        i += Character.charCount(codePoint)
    }
    if (added.isNotEmpty() && ctrlArmed) onCtrlConsumed()
}

private fun sendCodePoint(holder: FullScreenPtySession, codePoint: Int, ctrl: Boolean) {
    if (ctrl) {
        val ctrlChar = codePoint.toChar().uppercaseChar()
        if (ctrlChar in 'A'..'Z') {
            holder.sendCodePoint(ctrlChar - 'A' + 1)
            return
        }
    }
    holder.sendCodePoint(codePoint)
}

/** Consumes the event (returning true) only for keys [KeyHandler] recognizes as navigation/
 *  control - arrows, Home/End, PgUp/PgDn, Esc, Tab, Enter, Backspace, function keys. Plain
 *  character keys fall through unconsumed so they reach the (invisible) text field's own
 *  IME-driven input path instead. */
private fun handleSpecialKey(holder: FullScreenPtySession, event: KeyEvent, cursorApp: Boolean, keypadApp: Boolean): Boolean {
    val native = event.nativeKeyEvent
    val code = if (native.action == NativeKeyEvent.ACTION_DOWN) specialKeyCode(native, cursorApp, keypadApp) else null
    if (code == null) return false
    holder.sendText(code)
    return true
}

private fun specialKeyCode(native: NativeKeyEvent, cursorApp: Boolean, keypadApp: Boolean): String? {
    var mods = 0
    if (native.isShiftPressed) mods = mods or KeyHandler.KEYMOD_SHIFT
    if (native.isCtrlPressed) mods = mods or KeyHandler.KEYMOD_CTRL
    if (native.isAltPressed) mods = mods or KeyHandler.KEYMOD_ALT
    return KeyHandler.getCode(native.keyCode, mods, cursorApp, keypadApp)
}

private class GridMetrics(val columns: Int, val rows: Int, val cellWidth: Float, val cellHeight: Float)

/** Everything a single draw pass needs, bundled so the row/run/cursor helpers below don't each
 *  need their own long parameter list for values that never change within one frame. */
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

private fun rowRunText(line: TerminalRow?, from: Int, to: Int): String {
    if (line == null) return " ".repeat(to - from)
    val builder = StringBuilder(to - from)
    for (c in from until to) {
        val idx = line.findStartOfColumn(c)
        builder.append(if (idx < line.getSpaceUsed()) line.mText[idx] else ' ')
    }
    return builder.toString()
}

private fun resolveColor(colors: IntArray, styleColor: Int): Int =
    if (styleColor and 0xff000000.toInt() == 0xff000000.toInt()) styleColor else colors[styleColor]
