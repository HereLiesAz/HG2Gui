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

    private fun escape(text: String): String = text.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")
    private fun unescape(text: String): String = text.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\")

    /** Call once a command has actually run (or been dispatched to a full-screen surface) -
     *  not for every keystroke, and not for a blank/whitespace-only line. */
    fun record(context: Context, sessionLabel: String, command: String, timestampMs: Long) {
        if (command.isBlank()) return
        val line = "$timestampMs\t${escape(sessionLabel)}\t${escape(command)}\n"
        val target = file(context)
        val existing = if (target.exists()) target.readLines() else emptyList()
        val trimmed = (existing + line.trimEnd('\n')).takeLast(MAX_ENTRIES)
        target.writeText(trimmed.joinToString("\n", postfix = "\n"))
    }

    /** Every recorded entry, most recent first. [query], when non-blank, keeps only commands
     *  containing it (case-insensitive) - the whole point of a *searchable* cross-session log. */
    fun search(context: Context, query: String = ""): List<CommandHistoryEntry> {
        val target = file(context)
        if (!target.exists()) return emptyList()
        val entries = target.readLines().mapNotNull { line -> parseLine(line) }
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
