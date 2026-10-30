package com.hereliesaz.hg2gui.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ai.AiIntent
import com.hereliesaz.hg2gui.ai.AiIntentAnalyzer
import com.hereliesaz.hg2gui.ui.AzphaltChip
import com.hereliesaz.hg2gui.ui.AzphaltHeading
import com.hereliesaz.hg2gui.ui.StatusKind
import com.hereliesaz.hg2gui.ui.liveRegionPolite
import com.hereliesaz.hg2gui.ui.maxContentWidth
import com.hereliesaz.hg2gui.ui.minTouchSize
import com.hereliesaz.hg2gui.ui.screenInsets
import com.hereliesaz.hg2gui.ui.statusColor
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType
import hg2gui.shared.generated.resources.Res
import hg2gui.shared.generated.resources.aichat_authority
import hg2gui.shared.generated.resources.aichat_back
import hg2gui.shared.generated.resources.aichat_back_description
import hg2gui.shared.generated.resources.aichat_busy
import hg2gui.shared.generated.resources.aichat_empty
import hg2gui.shared.generated.resources.aichat_input_description
import hg2gui.shared.generated.resources.aichat_input_placeholder
import hg2gui.shared.generated.resources.aichat_needs_key
import hg2gui.shared.generated.resources.aichat_parts_title
import hg2gui.shared.generated.resources.aichat_send
import hg2gui.shared.generated.resources.aichat_send_busy
import hg2gui.shared.generated.resources.aichat_settings
import hg2gui.shared.generated.resources.aichat_settings_description
import hg2gui.shared.generated.resources.aichat_speaker_ai
import hg2gui.shared.generated.resources.aichat_speaker_you
import hg2gui.shared.generated.resources.aichat_title
import hg2gui.shared.generated.resources.aichat_use
import hg2gui.shared.generated.resources.aichat_use_description
import org.jetbrains.compose.resources.stringResource

/** Local typed intent is derived from [command]; the AI does not assign its own authority. */
data class AiMessage(
    val fromUser: Boolean,
    val text: String,
    val command: String? = null,
    val parts: List<Pair<String, String>> = emptyList(),
    val intent: AiIntent? = command?.let(AiIntentAnalyzer::analyze)
)

private const val PLACEHOLDER_ALPHA = 0.7f

/** Chat prose and explanation text: the type scale's body face at a reading size for a bubble. */
@Composable
private fun bubbleTextStyle(): TextStyle = LocalAzphaltType.current.body.copy(fontSize = 15.sp, lineHeight = 21.sp)

/** Secondary lines inside a bubble (consequences, part explanations). */
@Composable
private fun captionTextStyle(): TextStyle = LocalAzphaltType.current.body.copy(fontSize = 13.sp, lineHeight = 18.sp)

