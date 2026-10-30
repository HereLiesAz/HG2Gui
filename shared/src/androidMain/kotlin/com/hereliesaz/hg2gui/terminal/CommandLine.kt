package com.hereliesaz.hg2gui.terminal

/**
 * Pure (no Android) analysis of a typed command line, shared by [TerminalEngine]'s routing and
 * its tests.
 *
 * TerminalEngine used to key every policy decision (Disable, isolation, the package transaction
 * layer) on `line.substringBefore(' ')`, so `cd x && tool`, `env tool`, `time tool`, `a | tool`
 * and `bash -c tool` all slid past it. This is a small POSIX-shell lexer that finds *every*
 * command position - after `;`, `&&`, `||`, `|`, `&`, newlines, inside `( )`, `$( )`, backticks,
 * `<( )`, `bash -c '…'`, `eval …`, `find -exec …` and behind wrappers such as `env`/`time`/`sudo`
 * - so the engine can enforce on the command that will really run.
 *
 * What it deliberately cannot see (documented, not hidden): a command word produced by an
 * expansion (`$tool`, `$(which x)`), the contents of a sourced file or a script, shell functions
 * and aliases defined in the session, and commands another program spawns.
 */
internal object CommandLine {

    /** How a command position was reached; routing only trusts [DIRECT] for HG2Gui's own verbs. */
    enum class Kind {
        /** The command word of a plain stage (only `NAME=value` assignments before it). */
        DIRECT,

        /** Found behind a wrapper (`env`, `time`, `sudo`, `xargs`, …). */
        WRAPPED,

        /** Found inside `bash -c`, `eval`, `$( )`, backticks, `<( )` or `find -exec`. */
        NESTED
    }

    /** How a list segment is joined to the one before it. */
    enum class Connector { NONE, SEQ, AND, OR }

    /** One shell word: [value] is the unquoted text, [raw] exactly what was typed. */
    class Word(val value: String, val raw: String, val dynamic: Boolean)

    /**
     * A command that will run. [words] starts with the command word (unquoted) followed by its
     * arguments; [dynamic] is true when the command word itself is built by an expansion, so
     * [name] cannot be trusted.
     */
    class Invocation(val words: List<String>, val kind: Kind, val dynamic: Boolean) {
        val name: String get() = basename(words[0])
    }

    /** One simple command of a pipeline. [final] is the command that really runs (wrappers resolved). */
    class Stage(val assignments: Int, val words: List<Word>, val final: Invocation?)

    /** A top-level `;`/`&&`/`||`/newline separated piece of the line, verbatim in [text]. */
    class Segment(val connector: Connector, val text: String, val stages: List<Stage>)

    class ParsedLine(
        val segments: List<Segment>,
        val invocations: List<Invocation>,
        /** Control structures, subshells, background jobs, heredocs or dangling operators: do not split. */
        val complex: Boolean,
        /** An opening quote, `$(`, `${` or backtick was never closed. */
        val unterminated: Boolean,
        val hasHeredoc: Boolean
    )

    private const val MAX_DEPTH = 6

    fun basename(word: String): String = word.substringAfterLast('/')

    // ---------------------------------------------------------------------------------------
    // Plain word splitting (hg2package/hg2auth/remote arguments)
    // ---------------------------------------------------------------------------------------

