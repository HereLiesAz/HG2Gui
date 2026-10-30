package com.hereliesaz.hg2gui.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.managers.UpdateCheckPreference
import com.hereliesaz.hg2gui.terminal.ShellPreference
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType
import hg2gui.shared.generated.resources.Res
import hg2gui.shared.generated.resources.settings_about_description
import hg2gui.shared.generated.resources.settings_about_licences
import hg2gui.shared.generated.resources.settings_about_licences_description
import hg2gui.shared.generated.resources.settings_about_privacy
import hg2gui.shared.generated.resources.settings_about_privacy_description
import hg2gui.shared.generated.resources.settings_about_title
import hg2gui.shared.generated.resources.settings_ai_button
import hg2gui.shared.generated.resources.settings_ai_button_description
import hg2gui.shared.generated.resources.settings_ai_description
import hg2gui.shared.generated.resources.settings_ai_title
import hg2gui.shared.generated.resources.settings_back
import hg2gui.shared.generated.resources.settings_back_description
import hg2gui.shared.generated.resources.settings_developer_button
import hg2gui.shared.generated.resources.settings_developer_button_description
import hg2gui.shared.generated.resources.settings_developer_description
import hg2gui.shared.generated.resources.settings_developer_title
import hg2gui.shared.generated.resources.settings_environment_button
import hg2gui.shared.generated.resources.settings_environment_button_description
import hg2gui.shared.generated.resources.settings_environment_description
import hg2gui.shared.generated.resources.settings_environment_title
import hg2gui.shared.generated.resources.settings_fullscreen_description_off
import hg2gui.shared.generated.resources.settings_fullscreen_description_on
import hg2gui.shared.generated.resources.settings_fullscreen_option_fullscreen
import hg2gui.shared.generated.resources.settings_fullscreen_option_safe_area
import hg2gui.shared.generated.resources.settings_fullscreen_title
import hg2gui.shared.generated.resources.settings_history_button
import hg2gui.shared.generated.resources.settings_history_button_description
import hg2gui.shared.generated.resources.settings_history_description
import hg2gui.shared.generated.resources.settings_history_title
import hg2gui.shared.generated.resources.settings_isolation_button
import hg2gui.shared.generated.resources.settings_isolation_button_description
import hg2gui.shared.generated.resources.settings_isolation_description
import hg2gui.shared.generated.resources.settings_isolation_title
import hg2gui.shared.generated.resources.settings_motion_button
import hg2gui.shared.generated.resources.settings_motion_description_off
import hg2gui.shared.generated.resources.settings_motion_description_on
import hg2gui.shared.generated.resources.settings_motion_title
import hg2gui.shared.generated.resources.settings_option_off
import hg2gui.shared.generated.resources.settings_option_on
import hg2gui.shared.generated.resources.settings_pty_description
import hg2gui.shared.generated.resources.settings_pty_option_pipe
import hg2gui.shared.generated.resources.settings_pty_option_pty
import hg2gui.shared.generated.resources.settings_pty_title
import hg2gui.shared.generated.resources.settings_shell_description
import hg2gui.shared.generated.resources.settings_shell_none
import hg2gui.shared.generated.resources.settings_shell_title
import hg2gui.shared.generated.resources.settings_text_size_decrease
import hg2gui.shared.generated.resources.settings_text_size_description
import hg2gui.shared.generated.resources.settings_text_size_increase
import hg2gui.shared.generated.resources.settings_text_size_reset
import hg2gui.shared.generated.resources.settings_text_size_title
import hg2gui.shared.generated.resources.settings_title
import hg2gui.shared.generated.resources.settings_updates_description_off
import hg2gui.shared.generated.resources.settings_updates_description_on
import hg2gui.shared.generated.resources.settings_updates_title
import org.jetbrains.compose.resources.stringResource

/**
 * Bounds of the Text-size setting. The floor is 85%: below that the pill labels and chip text
 * drop under 10sp on the glass and the touch targets shrink with them, which the old 50% floor
 * allowed with no way back short of finding the stepper again. A stored value from before the
 * change is pulled up to the floor when Settings opens (see [clampFontScalePercent]).
 */