/** Natural-language command suggestion. Suggested commands are always returned for review. */
@Composable
fun AiChatScreen(
    fullscreen: Boolean,
    messages: List<AiMessage>,
    apiKeyConfigured: Boolean,
    busy: Boolean,
    onAsk: (String) -> Unit,
    onUseCommand: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = remember { LazyListState() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    // The point of this screen is the input row: open with the caret in it. A disabled (busy)
    // field cannot take focus, so a refused request is not an error worth surfacing.
    LaunchedEffect(apiKeyConfigured) {
        if (apiKeyConfigured) runCatching { focusRequester.requestFocus() }
    }

    // screenInsets() includes the keyboard: the whole column shrinks above it, which keeps the
    // input row (the last child) visible instead of under the IME.
    Box(
        Modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .screenInsets(fullscreen)
    ) {
        Column(Modifier.fillMaxHeight().maxContentWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AzphaltChip(
                    label = stringResource(Res.string.aichat_back),
                    onClick = onBack,
                    contentDescription = stringResource(Res.string.aichat_back_description)
                )
            }

            AzphaltHeading(
                stringResource(Res.string.aichat_title),
                Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp)
            )

            if (!apiKeyConfigured) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        stringResource(Res.string.aichat_needs_key),
                        color = Azphalt.currentGround.onPage,
                        style = captionTextStyle(),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    AzphaltChip(
                        label = stringResource(Res.string.aichat_settings),
                        onClick = onOpenSettings,
                        contentDescription = stringResource(Res.string.aichat_settings_description)
                    )
                }
                return@Column
            }

            if (messages.isEmpty()) {
                Text(
                    stringResource(Res.string.aichat_empty),
                    color = statusColor(StatusKind.Muted),
                    style = captionTextStyle(),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(messages) { message -> AiBubble(message, onUseCommand) }
            }

            if (busy) {
                // A live region, so "working" is announced rather than only shown as a "…" glyph.
                Text(
                    stringResource(Res.string.aichat_busy),
                    color = statusColor(StatusKind.Muted),
                    style = captionTextStyle(),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).liveRegionPolite()
                )
            }

            val canSend = !busy && input.isNotBlank()
            val send = {
                if (canSend) {
                    onAsk(input.trim())
                    input = ""
                }
            }
            val inputLabel = stringResource(Res.string.aichat_input_description)
            val inputStyle = captionTextStyle().copy(color = Azphalt.Yellow)

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .heightIn(min = minTouchSize())
                        .clip(AzphaltSurface.capsule)
                        .background(Azphalt.Ink)
                        .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .semantics { contentDescription = inputLabel },
                        textStyle = inputStyle,
                        cursorBrush = SolidColor(Azphalt.Yellow),
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (input.isEmpty()) {
                                    Text(
                                        stringResource(Res.string.aichat_input_placeholder),
                                        color = Azphalt.Yellow.copy(alpha = PLACEHOLDER_ALPHA),
                                        style = inputStyle
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
                AzphaltChip(
                    label = if (busy) "…" else stringResource(Res.string.aichat_send),
                    onClick = { send() },
                    enabled = canSend,
                    tint = Azphalt.hues[6],
                    contentDescription = if (busy) stringResource(Res.string.aichat_send_busy) else null
                )
            }
        }
    }
}

@Composable
private fun AiBubble(message: AiMessage, onUseCommand: (String) -> Unit) {
    val onPage = Azphalt.currentGround.onPage
    val muted = statusColor(StatusKind.Muted)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AzphaltSurface.recordTile)
            .background(Azphalt.Ink.copy(alpha = if (message.fromUser) .10f else .05f))
            .padding(12.dp)
    ) {
        // Who said it, in words: the bubble tint alone does not tell a screen reader (or a
        // colour-blind reader) a question from an answer.
        Text(
            stringResource(if (message.fromUser) Res.string.aichat_speaker_you else Res.string.aichat_speaker_ai),
            color = muted,
            style = LocalAzphaltType.current.micro,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(message.text, color = onPage, style = bubbleTextStyle())

        message.intent?.let { intent ->
            Spacer(Modifier.height(9.dp))
            Column(
                Modifier.fillMaxWidth().clip(AzphaltSurface.note).background(Azphalt.Ink.copy(alpha = .07f)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(
                        Res.string.aichat_authority,
                        intent.kind.name.replace('_', ' '),
                        intent.authority.uppercase()
                    ),
                    color = muted,
                    style = LocalAzphaltType.current.micro
                )
                intent.consequences.forEach { consequence ->
                    Text("• $consequence", color = onPage, style = captionTextStyle())
                }
            }
        }

        if (message.command != null) {
            Spacer(Modifier.height(4.dp))
            AzphaltChip(
                label = stringResource(Res.string.aichat_use),
                onClick = { onUseCommand(message.command) },
                contentDescription = stringResource(Res.string.aichat_use_description)
            )
        }
        if (message.parts.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(AzphaltSurface.note)
                    .background(Azphalt.Ink.copy(alpha = .09f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(Res.string.aichat_parts_title),
                    color = muted,
                    style = LocalAzphaltType.current.micro
                )
                message.parts.forEach { (token, explanation) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            token,
                            color = onPage,
                            style = captionTextStyle().copy(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.widthIn(min = 34.dp)
                        )
                        Text(explanation, color = onPage, style = captionTextStyle())
                    }
                }
            }
        }
    }
}
