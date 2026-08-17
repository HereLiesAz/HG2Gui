package com.hereliesaz.hg2gui.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush

/*
 * A plain-text editor for files the shell has no pty to hand to a real one - nano is a real
 * bootstrap pill, but running it through a non-pty ShellSession would render garbled. No
 * embedded command line like the old vi-like editor had: Save and Back are real pills, same
 * page, same capsule primitive as the rest of Azphalt.
 */

@Composable
fun EditorScreen(
    fileName: String,
    content: String?,
    error: String?,
    dirty: Boolean,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(Azphalt.currentGround.pageBrush())) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                fileName.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Azphalt.currentGround.onPage, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                ),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (content != null && dirty) {
                    Pill("SAVE", Azphalt.Ink, Azphalt.Yellow, onSave)
                }
                Pill("BACK", Azphalt.Ink.copy(alpha = .14f), Azphalt.currentGround.onPage, onBack)
            }
        }

        when {
            error != null -> Text(
                error,
                style = MaterialTheme.typography.bodyMedium.copy(color = Azphalt.currentGround.onPage.copy(alpha = .7f), fontSize = 13.sp),
                modifier = Modifier.padding(20.dp)
            )
            content == null -> Text(
                "LOADING…",
                style = MaterialTheme.typography.labelSmall.copy(color = Azphalt.currentGround.onPage.copy(alpha = .4f), fontSize = 10.sp),
                modifier = Modifier.padding(20.dp)
            )
            else -> BasicTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Azphalt.currentGround.onPage, fontSize = 13.sp, lineHeight = 19.sp
                ),
                cursorBrush = SolidColor(Azphalt.currentGround.onPage)
            )
        }
    }
}

@Composable
private fun Pill(label: String, background: Color, foreground: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium.copy(
                color = foreground, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
            )
        )
    }
}
