package com.hereliesaz.hg2gui.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hereliesaz.hg2gui.managers.TerminalHistoryEntry
import com.hereliesaz.hg2gui.terminal.ShellAliases
import kotlinx.coroutines.CompletableDeferred

/**
 * One terminal session's UI state: its own scrollback, command history and in-progress
 * input, independent of every other session. The platform layer owns one of these per
 * shell it keeps alive; TerminalScreen only ever reads and writes the active one.
 */
class SessionUiState(val id: String, name: String, cwd: String) {
    var name by mutableStateOf(name)
    var cwd by mutableStateOf(cwd)
    var buffer by mutableStateOf(listOf<TerminalHistoryEntry>())
    var commandHistory by mutableStateOf(listOf<String>())
    var historyIndex by mutableStateOf(-1)
    var tokens by mutableStateOf(listOf<String>())
    var inputText by mutableStateOf("")
    var composedPrefix by mutableStateOf("")
    var running by mutableStateOf(false)

    /** A carriage-return style progress/status frame currently being rewritten by the child. */
    var transientStatus by mutableStateOf<String?>(null)

    var pendingPrompt by mutableStateOf<String?>(null)
        private set
    private var pendingAnswer: CompletableDeferred<String>? = null

    suspend fun awaitPromptAnswer(prompt: String): String {
        ShellAliases.transientStatusLine(prompt)?.let { status ->
            transientStatus = status
            // This callback is only reached because ShellSession's generic idle-gap detector saw
            // an unterminated line. Returning immediately keeps a progress frame from becoming a
            // fake blocking prompt. A blank line is harmless to apt/dpkg while they are drawing
            // progress and lets the reader continue until the next real output frame arrives.
            return ""
        }

        transientStatus = null
        val deferred = CompletableDeferred<String>()
        pendingAnswer = deferred
        pendingPrompt = prompt
        val answer = deferred.await()
        pendingPrompt = null
        pendingAnswer = null
        return answer
    }

    fun answerPrompt(text: String) {
        pendingAnswer?.complete(text)
    }
}