    /**
     * POSIX-style word splitting with quotes and backslashes honoured and removed, and no
     * operators. Returns null for an unterminated quote instead of guessing what was meant.
     */
    fun words(line: String): List<String>? {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inWord = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c.isWhitespace() -> {
                    if (inWord) {
                        result += current.toString()
                        current.setLength(0)
                        inWord = false
                    }
                    i++
                }
                c == '\'' -> {
                    val end = line.indexOf('\'', i + 1)
                    if (end < 0) return null
                    current.append(line, i + 1, end)
                    inWord = true
                    i = end + 1
                }
                c == '"' -> {
                    i++
                    var closed = false
                    while (i < line.length) {
                        val d = line[i]
                        if (d == '"') {
                            closed = true
                            i++
                            break
                        }
                        if (d == '\\' && i + 1 < line.length && line[i + 1] in "\$`\"\\") {
                            current.append(line[i + 1])
                            i += 2
                        } else if (d == '\\' && i + 1 < line.length && line[i + 1] == '\n') {
                            i += 2
                        } else {
                            current.append(d)
                            i++
                        }
                    }
                    if (!closed) return null
                    inWord = true
                }
                c == '\\' -> {
                    if (i + 1 < line.length) {
                        if (line[i + 1] != '\n') {
                            current.append(line[i + 1])
                            inWord = true
                        }
                        i += 2
                    } else {
                        i++
                    }
                }
                else -> {
                    current.append(c)
                    inWord = true
                    i++
                }
            }
        }
        if (inWord) result += current.toString()
        return result
    }

    /**
     * Everything after the first [skipWords] words of [line], exactly as typed (quotes and
     * spacing intact). This is how `hg2auth root shell <command>` reaches `su -c` without being
     * split into words and glued back together, which used to drop the quotes that gave the
     * command its meaning. Null for an unterminated quote; empty when there is nothing after.
     */
    fun tail(line: String, skipWords: Int): String? {
        if (skipWords <= 0) return line.trim()
        var i = 0
        var seen = 0
        while (i < line.length && seen < skipWords) {
            while (i < line.length && line[i].isWhitespace()) i++
            if (i >= line.length) break
            while (i < line.length && !line[i].isWhitespace()) {
                when (line[i]) {
                    '\'' -> {
                        val end = line.indexOf('\'', i + 1)
                        if (end < 0) return null
                        i = end + 1
                    }
                    '"' -> {
                        i++
                        var closed = false
                        while (i < line.length) {
                            if (line[i] == '\\' && i + 1 < line.length) {
                                i += 2
                            } else if (line[i] == '"') {
                                closed = true
                                i++
                                break
                            } else {
                                i++
                            }
                        }
                        if (!closed) return null
                    }
                    '\\' -> i = minOf(i + 2, line.length)
                    else -> i++
                }
            }
            seen++
        }
        return line.substring(minOf(i, line.length)).trim()
    }

    // ---------------------------------------------------------------------------------------
    // Full command-line parse
    // ---------------------------------------------------------------------------------------

    fun parse(line: String): ParsedLine {
        val sink = Sink()
        val level = Lexer(line, sink, depth = 0, nested = false).run()
        return ParsedLine(
            segments = level.segments,
            invocations = sink.invocations,
            complex = level.complex || sink.complex,
            unterminated = level.unterminated,
            hasHeredoc = level.hasHeredoc || sink.heredoc
        )
    }

    private class Sink {
        val invocations = mutableListOf<Invocation>()
        var complex = false
        var heredoc = false
    }

    private class Level(
        val segments: List<Segment>,
        val complex: Boolean,
        val unterminated: Boolean,
        val hasHeredoc: Boolean
    )

    private val ASSIGNMENT = Regex("^[A-Za-z_][A-Za-z0-9_]*\\+?=")

    /** Reserved words that start a control structure; the words after them are commands again. */
    private val COMMAND_STARTING_RESERVED = setOf("if", "while", "until", "then", "else", "elif", "do", "!", "{", "coproc")

    /** Reserved words that only close or label a structure and never run anything. */
    private val INERT_RESERVED = setOf("fi", "done", "esac", "}", "in", ";;")

    /** Reserved words whose remaining words on the line are data, not commands. */
    private val DATA_RESERVED = setOf("for", "select", "case", "function")

    private class Lexer(
        private val text: String,
        private val sink: Sink,
        private val depth: Int,
        private val nested: Boolean
    ) {
        private val n = text.length
        private var i = 0

        private val value = StringBuilder()
        private var inWord = false
        private var wordStart = 0
        private var dynamic = false
        private var quotedAny = false

        private var stageWords = mutableListOf<Word>()
        private var stageAssignments = 0
        private var stageIgnored = false
        private var stages = mutableListOf<Stage>()
        private var redirectPending = false

        private val segments = mutableListOf<Segment>()
        private var segmentStart = 0
        private var segmentConnector = Connector.NONE
        private var complex = false
        private var unterminated = false
        private var heredoc = false

        private val commandPosition: Boolean get() = stageWords.isEmpty() && !stageIgnored

        fun run(): Level {
            while (i < n) {
                val c = text[i]
                when {
                    c == '\\' -> backslash()
                    c == '\'' -> singleQuote()
                    c == '"' -> doubleQuote()
                    c == '`' -> backtick()
                    c == '$' -> dollar()
                    c == '\n' -> {
                        endWord()
                        endStage()
                        endSegment(i, Connector.SEQ, 1)
                        i++
                    }
                    c == ' ' || c == '\t' || c == '\r' -> {
                        endWord()
                        i++
                    }
                    c == '#' && !inWord -> {
                        while (i < n && text[i] != '\n') i++
                    }
                    c == ';' -> {
                        endWord()
                        endStage()
                        val len = if (text.startsWith(";;&", i)) 3 else if (text.startsWith(";;", i) || text.startsWith(";&", i)) 2 else 1
                        if (len > 1) complex = true
                        endSegment(i, Connector.SEQ, len)
                        i += len
                    }
                    c == '&' -> ampersand()
                    c == '|' -> pipe()
                    c == '<' || c == '>' -> redirect()
                    c == '(' -> {
                        endWord()
                        // A subshell, `name() {…}` or `((…))`: never split these.
                        complex = true
                        endStage()
                        i++
                    }
                    c == ')' -> {
                        endWord()
                        complex = true
                        endStage()
                        i++
                    }
                    c == '*' || c == '?' || c == '[' -> {
                        startWord()
                        // An unquoted glob can name a different command than the one typed.
                        dynamic = true
                        value.append(c)
                        i++
                    }
                    else -> {
                        startWord()
                        value.append(c)
                        i++
                    }
                }
            }
            endWord()
            endStage()
            endSegment(n, Connector.NONE, 0)
            return Level(segments, complex, unterminated, heredoc)
        }

        // -- word construction --------------------------------------------------------------

        private fun startWord() {
            if (!inWord) {
                inWord = true
                wordStart = i
                value.setLength(0)
                dynamic = false
                quotedAny = false
            }
        }

        private fun endWord() {
            if (!inWord) return
            val raw = text.substring(wordStart, i)
            val word = Word(value.toString(), raw, dynamic)
            inWord = false
            value.setLength(0)
            addWord(word, quotedAny)
        }

        private fun addWord(word: Word, quoted: Boolean) {
            if (redirectPending) {
                redirectPending = false
                return
            }
            if (stageIgnored) {
                stageWords += word
                return
            }
            if (commandPosition && !quoted && !word.dynamic && ASSIGNMENT.containsMatchIn(word.raw)) {
                stageAssignments++
                return
            }
            if (commandPosition && !quoted && !word.dynamic) {
                val v = word.value
                when {
                    v == "[[" -> {
                        complex = true
                        stageIgnored = true
                        return
                    }
                    v in COMMAND_STARTING_RESERVED -> {
                        complex = true
                        return
                    }
                    v in INERT_RESERVED -> {
                        complex = true
                        return
                    }
                    v in DATA_RESERVED -> {
                        complex = true
                        stageIgnored = true
                        return
                    }
                }
            }
            stageWords += word
        }

        private fun endStage() {
            if (stageWords.isEmpty() && stageAssignments == 0) {
                stageIgnored = false
                redirectPending = false
                return
            }
            val words = stageWords
            val assignments = stageAssignments
            val ignored = stageIgnored
            stageWords = mutableListOf()
            stageAssignments = 0
            stageIgnored = false
            redirectPending = false
            val final = if (ignored || words.isEmpty()) null else resolve(words, Kind.DIRECT, depth, sink, nested)
            stages += Stage(assignments, words, final)
        }

        /**
         * Closes the current list segment at [at] and starts the next one after the [opLen]
         * operator characters. An operator with nothing on its right (`a &&`) or a doubled one is a
         * shell syntax error or continuation, so the line is marked complex rather than split.
         */
        private fun endSegment(at: Int, next: Connector, opLen: Int) {
            val segText = if (at > segmentStart) text.substring(segmentStart, at).trim() else ""
            if (segText.isNotEmpty()) {
                segments += Segment(segmentConnector, segText, stages)
            } else if (segmentConnector == Connector.AND || segmentConnector == Connector.OR) {
                complex = true
            } else if (next == Connector.AND || next == Connector.OR) {
                complex = true
            }
            stages = mutableListOf()
            segmentStart = minOf(at + opLen, n)
            segmentConnector = next
        }

        // -- operators -------------------------------------------------------------------------

        private fun ampersand() {
            if (text.startsWith("&>", i)) {
                // `&>file` / `&>>file`
                endWord()
                i += if (text.startsWith("&>>", i)) 3 else 2
                redirectPending = true
                return
            }
            if (text.startsWith("&&", i)) {
                endWord()
                endStage()
                endSegment(i, Connector.AND, 2)
                i += 2
                return
            }
            // A lone `&` backgrounds the job: keep the line whole.
            endWord()
            complex = true
            endStage()
            endSegment(i, Connector.SEQ, 1)
            i++
        }

        private fun pipe() {
            if (text.startsWith("||", i)) {
                endWord()
                endStage()
                endSegment(i, Connector.OR, 2)
                i += 2
                return
            }
            endWord()
            endStage()
            i += if (text.startsWith("|&", i)) 2 else 1
        }

        private fun redirect() {
            val c = text[i]
            // `<(…)` / `>(…)` process substitution is a word, not a redirection.
            if (i + 1 < n && text[i + 1] == '(') {
                startWord()
                dynamic = true
                val end = matchingParen(i + 1)
                if (end < 0) {
                    unterminated = true
                    i = n
                    return
                }
                nestedParse(text.substring(i + 2, end))
                value.append(text, i, end + 1)
                i = end + 1
                return
            }
            // A bare file-descriptor number right before the operator belongs to it (`2>&1`).
            if (inWord && value.isNotEmpty() && value.all { it.isDigit() } && wordStart + value.length == i) {
                inWord = false
                value.setLength(0)
            } else {
                endWord()
            }
            if (text.startsWith("<<<", i)) {
                i += 3
            } else if (text.startsWith("<<", i)) {
                heredoc = true
                complex = true
                i += if (text.startsWith("<<-", i)) 3 else 2
            } else if (c == '>' && (text.startsWith(">>", i))) {
                i += 2
            } else if (c == '>' && (text.startsWith(">&", i) || text.startsWith(">|", i))) {
                i += 2
            } else if (c == '<' && (text.startsWith("<&", i) || text.startsWith("<>", i))) {
                i += 2
            } else {
                i++
            }
            redirectPending = true
        }

        // -- quoting and expansion --------------------------------------------------------------

        private fun backslash() {
            startWord()
            if (i + 1 < n) {
                if (text[i + 1] != '\n') {
                    value.append(text[i + 1])
                    quotedAny = true
                }
                i += 2
            } else {
                i++
            }
        }

        private fun singleQuote() {
            startWord()
            quotedAny = true
            val end = text.indexOf('\'', i + 1)
            if (end < 0) {
                unterminated = true
                i = n
                return
            }
            value.append(text, i + 1, end)
            i = end + 1
        }

        private fun doubleQuote() {
            startWord()
            quotedAny = true
            i++
            while (i < n) {
                val d = text[i]
                when {
                    d == '"' -> {
                        i++
                        return
                    }
                    d == '\\' && i + 1 < n -> {
                        val e = text[i + 1]
                        if (e in "\$`\"\\") value.append(e) else if (e != '\n') value.append(d).append(e)
                        i += 2
                    }
                    d == '`' -> backtick()
                    d == '$' -> dollar()
                    else -> {
                        value.append(d)
                        i++
                    }
                }
            }
            unterminated = true
        }

        private fun backtick() {
            startWord()
            dynamic = true
            var j = i + 1
            while (j < n && text[j] != '`') {
                if (text[j] == '\\' && j + 1 < n) j++
                j++
            }
            if (j >= n) {
                unterminated = true
                i = n
                return
            }
            nestedParse(text.substring(i + 1, j).replace("\\`", "`"))
            value.append(text, i, j + 1)
            i = j + 1
        }

        private fun dollar() {
            startWord()
            if (i + 1 >= n) {
                value.append('$')
                i++
                return
            }
            val next = text[i + 1]
            when {
                next == '(' -> {
                    dynamic = true
                    val arithmetic = i + 2 < n && text[i + 2] == '('
                    val end = matchingParen(i + 1)
                    if (end < 0) {
                        unterminated = true
                        i = n
                        return
                    }
                    if (!arithmetic) nestedParse(text.substring(i + 2, end))
                    value.append(text, i, end + 1)
                    i = end + 1
                }
                next == '{' -> {
                    dynamic = true
                    val end = matchingBrace(i + 1)
                    if (end < 0) {
                        unterminated = true
                        i = n
                        return
                    }
                    // `${x:-$(cmd)}` still runs cmd.
                    scanSubstitutions(text.substring(i + 2, end))
                    value.append(text, i, end + 1)
                    i = end + 1
                }
                next == '\'' -> {
                    // ANSI-C quoting: $'a\'b'
                    quotedAny = true
                    var j = i + 2
                    while (j < n && text[j] != '\'') {
                        if (text[j] == '\\' && j + 1 < n) j++
                        j++
                    }
                    if (j >= n) {
                        unterminated = true
                        i = n
                        return
                    }
                    value.append(text, i + 2, j)
                    i = j + 1
                }
                next == '"' -> {
                    i++
                    doubleQuote()
                }
                else -> {
                    dynamic = true
                    value.append('$')
                    i++
                }
            }
        }

        /** Index of the `)` matching the `(` at [open], honouring quotes and nesting; -1 if none. */
        private fun matchingParen(open: Int): Int {
            var depthCount = 0
            var j = open
            while (j < n) {
                when (text[j]) {
                    '\\' -> j++
                    '\'' -> {
                        val end = text.indexOf('\'', j + 1)
                        if (end < 0) return -1
                        j = end
                    }
                    '"' -> {
                        j++
                        while (j < n && text[j] != '"') {
                            if (text[j] == '\\') j++
                            j++
                        }
                        if (j >= n) return -1
                    }
                    '(' -> depthCount++
                    ')' -> {
                        depthCount--
                        if (depthCount == 0) return j
                    }
                }
                j++
            }
            return -1
        }

        private fun matchingBrace(open: Int): Int {
            var depthCount = 0
            var j = open
            while (j < n) {
                when (text[j]) {
                    '\\' -> j++
                    '\'' -> {
                        val end = text.indexOf('\'', j + 1)
                        if (end < 0) return -1
                        j = end
                    }
                    '{' -> depthCount++
                    '}' -> {
                        depthCount--
                        if (depthCount == 0) return j
                    }
                }
                j++
            }
            return -1
        }

        /** Parses the commands inside `$( )`, backticks or `<( )` so they are policed too. */
        private fun nestedParse(body: String) {
            if (depth >= MAX_DEPTH) {
                sink.complex = true
                return
            }
            Lexer(body, sink, depth + 1, nested = true).run()
        }

        /** Finds `$(…)`/backticks inside a `${…}` body without treating the body as a command. */
        private fun scanSubstitutions(body: String) {
            var j = 0
            while (j < body.length) {
                val c = body[j]
                if (c == '\\') {
                    j += 2
                    continue
                }
                if (c == '$' && j + 1 < body.length && body[j + 1] == '(') {
                    val probe = Lexer(body, sink, depth + 1, nested = true)
                    val end = probe.matchingParen(j + 1)
                    if (end > 0) {
                        if (!(j + 2 < body.length && body[j + 2] == '(')) nestedParse(body.substring(j + 2, end))
                        j = end + 1
                        continue
                    }
                }
                if (c == '`') {
                    val end = body.indexOf('`', j + 1)
                    if (end > 0) {
                        nestedParse(body.substring(j + 1, end))
                        j = end + 1
                        continue
                    }
                }
                j++
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Resolving what a stage really runs
    // ---------------------------------------------------------------------------------------

    private val SHELLS = setOf("sh", "bash", "dash", "zsh", "ash", "ksh", "mksh", "fish")

    /** Wrapper name -> options that consume the next word. */
    private val WRAPPER_VALUE_OPTIONS: Map<String, Set<String>> = mapOf(
        "env" to setOf("-u", "-C", "--unset", "--chdir"),
        "sudo" to setOf(
            "-u", "-g", "-h", "-p", "-C", "-D", "-R", "-T", "-U", "-r", "-t",
            "--user", "--group", "--host", "--prompt", "--close-from", "--chdir", "--chroot",
            "--role", "--type", "--command-timeout", "--other-user"
        ),
        "doas" to setOf("-u", "-C"),
        "tsu" to emptySet(),
        "nice" to setOf("-n", "--adjustment"),
        "ionice" to setOf("-c", "-n", "-p", "-P", "-u"),
        "timeout" to setOf("-s", "-k", "--signal", "--kill-after"),
        "nohup" to emptySet(),
        "setsid" to emptySet(),
        "command" to emptySet(),
        "builtin" to emptySet(),
        "exec" to setOf("-a"),
        "time" to setOf("-f", "-o", "--format", "--output"),
        "stdbuf" to setOf("-i", "-o", "-e"),
        "unbuffer" to emptySet(),
        "watch" to setOf("-n", "--interval", "-w"),
        "xargs" to setOf(
            "-a", "-d", "-E", "-I", "-L", "-n", "-P", "-s",
            "--arg-file", "--delimiter", "--eof", "--replace", "--max-lines", "--max-args", "--max-procs", "--max-chars"
        )
    )

    /** Wrappers that take this many leading positional words before the command. */
    private val WRAPPER_POSITIONALS = mapOf("timeout" to 1)

    /** Wrappers that accept `NAME=value` words before the command. */
    private val WRAPPERS_WITH_ASSIGNMENTS = setOf("env", "sudo", "doas")

    /**
     * The index of the command a wrapper at [index] launches, or -1 when it launches none (a bare
     * `env`, or `command -v tool`, which only looks the name up).
     */
    private fun wrapperTarget(words: List<Word>, index: Int, name: String): Int {
        val valueOptions = WRAPPER_VALUE_OPTIONS[name] ?: return -1
        var j = index + 1
        var positionals = WRAPPER_POSITIONALS[name] ?: 0
        var lookupOnly = false
        while (j < words.size) {
            val w = words[j].value
            when {
                w == "--" -> {
                    j++
                    break
                }
                w.startsWith("-") && w.length > 1 -> {
                    if ((name == "command") && (w == "-v" || w == "-V")) lookupOnly = true
                    // `-S "cmd args"` on env hands a whole command string to env.
                    j += if (w in valueOptions) 2 else 1
                }
                name in WRAPPERS_WITH_ASSIGNMENTS && ASSIGNMENT.containsMatchIn(words[j].raw) -> j++
                positionals > 0 -> {
                    positionals--
                    j++
                }
                else -> break
            }
        }
        if (lookupOnly) return -1
        return if (j < words.size) j else -1
    }

    private fun resolve(
        words: List<Word>,
        startKind: Kind,
        depth: Int,
        sink: Sink,
        nested: Boolean
    ): Invocation? {
        var index = 0
        var kind = if (nested && startKind == Kind.DIRECT) Kind.NESTED else startKind
        var guard = 0
        while (index < words.size && guard++ < 12) {
            val w = words[index]
            val name = basename(w.value)
            if (!w.dynamic && name in WRAPPER_VALUE_OPTIONS) {
                val target = wrapperTarget(words, index, name)
                if (target < 0) return null
                if (kind == Kind.DIRECT) kind = Kind.WRAPPED
                index = target
                continue
            }
            val invocation = Invocation(words.drop(index).map { it.value }, kind, w.dynamic)
            sink.invocations += invocation
            if (!w.dynamic) followNested(name, words, index, depth, sink)
            return invocation
        }
        return null
    }

    /** Commands that carry another command line as an argument. */
    private fun followNested(name: String, words: List<Word>, index: Int, depth: Int, sink: Sink) {
        if (depth >= MAX_DEPTH) {
            sink.complex = true
            return
        }
        val args = words.drop(index + 1)
        fun parseScript(script: String) {
            val inner = Lexer(script, sink, depth + 1, nested = true)
            inner.run()
        }
        when {
            name in SHELLS || name == "su" -> {
                var k = 0
                while (k < args.size) {
                    val a = args[k].value
                    if (a == "--command") {
                        args.getOrNull(k + 1)?.let { parseScript(it.value) }
                        return
                    }
                    if (a.startsWith("-") && !a.startsWith("--") && a.length > 1) {
                        if ('c' in a.drop(1)) {
                            args.getOrNull(k + 1)?.let { parseScript(it.value) }
                            return
                        }
                        k += if (a in setOf("-o", "+o", "-O", "+O")) 2 else 1
                    } else if (a.startsWith("--")) {
                        k += if (a in setOf("--rcfile", "--init-file")) 2 else 1
                    } else if (name == "su") {
                        k++ // `su user -c cmd`: keep scanning for -c
                    } else {
                        return // a script file; its contents are not visible here
                    }
                }
            }
            name == "eval" -> {
                if (args.isNotEmpty()) parseScript(args.joinToString(" ") { it.value })
            }
            name == "find" -> {
                var k = 0
                while (k < args.size) {
                    val a = args[k].value
                    if (a == "-exec" || a == "-execdir" || a == "-ok" || a == "-okdir") {
                        var end = k + 1
                        while (end < args.size && args[end].value != ";" && args[end].value != "+") end++
                        val command = args.subList(k + 1, minOf(end, args.size))
                        if (command.isNotEmpty()) resolve(command, Kind.NESTED, depth + 1, sink, nested = true)
                        k = end
                    }
                    k++
                }
            }
        }
    }
}
