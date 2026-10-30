package com.hereliesaz.hg2gui.ui.terminal

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.terminal.*
import com.hereliesaz.hg2gui.ui.AzphaltChip
import com.hereliesaz.hg2gui.ui.StatusKind
import com.hereliesaz.hg2gui.ui.headingRole
import com.hereliesaz.hg2gui.ui.liveRegionPolite
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.minTouchSize
import com.hereliesaz.hg2gui.ui.screenInsets
import com.hereliesaz.hg2gui.ui.statusColor
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType
import com.termux.terminal.KeyHandler
import kotlinx.coroutines.launch

/** Index of the red hue in [Azphalt.hues] (the app's destructive/primary colour). */
private const val DESTRUCTIVE_HUE = 6

/**
 * Native projection of an already-running terminal UI.
 *
 * The screen is a root: it pads itself for the system bars, the display cutout and the keyboard
 * (pass [fullscreen] = the immersive-mode preference). A failed native interaction no longer
 * throws the user into the raw terminal on its own - it explains what happened and *offers* RAW,
 * which they can decline. CLOSE asks before ending the program, like the Back gesture does.
 */
@Composable
fun AdaptiveTuiWrapperScreen(
    holder: FullScreenPtySession,
    onRawTerminal: () -> Unit,
    onExit: () -> Unit,
    fullscreen: Boolean = false
) {
    @Suppress("UNUSED_VARIABLE") val generation = holder.generation
    val snapshot = TuiTerminalAdapter.snapshot(holder)
    val scope = rememberCoroutineScope()
    var interactionError by remember { mutableStateOf<String?>(null) }
    var confirmClose by remember { mutableStateOf(false) }

    if (snapshot == null || !snapshot.isWrappable) {
        FullScreenTerminalScreen(holder = holder, onExit = onExit, forceRaw = true, fullscreen = fullscreen)
        return
    }

    fun handle(result: TuiInteractionController.Result) {
        interactionError = if (result.success) null else interactionFailureMessage(result.reason)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.page)
            .screenInsets(fullscreen)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        WrapperHeader(holder, snapshot, onRawTerminal, onRequestClose = { confirmClose = true })
        interactionError?.let { message ->
            InteractionBanner(message, onUseRaw = onRawTerminal, onDismiss = { interactionError = null })
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (snapshot.tabs.isNotEmpty()) {
                TabStrip(snapshot.tabs) { target -> scope.launch { handle(TuiInteractionController.activateTab(holder, target)) } }
            }
            snapshot.regions.forEach { region ->
                RegionCard(
                    region = region,
                    mouseAware = snapshot.mouseAware,
                    onMouseRow = { row, column -> handle(TuiInteractionController.mouseClick(holder, row, column)) }
                )
            }
            snapshot.layers.forEachIndexed { index, layer ->
                MenuLayer(
                    layer = layer,
                    layerIndex = index,
                    inputOwner = snapshot.activeLayerIndex == index,
                    onChoose = { target -> scope.launch { handle(TuiInteractionController.activate(holder, index, target)) } }
                )
            }
            snapshot.prompt?.let { prompt -> PromptCard(holder, prompt) }
            if (snapshot.mouseAware) {
                Text(
                    "Mouse-aware terminal UI detected. Contiguous semantic rows can receive native taps; RAW remains available for unmodeled gestures.",
                    color = statusColor(StatusKind.Muted), fontSize = 11.sp
                )
            }
        }
    }

    if (confirmClose) {
        CloseProgramConfirm(holder, onDismiss = { confirmClose = false }, onExit = onExit)
    }
}

/**
 * Why a native tap did not take, in words, plus what the user can do about it. [reason] is the
 * controller's own explanation when it gave one.
 */
internal fun interactionFailureMessage(reason: String?): String {
    val detail = reason?.trim()?.takeIf { it.isNotEmpty() }
    val cause = if (detail != null) "That didn't reach the program ($detail)." else "That didn't reach the program."
    return "$cause The raw terminal can send the exact keystrokes."
}

