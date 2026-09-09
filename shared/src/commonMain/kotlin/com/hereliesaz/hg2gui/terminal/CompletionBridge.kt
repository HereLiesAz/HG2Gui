package com.hereliesaz.hg2gui.terminal

/**
 * A shell-agnostic completion candidate. Shell/framework-specific completion systems are adapters
 * that produce these; HG2Gui surfaces only consume this model.
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
    val tokenPrefix: String get() = beforeCursor.substringAfterLast(' ')
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

    fun merge(candidates: List<CompletionCandidate>): List<CompletionCandidate> {
        val bestByInsert = linkedMapOf<String, CompletionCandidate>()
        candidates.forEach { candidate ->
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
        return candidates.filter {
            it.insertText.startsWith(prefix, ignoreCase = true) ||
                it.label.startsWith(prefix, ignoreCase = true)
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
