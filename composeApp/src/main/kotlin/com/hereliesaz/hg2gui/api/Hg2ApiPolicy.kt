package com.hereliesaz.hg2gui.api

import java.security.MessageDigest

/*
 * Pure (no Android types) policy and formatting helpers behind the signature-permission API. They
 * live apart from Hg2ApiReceiver and the three API activities so Hg2ApiPolicyTest can exercise
 * them as plain JVM unit tests - android.jar is stubbed there, so anything touching Intent, JSON or
 * Log would throw "not mocked".
 */

/** First notification id of the range [clampNotificationId] confines NOTIFY to. */
internal const val NOTIFY_ID_BASE = 0x60000

/** Width of the NOTIFY id range - ids land in [NOTIFY_ID_BASE, NOTIFY_ID_BASE + NOTIFY_ID_SPAN). */
internal const val NOTIFY_ID_SPAN = 0x10000

/**
 * Maps a caller-chosen NOTIFY `id` into a private range no HG2Gui-owned notification uses, so an
 * external app can neither overwrite nor cancel the foreground-service notifications (2101 long
 * tasks, 4201 MCP server), the update notifications (2001/2002) or the activity-launch prompts
 * (0x4A000..0x59FFF, see Hg2ApiReceiver.launchOrNotify). The same caller `id` always maps to the
 * same slot, so re-notifying with one id still updates that notification in place.
 */
internal fun clampNotificationId(requested: Int): Int = NOTIFY_ID_BASE + (requested and (NOTIFY_ID_SPAN - 1))

/**
 * Whether the clipboard can honestly be read right now. Android 10+ hands a background app an
 * empty/null primary clip rather than an error, so an unconditional read used to report
 * "success, text is empty" when the truth was "not allowed" - the caller could not tell a blank
 * clipboard from a blocked read.
 */
internal fun clipboardReadPossible(sdkInt: Int, appInForeground: Boolean): Boolean =
    sdkInt < ANDROID_10 || appInForeground

private const val ANDROID_10 = 29

/** Longest command an authority (root/ADB) request may carry - and so the longest the approval dialog must show. */
internal const val MAX_AUTHORITY_COMMAND_CHARS = 8_192

/**
 * The `hg2auth ...` line the in-process TerminalEngine parses for an approved root/ADB command,
 * and the exact command text that line yields once the engine has tokenized it.
 *
 * The engine does not take a structured argument: it re-tokenizes the line with a small regex that
 * strips one layer of quotes and re-joins the words with single spaces. Handing it the raw command
 * therefore lets the text that runs differ from the text the approval dialog showed (the old
 * dialog showed `sh -c "cd /x && rm -rf *"`, the engine ran `sh -c cd /x && rm -rf *`).
 * [lossless] closes that gap from this side: it quotes the command so the engine's tokenizer
 * returns it unchanged, and proves that by running a port of the tokenizer over the result. A
 * command that cannot be carried unchanged is refused, never approximated.
 *
 * [engineWords] must stay equivalent to `TerminalEngine.shellWords`. Hg2ApiAuthorityActivity does
 * not rely on that equivalence alone: it also compares the confirmation prompt the engine actually
 * emits against [expectedPrompt] and refuses to answer "y" to anything else.
 */
internal object Hg2ApiAuthorityCommand {

    private val WORD = Regex("""(?:[^\s"']+|"[^"]*"|'[^']*')+""")

    /** Port of TerminalEngine.shellWords: whitespace-split, quotes kept together, ONE layer stripped per quote kind. */
    fun engineWords(line: String): List<String> = WORD.findAll(line)
        .map { it.value.trim().removeSurrounding("\"").removeSurrounding("'") }
        .toList()

    /** The command text the engine would execute for [line]: every word after `hg2auth <authority> shell`, space-joined. */
    fun effectiveCommand(line: String): String = engineWords(line).drop(3).joinToString(" ").trim()

    /**
     * `hg2auth <authority> shell <quoted command>` for [command] (trimmed first - the engine trims
     * too), or null when [authority] is unknown, the command is blank/too long, or no quoting
     * survives the engine's tokenizer unchanged.
     */
    fun lossless(authority: String, command: String): String? {
        if (authority != "root" && authority != "adb") return null
        val wanted = command.trim()
        if (wanted.isEmpty() || wanted.length > MAX_AUTHORITY_COMMAND_CHARS) return null
        val candidates = listOf("'$wanted'", "\"$wanted\"", wanted)
        for (candidate in candidates) {
            val line = "hg2auth $authority shell $candidate"
            if (effectiveCommand(line) == wanted) return line
        }
        return null
    }

    /** The confirmation prompt TerminalEngine.runAuthority emits before it runs [command] (see TerminalEngine "root"/"adb" shell branches). */
    fun expectedPrompt(authority: String, command: String): String =
        if (authority == "root") "Run as root? $command [y/N]" else "Run through ADB shell? $command [y/N]"
}

/**
 * Lifecycle-style guard for an EXECUTE command line, layered over the engine's own headless policy
 * as defence in depth (the engine is the authority; this only stops the obvious before it gets there).
 * Deny-by-default tokenization: split on whitespace and shell control/substitution characters,
 * drop quotes/backslashes, compare basenames case-insensitively - the same scheme as
 * [containsForegroundOnlyCommand]. Denied when:
 *  - any token is `hg2package` and the token after it is not an operation [lifecycleOperationAllowed]
 *    accepts (so `hg2package disable ...`, `isolate`, `update`, and a bare/unparseable `hg2package`);
 *  - a `vfs` token is directly followed by `mount` (needs a root prompt, never headless).
 * Deliberately over-broad: a false positive just means the caller runs that command with HG2Gui in the foreground.
 */
internal fun containsBackgroundDeniedCommand(command: String, appInForeground: Boolean): Boolean {
    val tokens = command.split(Regex("""[\s;|&()`<>{}$=]+"""))
        .map { it.replace(Regex("""["'\\]"""), "").substringAfterLast('/').lowercase() }
        .filter(String::isNotEmpty)
    for ((index, token) in tokens.withIndex()) {
        val next = tokens.getOrNull(index + 1)
        if (token == "hg2package" && (next == null || !lifecycleOperationAllowed(next, appInForeground))) return true
        if (token == "vfs" && next == "mount") return true
    }
    return false
}

/** Hex SHA-256 of [text] truncated to [chars] hex digits - enough to correlate an audit row with a command without storing it. */
internal fun commandDigest(text: String, chars: Int = 16): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(chars)

/** [text] cut to [max] characters with an ellipsis when it was longer - for the bounded audit preview of an approved command. */
internal fun auditPreview(text: String, max: Int = 200): String =
    if (text.length <= max) text else text.take(max) + "..."

/** Keeps the newest [max] non-blank lines of an ndjson audit file (oldest dropped first). */
internal fun retainAuditLines(lines: List<String>, max: Int): List<String> =
    lines.filter(String::isNotBlank).takeLast(max)