/** Title + status line on top; CLOSE at the far left and RAW/INTR at the right, never adjacent. */
@Composable
private fun WrapperHeader(
    holder: FullScreenPtySession,
    snapshot: TuiSnapshot,
    onRawTerminal: () -> Unit,
    onRequestClose: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(
            snapshot.title ?: holder.commandLine,
            color = Azphalt.currentGround.onPage,
            style = LocalAzphaltType.current.heading,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.headingRole()
        )
        Text(
            "LIVE TERMINAL INTERFACE · ${snapshot.profile.name}",
            color = statusColor(StatusKind.Muted),
            style = LocalAzphaltType.current.eyebrow
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AzphaltChip(
                label = "CLOSE",
                onClick = onRequestClose,
                tint = Azphalt.hues[DESTRUCTIVE_HUE],
                contentDescription = "Close program"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AzphaltChip("RAW", onRawTerminal, contentDescription = "Switch to raw terminal")
                AzphaltChip("INTR", { holder.sendInterrupt() }, contentDescription = "Send interrupt")
            }
        }
    }
}

/**
 * Shown when a native interaction failed. A live region (announced without moving focus), it says
 * what happened and offers the raw terminal as a choice rather than switching to it.
 */
@Composable
private fun InteractionBanner(message: String, onUseRaw: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(Azphalt.Ink.copy(alpha = .09f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .liveRegionPolite()
    ) {
        Text(message, color = Azphalt.currentGround.onPage, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AzphaltChip("USE RAW TERMINAL", onUseRaw)
            AzphaltChip("DISMISS", onDismiss)
        }
    }
}

@Composable
private fun TabStrip(tabs: List<TuiTab>, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            AzphaltChip(
                label = tab.label,
                onClick = { if (!tab.active) onPick(index) },
                selected = tab.active,
                role = Role.Tab
            )
        }
    }
}