const val FONT_SCALE_MIN_PERCENT = 85
const val FONT_SCALE_MAX_PERCENT = 200
const val FONT_SCALE_DEFAULT_PERCENT = 100
private const val FONT_SCALE_STEP_PERCENT = 10

private const val PRIVACY_POLICY_URL = "https://github.com/HereLiesAz/HG2Gui/blob/HEAD/docs/PRIVACY_POLICY.md"
private const val SOURCE_URL = "https://github.com/HereLiesAz/HG2Gui"

/**
 * [percent] forced into [FONT_SCALE_MIN_PERCENT]..[FONT_SCALE_MAX_PERCENT]. Public so the Activity
 * can apply it to the stored value when it reads the preference, before Settings is ever opened.
 */
fun clampFontScalePercent(percent: Int): Int = percent.coerceIn(FONT_SCALE_MIN_PERCENT, FONT_SCALE_MAX_PERCENT)

/** One stepper press: [direction] is -1 or +1 step; the result is always inside the allowed range. */
internal fun steppedFontScalePercent(current: Int, direction: Int): Int =
    clampFontScalePercent(current + direction * FONT_SCALE_STEP_PERCENT)

@Composable
fun SettingsScreen(
    fullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    fontScalePercent: Int,
    onFontScalePercentChange: (Int) -> Unit,
    usePty: Boolean,
    onUsePtyChange: (Boolean) -> Unit,
    onOpenMcpServer: () -> Unit,
    onOpenAiSettings: () -> Unit,
    onOpenEnvironment: () -> Unit,
    onOpenHistory: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val availableShells = remember { ShellPreference.available(context) }
    var storedShell by remember { mutableStateOf(ShellPreference.selected(context)) }
    val shownShell = ShellPreference.effectiveSelection(storedShell, availableShells)
    var autoUpdateCheck by remember { mutableStateOf(UpdateCheckPreference.isEnabled(context)) }
    val versionName = remember { appVersionName(context) }

    // A value saved before the 85% floor existed (down to 50%) would leave the UI at an unusable
    // size with the "-" button disabled; lift it to the floor as soon as Settings is shown.
    LaunchedEffect(fontScalePercent) {
        val clamped = clampFontScalePercent(fontScalePercent)
        if (clamped != fontScalePercent) onFontScalePercentChange(clamped)
    }

    SettingsPage(title = stringResource(Res.string.settings_title), fullscreen = fullscreen, onBack = onBack) {
        val fullscreenTitle = stringResource(Res.string.settings_fullscreen_title)
        SettingRow(
            title = fullscreenTitle,
            description = if (fullscreen)
                stringResource(Res.string.settings_fullscreen_description_on)
            else
                stringResource(Res.string.settings_fullscreen_description_off)
        ) {
            SegmentedToggle(
                groupLabel = fullscreenTitle,
                leftLabel = stringResource(Res.string.settings_fullscreen_option_safe_area),
                rightLabel = stringResource(Res.string.settings_fullscreen_option_fullscreen),
                rightSelected = fullscreen,
                onSelectRight = onFullscreenChange
            )
        }

        SettingRow(
            title = stringResource(Res.string.settings_text_size_title),
            description = stringResource(Res.string.settings_text_size_description)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Stepper(
                    value = fontScalePercent,
                    min = FONT_SCALE_MIN_PERCENT,
                    max = FONT_SCALE_MAX_PERCENT,
                    label = { "$it%" },
                    decreaseDescription = stringResource(Res.string.settings_text_size_decrease),
                    increaseDescription = stringResource(Res.string.settings_text_size_increase),
                    onChange = { direction -> onFontScalePercentChange(steppedFontScalePercent(fontScalePercent, direction)) }
                )
                if (fontScalePercent != FONT_SCALE_DEFAULT_PERCENT) {
                    AzphaltChip(
                        label = stringResource(Res.string.settings_text_size_reset),
                        onClick = { onFontScalePercentChange(FONT_SCALE_DEFAULT_PERCENT) }
                    )
                }
            }
        }

        val ptyTitle = stringResource(Res.string.settings_pty_title)
        SettingRow(title = ptyTitle, description = stringResource(Res.string.settings_pty_description)) {
            SegmentedToggle(
                groupLabel = ptyTitle,
                leftLabel = stringResource(Res.string.settings_pty_option_pipe),
                rightLabel = stringResource(Res.string.settings_pty_option_pty),
                rightSelected = usePty,
                onSelectRight = onUsePtyChange
            )
        }

        val shellTitle = stringResource(Res.string.settings_shell_title)
        SettingRow(title = shellTitle, description = stringResource(Res.string.settings_shell_description)) {
            if (availableShells.isEmpty()) {
                // No shell is executable: say so rather than offering one that cannot start.
                Text(
                    stringResource(Res.string.settings_shell_none),
                    color = statusColor(StatusKind.Warning),
                    style = settingDescriptionStyle(),
                    modifier = Modifier.liveRegionPolite()
                )
            } else {
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    availableShells.forEach { shell ->
                        AzphaltChip(
                            label = shell,
                            onClick = {
                                ShellPreference.setSelected(context, shell)
                                storedShell = shell
                            },
                            selected = shell == shownShell,
                            role = Role.RadioButton,
                            contentDescription = "$shellTitle: $shell"
                        )
                    }
                }
            }
        }

        SettingRow(
            title = stringResource(Res.string.settings_environment_title),
            description = stringResource(Res.string.settings_environment_description)
        ) {
            AzphaltChip(
                label = stringResource(Res.string.settings_environment_button),
                onClick = onOpenEnvironment,
                contentDescription = stringResource(Res.string.settings_environment_button_description)
            )
        }

        SettingRow(
            title = stringResource(Res.string.settings_history_title),
            description = stringResource(Res.string.settings_history_description)
        ) {
            AzphaltChip(
                label = stringResource(Res.string.settings_history_button),
                onClick = onOpenHistory,
                contentDescription = stringResource(Res.string.settings_history_button_description)
            )
        }

        SettingRow(
            title = stringResource(Res.string.settings_isolation_title),
            description = stringResource(Res.string.settings_isolation_description)
        ) {
            AzphaltChip(
                label = stringResource(Res.string.settings_isolation_button),
                onClick = {
                    context.startActivity(
                        Intent().setClassName(
                            context.packageName,
                            "${context.packageName}.IsolationAuditActivity"
                        )
                    )
                },
                contentDescription = stringResource(Res.string.settings_isolation_button_description)
            )
        }

        SettingRow(
            title = stringResource(Res.string.settings_ai_title),
            description = stringResource(Res.string.settings_ai_description)
        ) {
            AzphaltChip(
                label = stringResource(Res.string.settings_ai_button),
                onClick = onOpenAiSettings,
                contentDescription = stringResource(Res.string.settings_ai_button_description)
            )
        }

        SettingRow(
            title = stringResource(Res.string.settings_developer_title),
            description = stringResource(Res.string.settings_developer_description)
        ) {
            AzphaltChip(
                label = stringResource(Res.string.settings_developer_button),
                onClick = onOpenMcpServer,
                contentDescription = stringResource(Res.string.settings_developer_button_description)
            )
        }

        val updatesTitle = stringResource(Res.string.settings_updates_title)
        SettingRow(
            title = updatesTitle,
            description = if (autoUpdateCheck)
                stringResource(Res.string.settings_updates_description_on)
            else
                stringResource(Res.string.settings_updates_description_off)
        ) {
            SegmentedToggle(
                groupLabel = updatesTitle,
                leftLabel = stringResource(Res.string.settings_option_off),
                rightLabel = stringResource(Res.string.settings_option_on),
                rightSelected = autoUpdateCheck,
                onSelectRight = { enabled ->
                    autoUpdateCheck = enabled
                    UpdateCheckPreference.setEnabled(context, enabled)
                }
            )
        }

        // HG2Gui has no in-app motion switch of its own: it follows Android's animator scale
        // (see AzphaltMotion), so this row reports the state and points at the system setting.
        SettingRow(
            title = stringResource(Res.string.settings_motion_title),
            description = if (rememberReducedMotion())
                stringResource(Res.string.settings_motion_description_off)
            else
                stringResource(Res.string.settings_motion_description_on)
        ) {
            AzphaltChip(
                label = stringResource(Res.string.settings_motion_button),
                onClick = { startActivitySafely(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
        }

        SettingRow(
            title = stringResource(Res.string.settings_about_title),
            description = stringResource(Res.string.settings_about_description, versionName)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AzphaltChip(
                    label = stringResource(Res.string.settings_about_privacy),
                    onClick = { startActivitySafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) },
                    contentDescription = stringResource(Res.string.settings_about_privacy_description)
                )
                AzphaltChip(
                    label = stringResource(Res.string.settings_about_licences),
                    onClick = { startActivitySafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL))) },
                    contentDescription = stringResource(Res.string.settings_about_licences_description)
                )
            }
        }
    }
}

