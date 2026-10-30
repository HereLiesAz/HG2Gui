package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hereliesaz.hg2gui.managers.TerminalHistoryEntry
import com.hereliesaz.hg2gui.terminal.CompletionCandidate
import com.hereliesaz.hg2gui.terminal.ShellAliases
import com.hereliesaz.hg2gui.terminal.ShellPresentation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * What a session tab should badge: the command it is running, a prompt it is blocked on, or output
 * that finished while another tab was in front. Each carries a [glyph] for the tab label (a shape,
 * so it never depends on colour) and a [description] for screen readers.
 */
enum class SessionActivity(val glyph: String, val description: String?) {
    Idle("", null),
    Running("…", "running"),
    WaitingForInput("?", "waiting for input"),
    Finished("✓", "finished")
}

/** Waiting beats running (the command is blocked on the user), and a finished badge only shows once idle. */
internal fun sessionActivityOf(running: Boolean, waitingForInput: Boolean, finishedWhileAway: Boolean): SessionActivity = when {
    waitingForInput -> SessionActivity.WaitingForInput
    running -> SessionActivity.Running
    finishedWhileAway -> SessionActivity.Finished
    else -> SessionActivity.Idle
}

/**
 * Remembers how many lines of each output entry have already been revealed, so the line wipe plays
 * once per line: scrolling an entry out of the lazy list and back in (which recreates its
 * composables) must not replay it, and lines that stream in later wipe in on arrival only.
 */
class OutputWipeLedger {
    private val shown = HashMap<Long, Int>()

    /** Lines of [entryId] that have already been on screen. */
    fun shownLines(entryId: Long): Int = shown[entryId] ?: 0

    /** Records that [lineCount] lines of [entryId] are now on screen (never lowers the count). */
    fun record(entryId: Long, lineCount: Int) {
        if (lineCount > (shown[entryId] ?: 0)) shown[entryId] = lineCount
    }

    /** Drops entries that left the buffer so the ledger cannot grow without bound. */
    fun retainOnly(entryIds: Set<Long>) {
        shown.keys.retainAll(entryIds)
    }
}

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
    var shellPresentation by mutableStateOf(ShellPresentation(cwd = cwd))

    /**
     * Scroll position of this session's output. Owned here (not remembered by the screen) so a tab
     * switch, or leaving the terminal for Settings and coming back, returns to where the user was.
     */
    val listState: LazyListState by lazy(LazyThreadSafetyMode.NONE) { LazyListState() }

    /** True while the output should keep its newest line in view; cleared when the user scrolls away. */
    var followTail by mutableStateOf(true)

    /** Which output lines have already played their entrance (see [OutputWipeLedger]). */
    val wipeLedger = OutputWipeLedger()

    /** A command finished while this tab was not the one in front; cleared when it is shown again. */
    var finishedWhileAway by mutableStateOf(false)

    /** What this session's tab should badge. */
    val activity: SessionActivity
        get() = sessionActivityOf(running, pendingPrompt != null, finishedWhileAway)

    /**
     * Re-derives [shellPresentation] after a command: the current [cwd], the repository state
     * [probe] reports for it, and [exitCode]. A result for a directory the session has since left
     * is dropped. The default probe reports nothing, so the cwd and exit status still update.
     */
    suspend fun refreshShellPresentation(exitCode: Int?, probe: ShellProbe = NoShellProbe) {
        val forCwd = cwd
        val next = ShellStatusProjector.project(shellPresentation, forCwd, exitCode, probe)
        if (cwd == forCwd && next != shellPresentation) shellPresentation = next
    }

    /**
     * Candidates supplied by the Completion Bridge for the current composed command. They are
     * semantic values, not pre-rendered UI, so pills, dialogs, pickers and future surfaces can all
     * consume the same result.
     */
    var completionCandidates by mutableStateOf(listOf<CompletionCandidate>())
        private set
    var completionRequestKey by mutableStateOf<String?>(null)
        private set

    fun updateCompletions(requestKey: String, candidates: List<CompletionCandidate>) {
        completionRequestKey = requestKey
        completionCandidates = candidates
    }

    fun clearCompletions() {
        completionRequestKey = null
        completionCandidates = emptyList()
    }

    /** A carriage-return style progress/status frame currently being rewritten by the child. */
    var transientStatus by mutableStateOf<String?>(null)

    var pendingPrompt by mutableStateOf<String?>(null)
        private set

    /**
     * The prompt the child is waiting on with its surrounding lines intact (bounded by
     * [ConfirmPrompts.detailOf]), where [pendingPrompt] keeps only the final logical line. A
     * confirmation dialog shows this so a multi-line command in "Run as root? ..." is read in full.
     */
    var pendingPromptDetail by mutableStateOf<String?>(null)
        private set
    private var pendingAnswer: CompletableDeferred<String>? = null

    // Only one prompt can be pending on a session at a time; awaitPromptAnswer used to
    // overwrite pendingAnswer/pendingPrompt unconditionally, so a second caller (e.g. an
    // SSH/Workflow wizard) started while a real shell command was stalled on stdin would
    // silently orphan the first caller's deferred forever. Serializing through this mutex
    // makes a second call queue behind the first instead of clobbering it.
    private val promptMutex = Mutex()

    suspend fun awaitPromptAnswer(prompt: String): String = promptMutex.withLock {
        ShellAliases.transientStatusLine(prompt)?.let { status ->
            transientStatus = status
            return@withLock ""
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
        pendingPromptDetail = ConfirmPrompts.detailOf(prompt)
        // finally: a cancelled caller (tab closed, command killed) must not leave a stale prompt
        // on screen or a dead deferred that the next answerPrompt() would complete into nothing.
        try {
            deferred.await()
        } finally {
            if (pendingAnswer === deferred) {
                pendingPrompt = null
                pendingPromptDetail = null
                pendingAnswer = null
            }
        }
    }

    fun answerPrompt(text: String) {
        pendingAnswer?.complete(text)
    }

    /** Session teardown: releases any command still waiting in [awaitPromptAnswer] with an empty
     *  answer so its coroutine/thread can finish instead of waiting forever. */
    fun destroy() {
        pendingAnswer?.complete("")
    }
}
