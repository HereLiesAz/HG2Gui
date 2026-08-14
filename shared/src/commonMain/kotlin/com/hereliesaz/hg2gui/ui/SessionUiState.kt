package com.hereliesaz.hg2gui.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hereliesaz.hg2gui.managers.TerminalHistoryEntry
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
    var running by mutableStateOf(false)

    // The prompt text of a command that's stalled mid-run waiting on stdin, or null the rest of
    // the time. Set by awaitPromptAnswer (called from the platform layer's onNeedInput bridge,
    // on a background thread) and cleared once answerPrompt delivers a reply.
    var pendingPrompt by mutableStateOf<String?>(null)
        private set
    private var pendingAnswer: CompletableDeferred<String>? = null

    suspend fun awaitPromptAnswer(prompt: String): String {
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
