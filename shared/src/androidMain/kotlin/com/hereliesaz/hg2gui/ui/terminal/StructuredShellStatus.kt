package com.hereliesaz.hg2gui.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.terminal.FullScreenPtySession
import com.hereliesaz.hg2gui.terminal.SemanticCompletionProviders
import com.hereliesaz.hg2gui.terminal.ShellFamily
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Native projection of status that shell prompt frameworks normally encode into decorated text. */
@Composable
fun StructuredShellStatus(holder: FullScreenPtySession) {
    @Suppress("UNUSED_VARIABLE") val generation = holder.generation
    val presentation = holder.shellPresentation
    if (!presentation.hasStructuredStatus && presentation.shell == ShellFamily.UNKNOWN) return

    var chooseBranch by remember(holder) { mutableStateOf(false) }
    val context = LocalContext.current
    val branches by produceState(
        initialValue = emptyList(),
        key1 = chooseBranch,
        key2 = presentation.cwd,
        key3 = presentation.gitBranch
    ) {
        value = if (chooseBranch) {
            withContext(Dispatchers.IO) {
                SemanticCompletionProviders.gitBranches(context, presentation.cwd)
            }
        } else emptyList()
    }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (presentation.shell != ShellFamily.UNKNOWN) {
            StatusChip(presentation.shell.name.lowercase())
        }
        val identity = listOfNotNull(presentation.user, presentation.host).joinToString("@").takeIf(String::isNotBlank)
        identity?.let { StatusChip(it) }
        presentation.cwd?.let { StatusChip(it) }
        presentation.gitBranch?.let { branch ->
            StatusChip(
                text = "${if (presentation.gitDirty) "● " else ""}$branch",
                onClick = { chooseBranch = true }
            )
        }
        presentation.lastExitCode?.let { code ->
            StatusChip(if (code == 0) "exit 0" else "exit $code")
        }
    }

    if (chooseBranch) {
        AlertDialog(
            onDismissRequest = { chooseBranch = false },
            title = { Text("Switch Git branch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (branches.isEmpty()) {
                        Text("No branch inventory is available for this directory.")
                    } else {
                        branches.forEach { branch ->
                            Text(
                                branch,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Azphalt.Ink.copy(alpha = .08f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        holder.sendText("git switch -- ${shellQuote(branch)}\n")
                                        chooseBranch = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                fontWeight = if (branch == presentation.gitBranch) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Text("CLOSE", modifier = Modifier.clickable { chooseBranch = false }.padding(12.dp))
            }
        )
    }
}

@Composable
private fun StatusChip(text: String, onClick: (() -> Unit)? = null) {
    Text(
        text,
        color = Azphalt.Yellow,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Azphalt.White.copy(alpha = .08f), RoundedCornerShape(999.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
