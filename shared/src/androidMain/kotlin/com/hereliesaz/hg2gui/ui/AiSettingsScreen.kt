package com.hereliesaz.hg2gui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import hg2gui.shared.generated.resources.Res
import hg2gui.shared.generated.resources.aisettings_discard_cancel
import hg2gui.shared.generated.resources.aisettings_discard_confirm
import hg2gui.shared.generated.resources.aisettings_discard_message
import hg2gui.shared.generated.resources.aisettings_discard_title
import hg2gui.shared.generated.resources.aisettings_field_description
import hg2gui.shared.generated.resources.aisettings_field_empty
import hg2gui.shared.generated.resources.aisettings_field_masked
import hg2gui.shared.generated.resources.aisettings_field_placeholder
import hg2gui.shared.generated.resources.aisettings_hide_key
import hg2gui.shared.generated.resources.aisettings_key_description
import hg2gui.shared.generated.resources.aisettings_key_title
import hg2gui.shared.generated.resources.aisettings_save
import hg2gui.shared.generated.resources.aisettings_save_failed
import hg2gui.shared.generated.resources.aisettings_show_key
import hg2gui.shared.generated.resources.aisettings_title
import org.jetbrains.compose.resources.stringResource

/** The text to store for [draft]: trimmed (a pasted key often carries a trailing newline), or null when blank. */
internal fun normalizedApiKey(draft: String): String? = draft.trim().ifEmpty { null }

/** True when [draft] would change what is [saved], ignoring surrounding whitespace. */
internal fun isApiKeyDraftDirty(saved: String?, draft: String): Boolean = normalizedApiKey(draft) != saved?.trim()?.ifEmpty { null }

/**
 * A single masked API-key field + Save. Tapping the field unmasks it for editing and focuses it
 * (the keyboard comes up on that one tap); the key stays dotted out unless the user chooses
 * SHOW KEY to check what they pasted. This is the safer of the two secret fields in the app, not
 * a shared idiom with McpServerScreen's pairing token. The key is stored encrypted
 * (EncryptedSharedPreferences with an Android Keystore master key - see AiSettings), never in
 * plain text.
 *
 * Leaving with an unsaved edit asks first (the Back chip and the system Back gesture both do);
 * [saveFailed] is set by the caller when the last [onSave] could not be persisted, so the screen
 * never reads as "saved" for a key that is lost on restart.
 */
@Composable
fun AiSettingsScreen(
    fullscreen: Boolean,
    apiKey: String?,
    onSave: (String?) -> Unit,
    onBack: () -> Unit,
    /** True when the last [onSave] could not be stored (see `AiSettings.saveApiKey`); shown as an error. */
    saveFailed: Boolean = false
) {
    var editing by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(apiKey.orEmpty()) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val dirty = isApiKeyDraftDirty(apiKey, draft)

    // One tap on the masked row both reveals the field and puts the caret in it.
    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }
    // Only intercept the system Back gesture while there is something to lose.
    BackHandler(enabled = dirty) { confirmDiscard = true }

    SettingsPage(
        title = stringResource(Res.string.aisettings_title),
        fullscreen = fullscreen,
        onBack = { if (dirty) confirmDiscard = true else onBack() }
    ) {
        SettingRow(
            title = stringResource(Res.string.aisettings_key_title),
            description = stringResource(Res.string.aisettings_key_description)
        ) {
          Column(Modifier.fillMaxWidth()) {
            val fieldLabel = stringResource(Res.string.aisettings_field_description)
            val empty = draft.isBlank()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minTouchSize())
                    .clip(AzphaltSurface.capsule)
                    .background(Azphalt.Ink)
                    .clickable(enabled = !editing, role = Role.Button) { editing = true }
                    .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val fieldStyle = settingDescriptionStyle().copy(color = Azphalt.Yellow, fontFamily = FontFamily.Monospace)
                if (editing) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .semantics { contentDescription = fieldLabel },
                        textStyle = fieldStyle,
                        cursorBrush = SolidColor(Azphalt.Yellow),
                        singleLine = true,
                        visualTransformation = if (showKey || empty) VisualTransformation.None else PasswordVisualTransformation(),
                        // A key is neither prose nor a word: no autocorrect, no capitalisation, and
                        // the password keyboard so it is not learned into the suggestion dictionary.
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onSave(normalizedApiKey(draft)) }),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (empty) {
                                    Text(
                                        stringResource(Res.string.aisettings_field_placeholder),
                                        color = Azphalt.Yellow.copy(alpha = PLACEHOLDER_ALPHA),
                                        style = fieldStyle
                                    )
                                }
                                inner()
                            }
                        }
                    )
                } else {
                    Text(
                        when {
                            empty -> stringResource(Res.string.aisettings_field_empty)
                            showKey -> draft
                            else -> stringResource(Res.string.aisettings_field_masked)
                        },
                        color = Azphalt.Yellow,
                        style = fieldStyle
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                AzphaltChip(
                    label = stringResource(Res.string.aisettings_save),
                    onClick = { onSave(normalizedApiKey(draft)) },
                    tint = Azphalt.hues[6]
                )
                if (!empty) {
                    AzphaltChip(
                        label = stringResource(if (showKey) Res.string.aisettings_hide_key else Res.string.aisettings_show_key),
                        onClick = { showKey = !showKey }
                    )
                }
            }

            if (saveFailed) {
                Text(
                    stringResource(Res.string.aisettings_save_failed),
                    color = statusColor(StatusKind.Error),
                    style = settingDescriptionStyle(),
                    modifier = Modifier.padding(top = 8.dp).liveRegionPolite()
                )
            }
          }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(Res.string.aisettings_discard_title),
            message = stringResource(Res.string.aisettings_discard_message),
            confirmLabel = stringResource(Res.string.aisettings_discard_confirm),
            cancelLabel = stringResource(Res.string.aisettings_discard_cancel),
            onConfirm = {
                confirmDiscard = false
                onBack()
            },
            onDismiss = { confirmDiscard = false }
        )
    }
}

private const val PLACEHOLDER_ALPHA = 0.7f
