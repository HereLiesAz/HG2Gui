package com.hereliesaz.hg2gui.terminal

/**
 * A shell-agnostic completion candidate. Shell/framework-specific completion systems are adapters
 * that produce these; HG2Gui surfaces only consume this model.
 *
 * [value] is the raw text (a file name, a branch, a host); [insertText] is what is typed into the
 * command line and therefore must be shell syntax. Leaving [insertText] at its default asks
 * [CompletionNormalizer.merge] to fill it with [value] quoted as one inert shell word
 * ([CompletionWords.quote]), so a name containing a space, `;`, `$(...)` or a backtick can never
 * split into extra arguments or run. A provider that sets [insertText] explicitly owns its quoting.
 *
 * [enumerationComplete] is deliberately explicit. A provider may know useful candidates without
 * knowing every valid value (for example hosts from known_hosts); only a complete enumeration may
 * replace open text input with a native choice-only surface.
 */
data class CompletionCandidate(
    val value: String,
    val label: String = value,
    val description: String? = null,
    val kind: CompletionKind = CompletionKind.OTHER,
    val source: CompletionSource = CompletionSource.OTHER,
    val insertText: String = value,
    val priority: Int = 0,
    val appendSpace: Boolean = true,
    val enumerationComplete: Boolean = false
)

enum class CompletionKind {
    COMMAND,
    SUBCOMMAND,
    OPTION,
    VALUE,
    FILE,
    DIRECTORY,
    PACKAGE,
    BRANCH,
    HOST,
    SERVICE,
    OTHER
}

enum class CompletionSource {
    BASH,
    ZSH,
    FISH,
    PACKAGE_MANAGER,
    COMMAND_PROTOCOL,
    FILESYSTEM,
    HISTORY,
    STATIC,
    OTHER
}

data class CompletionRequest(
    val line: String,
    val cursor: Int = line.length,
    val cwd: String? = null,
    val shell: ShellFamily = ShellFamily.UNKNOWN,
    val provider: ShellCompletionProvider = ShellCompletionProvider.NONE
) {
    val safeCursor: Int get() = cursor.coerceIn(0, line.length)
    val beforeCursor: String get() = line.substring(0, safeCursor)

    /** Index in [line] where the word under the cursor starts; quote and escape aware. */
    val tokenStart: Int get() = CompletionWords.currentTokenStart(beforeCursor)

    /**
     * The word under the cursor exactly as typed (quotes and backslashes included). This is the
     * same span [applyCompletion] replaces, so what is matched is what is overwritten.
     */
    val tokenPrefix: String get() = beforeCursor.substring(tokenStart)

    /**
     * [tokenPrefix] with the user's quoting removed (`My\ F` -> `My F`): the literal text to
     * compare against raw candidate values and to hand to a shell's completion primitive as data.
     */
    val tokenValue: String get() = CompletionWords.unquote(tokenPrefix)

    /** True when the word under the cursor is the first word of the line (a command name). */
    val isCommandPosition: Boolean get() = beforeCursor.substring(0, tokenStart).isBlank()
}

interface CompletionProvider {
    val source: CompletionSource
    suspend fun complete(request: CompletionRequest): List<CompletionCandidate>
}

class CompletionBridge(private val providers: List<CompletionProvider>) {
    suspend fun complete(request: CompletionRequest): List<CompletionCandidate> =
        CompletionNormalizer.merge(
            providers.flatMap { provider ->
                provider.complete(request).map { candidate ->
                    if (candidate.source == CompletionSource.OTHER) candidate.copy(source = provider.source) else candidate
                }
            }
        )
}

/** Parses and normalizes the common line-oriented completion formats emitted by shells. */
object CompletionNormalizer {
    fun parseFish(output: String): List<CompletionCandidate> =
        parseTabDelimited(output, CompletionSource.FISH)

    fun parseBash(output: String): List<CompletionCandidate> =
        parseTabDelimited(output, CompletionSource.BASH)

    fun parseZsh(output: String): List<CompletionCandidate> =
        parseTabDelimited(output, CompletionSource.ZSH)