@Composable
private fun RegionCard(
    region: TuiRegion,
    mouseAware: Boolean,
    onMouseRow: (row: Int, column: Int) -> Unit
) {
    val contiguous = region.endRow - region.startRow + 1 == region.lines.size
    val muted = statusColor(StatusKind.Muted)
    Column(Modifier.fillMaxWidth().background(Azphalt.Ink.copy(alpha = .07f), RoundedCornerShape(20.dp)).padding(12.dp)) {
        Text(region.kind.name, color = muted, fontWeight = FontWeight.Black, style = LocalAzphaltType.current.eyebrow)
        region.lines.forEachIndexed { index, line ->
            val firstContentColumn = line.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
            Text(
                line,
                color = Azphalt.currentGround.onPage,
                fontSize = 11.sp,
                modifier = if (mouseAware && contiguous) {
                    Modifier.fillMaxWidth().clickable { onMouseRow(region.startRow + index, firstContentColumn) }
                } else {
                    Modifier.fillMaxWidth()
                }
            )
        }
        region.progress?.let { progress ->
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}

@Composable
private fun MenuLayer(layer: TuiLayer, layerIndex: Int, inputOwner: Boolean, onChoose: (Int) -> Unit) {
    val muted = statusColor(StatusKind.Muted)
    val warning = statusColor(StatusKind.Warning)
    val eyebrow = LocalAzphaltType.current.eyebrow
    Column(
        Modifier.fillMaxWidth().padding(start = (layer.depth * 12).dp)
            .background(Azphalt.Ink.copy(alpha = if (layer.modal) .14f else .07f), RoundedCornerShape(if (layer.modal) 20.dp else 24.dp))
            .padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                layer.heading ?: "MENU ${layerIndex + 1}",
                modifier = Modifier.weight(1f),
                color = if (inputOwner) Azphalt.currentGround.onPage else muted,
                style = eyebrow
            )
            if (layer.modal) Text("MODAL", color = if (inputOwner) warning else muted, style = eyebrow)
            if (layer.scrollable) Text(" SCROLL", color = muted, style = eyebrow)
        }
        layer.items.forEachIndexed { index, item ->
            val active = layer.activeIndex == index
            val highlighted = active && inputOwner
            val prefix = when (item.control) {
                TuiControlKind.CHECKBOX -> if (item.checked == true) "☑ " else "☐ "
                TuiControlKind.RADIO -> if (item.checked == true) "◉ " else "○ "
                TuiControlKind.ACTION -> ""
            }
            val itemRole = when (item.control) {
                TuiControlKind.CHECKBOX -> Role.Checkbox
                TuiControlKind.RADIO -> Role.RadioButton
                TuiControlKind.ACTION -> Role.Button
            }
            val textColor = when {
                highlighted -> Azphalt.Yellow
                inputOwner -> Azphalt.currentGround.onPage
                else -> muted
            }
            Box(
                Modifier.fillMaxWidth().background(if (highlighted) Azphalt.Ink else Azphalt.Ink.copy(alpha = .08f), RoundedCornerShape(18.dp))
                    .clickable(enabled = item.enabled && layer.activeIndex != null && inputOwner, role = itemRole) { onChoose(index) }
                    .semantics {
                        if (item.checked != null) stateDescription = if (item.checked == true) "checked" else "not checked"
                        if (highlighted) selected = true
                    }
                    .heightIn(min = minTouchSize())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The ballot glyphs are decoration for the eye; the state is spoken via the
                    // role and stateDescription above, so keep TalkBack from reading their names.
                    if (prefix.isNotEmpty()) {
                        Text(
                            prefix,
                            color = textColor,
                            fontSize = 14.sp,
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                    }
                    Text(
                        item.label,
                        color = textColor,
                        fontWeight = if (highlighted) FontWeight.Black else FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        if (layer.activeIndex == null) {
            Text("Selection state is not observable; use RAW for this layer.", color = muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PromptCard(holder: FullScreenPtySession, prompt: TuiPrompt) {
    if (prompt.kind == TuiPromptKind.CONFIRMATION) {
        Column(Modifier.fillMaxWidth().background(Azphalt.Ink.copy(alpha = .08f), RoundedCornerShape(22.dp)).padding(14.dp)) {
            Text(prompt.label, color = Azphalt.currentGround.onPage, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(Modifier.align(Alignment.End).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AzphaltChip("NO", { TuiInteractionController.confirm(holder, false) }, contentDescription = "Answer no")
                AzphaltChip("YES", { TuiInteractionController.confirm(holder, true) }, contentDescription = "Answer yes")
            }
        }
        return
    }
    var value by remember(prompt.row) { mutableStateOf("") }
    val onPage = Azphalt.currentGround.onPage
    fun submit() {
        holder.sendText(value)
        sendKey(holder, NativeKeyEvent.KEYCODE_ENTER)
        value = ""
    }
    Column(Modifier.fillMaxWidth().background(Azphalt.Ink.copy(alpha = .08f), RoundedCornerShape(22.dp)).padding(14.dp)) {
        Text(prompt.label, color = onPage, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        // Material's default field colours come from the theme's onSurface (Yellow), which is the
        // page colour on the Mustard ground - say what the text and outline are instead.
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            visualTransformation = if (prompt.kind == TuiPromptKind.PASSWORD) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (prompt.kind == TuiPromptKind.PASSWORD) KeyboardType.Password else KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = onPage,
                unfocusedTextColor = onPage,
                cursorColor = onPage,
                focusedBorderColor = onPage,
                unfocusedBorderColor = onPage.copy(alpha = .6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .semantics { contentDescription = prompt.label }
        )
        AzphaltChip(
            label = "SUBMIT",
            onClick = { submit() },
            modifier = Modifier.align(Alignment.End).padding(top = 6.dp)
        )
    }
}

private fun sendKey(holder: FullScreenPtySession, keyCode: Int) {
    val code = KeyHandler.getCode(keyCode, 0, holder.cursorApplicationMode, holder.keypadApplicationMode)
    if (code != null) holder.sendText(code)
}
