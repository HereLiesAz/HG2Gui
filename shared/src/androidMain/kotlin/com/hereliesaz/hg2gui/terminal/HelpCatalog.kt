package com.hereliesaz.hg2gui.terminal

import android.content.Context
import androidx.core.content.edit
import com.hereliesaz.hg2gui.util.Utils
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Flag/option hints for shell binaries, discovered by actually running `<binary> --help` instead
 * of relying only on [com.hereliesaz.hg2gui.ui.menu.CommandTree]'s own hand-curated
 * SHELL_HINTS - which only ever covered the handful of commands worth hand-picking for, leaving
 * every other binary on PATH with nothing but a bare file… child.
 *
 * [MenuNode.resolveChildren] runs synchronously during composition (see PillMenu's own
 * `remember(anchor.id) { anchor.resolveChildren?.invoke() }`) - spawning a process and blocking
 * on it there would jank the UI for however long that binary takes to print its help text (or
 * hang, for one that doesn't actually understand `--help`). So this isn't a live probe: [warm]
 * runs once in the background, well before any pill actually needs the result, and [hintsFor] -
 * the one function a resolveChildren lambda actually calls - only ever reads the cache [warm]
 * already filled in, never runs a process itself.
 */
object HelpCatalog {
    private const val PREFS_NAME = "hg2gui_help_cache"
    private const val FIELD_SEP = ""
    private const val TIMEOUT_SECONDS = 3L
    private const val MAX_FLAGS_PER_BINARY = 6
    private const val MAX_OUTPUT_CHARS = 8_000

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Cached flag hints for [binaryName] - empty before [warm] has covered it, or if none were
     *  found (no `--help` support, timed out, nothing recognizable in its output). Read-only and
     *  synchronous - safe to call from a resolveChildren lambda. */
    fun hintsFor(context: Context, binaryName: String): List<String> {
        val raw = prefs(context).getString(binaryName, null) ?: return emptyList()
        return if (raw.isEmpty()) emptyList() else raw.split(FIELD_SEP)
    }

    /** Runs `<binDir>/<name> --help` for every name in [binaries] not already cached (from a
     *  previous [warm] call, possibly a previous app run - the cache persists), parsing out
     *  whatever flags it can find. Meant to run off the main thread; blocks the calling thread
     *  for as long as each still-uncached binary takes to answer (bounded per-binary by
     *  [TIMEOUT_SECONDS]), not the composition PillMenu itself runs on. Safe to call repeatedly -
     *  already-cached names are skipped, so a second call after `apt install`ing something new
     *  only probes the newly-discovered binaries. */
    fun warm(context: Context, binDir: File, binaries: List<String>) {
        val p = prefs(context)
        for (name in binaries) {
            if (p.contains(name)) continue
            val flags = probe(File(binDir, name))
            p.edit { putString(name, flags.joinToString(FIELD_SEP)) }
        }
    }

    private fun probe(binary: File): List<String> {
        if (!binary.canExecute()) return emptyList()
        return try {
            val process = ProcessBuilder(binary.absolutePath, "--help")
                .redirectErrorStream(true)
                .start()
            // Nothing prompts for input on a --help run; closing stdin immediately signals EOF
            // to anything that (incorrectly) waits on it instead of hanging for the full timeout.
            process.outputStream.close()
            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            if (finished) {
                val output = process.inputStream.bufferedReader().readText().take(MAX_OUTPUT_CHARS)
                parseFlags(output)
            } else {
                emptyList()
            }
        } catch (e: IOException) {
            // This binary's --help either isn't there or blew up mid-run - skip it, same as a
            // timeout; there's nothing else worth doing with a probe failure.
            Utils.log(e)
            emptyList()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            emptyList()
        }
    }

    // A common getopt-style help line: leading whitespace, a flag token ("-h", "--help", "-o
    // FILE"), optionally several comma-separated short/long forms, then at least two spaces
    // before a description. Covers the overwhelming majority of GNU/BSD-style --help output;
    // anything shaped differently (a custom help format, a tool with no --help at all) just
    // contributes nothing here, same as it always did before this existed.
    private val FLAG_LINE = Regex("^\\s*(-[\\w-]+(?:,\\s*-[\\w-]+)*)(?:[= ]\\S+)?\\s{2,}\\S")

    private fun parseFlags(helpText: String): List<String> {
        val flags = LinkedHashSet<String>()
        helpText.lineSequence()
            .mapNotNull { line -> FLAG_LINE.find(line) }
            .forEach { match ->
                if (flags.size < MAX_FLAGS_PER_BINARY) {
                    match.groupValues[1].split(",").forEach { flags.add(it.trim()) }
                }
            }
        return flags.take(MAX_FLAGS_PER_BINARY)
    }
}
