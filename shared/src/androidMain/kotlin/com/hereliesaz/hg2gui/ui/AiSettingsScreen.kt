package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.pageBrush

/**
 * A single masked API-key field + Save - unlike McpServerScreen's pairing token, tapping this
 * never reveals the plaintext key, only unmasks the field enough to edit it (still dotted out via
 * PasswordVisualTransformation while a value is present). The safer of the two, not a shared
 * idiom with it. Stored unencrypted (see AiSettings' own doc comment) - flagged in USER_GUIDE.md
 * the same way the MCP token risk is, not hidden as a non-issue.
 */
@Composable
fun AiSettingsScreen(
    fullscreen: Boolean,
    apiKey: String?,
    onSave: (String?) -> Unit,
    onBack: () -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(apiKey.orEmpty()) }

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

        Eyebrow("AI settings")

        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                "API KEY", color = Azphalt.Ink,
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
            )
            Text(
                "An Anthropic API key, used only for the AI command-search chat. Stored " +
                    "unencrypted on this device, same as this app's other local settings.",
                color = Azphalt.Ink.copy(alpha = .6f), fontSize = 11.sp, lineHeight = 15.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .clickable(enabled = !revealed) { revealed = true }
                    .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (revealed) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Azphalt.Yellow, fontSize = 12.sp, fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(Azphalt.Yellow),
                        singleLine = true,
                        visualTransformation = if (draft.isNotEmpty()) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSave(draft.ifBlank { null }) })
                    )
                } else {
                    Text(
                        if (draft.isNotBlank()) "•••• tap to edit" else "not set — tap to add",
                        color = Azphalt.Yellow, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.hues[6])
                    .clickable { onSave(draft.ifBlank { null }) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    "SAVE", color = Azphalt.White,
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
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
