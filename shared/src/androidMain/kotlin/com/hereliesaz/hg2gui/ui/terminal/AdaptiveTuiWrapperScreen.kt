package com.hereliesaz.hg2gui.ui.terminal

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.terminal.FullScreenPtySession
import com.hereliesaz.hg2gui.terminal.TuiLayer
import com.hereliesaz.hg2gui.terminal.TuiPromptKind
import com.hereliesaz.hg2gui.terminal.TuiSnapshot
import com.hereliesaz.hg2gui.terminal.TuiTerminalAdapter
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.termux.terminal.KeyHandler

/**
 * Native projection of an already-running terminal UI. The child process remains authoritative:
 * every tap is translated back into the same navigation/input bytes the raw TUI expects. If the
 * current screen cannot be modeled confidently, callers should show the raw terminal instead.
 */
@Composable
fun AdaptiveTuiWrapperScreen(
    holder: FullScreenPtySession,
    onRawTerminal: () -> Unit,
    onExit: () -> Unit
) {
    // Reading generation makes this recompute whenever the PTY changes its screen.
    @Suppress("UNUSED_VARIABLE")
    val generation = holder.generation
    val snapshot = TuiTerminalAdapter.snapshot(holder)

    if (snapshot == null || !snapshot.isWrappable) {
        FullScreenTerminalScreen(holder = holder, onExit = onExit, forceRaw = true)
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.page)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        WrapperHeader(holder, snapshot, onRawTerminal, onExit)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            snapshot.layers.forEachIndexed { index, layer ->
                MenuLayer(holder, layer, index)
            }
            snapshot.prompt?.let { prompt ->
                var value by remember(prompt.row) { mutableStateOf("") }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Azphalt.Ink.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        prompt.label,
                        color = Azphalt.currentGround.onPage,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        singleLine = true,
                        visualTransformation = if (prompt.kind == TuiPromptKind.PASSWORD) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Text(
                        "SUBMIT",
                        color = Azphalt.Yellow,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 10.dp)
                            .background(Azphalt.Ink, RoundedCornerShape(999.dp))
                            .clickable {
                                holder.sendText(value)
                                sendKey(holder, NativeKeyEvent.KEYCODE_ENTER)
                                value = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WrapperHeader(
    holder: FullScreenPtySession,
    snapshot: TuiSnapshot,
    onRawTerminal: () -> Unit,
    onExit: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                snapshot.title ?: holder.commandLine,
                color = Azphalt.currentGround.onPage,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Text(
                "LIVE TERMINAL INTERFACE",
                color = Azphalt.currentGround.onPage.copy(alpha = 0.55f),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }
        HeaderAction("RAW", onRawTerminal)
        HeaderAction("INTR") { holder.sendInterrupt() }
        HeaderAction("CLOSE") {
            holder.kill()
            onExit()
        }
    }
}

@Composable
private fun HeaderAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Azphalt.Yellow,
        fontWeight = FontWeight.Black,
        fontSize = 9.sp,
        modifier = Modifier
            .padding(start = 8.dp)
            .background(Azphalt.Ink, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun MenuLayer(holder: FullScreenPtySession, layer: TuiLayer, layerIndex: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Azphalt.Ink.copy(alpha = 0.07f), RoundedCornerShape(24.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            layer.heading ?: "MENU ${layerIndex + 1}",
            color = Azphalt.currentGround.onPage.copy(alpha = 0.62f),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp
        )
        layer.items.forEachIndexed { index, item ->
            val active = layer.activeIndex == index
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (active) Azphalt.Ink else Azphalt.Ink.copy(alpha = 0.08f),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(enabled = item.enabled && layer.activeIndex != null) {
                        chooseMenuItem(holder, layer, index)
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    item.label,
                    color = if (active) Azphalt.Yellow else Azphalt.currentGround.onPage,
                    fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
        if (layer.activeIndex == null) {
            Spacer(Modifier.padding(top = 1.dp))
            Text(
                "Selection state not exposed by this screen; use RAW for this layer.",
                color = Azphalt.currentGround.onPage.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}

private fun chooseMenuItem(holder: FullScreenPtySession, layer: TuiLayer, targetIndex: Int) {
    val current = layer.activeIndex ?: return
    val delta = targetIndex - current
    val keyCode = if (delta >= 0) NativeKeyEvent.KEYCODE_DPAD_DOWN else NativeKeyEvent.KEYCODE_DPAD_UP
    repeat(kotlin.math.abs(delta)) { sendKey(holder, keyCode) }
    sendKey(holder, NativeKeyEvent.KEYCODE_ENTER)
}

private fun sendKey(holder: FullScreenPtySession, keyCode: Int) {
    val code = KeyHandler.getCode(
        keyCode,
        0,
        holder.cursorApplicationMode,
        holder.keypadApplicationMode
    )
    if (code != null) holder.sendText(code)
}
