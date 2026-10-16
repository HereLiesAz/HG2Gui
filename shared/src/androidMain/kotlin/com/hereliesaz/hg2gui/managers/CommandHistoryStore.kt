package com.hereliesaz.hg2gui.managers

import android.content.Context
import java.io.File

/**
 * W5 (docs/HG2Gui Termux Coverage.dc.html): "no cross-session scrollback/history search" -
 * SessionUiState.commandHistory already tracks up/down-arrow recall, but it's per-session and
 * in-memory only, gone the moment a tab closes or the app restarts. This is the missing piece:
 * a flat, append-only log every tab writes into, searchable across all of them.
 *
 * One tab-separated line per command - not JSON, not a database, matching this codebase's own
 * SharedPreferences-for-a-handful-of-scalars precedent (SshPresets, WorkflowStore) scaled up
 * one notch for an append-only log a preferences file isn't shaped for. Command text is escaped
 * so an embedded tab/newline (a pasted multi-line paste, rare but possible) can't corrupt the
 * line format.
 */
data class CommandHistoryEntry(val timestampMs: Long, val sessionLabel: String, val command: String)

object CommandHistoryStore {
    private const val FILE_NAME = "command_history.tsv"

    // Same shape as ShellSession's own D5 PENDING_TRIM_THRESHOLD doc comment - a cap that exists
    // so a long-lived install's history file can't grow without bound, not a meaningful limit on
    // how much history a search actually needs.
    private const val MAX_ENTRIES = 2000
    private const val PARTS_PER_LINE = 3

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    internal fun escape(text: String): String = buildString(text.length) {
        for (c in text) {
            when (c) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(c)
            }
        }
    }

    /** Exact inverse of [escape]: one left-to-right scan, so an escaped backslash followed by
     *  `n` (a literal `\n` in the command) is never re-read as a newline escape. */
    internal fun unescape(text: String): String = buildString(text.length) {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length) {
                when (text[i + 1]) {
                    '\\' -> append('\\')
                    't' -> append('\t')
                    'n' -> append('\n')
                    'r' -> append('\r')
                    else -> { append(c); append(text[i + 1]) }
                }
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }

    /** Call once a command has actually run (or been dispatched to a full-screen surface) -
     *  not for every keystroke, and not for a blank/whitespace-only line. */
    fun record(context: Context, sessionLabel: String, command: String, timestampMs: Long) {
        if (command.isBlank()) return
        val line = "$timestampMs\t${escape(sessionLabel)}\t${escape(command)}"
        // Every tab records into the one file: serialize the read-modify-write, and replace the
        // file atomically so a crash mid-write can't truncate the whole history.
        synchronized(this) {
            val target = file(context)
            val existing = if (target.exists()) target.readLines() else emptyList()
            val trimmed = (existing + line).takeLast(MAX_ENTRIES)
            val temp = File(target.parentFile, "$FILE_NAME.tmp")
            temp.writeText(trimmed.joinToString("\n", postfix = "\n"))
            if (!temp.renameTo(target)) {
                target.writeText(temp.readText())
                temp.delete()
            }
        }
    }

    /** Every recorded entry, most recent first. [query], when non-blank, keeps only commands
     *  containing it (case-insensitive) - the whole point of a *searchable* cross-session log. */
    fun search(context: Context, query: String = ""): List<CommandHistoryEntry> {
        val target = file(context)
        val lines = synchronized(this) { if (target.exists()) target.readLines() else return emptyList() }
        val entries = lines.mapNotNull { line -> parseLine(line) }
        val filtered = if (query.isBlank()) entries else entries.filter { it.command.contains(query, ignoreCase = true) }
        return filtered.sortedByDescending { it.timestampMs }
    }

    private fun parseLine(line: String): CommandHistoryEntry? {
        val parts = line.split("\t", limit = PARTS_PER_LINE)
        val timestamp = parts.getOrNull(0)?.toLongOrNull()
        if (parts.size != PARTS_PER_LINE || timestamp == null) return null
        return CommandHistoryEntry(timestamp, unescape(parts[1]), unescape(parts[2]))
    }
}
