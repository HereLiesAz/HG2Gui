package com.hereliesaz.hg2gui.terminal

/**
 * Shell-word mechanics shared by completion: where the word under the cursor starts, what it means
 * once the user's quotes/escapes are removed, and how to turn an arbitrary candidate string back
 * into one inert shell word.
 *
 * Completion candidates are data from places the user does not control (file names out of a
 * downloaded archive, branch names out of a cloned repo, `known_hosts` lines, package lists).
 * Inserting such a string raw lets `a b` become two arguments and `x;rm -rf ~`, `$(cmd)` or a
 * backtick run something the moment the user presses RUN. [quote] makes every candidate exactly one
 * literal word, and [currentTokenStart]/[unquote] make the quoted form round-trip so the user can
 * keep completing inside it (`cd My\ Dir/` and then Tab again).
 *
 * Pure text logic, no platform types, so it is unit-testable without an Android runtime.
 */
object CompletionWords {
    /** Punctuation that never has meaning to a POSIX/bash/zsh/fish word, so it is left unescaped. */
    private const val SAFE_PUNCTUATION = "_-.,/:=@%+~"

    private const val NONE = 0
    private const val SINGLE = 1
    private const val DOUBLE = 2
    private const val ANSI_C = 3

    /**
     * True when [c] can appear in a shell word with no quoting. Surrogates count as safe so a
     * non-BMP character (an emoji in a file name) is never split by an inserted backslash.
     */
    fun isSafeChar(c: Char): Boolean =
        c.isLetterOrDigit() || c in SAFE_PUNCTUATION || c.isSurrogate()

    /**
     * Returns [value] as exactly one literal shell word. Each unsafe character is backslash-escaped
     * (so the result stays a prefix-editable token); a value holding a control character (a file
     * name with a newline or tab, which a backslash cannot express) becomes one `$'...'` ANSI-C
     * string instead. An empty value becomes `''`. Idempotence is the caller's job: quote a raw
     * value once.
     */
    fun quote(value: String): String {
        if (value.isEmpty()) return "''"
        if (value.any { isControl(it) }) return ansiCQuote(value)
        val out = StringBuilder(value.length + 4)
        for (c in value) {
            if (!isSafeChar(c)) out.append('\\')
            out.append(c)
        }
        return out.toString()
    }

    /**
     * Index in [before] where the word being typed starts: just after the last whitespace that is
     * not escaped (`\ `) and not inside single, double or `$'...'` quotes. Cursor text only; an
     * unterminated quote simply keeps the word open to the end.
     */
    fun currentTokenStart(before: String): Int {
        var start = 0
        var state = NONE
        var dollar = false
        var i = 0
        while (i < before.length) {
            val c = before[i]
            when (state) {
                NONE -> {
                    when {
                        c == '\\' -> i++
                        c == '\'' -> state = if (dollar) ANSI_C else SINGLE
                        c == '"' -> state = DOUBLE
                        c.isWhitespace() -> start = i + 1
                    }
                    dollar = c == '$' && state == NONE
                }
                SINGLE -> if (c == '\'') state = NONE
                DOUBLE -> when (c) {
                    '\\' -> i++
                    '"' -> state = NONE
                }
                else -> when (c) {
                    '\\' -> i++
                    '\'' -> state = NONE
                }
            }
            i++
        }
        return start
    }

    /**
     * What a (possibly partial) shell word means after the shell removes quoting: `My\ F` and
     * `"My F` both give `My F`; `$'a\tb'` gives `a<TAB>b`. Used to match the user's text against
     * raw candidate values and as the data handed to a shell's own completion primitive. Expansions
     * are not performed: `$HOME` stays the six characters it is.
     */
    fun unquote(token: String): String {
        val out = StringBuilder(token.length)
        var state = NONE
        var dollar = false
        var i = 0
        while (i < token.length) {
            val c = token[i]
            when (state) {
                NONE -> {
                    when {
                        c == '\\' -> {
                            if (i + 1 < token.length) {
                                i++
                                out.append(token[i])
                            }
                        }
                        c == '\'' -> if (dollar) {
                            out.setLength(out.length - 1)
                            state = ANSI_C
                        } else {
                            state = SINGLE
                        }
                        c == '"' -> state = DOUBLE
                        else -> out.append(c)
                    }
                    dollar = c == '$' && state == NONE
                }
                SINGLE -> if (c == '\'') state = NONE else out.append(c)
                DOUBLE -> when {
                    c == '"' -> state = NONE
                    c == '\\' && i + 1 < token.length && token[i + 1] in DOUBLE_QUOTE_ESCAPES -> {
                        i++
                        out.append(token[i])
                    }
                    else -> out.append(c)
                }
                else -> when {
                    c == '\'' -> state = NONE
                    c == '\\' && i + 1 < token.length -> i = decodeAnsiEscape(token, i + 1, out)
                    else -> out.append(c)
                }
            }
            i++
        }
        return out.toString()
    }

    private const val DOUBLE_QUOTE_ESCAPES = "\$`\"\\\n"

    private fun isControl(c: Char): Boolean = c.code < 0x20 || c.code == 0x7f

    private fun ansiCQuote(value: String): String {
        val out = StringBuilder(value.length + 6)
        out.append("\$'")
        for (c in value) {
            when {
                c == '\\' -> out.append("\\\\")
                c == '\'' -> out.append("\\'")
                c == '\n' -> out.append("\\n")
                c == '\t' -> out.append("\\t")
                c == '\r' -> out.append("\\r")
                isControl(c) -> out.append("\\x").append(c.code.toString(16).padStart(2, '0'))
                else -> out.append(c)
            }
        }
        out.append('\'')
        return out.toString()
    }

    /**
     * Decodes the escape whose first character after the backslash is at [at] and appends the
     * result to [out]. Returns the index of the last character consumed.
     */
    private fun decodeAnsiEscape(s: String, at: Int, out: StringBuilder): Int {
        val c = s[at]
        when (c) {
            'n' -> out.append('\n')
            't' -> out.append('\t')
            'r' -> out.append('\r')
            'a' -> out.append('\u0007')
            'b' -> out.append('\b')
            'e', 'E' -> out.append('\u001b')
            'f' -> out.append('\u000c')
            'v' -> out.append('\u000b')
            '\\', '\'', '"', '?' -> out.append(c)
            'x' -> {
                var end = at + 1
                while (end < s.length && end < at + 3 && s[end].digitToIntOrNull(16) != null) end++
                if (end == at + 1) {
                    out.append("\\x")
                    return at
                }
                out.append(s.substring(at + 1, end).toInt(16).toChar())
                return end - 1
            }
            in '0'..'7' -> {
                var end = at
                while (end < s.length && end < at + 3 && s[end] in '0'..'7') end++
                out.append(s.substring(at, end).toInt(8).toChar())
                return end - 1
            }
            else -> out.append('\\').append(c)
        }
        return at
    }
}
