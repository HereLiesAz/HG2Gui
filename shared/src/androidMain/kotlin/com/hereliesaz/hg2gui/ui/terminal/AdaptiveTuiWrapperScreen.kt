package com.hereliesaz.hg2gui.ui.terminal

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.terminal.*
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.termux.terminal.KeyHandler
import kotlinx.coroutines.launch

/** Native projection of an already-running terminal UI. */
@Composable
fun AdaptiveTuiWrapperScreen(
    holder: FullScreenPtySession,
    onRawTerminal: () -> Unit,
    onExit: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE") val generation = holder.generation
    val snapshot = TuiTerminalAdapter.snapshot(holder)
    val scope = rememberCoroutineScope()
    var interactionError by remember { mutableStateOf<String?>(null) }

    if (snapshot == null || !snapshot.isWrappable) {
        FullScreenTerminalScreen(holder = holder, onExit = onExit, forceRaw = true)
        return
    }

    fun handle(result: TuiInteractionController.Result) {
        interactionError = result.reason
        if (!result.success) onRawTerminal()
    }

    Column(
        Modifier.fillMaxSize().background(Azphalt.currentGround.page).padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        WrapperHeader(holder, snapshot, onRawTerminal, onExit)
        interactionError?.let {
            Text(it, color = Azphalt.currentGround.onPage.copy(alpha = .62f), fontSize = 10.sp, modifier = Modifier.padding(bottom = 8.dp))
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
                    color = Azphalt.currentGround.onPage.copy(alpha = .45f), fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun WrapperHeader(holder: FullScreenPtySession, snapshot: TuiSnapshot, onRawTerminal: () -> Unit, onExit: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(snapshot.title ?: holder.commandLine, color = Azphalt.currentGround.onPage, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(
                "LIVE TERMINAL INTERFACE · ${snapshot.profile.name}",
                color = Azphalt.currentGround.onPage.copy(alpha = .55f),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }
        HeaderAction("RAW", onRawTerminal)
        HeaderAction("INTR") { holder.sendInterrupt() }
        HeaderAction("CLOSE") { holder.kill(); onExit() }
    }
}

@Composable
private fun HeaderAction(label: String, onClick: () -> Unit) {
    Text(label, color = Azphalt.Yellow, fontWeight = FontWeight.Black, fontSize = 9.sp,
        modifier = Modifier.padding(start = 8.dp).background(Azphalt.Ink, RoundedCornerShape(999.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp))
}

@Composable
private fun TabStrip(tabs: List<TuiTab>, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tabs.forEachIndexed { index, tab ->
            Text(
                tab.label,
                color = if (tab.active) Azphalt.Yellow else Azphalt.currentGround.onPage,
                fontWeight = if (tab.active) FontWeight.Black else FontWeight.SemiBold,
                modifier = Modifier.background(Azphalt.Ink.copy(alpha = if (tab.active) 1f else .08f), RoundedCornerShape(999.dp))
                    .clickable(enabled = !tab.active) { onPick(index) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 11.sp
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
    Column(Modifier.fillMaxWidth().background(Azphalt.Ink.copy(alpha = .07f), RoundedCornerShape(20.dp)).padding(12.dp)) {
        Text(region.kind.name, color = Azphalt.currentGround.onPage.copy(alpha = .5f), fontWeight = FontWeight.Black, fontSize = 9.sp)
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
    Column(
        Modifier.fillMaxWidth().padding(start = (layer.depth * 12).dp)
            .background(Azphalt.Ink.copy(alpha = if (layer.modal) .14f else .07f), RoundedCornerShape(if (layer.modal) 20.dp else 24.dp))
            .padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(layer.heading ?: "MENU ${layerIndex + 1}", modifier = Modifier.weight(1f), color = Azphalt.currentGround.onPage.copy(alpha = if (inputOwner) .72f else .48f), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
            if (layer.modal) Text("MODAL", color = Azphalt.Yellow.copy(alpha = if (inputOwner) 1f else .55f), fontWeight = FontWeight.Black, fontSize = 8.sp)
            if (layer.scrollable) Text(" SCROLL", color = Azphalt.currentGround.onPage.copy(alpha = .45f), fontSize = 8.sp)
        }
        layer.items.forEachIndexed { index, item ->
            val active = layer.activeIndex == index
            val prefix = when (item.control) {
                TuiControlKind.CHECKBOX -> if (item.checked == true) "☑ " else "☐ "
                TuiControlKind.RADIO -> if (item.checked == true) "◉ " else "○ "
                TuiControlKind.ACTION -> ""
            }
            Box(
                Modifier.fillMaxWidth().background(if (active && inputOwner) Azphalt.Ink else Azphalt.Ink.copy(alpha = .08f), RoundedCornerShape(18.dp))
                    .clickable(enabled = item.enabled && layer.activeIndex != null && inputOwner) { onChoose(index) }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(prefix + item.label,
                    color = if (active && inputOwner) Azphalt.Yellow else if (inputOwner) Azphalt.currentGround.onPage else Azphalt.currentGround.onPage.copy(alpha = .48f),
                    fontWeight = if (active && inputOwner) FontWeight.Black else FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        if (layer.activeIndex == null) Text("Selection state is not observable; use RAW for this layer.", color = Azphalt.currentGround.onPage.copy(alpha = .5f), fontSize = 10.sp)
    }
}

@Composable
private fun PromptCard(holder: FullScreenPtySession, prompt: TuiPrompt) {
    if (prompt.kind == TuiPromptKind.CONFIRMATION) {
        Column(Modifier.fillMaxWidth().background(Azphalt.Ink.copy(alpha = .08f), RoundedCornerShape(22.dp)).padding(14.dp)) {
            Text(prompt.label, color = Azphalt.currentGround.onPage, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(Modifier.align(Alignment.End).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderAction("NO") { TuiInteractionController.confirm(holder, false) }
                HeaderAction("YES") { TuiInteractionController.confirm(holder, true) }
            }
        }
        return
    }
    var value by remember(prompt.row) { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().background(Azphalt.Ink.copy(alpha = .08f), RoundedCornerShape(22.dp)).padding(14.dp)) {
        Text(prompt.label, color = Azphalt.currentGround.onPage, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true,
            visualTransformation = if (prompt.kind == TuiPromptKind.PASSWORD) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Text("SUBMIT", color = Azphalt.Yellow, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.End).padding(top = 10.dp).background(Azphalt.Ink, RoundedCornerShape(999.dp)).clickable {
                holder.sendText(value); sendKey(holder, NativeKeyEvent.KEYCODE_ENTER); value = ""
            }.padding(horizontal = 16.dp, vertical = 9.dp))
    }
}

private fun sendKey(holder: FullScreenPtySession, keyCode: Int) {
    val code = KeyHandler.getCode(keyCode, 0, holder.cursorApplicationMode, holder.keypadApplicationMode)
    if (code != null) holder.sendText(code)
}
