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

    private const val MAX_COMMAND_NAME_COMPLETIONS = 5

    /** W1 (docs/HG2Gui Termux Coverage.dc.html): "tab key does nothing" for a package name still
     *  being typed - known command names (real PATH binaries plus this app's own builtins)
     *  starting with [prefix], nearest matches first, capped at [limit]. Only meaningful while
     *  [prefix] is still the whole line (no space yet) - the caller is responsible for that
     *  check, same as [autosuggest] leaves whole-vs-partial-line judgment to its own caller. */
    fun commandNameCompletions(prefix: String, known: List<String>, limit: Int = MAX_COMMAND_NAME_COMPLETIONS): List<String> {
        if (prefix.isBlank()) return emptyList()
        return known.filter { it != prefix && it.startsWith(prefix) }.sorted().take(limit)
    }

    private val NOT_FOUND_PATTERNS = listOf("not found", "no such file or directory")

    fun looksLikeNotFound(output: String): Boolean {
        val lower = output.lowercase()
        return NOT_FOUND_PATTERNS.any { lower.contains(it) }
    }

    // Matches the shapes real prompts actually use: "[y/N]", "(yes/no)", "Y/n?", etc.
    private val YES_NO_PATTERN = Regex("""(?i)[\[(]\s*y(?:es)?\s*/\s*n(?:o)?\s*[\])]|\by\s*/\s*n\b""")

    fun looksLikeYesNo(prompt: String): Boolean = YES_NO_PATTERN.containsMatchIn(prompt)

    // Matches ssh/sudo/su's own prompt text: "password:", "Enter passphrase for key '...'", etc.
    private val PASSWORD_PATTERN = Regex("""(?i)password\s*:|passphrase\s+for\s+key""")

    fun looksLikePassword(prompt: String): Boolean = PASSWORD_PATTERN.containsMatchIn(prompt)

    // A bracketed/parenthesized list of single-token choices more general than plain yes/no -
    // dpkg's conffile prompt ("(Y/I/N/O/D/Z)"), git's interactive add ("[y,n,q,a,d,e,?]"), any
    // tool offering a short menu inline rather than across several lines. Only the LAST such
    // group in the accumulated stall text is used - earlier lines can legitimately contain
    // unrelated bracketed text (explanatory prose above the actual prompt), but the real prompt
    // a stalled shell is actually waiting on is always the last thing printed.
    private val BRACKETED_CHOICES_PATTERN = Regex("""[\[(]\s*([A-Za-z0-9?]+(?:\s*[/,]\s*[A-Za-z0-9?]+)+)\s*[\])]""")
    private const val MAX_BRACKETED_CHOICES = 8

    /** The individual tokens of the last bracketed choice list in [prompt] (e.g. "(Y/I/N/O/D/Z)"
     *  -> ["Y","I","N","O","D","Z"]), or null if none - checked only after [looksLikeYesNo]
     *  itself comes back false, so a plain y/n prompt keeps its own dedicated YES/NO pair rather
     *  than a same-shaped generic list. */
    fun bracketedChoices(prompt: String): List<String>? {
        val match = BRACKETED_CHOICES_PATTERN.findAll(prompt).lastOrNull() ?: return null
        val choices = match.groupValues[1].split(Regex("""\s*[/,]\s*""")).map { it.trim() }.filter { it.isNotEmpty() }
        return choices.takeIf { it.size in 2..MAX_BRACKETED_CHOICES }
    }

    // A `select`-style numbered menu spanning several lines - "1) apple", "2) banana", ... -
    // the shape bash's own `select` builtin prints, along with tzselect/dpkg-reconfigure/
    // update-alternatives and similar. Requires at least two such lines before treating it as a
    // menu at all, since a single "1) " could just as easily be an ordinary numbered list in a
    // file a command happened to cat, not a prompt.
    private val NUMBERED_CHOICE_LINE = Regex("""(?m)^\s*(\d{1,3})[).]\s+(\S.*)$""")
    private const val MIN_NUMBERED_CHOICES = 2

    /** (number, label) pairs for a numbered menu found anywhere in [prompt] - so the UI can show
     *  what each number actually means instead of a bare digit - or null if fewer than
     *  [MIN_NUMBERED_CHOICES] such lines are present. */
    fun numberedMenuChoices(prompt: String): List<Pair<String, String>>? {
        val matches = NUMBERED_CHOICE_LINE.findAll(prompt)
            .map { it.groupValues[1] to it.groupValues[2].trim() }
            .toList()
        return matches.takeIf { it.size >= MIN_NUMBERED_CHOICES }
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
