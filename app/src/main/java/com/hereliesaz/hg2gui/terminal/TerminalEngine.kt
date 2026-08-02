package com.hereliesaz.hg2gui.terminal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.MainManager
import com.hereliesaz.hg2gui.tuils.PrivateIOReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Decides where a command runs, and collects what it prints.
 *
 * HG2Gui has two execution paths that have nothing in common:
 *
 *  - the built-in commands (`apps`, `cntcts`, `wifi`, `notifications`, `theme`, ...) are Java
 *    classes calling Android APIs. They do not return their output; they broadcast it, so it
 *    has to be caught rather than read.
 *  - everything else is a real shell command, run in a [ShellSession] that returns its output
 *    directly.
 *
 * Built-ins win ties. `apps` means the launcher's app list, not whatever `apps` might happen
 * to resolve to on PATH — the Android verbs are the reason this app exists, and shadowing them
 * with a coincidental binary would be a surprise.
 */
class TerminalEngine(
    private val context: Context,
    private val main: MainManager,
    home: File? = null
) {

    private val shell = ShellSession(home)

    /**
     * Names of the built-in commands, taken from the live CommandGroup rather than a hardcoded
     * list, so a command added to the `raw` package is routed correctly without touching this.
     */
    private val builtins: Set<String> =
        main.mainPack?.commandGroup?.commandNames?.toSet().orEmpty()

    /** Output broadcast by built-ins between [beginCapture] and [endCapture]. */
    private val captured = StringBuilder()
    private var capturing = false

    private val outputReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!capturing) return
            val text = intent?.getCharSequenceExtra(PrivateIOReceiver.TEXT) ?: return
            synchronized(captured) {
                if (captured.isNotEmpty()) captured.append('\n')
                captured.append(text)
            }
        }
    }

    init {
        LocalBroadcastManager.getInstance(context).registerReceiver(
            outputReceiver, IntentFilter(PrivateIOReceiver.ACTION_OUTPUT)
        )
    }

    val workingDirectory: String get() = shell.workingDirectory

    /**
     * Runs [line] and returns everything it printed.
     *
     * Suspends on [Dispatchers.IO]: a shell command blocks until the sentinel comes back, and
     * doing that on the main thread would freeze the UI for the length of the command.
     */
    suspend fun run(line: String): String = withContext(Dispatchers.IO) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@withContext ""

        val verb = trimmed.substringBefore(' ')

        if (verb in builtins) runBuiltin(trimmed) else shell.exec(trimmed).output
    }

    /**
     * Built-ins report asynchronously over LocalBroadcastManager, so the call is bracketed by
     * a capture window and then given a moment to deliver. Commands that keep working after
     * they return (a network fetch, say) will land their output in a later window; those need
     * to stream rather than be captured, which the record-tile UI does not model yet.
     */
    private fun runBuiltin(line: String): String {
        synchronized(captured) {
            captured.setLength(0)
        }
        capturing = true
        try {
            // onCommand is overloaded on the second parameter (String alias / LaunchInfo), so
            // a bare null is ambiguous. No alias applies here — the line is already expanded.
            main.onCommand(line, null as String?, false)
            // LocalBroadcastManager dispatches on the main looper; yield long enough for the
            // synchronous portion of the command to have been delivered.
            Thread.sleep(120L)
        } catch (e: Exception) {
            return "error: ${e.message}"
        } finally {
            capturing = false
        }
        return synchronized(captured) { captured.toString() }
    }

    fun destroy() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(outputReceiver)
        shell.close()
    }
}
