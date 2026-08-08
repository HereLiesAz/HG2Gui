package com.hereliesaz.hg2gui.terminal

/**
 * Native, Kotlin-side reimplementation of the handful of oh-my-zsh convenience features the
 * app actually wants (short git/ls/cd aliases, "did you mean", history autosuggestion). This
 * works identically no matter which shell tier ShellSession picked - bundled zsh, bootstrapped
 * bash, or bare /system/bin/sh - because none of it depends on the shell's own alias table or
 * line editor, only on the app's own command history and the line the UI is about to send.
 */
object ShellAliases {
    val table: Map<String, String> = mapOf(
        "gs" to "git status",
        "ga" to "git add",
        "gaa" to "git add --all",
        "gc" to "git commit",
        "gcm" to "git commit -m",
        "gp" to "git push",
        "gl" to "git pull",
        "glog" to "git log --oneline --graph --decorate",
        "gd" to "git diff",
        "gco" to "git checkout",
        "gb" to "git branch",
        "ll" to "ls -la",
        "la" to "ls -a",
        "l" to "ls -CF",
        ".." to "cd ..",
        "..." to "cd ../..",
        "c" to "clear",
        "h" to "history"
    )

    /** Expands a leading alias word in [line] to its full form; a no-op if none matches. */
    fun expand(line: String): String {
        val firstSpace = line.indexOf(' ')
        val head = if (firstSpace == -1) line else line.substring(0, firstSpace)
        val expansion = table[head] ?: return line
        val rest = if (firstSpace == -1) "" else line.substring(firstSpace)
        return expansion + rest
    }

    /**
     * After a command has run, the shortest key whose expansion [ranLine] just spelled out in
     * full - so the UI can hint "you could have typed gs" - or null if no alias would have
     * shortened it (including when the alias was already used).
     */
    fun hintForRanCommand(ranLine: String): Pair<String, String>? =
        table.entries
            .filter { (_, expansion) -> ranLine == expansion || ranLine.startsWith("$expansion ") }
            .maxByOrNull { it.value.length }
            ?.toPair()

    /** The most recent history entry sharing [input] as a prefix, minus that prefix - or null. */
    fun autosuggest(input: String, history: List<String>): String? {
        if (input.isBlank()) return null
        for (i in history.indices.reversed()) {
            val h = history[i]
            if (h.length > input.length && h.startsWith(input)) return h.removePrefix(input)
        }
        return null
    }

    private val NOT_FOUND_PATTERNS = listOf("not found", "no such file or directory")

    fun looksLikeNotFound(output: String): Boolean {
        val lower = output.lowercase()
        return NOT_FOUND_PATTERNS.any { lower.contains(it) }
    }

    /** Closest known command word to [failed], among alias keys/expansions plus [known] extras. */
    fun didYouMean(failed: String, known: List<String> = emptyList()): String? {
        if (failed.isBlank() || table.containsKey(failed)) return null
        val candidates = (table.keys + table.values.map { it.substringBefore(' ') } + known)
            .distinct()
            .filter { it.isNotBlank() && it != failed }
        return candidates
            .map { it to levenshtein(failed, it) }
            .filter { (candidate, dist) -> dist <= (candidate.length / 2 + 1).coerceAtLeast(2) }
            .minByOrNull { it.second }
            ?.first
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[a.length][b.length]
    }
}
