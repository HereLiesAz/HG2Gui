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
            return ""
        }

        transientStatus = null

        // The PTY reader can have an entire command transcript buffered when an unterminated
        // prompt stalls. Interactive UI must classify the prompt the child is waiting on, not
        // every line that happened before it. Keep a multi-line numbered menu intact, but reduce
        // ordinary y/n, password and bracket prompts to their final carriage-return/logical line.
        val logicalTail = prompt.substringAfterLast('\n').substringAfterLast('\r').trim()
        val uiPrompt = when {
            ShellAliases.numberedMenuChoices(prompt) != null -> prompt
            ShellAliases.looksLikeYesNo(logicalTail) -> logicalTail
            ShellAliases.looksLikePassword(logicalTail) -> logicalTail
            ShellAliases.bracketedChoices(logicalTail) != null -> logicalTail
            logicalTail.isNotEmpty() -> logicalTail
            else -> prompt
        }

        val deferred = CompletableDeferred<String>()
        pendingAnswer = deferred
        pendingPrompt = uiPrompt
        val answer = deferred.await()
        pendingPrompt = null
        pendingAnswer = null
        return answer
    }

    fun answerPrompt(text: String) {
        pendingAnswer?.complete(text)
    }
}