/** The installed versionName, or "" when it cannot be read. */
@Suppress("DEPRECATION")
private fun appVersionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull()
        .orEmpty()

/** Starts [intent], doing nothing when no installed app can handle it (no browser, no settings app). */
private fun startActivitySafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Nothing can open it on this device; there is no better fallback than staying put.
    }
}

/**
 * The frame every settings-style screen shares (Settings, AI settings, MCP server): the ground
 * page, system-bar / cutout / keyboard insets, a column capped at a readable width, a back chip
 * with a real 48dp hit box, an [AzphaltHeading] title and a vertical scroll. Scrolling is part of
 * the contract - at a large Text-size setting, in landscape, or with the keyboard up, the last row
 * must stay reachable.
 */
@Composable
internal fun SettingsPage(
    title: String,
    fullscreen: Boolean,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .screenInsets(fullscreen)
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .maxContentWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AzphaltChip(
                    label = stringResource(Res.string.settings_back),
                    onClick = onBack,
                    contentDescription = stringResource(Res.string.settings_back_description)
                )
            }
            AzphaltHeading(title, Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp))
            content()
        }
    }
}

/** Body text of a setting's explanation: the type scale's body face at caption size. */
@Composable
internal fun settingDescriptionStyle(): TextStyle =
    LocalAzphaltType.current.body.copy(fontSize = 13.sp, lineHeight = 18.sp)

