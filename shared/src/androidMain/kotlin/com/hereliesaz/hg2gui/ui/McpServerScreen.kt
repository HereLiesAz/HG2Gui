package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.pageBrush

/**
 * Server on/off, the shell-exec toggle (biometric-gated on enable - the actual BiometricPrompt
 * call happens one layer up, in TerminalActivity, which owns the FragmentActivity context; this
 * screen only ever asks for it via [onRequestShellExec] and shows whatever the caller decides),
 * and a tap-to-reveal pairing token. Reached from Settings, not the terminal's own pill stack -
 * this is device configuration, not something to run.
 */
@Composable
fun McpServerScreen(
    fullscreen: Boolean,
    running: Boolean,
    port: Int,
    token: String?,
    shellExecEnabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestShellExec: () -> Unit,
    onDisableShellExec: () -> Unit,
    onBack: () -> Unit
) {
    var tokenRevealed by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .then(if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    "‹ BACK", color = Azphalt.Yellow,
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
        }

        Eyebrow("MCP server")

        McpSettingRow(
            title = "Server",
            // MCP-7: loopback-only means no other device on the network can reach this - it does
            // not mean no other app can. Any app on this device with plain INTERNET permission
            // can open the same socket; the token below is what actually stands between it and
            // this app's sandboxed files, not the network boundary.
            description = if (running)
                "Running on 127.0.0.1:$port, reachable by any app on this device (not just over " +
                    "the network) — the token below is what actually gates it. Present it once to pair."
            else
                "Off. Starting it opens a loopback socket any app on this device can connect to " +
                    "(not just via adb forward) to read/write this app's sandboxed files, gated by a " +
                    "pairing token."
        ) {
            McpToggle(leftLabel = "OFF", rightLabel = "ON", rightSelected = running) { wantOn ->
                if (wantOn) onStart() else onStop()
            }
        }

        if (running) {
            McpSettingRow(
                title = "Pairing token",
                description = "Required on every connection. Regenerated fresh each time the " +
                    "server starts — never shown to anyone but you."
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Azphalt.Ink)
                        .clickable { tokenRevealed = !tokenRevealed }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        if (tokenRevealed) (token ?: "") else "•••• tap to reveal",
                        color = Azphalt.Yellow, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                }
            }

            McpSettingRow(
                title = "Connect",
                description = "adb forward tcp:$port tcp:$port"
            ) {}

            McpSettingRow(
                title = "Shell execution",
                description = if (shellExecEnabled)
                    "Enabled — a paired agent can run real shell commands, on a session " +
                        "separate from your own terminal tabs. Confirmed by biometric unlock."
                else
                    "Disabled by default. Enabling it requires a biometric confirmation — this " +
                        "is the one switch in this screen that lets a connected agent run " +
                        "arbitrary commands, not just read/write sandboxed files."
            ) {
                McpToggle(leftLabel = "OFF", rightLabel = "ON", rightSelected = shellExecEnabled) { wantOn ->
                    if (wantOn) onRequestShellExec() else onDisableShellExec()
                }
            }
        }
    }
}

@Composable
private fun McpSettingRow(title: String, description: String, control: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            title.uppercase(), color = Azphalt.Ink,
            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
        )
        Text(
            // .55f, the style guide's own "text-muted" token - not .6f, which no other caption
            // in the app uses for this role.
            description, color = Azphalt.Ink.copy(alpha = .55f),
            fontSize = 11.sp, lineHeight = 15.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )
        control()
    }
}

@Composable
private fun McpToggle(
    leftLabel: String,
    rightLabel: String,
    rightSelected: Boolean,
    onSelectRight: (Boolean) -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Azphalt.Ink.copy(alpha = .14f))
            .padding(3.dp)
    ) {
        listOf(leftLabel to false, rightLabel to true).forEach { (label, selected) ->
            val on = selected == rightSelected
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (on) Azphalt.Ink else Color.Transparent)
                    .clickable { onSelectRight(selected) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    label, color = if (on) Azphalt.Yellow else Azphalt.Ink.copy(alpha = .55f),
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
        }
    }
}
