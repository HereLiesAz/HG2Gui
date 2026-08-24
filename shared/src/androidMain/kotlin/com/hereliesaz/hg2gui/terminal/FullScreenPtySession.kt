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
 * S1 (docs/HG2Gui Termux Coverage.dc.html): a dedicated pty-backed session for one full-screen
 * program (vim, htop, tmux, ...) - separate from the tab's own line-based [ShellSession], which
 * exists only to flatten one-shot command output into plain text. See FullScreenCommands.kt for
 * what gets routed here and TerminalActivity's onRun for the dispatch.
 *
 * Spawns a fresh `bash -l -c "<command>"` under the same bootstrap env the tab's own shell uses
 * ([ShellSession.bootstrapBashEnv]), in the tab's current working directory - shell state
 * (cd/export/aliases) does not carry over from the tab's already-running shell, the same
 * per-invocation scope every other command in this app already runs under (see ShellSession's
 * own S5 doc comment).
 *
 * Wraps upstream Termux's own [TerminalSession] - the event-driven pty driver (reader/writer/
 * waiter threads, ByteQueue-backed, correct SIGWINCH/termios via the same JNI bridge
 * ShellSession's own pty tier uses) that ShellSession's stream() bypasses in favor of a polling
 * loop suited to one-shot flattened output. A live full-screen program needs the real thing -
 * this is that real thing, previously vendored but never wired to any UI.
 *
 * Unverified on a physical device, same caveat as ShellSession's own pty tier and PtyPreference -
 * there is no hardware in this environment to have run vim/htop/tmux against this and confirmed
 * raw keystroke input actually reaches them correctly.
 */
// TerminalSessionClient (terminal-emulator, vendored from upstream Termux) is an 11-method
// callback interface - most of what it asks for genuinely has nothing to do here (this app has
// no clipboard/log-sink integration for a pty session), so those overrides are deliberately
// empty rather than routed to a comment explaining "on purpose" on each one individually.
@Suppress("TooManyFunctions", "EmptyFunctionBlock")
class FullScreenPtySession private constructor(
    val session: TerminalSession,
    val commandLine: String
) : TerminalSessionClient {

    /** Bumped on every screen-affecting callback - callers observe this via Compose's snapshot
     *  system (a plain state read) to know when to re-read the emulator's screen buffer. The
     *  value itself carries no meaning beyond being distinct each time. */
    var generation by mutableIntStateOf(0)
        private set

    /** Null while the child is still running; the shell's raw exit status once it isn't (see
     *  [TerminalSession.exitStatus]'s own doc comment for the sign convention). */
    var exitCode: Int? by mutableStateOf(null)
        private set

    var cursorVisible by mutableStateOf(true)
        private set

    val cursorApplicationMode: Boolean
        get() = session.emulator?.isCursorKeysApplicationMode() ?: false

    val keypadApplicationMode: Boolean
        get() = session.emulator?.isKeypadApplicationMode() ?: false

    override fun onTextChanged(changedSession: TerminalSession) {
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

    /** Must be called once real pixel geometry is known (from the renderer's own layout pass) -
     *  the first call is what actually opens the pty and spawns the child (see
     *  [TerminalSession.updateSize]'s own doc comment); safe to call again on a real resize. */
    fun updateSize(columns: Int, rows: Int, cellWidthPx: Int, cellHeightPx: Int) {
        session.updateSize(columns, rows, cellWidthPx, cellHeightPx)
    }

    fun sendText(text: String) {
        session.write(text)
    }

    fun sendCodePoint(codePoint: Int) {
        session.writeCodePoint(false, codePoint)
    }

    /** Sends the real INTR byte (Ctrl-C) - same control byte ShellSession's own S2 [interrupt]
     *  writes on its pty tier, here going straight to this session's own dedicated pty instead. */
    fun sendInterrupt() {
        session.write(byteArrayOf(INTR_BYTE), 0, 1)
    }

    fun kill() {
        session.finishIfRunning()
    }

    companion object {
        private const val INITIAL_TRANSCRIPT_ROWS = 2000

        // S2: the same INTR control byte ShellSession's own [interrupt] writes on its pty tier.
        private const val INTR_BYTE: Byte = 0x03

        /** Null if the Termux bootstrap isn't installed/executable - same precondition
         *  [ShellSession.forAndroid]'s own bootstrap tier requires; a full-screen program has no
         *  meaningful fallback onto the bare system shell (no ncurses terminfo, usually missing
         *  entirely). */
        fun launch(context: Context, cwd: String, commandLine: String): FullScreenPtySession? {
            val home = File(cwd).takeIf { it.isDirectory }
            val (_, env) = ShellSession.bootstrapBashEnv(context, home) ?: return null
            val bash = File(env.getValue("PREFIX"), "bin/bash")
            val envArray = env.map { (k, v) -> "$k=$v" }.toTypedArray()
            val termuxSession = TerminalSession(
                bash.absolutePath,
                cwd,
                arrayOf(bash.absolutePath, "-l", "-c", commandLine),
                envArray,
                INITIAL_TRANSCRIPT_ROWS,
                null
            )
            val holder = FullScreenPtySession(termuxSession, commandLine)
            termuxSession.updateTerminalSessionClient(holder)
            return holder
        }
    }
}