@Composable
internal fun SettingRow(title: String, description: String, control: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            title.uppercase(),
            color = Azphalt.currentGround.onPage,
            style = LocalAzphaltType.current.capsule,
            modifier = Modifier.headingRole()
        )
        Text(
            description,
            color = statusColor(StatusKind.Muted),
            style = settingDescriptionStyle(),
            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
        )
        control()
    }
}

/**
 * Two mutually exclusive options as radio chips. The chosen one is filled and the other outlined
 * (shape and weight, not hue alone), and each is announced as "<group>, <option>, radio button,
 * selected / not selected".
 */
@Composable
internal fun SegmentedToggle(
    groupLabel: String,
    leftLabel: String,
    rightLabel: String,
    rightSelected: Boolean,
    onSelectRight: (Boolean) -> Unit
) {
    Row(
        Modifier.selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(leftLabel to false, rightLabel to true).forEach { (label, value) ->
            AzphaltChip(
                label = label,
                onClick = { onSelectRight(value) },
                selected = value == rightSelected,
                role = Role.RadioButton,
                contentDescription = "$groupLabel: $label"
            )
        }
    }
}

/**
 * "-" [value] "+" with spoken labels for both glyph buttons. [onChange] receives the direction
 * (-1 or +1); the caller applies its own step and range. The value is a polite live region, so a
 * screen reader announces each new percentage.
 */
@Composable
private fun Stepper(
    value: Int,
    min: Int,
    max: Int,
    label: (Int) -> String,
    decreaseDescription: String,
    increaseDescription: String,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        AzphaltChip(
            label = "−",
            onClick = { onChange(-1) },
            enabled = value > min,
            contentDescription = decreaseDescription
        )
        Box(
            Modifier
                .liveRegionPolite()
                .clip(AzphaltSurface.capsule)
                .background(Azphalt.Ink)
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            Text(label(value), color = Azphalt.Yellow, style = LocalAzphaltType.current.chip)
        }
        AzphaltChip(
            label = "+",
            onClick = { onChange(1) },
            enabled = value < max,
            contentDescription = increaseDescription
        )
    }
}
