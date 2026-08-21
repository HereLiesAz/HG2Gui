package com.hereliesaz.hg2gui.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface

/** [parts] is a flag-by-flag "what each part does" breakdown for a command reply - see
 *  HG2Gui_Redesign.dc.html's "Phase 4" concept. Empty when the model didn't include one. */
data class AiMessage(
    val fromUser: Boolean,
    val text: String,
    val command: String? = null,
    val parts: List<Pair<String, String>> = emptyList()
)

/**
 * Single-turn natural-language -> command suggestion, styled like every other full-screen
 * surface (McpServerScreen, CommandGuideScreen): back pill, Eyebrow, then content. Never runs
 * anything itself - a reply with a command shows a USE pill that hands it to the terminal's
 * input line for the user to review and press Run, the same "assemble, don't auto-execute" rule
 * every wizard-produced command already follows.
 */
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

    // Without this, a reply that pushes the transcript past one screen left the user's own
    // question and the AI's answer both below the fold with no scroll and no "new message"
    // affordance - listState existed but nothing ever drove it.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

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

        Eyebrow("AI")

        if (!apiKeyConfigured) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    "Add an Anthropic API key in Settings → AI first.",
                    color = Azphalt.currentGround.onPage.copy(alpha = .7f), fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Azphalt.Ink)
                        .clickable(onClick = onOpenSettings)
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        "AI SETTINGS ›", color = Azphalt.Yellow,
                        fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(messages) { message ->
                AiBubble(message, onUseCommand)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Azphalt.Yellow, fontSize = 12.sp),
                    cursorBrush = SolidColor(Azphalt.Yellow),
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (input.isNotBlank() && !busy) {
                            onAsk(input.trim())
                            input = ""
                        }
                    })
                )
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    // A capsule is never tinted, faded, or given alpha (style guide "03 -
                    // Transparency") - idle uses the same ink-14% wash every other disabled
                    // capsule in the app does, not a faded copy of its own hue.
                    .background(if (!busy && input.isNotBlank()) Azphalt.hues[6] else Azphalt.Ink.copy(alpha = .14f))
                    .clickable(enabled = !busy && input.isNotBlank()) {
                        onAsk(input.trim())
                        input = ""
                    }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    if (busy) "…" else "SEND",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (!busy && input.isNotBlank()) Azphalt.White else Azphalt.currentGround.onPage.copy(alpha = .55f),
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun AiBubble(message: AiMessage, onUseCommand: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AzphaltSurface.recordTile) // a message IS a record
            .background(Azphalt.Ink.copy(alpha = if (message.fromUser) .10f else .05f))
            .padding(12.dp)
    ) {
        Text(
            message.text,
            style = MaterialTheme.typography.bodyMedium.copy(color = Azphalt.currentGround.onPage)
        )
        if (message.command != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .clickable { onUseCommand(message.command) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "USE ▸", color = Azphalt.Yellow,
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
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
                    "WHAT EACH PART DOES",
                    // .55f, the app-wide "text-muted, eyebrows and captions" tier - the 45% tier
                    // is for micro labels inside a pill, which this section caption isn't.
                    color = Azphalt.currentGround.onPage.copy(alpha = .55f),
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em
                )
                message.parts.forEach { (token, explanation) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            token,
                            color = Azphalt.currentGround.onPage,
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.widthIn(min = 34.dp)
                        )
                        Text(
                            explanation,
                            color = Azphalt.currentGround.onPage.copy(alpha = .78f),
                            fontSize = 12.sp, lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 9.dp)
    )
}
