package com.hereliesaz.hg2gui.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.AzphaltChip
import com.hereliesaz.hg2gui.ui.AzphaltHeading
import com.hereliesaz.hg2gui.ui.StatusKind
import com.hereliesaz.hg2gui.ui.liveRegionPolite
import com.hereliesaz.hg2gui.ui.screenInsets
import com.hereliesaz.hg2gui.ui.statusColor
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType

/*
 * A plain-text editor for files the shell has no pty to hand to a real one - nano is a real
 * bootstrap pill, but running it through a non-pty ShellSession would render garbled. No
 * embedded command line like the old vi-like editor had: Save and Back are real pills, same
 * page, same capsule primitive as the rest of Azphalt.
 *
 * The screen is a root: it pads itself for the system bars, display cutout and keyboard
 * (`screenInsets`), so the title row is never under the status bar and the text field shrinks
 * above the keyboard instead of being covered by it.
 */

/** Extensions of files that are prose, where the app's proportional face reads better. */
private val PROSE_EXTENSIONS = setOf("txt", "md", "markdown", "text", "rst", "rtf")

/** Longest rendered line the unwrapped field will lay out (dp); keeps the width a valid constraint. */
private const val MAX_UNWRAPPED_WIDTH_DP = 16000

/** Side padding of the text field, in both layouts. */
private val FIELD_HORIZONTAL_PADDING = 20.dp

/** Blank columns past the longest line, so the caret at a line's end is not against the edge. */
private const val UNWRAPPED_TRAILING_COLUMNS = 4

/**
 * Whether [fileName] should be edited in a monospace face: code, configuration, logs and dotfiles
 * (`.sh`, `.conf`, `.ini`, `.xml`, `.bashrc`, `Makefile`...) - everything except plain prose
 * (`.txt`, `.md`, ...). Columns and indentation only line up in monospace.
 */
fun editorUsesMonospace(fileName: String): Boolean {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return extension !in PROSE_EXTENSIONS
}

/**
 * The width, in columns, an unwrapped field must be to show [content]'s longest line whole, plus
 * trailing room for the caret. [minColumns] is the viewport width in columns; the result is never
 * smaller. Pure so the long-line behaviour can be tested without a layout.
 */
fun unwrappedColumns(content: String, minColumns: Int): Int {
    var longest = 0
    var current = 0
    for (ch in content) {
        if (ch == '\n') {
            if (current > longest) longest = current
            current = 0
        } else {
            current++
        }
    }
    if (current > longest) longest = current
    return maxOf(minColumns, longest + UNWRAPPED_TRAILING_COLUMNS)
}

@Composable
fun EditorScreen(
    fileName: String,
    content: String?,
    error: String?,
    dirty: Boolean,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissError: () -> Unit = {},
    fullscreen: Boolean = false
) {
    val ground = Azphalt.currentGround
    // Soft-wrap survives rotation (the Activity is recreated on it). Off means long lines scroll
    // sideways instead of folding - what you want for a config file or a log.
    var wrap by rememberSaveable(fileName) { mutableStateOf(true) }
    val mono = editorUsesMonospace(fileName)

    Column(modifier.fillMaxSize().background(ground.pageBrush()).screenInsets(fullscreen)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            AzphaltHeading(fileName)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AzphaltChip("‹ BACK", onBack, contentDescription = "Back")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (content != null) {
                        AzphaltChip(
                            label = "WRAP",
                            onClick = { wrap = !wrap },
                            selected = wrap,
                            contentDescription = "Soft wrap long lines"
                        )
                    }
                    if (content != null && dirty) {
                        AzphaltChip("SAVE", onSave, contentDescription = "Save file")
                    }
                }
            }
        }

        // Once text is loaded, an error (a failed save) is a dismissible banner over the field -
        // never a replacement for it, which would hide the very edits the save failed to keep.
        if (error != null && content != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Azphalt.Ink.copy(alpha = .14f))
                    .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                    .liveRegionPolite(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    error,
                    color = ground.onPage,
                    style = LocalAzphaltType.current.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    modifier = Modifier.weight(1f)
                )
                AzphaltChip("DISMISS", onDismissError)
            }
        }

        when {
            error != null && content == null -> Text(
                error,
                color = ground.onPage,
                style = LocalAzphaltType.current.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
                modifier = Modifier.padding(20.dp).liveRegionPolite()
            )
            content == null -> Text(
                "LOADING…",
                color = statusColor(StatusKind.Muted),
                style = LocalAzphaltType.current.eyebrow,
                modifier = Modifier.padding(20.dp).liveRegionPolite()
            )
            else -> EditorField(
                content = content,
                onContentChange = onContentChange,
                mono = mono,
                wrap = wrap,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EditorField(
    content: String,
    onContentChange: (String) -> Unit,
    mono: Boolean,
    wrap: Boolean,
    modifier: Modifier = Modifier
) {
    val onPage = Azphalt.currentGround.onPage
    val textStyle = if (mono) {
        TextStyle(fontFamily = FontFamily.Monospace, color = onPage, fontSize = 13.sp, lineHeight = 19.sp)
    } else {
        LocalAzphaltType.current.body.copy(color = onPage, fontSize = 14.sp, lineHeight = 21.sp)
    }
    val focusRequester = remember { FocusRequester() }
    // The manifest asks for the keyboard to be up on open (stateAlwaysVisible); give it something
    // to type into rather than a keyboard with no focused field.
    LaunchedEffect(wrap) { runCatching { focusRequester.requestFocus() } }

    if (wrap) {
        BasicTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = modifier.padding(horizontal = FIELD_HORIZONTAL_PADDING, vertical = 4.dp).focusRequester(focusRequester),
            textStyle = textStyle,
            cursorBrush = SolidColor(onPage)
        )
        return
    }

    // BasicTextField has no "don't wrap" switch, so the field is simply laid out wide enough for
    // its longest line and the box around it scrolls sideways.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val cellPx = remember(measurer, textStyle, density.density, density.fontScale) {
        measurer.measure("M", textStyle).size.width.coerceAtLeast(1)
    }
    BoxWithConstraints(modifier) {
        val viewportPx = with(density) { maxWidth.roundToPx() }
        val columns = remember(content, viewportPx, cellPx) { unwrappedColumns(content, viewportPx / cellPx) }
        val textWidth = with(density) { (columns * cellPx).toDp() }.coerceAtMost(MAX_UNWRAPPED_WIDTH_DP.dp)
        val fieldWidth = textWidth + FIELD_HORIZONTAL_PADDING * 2
        Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .width(fieldWidth)
                    .fillMaxHeight()
                    .padding(horizontal = FIELD_HORIZONTAL_PADDING, vertical = 4.dp)
                    .focusRequester(focusRequester),
                textStyle = textStyle,
                cursorBrush = SolidColor(onPage)
            )
        }
    }
}