    fun parseTabDelimited(output: String, source: CompletionSource): List<CompletionCandidate> =
        output.lineSequence()
            .map(String::trimEnd)
            .filter(String::isNotBlank)
            .mapNotNull { line ->
                val value = line.substringBefore('\t').trim()
                if (value.isBlank()) return@mapNotNull null
                val description = line.substringAfter('\t', "").trim().ifBlank { null }
                CompletionCandidate(
                    value = value,
                    description = description,
                    kind = inferKind(value, description),
                    source = source,
                    appendSpace = !value.endsWith('/')
                )
            }
            .toList()

    /**
     * Gives a candidate whose [CompletionCandidate.insertText] was left at its default the
     * shell-quoted form of its value. Safe to apply more than once: an already-quoted candidate has
     * `insertText != value` (or needed no quoting) and is returned unchanged.
     */
    fun shellSafe(candidate: CompletionCandidate): CompletionCandidate {
        if (candidate.insertText != candidate.value) return candidate
        val quoted = CompletionWords.quote(candidate.value)
        return if (quoted == candidate.insertText) candidate else candidate.copy(insertText = quoted)
    }

    /**
     * A file name that starts with `-` would be read by the command as an option once inserted
     * (`rm -rf` offered as a "file" out of a downloaded archive). When nothing has been typed yet,
     * such a name is offered as `./-name` instead; once the user has typed the dash themselves
     * they asked for it, and the name is left alone.
     */
    fun guardOptionLookalike(value: String, typedValue: String): String =
        if (typedValue.isEmpty() && value.startsWith("-")) "./$value" else value

    fun merge(candidates: List<CompletionCandidate>): List<CompletionCandidate> {
        val bestByInsert = linkedMapOf<String, CompletionCandidate>()
        candidates.map { shellSafe(it) }.forEach { candidate ->
            val key = candidate.insertText
            val old = bestByInsert[key]
            bestByInsert[key] = when {
                old == null -> candidate
                candidate.priority > old.priority -> candidate
                candidate.priority < old.priority -> old
                old.enumerationComplete.not() && candidate.enumerationComplete -> candidate
                old.description == null && candidate.description != null -> candidate
                else -> old
            }
        }
        return bestByInsert.values.sortedWith(
            compareByDescending<CompletionCandidate> { it.priority }
                .thenBy { kindRank(it.kind) }
                .thenBy { it.label.lowercase() }
        )
    }

    fun filterFor(request: CompletionRequest, candidates: List<CompletionCandidate>): List<CompletionCandidate> {
        val prefix = request.tokenPrefix
        if (prefix.isBlank()) return candidates
        // The typed word may be quoted/escaped (`My\ F`) while candidate values are raw, so a
        // candidate matches on its inserted syntax, its label, or its raw value vs the unquoted word.
        val typed = request.tokenValue
        return candidates.filter {
            it.insertText.startsWith(prefix, ignoreCase = true) ||
                it.label.startsWith(prefix, ignoreCase = true) ||
                it.value.startsWith(typed, ignoreCase = true)
        }
    }

    fun inferKind(value: String, description: String?): CompletionKind {
        val d = description.orEmpty().lowercase()
        return when {
            value.startsWith("--") || (value.startsWith('-') && value.length > 1) -> CompletionKind.OPTION
            value.endsWith('/') -> CompletionKind.DIRECTORY
            "directory" in d || "folder" in d -> CompletionKind.DIRECTORY
            "file" in d || value.contains('/') -> CompletionKind.FILE
            "branch" in d -> CompletionKind.BRANCH
            "package" in d -> CompletionKind.PACKAGE
            "host" in d || "hostname" in d -> CompletionKind.HOST
            "service" in d || "daemon" in d -> CompletionKind.SERVICE
            "subcommand" in d || "command" in d -> CompletionKind.SUBCOMMAND
            else -> CompletionKind.VALUE
        }
    }

    private fun kindRank(kind: CompletionKind): Int = when (kind) {
        CompletionKind.SUBCOMMAND -> 0
        CompletionKind.OPTION -> 1
        CompletionKind.BRANCH -> 2
        CompletionKind.PACKAGE -> 3
        CompletionKind.HOST -> 4
        CompletionKind.SERVICE -> 5
        CompletionKind.DIRECTORY -> 6
        CompletionKind.FILE -> 7
        CompletionKind.COMMAND -> 8
        CompletionKind.VALUE -> 9
        CompletionKind.OTHER -> 10
    }
}
