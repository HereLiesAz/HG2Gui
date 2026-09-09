package com.hereliesaz.hg2gui.terminal

/**
 * Side-effect-free parser for the declarative subset of Bash/Zsh completion definitions.
 *
 * Completion functions are executable shell code, so HG2Gui never sources or invokes them merely
 * to populate UI. This parser intentionally recognizes only literal word sets that can be read as
 * data. Anything dynamic is ignored and the ordinary conservative completion fallback remains.
 */
object StaticCompletionDefinitions {
    private val bashWordSet = Regex(
        """(?:complete\s+[^\n]*?-W|compgen\s+-W)\s+([\"'])(.*?)\1""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )
    private val zshChoiceSet = Regex("""\([^()\n]+\)""")

    fun parseBash(text: String, prefix: String): List<CompletionCandidate> =
        bashWordSet.findAll(text)
            .flatMap { match -> literalWords(match.groupValues[2]).asSequence() }
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .distinct()
            .map { word ->
                CompletionCandidate(
                    value = word,
                    kind = CompletionNormalizer.inferKind(word, "bash static completion"),
                    source = CompletionSource.BASH,
                    priority = 22
                )
            }
            .toList()

    fun parseZsh(text: String, prefix: String): List<CompletionCandidate> =
        text.lineSequence()
            .filter { line -> "_arguments" in line || "_values" in line || "_describe" in line }
            .flatMap { line ->
                zshChoiceSet.findAll(line).flatMap { match -> literalWords(match.value.removeSurrounding("(", ")")).asSequence() }
            }
            .map { it.trim('"', '\'', '[', ']', '{', '}') }
            .filter { it.isNotBlank() && '$' !in it && '`' !in it && it.startsWith(prefix, ignoreCase = true) }
            .distinct()
            .map { word ->
                CompletionCandidate(
                    value = word,
                    kind = CompletionNormalizer.inferKind(word, "zsh static completion"),
                    source = CompletionSource.ZSH,
                    priority = 22
                )
            }
            .toList()

    private fun literalWords(raw: String): List<String> = raw
        .replace("\\ ", "\u0000")
        .split(Regex("\\s+"))
        .map { it.replace('\u0000', ' ').trim() }
        .filter { it.isNotBlank() && '$' !in it && '`' !in it && "$(" !in it && "\${" !in it }
}
