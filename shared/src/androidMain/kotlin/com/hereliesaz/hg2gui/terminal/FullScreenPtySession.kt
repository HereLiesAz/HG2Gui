package com.hereliesaz.hg2gui.terminal

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File

/**
 * Dedicated PTY-backed session for interactive programs and shells. The terminal emulator remains
 * the source of truth; higher-level adapters may project its state into native HG2Gui surfaces.
 */
@Suppress("TooManyFunctions", "EmptyFunctionBlock")
class FullScreenPtySession private constructor(
    val session: TerminalSession,
    val commandLine: String,
    initialShellPresentation: ShellPresentation
) : TerminalSessionClient {

    var generation by mutableIntStateOf(0)
        private set

    var exitCode: Int? by mutableStateOf(null)
        private set

    var cursorVisible by mutableStateOf(true)
        private set

    var shellPresentation by mutableStateOf(initialShellPresentation)
        private set

    val cursorApplicationMode: Boolean
        get() = session.emulator?.isCursorKeysApplicationMode() ?: false

    val keypadApplicationMode: Boolean
        get() = session.emulator?.isKeypadApplicationMode() ?: false

    override fun onTextChanged(changedSession: TerminalSession) {
        refreshShellPresentation(changedSession)
        generation++
    }

    override fun onTitleChanged(updatedSession: TerminalSession) {}

    override fun onSessionFinished(finishedSession: TerminalSession) {
        exitCode = finishedSession.exitStatus
        generation++
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String?) {}
    override fun onPasteTextFromClipboard(session: TerminalSession?) {}
    override fun onBell(session: TerminalSession) {}

    override fun onColorsChanged(changedSession: TerminalSession) {
        generation++
    }

    override fun onTerminalCursorStateChange(state: Boolean) {
        cursorVisible = state
    }

    override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}
    override fun getTerminalCursorStyle(): Int? = null
    override fun logError(tag: String?, message: String?) {}
    override fun logWarn(tag: String?, message: String?) {}
    override fun logInfo(tag: String?, message: String?) {}
    override fun logDebug(tag: String?, message: String?) {}
    override fun logVerbose(tag: String?, message: String?) {}
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
    override fun logStackTrace(tag: String?, e: Exception?) {}

    fun updateSize(columns: Int, rows: Int, cellWidthPx: Int, cellHeightPx: Int) {
        session.updateSize(columns, rows, cellWidthPx, cellHeightPx)
    }

    fun sendText(text: String) {
        session.write(text)
    }

    fun sendCodePoint(codePoint: Int) {
        session.writeCodePoint(false, codePoint)
    }

    fun sendInterrupt() {
        session.write(byteArrayOf(INTR_BYTE), 0, 1)
    }

    fun kill() {
        session.finishIfRunning()
    }

    private fun refreshShellPresentation(changedSession: TerminalSession) {
        val family = shellPresentation.shell
        if (family !in setOf(ShellFamily.BASH, ShellFamily.ZSH, ShellFamily.FISH, ShellFamily.SH)) return
        val emulator = changedSession.emulator ?: return
        if (emulator.isAlternateBufferActive()) return
        val transcript = emulator.screen.transcriptTextWithoutJoinedLines.trimEnd()
        val lastLine = transcript.substringAfterLast('\n').trimEnd()
        if (lastLine.isBlank()) return
        val inferred = ShellPromptParser.parse(
            rawPrompt = lastLine,
            shell = family,
            framework = shellPresentation.framework,
            fallbackCwd = shellPresentation.cwd
        ).copy(completionProvider = shellPresentation.completionProvider)
        shellPresentation = CooperativeShellPresentation.forFamily(family, inferred) ?: inferred
    }

    companion object {
        private const val INITIAL_TRANSCRIPT_ROWS = 2000
        private const val INTR_BYTE: Byte = 0x03

        fun launch(context: Context, cwd: String, commandLine: String): FullScreenPtySession? {
            val home = File(cwd).takeIf { it.isDirectory }
            val (_, env) = ShellSession.bootstrapBashEnv(context, home) ?: return null
            val selectedName = ShellPreference.selected(context)
            val selected = ShellPreference.executable(context, selectedName)
                ?: ShellPreference.executable(context, ShellPreference.BASH)
                ?: return null
            val selectedFamily = ShellCommandProtocol.family(
                if (selectedName == ShellPreference.BASH || selected.name != "libbin_bash.so") selectedName
                else ShellPreference.BASH
            )
            val envArray = env.map { (k, v) -> "$k=$v" }.toTypedArray()
            val args = when (selectedFamily) {
                ShellFamily.FISH -> arrayOf(selected.absolutePath, "-l", "-c", commandLine)
                ShellFamily.BASH, ShellFamily.ZSH -> arrayOf(selected.absolutePath, "-l", "-c", commandLine)
                else -> arrayOf(selected.absolutePath, "-c", commandLine)
            }
            val termuxSession = TerminalSession(
                selected.absolutePath,
                cwd,
                args,
                envArray,
                INITIAL_TRANSCRIPT_ROWS,
                null
            )
            val detected = ShellAdapterRegistry.detect(context, cwd)
            val leading = commandLine.trim().substringBefore(' ').substringAfterLast('/')
            val explicitShell = when (leading) {
                "bash" -> ShellFamily.BASH
                "zsh" -> ShellFamily.ZSH
                "fish" -> ShellFamily.FISH
                "sh" -> ShellFamily.SH
                else -> ShellFamily.UNKNOWN
            }
            val basePresentation = if (explicitShell == ShellFamily.UNKNOWN) {
                ShellPresentation(cwd = cwd)
            } else {
                detected.copy(shell = explicitShell, cwd = cwd, completionProvider = when (explicitShell) {
                    ShellFamily.BASH -> ShellCompletionProvider.BASH_COMPLETION
                    ShellFamily.ZSH -> ShellCompletionProvider.ZSH_COMPLETION
                    ShellFamily.FISH -> ShellCompletionProvider.FISH_COMPLETION
                    else -> ShellCompletionProvider.NONE
                })
            }
            val presentation = if (explicitShell == ShellFamily.UNKNOWN) {
                basePresentation
            } else {
                CooperativeShellPresentation.forFamily(explicitShell, basePresentation) ?: basePresentation
            }
            val holder = FullScreenPtySession(termuxSession, commandLine, presentation)
            termuxSession.updateTerminalSessionClient(holder)
            return holder
        }
    }
}
