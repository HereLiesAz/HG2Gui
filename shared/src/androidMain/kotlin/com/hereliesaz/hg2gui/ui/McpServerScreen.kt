package com.hereliesaz.hg2gui.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PersistableBundle
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import hg2gui.shared.generated.resources.Res
import hg2gui.shared.generated.resources.mcp_connect_copied
import hg2gui.shared.generated.resources.mcp_connect_copy
import hg2gui.shared.generated.resources.mcp_connect_copy_description
import hg2gui.shared.generated.resources.mcp_connect_description
import hg2gui.shared.generated.resources.mcp_connect_title
import hg2gui.shared.generated.resources.mcp_notifications_denied
import hg2gui.shared.generated.resources.mcp_notifications_open_settings
import hg2gui.shared.generated.resources.mcp_option_off
import hg2gui.shared.generated.resources.mcp_option_on
import hg2gui.shared.generated.resources.mcp_server_description_off
import hg2gui.shared.generated.resources.mcp_server_description_on
import hg2gui.shared.generated.resources.mcp_server_title
import hg2gui.shared.generated.resources.mcp_shell_description_off
import hg2gui.shared.generated.resources.mcp_shell_description_on
import hg2gui.shared.generated.resources.mcp_shell_title
import hg2gui.shared.generated.resources.mcp_title
import hg2gui.shared.generated.resources.mcp_token_copied
import hg2gui.shared.generated.resources.mcp_token_copy
import hg2gui.shared.generated.resources.mcp_token_copy_description
import hg2gui.shared.generated.resources.mcp_token_description
import hg2gui.shared.generated.resources.mcp_token_hidden
import hg2gui.shared.generated.resources.mcp_token_hidden_description
import hg2gui.shared.generated.resources.mcp_token_hide
import hg2gui.shared.generated.resources.mcp_token_show
import hg2gui.shared.generated.resources.mcp_token_title
import hg2gui.shared.generated.resources.mcp_token_value_description
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** How long the "copied" confirmation stays on screen. */
private const val COPIED_NOTICE_MS = 3_000L

/** The extras key Android 13+ reads to keep a clip out of clipboard previews. */
private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

private enum class Copied { Token, Connect }

