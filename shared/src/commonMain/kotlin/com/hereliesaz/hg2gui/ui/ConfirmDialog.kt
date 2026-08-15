package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hereliesaz.hg2gui.ui.menu.Azphalt

/*
 * The one confirmation primitive in the app - every destructive/irreversible action (delete,
 * overwrite-on-rename/move/copy, closing a session mid-command, discarding unsaved edits) routes
 * through this rather than each call site rolling its own prompt. Same capsule language as the
 * rest of Azphalt: an ink panel at the "record tile" 26dp radius (DESIGN.md §7), two capsule
 * pills, no border/shadow/blur/icon - `usePlatformDefaultWidth = false` so the panel is sized and
 * shaped by us rather than the platform dialog window's own default card chrome.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelLabel: String = "CANCEL"
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(26.dp))
                .background(Azphalt.Ink)
                .padding(22.dp)
        ) {
            Column {
                Text(
                    title.uppercase(), color = Azphalt.Yellow,
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.07.em
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    message, color = Azphalt.White.copy(alpha = .8f),
                    fontSize = 12.sp, lineHeight = 17.sp
                )
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Azphalt.White.copy(alpha = .12f))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cancelLabel.uppercase(), color = Azphalt.White.copy(alpha = .75f),
                            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(percent = 50))
                            // hues[6] is the same "red — primary / run" acquire hue AzphaltColors
                            // marks as the destructive/primary-action tone elsewhere in the app.
                            .background(Azphalt.hues[6])
                            .clickable(onClick = onConfirm)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            confirmLabel.uppercase(), color = Azphalt.White,
                            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                        )
                    }
                }
            }
        }
    }
}
