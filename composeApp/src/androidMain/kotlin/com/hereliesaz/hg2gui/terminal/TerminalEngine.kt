package com.hereliesaz.hg2gui.terminal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.MainManager
import com.hereliesaz.hg2gui.commands.BaseCommandGroup
import com.hereliesaz.hg2gui.tuils.PrivateIOReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Decides where a command runs, and collects what it prints.
 */
class TerminalEngine(
    private val context: Context,
    private val main: MainManager,
    home: File? = null
) {

    private val shell = ShellSession.forAndroid(home, context)

    private val builtins: Set<String> =
        main.mainPack?.commandGroup?.commandNames?.toSet().orEmpty()

    // Aliases aren't command classes, so they're never in `builtins` - checked separately, and
    // live (not cached) since `alias add` can create one at any point in the session.
    private fun isAlias(verb: String): Boolean =
        main.mainPack?.aliasManager?.getAliases(true)?.any { it.name == verb } == true

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
     * Runs [line] and returns a Flow of its output lines.
     */
    fun run(line: String): Flow<String> = callbackFlow {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            close()
            return@callbackFlow
        }

        val verb = trimmed.substringBefore(' ')

        if (verb in builtins || isAlias(verb)) {
            launch(Dispatchers.IO) {
                val result = runBuiltin(trimmed)
                send(result)
                close()
            }
        } else {
            launch(Dispatchers.IO) {
                shell.stream(trimmed) { line ->
                    trySend(line)
                }
                close()
            }
        }
        awaitClose { }
    }

    private fun runBuiltin(line: String): String {
        synchronized(captured) {
            captured.setLength(0)
        }
        capturing = true
        val done = CountDownLatch(1)
        main.setCommandCompletionListener { done.countDown() }
        try {
            main.onCommand(line, null as String?, false)
            val completed = done.await(8, TimeUnit.SECONDS)
            if (completed) {
                Thread.sleep(50L)
            }
        } catch (e: Exception) {
            return "error: ${e.message}"
        } finally {
            capturing = false
            main.setCommandCompletionListener(null)
        }
        return synchronized(captured) { captured.toString() }
    }

    fun destroy() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(outputReceiver)
        shell.close()
    }
}