/**
 * Server on/off, the shell-exec toggle (biometric-gated on enable - the actual BiometricPrompt
 * call happens one layer up, in TerminalActivity, which owns the FragmentActivity context; this
 * screen only ever asks for it via [onRequestShellExec] and shows whatever the caller decides),
 * and a tap-to-reveal, copyable pairing token with the matching `adb forward` line. Reached from
 * Settings, not the terminal's own pill stack - this is device configuration, not something to run.
 *
 * [notificationsDenied] is set by the caller when the server could not start because Android's
 * notification permission was refused (the foreground-service notification is the only visible
 * sign a shell is exposed); the screen then explains why nothing started and offers the system
 * settings page instead of leaving the ON toggle silently inert.
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
    onBack: () -> Unit,
    /** True when starting was blocked by a denied POST_NOTIFICATIONS permission. */
    notificationsDenied: Boolean = false
) {
    val context = LocalContext.current
    var tokenRevealed by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf<Copied?>(null) }
    val connectCommand = "adb forward tcp:$port tcp:$port"

    // The confirmation is transient; clearing it also lets the same copy be announced again.
    LaunchedEffect(copied) {
        if (copied != null) {
            delay(COPIED_NOTICE_MS)
            copied = null
        }
    }

    SettingsPage(title = stringResource(Res.string.mcp_title), fullscreen = fullscreen, onBack = onBack) {
        val serverTitle = stringResource(Res.string.mcp_server_title)
        SettingRow(
            title = serverTitle,
            // MCP-7: loopback-only means no other device on the network can reach this - it does
            // not mean no other app can. Any app on this device with plain INTERNET permission
            // can open the same socket; the token below is what actually stands between it and
            // this app's sandboxed files, not the network boundary.
            description = if (running)
                stringResource(Res.string.mcp_server_description_on, port.toString())
            else
                stringResource(Res.string.mcp_server_description_off)
        ) {
            SegmentedToggle(
                groupLabel = serverTitle,
                leftLabel = stringResource(Res.string.mcp_option_off),
                rightLabel = stringResource(Res.string.mcp_option_on),
                rightSelected = running
            ) { wantOn ->
                if (wantOn) onStart() else onStop()
            }
            if (notificationsDenied && !running) {
                Text(
                    stringResource(Res.string.mcp_notifications_denied),
                    color = statusColor(StatusKind.Error),
                    style = settingDescriptionStyle(),
                    modifier = Modifier.padding(top = 8.dp).liveRegionPolite()
                )
                AzphaltChip(
                    label = stringResource(Res.string.mcp_notifications_open_settings),
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                            )
                        } catch (_: android.content.ActivityNotFoundException) {
                            // No settings app to open; the explanation above still stands.
                        }
                    }
                )
            }
        }

        if (running) {
            SettingRow(
                title = stringResource(Res.string.mcp_token_title),
                description = stringResource(Res.string.mcp_token_description)
            ) {
                val tokenText = token.orEmpty()
                CodeCapsule(
                    text = if (tokenRevealed) tokenText else stringResource(Res.string.mcp_token_hidden),
                    description = if (tokenRevealed) {
                        stringResource(Res.string.mcp_token_value_description, tokenText)
                    } else {
                        stringResource(Res.string.mcp_token_hidden_description)
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AzphaltChip(
                        label = stringResource(if (tokenRevealed) Res.string.mcp_token_hide else Res.string.mcp_token_show),
                        onClick = { tokenRevealed = !tokenRevealed }
                    )
                    AzphaltChip(
                        label = stringResource(Res.string.mcp_token_copy),
                        onClick = {
                            copyToClipboard(context, "HG2Gui MCP pairing token", tokenText, sensitive = true)
                            copied = Copied.Token
                        },
                        enabled = tokenText.isNotEmpty(),
                        contentDescription = stringResource(Res.string.mcp_token_copy_description)
                    )
                }
                CopiedNotice(
                    when (copied) {
                        Copied.Token -> stringResource(Res.string.mcp_token_copied)
                        Copied.Connect -> stringResource(Res.string.mcp_connect_copied)
                        null -> null
                    }
                )
            }

            SettingRow(
                title = stringResource(Res.string.mcp_connect_title),
                description = stringResource(Res.string.mcp_connect_description)
            ) {
                CodeCapsule(text = connectCommand, description = connectCommand)
                AzphaltChip(
                    label = stringResource(Res.string.mcp_connect_copy),
                    onClick = {
                        copyToClipboard(context, "HG2Gui MCP adb forward", connectCommand, sensitive = false)
                        copied = Copied.Connect
                    },
                    contentDescription = stringResource(Res.string.mcp_connect_copy_description)
                )
            }

            val shellTitle = stringResource(Res.string.mcp_shell_title)
            SettingRow(
                title = shellTitle,
                description = if (shellExecEnabled)
                    stringResource(Res.string.mcp_shell_description_on)
                else
                    stringResource(Res.string.mcp_shell_description_off)
            ) {
                SegmentedToggle(
                    groupLabel = shellTitle,
                    leftLabel = stringResource(Res.string.mcp_option_off),
                    rightLabel = stringResource(Res.string.mcp_option_on),
                    rightSelected = shellExecEnabled
                ) { wantOn ->
                    if (wantOn) onRequestShellExec() else onDisableShellExec()
                }
            }
        }
    }
}

/** A monospace value on an ink panel (a token, a command line). [description] is what a screen reader speaks. */
@Composable
private fun CodeCapsule(text: String, description: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(AzphaltSurface.note)
            .background(Azphalt.Ink)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = description }
    ) {
        Text(
            text,
            color = Azphalt.Yellow,
            style = settingDescriptionStyle().copy(fontFamily = FontFamily.Monospace)
        )
    }
}

/** The transient "copied" line; empty when [message] is null. A polite live region so it is announced. */
@Composable
private fun CopiedNotice(message: String?) {
    Box(Modifier.liveRegionPolite()) {
        if (message != null) {
            Text(
                message,
                color = statusColor(StatusKind.Success),
                style = settingDescriptionStyle(),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/**
 * Puts [text] on the clipboard. [sensitive] asks Android 13+ not to show the value in the
 * clipboard-preview overlay, which matters for the pairing token.
 */
private fun copyToClipboard(context: Context, label: String, text: String, sensitive: Boolean) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    val clip = ClipData.newPlainText(label, text)
    if (sensitive) {
        clip.description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
    }
    clipboard.setPrimaryClip(clip)
}
