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
    private val home: File? = null
) {

    // var, not val: a fresh `bootstrap` run replaces this once the install actually lands (see
    // the "bootstrap" branch of run() below). Without that, a session that picked the
    // /system/bin/sh fallback before a bootstrap existed - or before this specific run's install
    // finished - stays pinned to it for the rest of its life: every command after that keeps
    // failing with the bare system shell's "inaccessible or not found", even though
    // CommandTree.from() re-scans the filesystem live on every render and correctly starts
    // offering apt/pkg/coreutils pills the moment the bootstrap directory is actually populated.
    private var shell = ShellSession.forAndroid(home, context)

    // Only bootstrap still needs an HTTP client, now that RSS/weather/etc. are gone with the
    // rest of the legacy engine. Shared across every TerminalEngine instance (one per terminal
    // tab, plus the MCP service, plus one-off uses like ScriptInstaller's `azp` installer) via
    // the companion object below - see its doc comment for why a per-instance client is unsafe.
    private val client = sharedHttpClient(context)

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
                // Re-pick the shell now that the bootstrap either just landed or, on failure,
                // was rolled back - forAndroid() re-checks DistroManager.isInstalled() and a
                // real bash's presence itself, so this converges to whatever's actually usable
                // now instead of leaving `shell` pinned to whatever was true at construction.
                val old = shell
                shell = ShellSession.forAndroid(home, context)
                old.close()
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
     * discards the exit code `ShellSession.stream` itself returns). Always declines a stalled
     * interactive prompt with `null` - there's no UI here to ask a human, and a one-shot
     * dependency install has no business waiting on one - but declining doesn't bail immediately:
     * [ShellSession.stream]'s own timeout is what actually ends it, so a stalled prompt here still
     * runs out the same clock a real hang would before this returns. Bypasses the
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

    companion object {
        // OkHttp's Cache/DiskLruCache is documented as unsafe to open twice concurrently against
        // the same directory from the same process. Every TerminalEngine used to build its own
        // OkHttpClient pointed at the identical `context.cacheDir/"http"` directory - with one
        // instance per terminal tab, one for the MCP service, and one for ScriptInstaller's `azp`
        // installs, two of those live at once (two tabs, a tab plus the MCP service, an install
        // alongside an open tab) raced the same DiskLruCache directory, risking IOExceptions or
        // cache corruption. OkHttpClient instances are meant to be shared/reused anyway, so this
        // lazily builds a single client (and single Cache) the first time any TerminalEngine
        // needs one, and every later instance reuses it. Double-checked locking keeps the common
        // case (already initialized) lock-free while still being safe if two TerminalEngines are
        // constructed concurrently on different threads.
        @Volatile
        private var instance: OkHttpClient? = null

        private fun sharedHttpClient(context: Context): OkHttpClient {
            return instance ?: synchronized(this) {
                instance ?: OkHttpClient.Builder()
                    .cache(Cache(File(context.applicationContext.cacheDir, "http"), (10 * 1024 * 1024).toLong()))
                    .build()
                    .also { instance = it }
            }
        }
    }
}
