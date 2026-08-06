package com.hereliesaz.hg2gui.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface TermuxTerminalController {
    fun sendCommand(command: String)
}

@Composable
expect fun TermuxTerminalView(
    controller: TermuxTerminalController,
    modifier: Modifier = Modifier
)
