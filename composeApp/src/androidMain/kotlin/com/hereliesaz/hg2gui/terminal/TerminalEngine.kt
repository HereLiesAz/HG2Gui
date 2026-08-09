package com.hereliesaz.hg2gui.terminal

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

/**
 * Decides where a command runs, and collects what it prints: a fixed [Builtins] verb, the
 * bootstrap installer (the one command that streams progress rather than returning a single
 * result), or the real shell.
 */
class TerminalEngine(
    private val context: Context,
    home: File? = null
) {

    private val shell = ShellSession.forAndroid(home, context)

    // Only bootstrap still needs an HTTP client, now that RSS/weather/etc. are gone with the
    // rest of the legacy engine.
    private val client = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http"), (10 * 1024 * 1024).toLong()))
        .build()

    val workingDirectory: String get() = shell.workingDirectory

    /**
     * Runs [line] and returns a Flow of its output lines. [onNeedInput] is asked, suspending,
     * for an answer whenever the shell looks like it's stalled waiting on stdin - a real prompt,
     * not a hang. It bridges into ShellSession's blocking callback via runBlocking, which is
     * safe here since this whole branch already runs on a background dispatcher.
     */
    fun run(line: String, onNeedInput: suspend (prompt: String) -> String): Flow<String> = callbackFlow {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            close()
            return@callbackFlow
        }

        val verb = trimmed.substringBefore(' ')

        if (verb == "bootstrap") {
            launch(Dispatchers.IO) {
                DistroManager.bootstrap(context, client).collect { trySend(it) }
                close()
            }
        } else if (verb in Builtins.NAMES) {
            launch(Dispatchers.IO) {
                send(Builtins.run(context, trimmed))
                close()
            }
        } else {
            launch(Dispatchers.IO) {
                shell.stream(
                    trimmed,
                    onLine = { line -> trySend(line) },
                    onNeedInput = { prompt -> runBlocking { onNeedInput(prompt) } }
                )
                close()
            }
        }
        awaitClose { }
    }

    /**
     * Runs [line] to completion on the real shell and returns its final transcript plus exit
     * code - for headless one-shot commands (e.g. `pkg install` while resolving a `kind:"script"`
     * package's dependencies, see `azp/ScriptInstaller.kt`) that need to know whether the command
     * actually succeeded, unlike [run]'s `Flow<String>` (which streams the transcript but
     * discards the exit code `ShellSession.stream` itself returns). Always answers a stalled
     * interactive prompt with `null` (bail rather than hang) - there's no UI here to ask a human,
     * and a one-shot dependency install has no business waiting on one. Bypasses the
     * `bootstrap`/[Builtins] branches [run] has, since neither is a real shell command this needs
     * to route through.
     */
    suspend fun runToCompletion(line: String): Pair<String, Int> = withContext(Dispatchers.IO) {
        var transcript = ""
        val exitCode = shell.stream(line, onLine = { transcript = it }, onNeedInput = { null })
        transcript to exitCode
    }

    fun destroy() {
        shell.close()
    }
}
