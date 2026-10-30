package com.hereliesaz.hg2gui.ui

/**
 * The two sticky modifier keys on the terminal's key row. Tapping one *arms* it; the next
 * character typed into the command line is then interpreted as `CTRL+<char>` / `ALT+<char>`
 * instead of being inserted (see [ComposerKeys.apply]), and the key disarms itself.
 */
enum class ComposerModifier(val label: String) {
    Ctrl("CTRL"),
    Alt("ALT")
}

/**
 * What an armed modifier plus a typed character means for the command line. The composer is a
 * command *builder* (pills, tokens, a typed operand), not a PTY, so only the readline bindings
 * that make sense there are honoured; everything else is [Unsupported] and the screen says so
 * rather than silently swallowing the key.
 */
internal sealed interface ModifiedKey {
    /** Replace the typed operand with [text]. */
    data class Replace(val text: String) : ModifiedKey

    /** CTRL+C: stop the running command, or abandon the line being composed when idle. */
    object Interrupt : ModifiedKey

    /** CTRL+J / CTRL+M: the same as pressing RUN. */
    object Run : ModifiedKey

    /** CTRL+P: previous command from history. */
    object HistoryPrevious : ModifiedKey

    /** CTRL+N: next command from history. */
    object HistoryNext : ModifiedKey

    /** CTRL+D on an empty line: end of input for a command that is waiting on stdin. */
    object EndOfInput : ModifiedKey

    /** The combination has no meaning on this command line; [reason] is shown to the user. */
    data class Unsupported(val reason: String) : ModifiedKey
}

/**
 * Pure rules for the armed CTRL / ALT keys, kept out of the composable so they can be unit tested.
 * The command line exposes no caret (it is a plain `String` field whose caret always follows the
 * typed text), so every edit here acts on the *end* of the text, which is where readline's caret
 * sits for a line being typed.
 */
internal object ComposerKeys {
    /**
     * The single character the user just typed, or null. A soft keyboard reports a typed character
     * as the previous text with one character appended; anything else (a paste, a deletion, an edit
     * in the middle of the line) is not a keystroke and must not be reinterpreted.
     */
    fun detectTypedChar(before: String, after: String): Char? =
        if (after.length == before.length + 1 && after.startsWith(before)) after.last() else null

    /** The hint shown above the command line while [modifier] is armed. */
    fun hint(modifier: ComposerModifier): String = when (modifier) {
        ComposerModifier.Ctrl -> "CTRL armed - type C, D, H, J, N, P, U or W"
        ComposerModifier.Alt -> "ALT armed - type . (last argument), U, L or C"
    }

    /** Interprets [key] under [modifier] against the typed operand [text]. */
    fun apply(modifier: ComposerModifier, key: Char, text: String, lastCommand: String?): ModifiedKey {
        val k = key.lowercaseChar()
        return when (modifier) {
            ComposerModifier.Ctrl -> applyCtrl(k, text)
            ComposerModifier.Alt -> applyAlt(k, text, lastCommand)
        }
    }

    private fun applyCtrl(k: Char, text: String): ModifiedKey = when (k) {
        'c' -> ModifiedKey.Interrupt
        'u' -> ModifiedKey.Replace("")
        'w' -> ModifiedKey.Replace(deleteLastWord(text))
        'h' -> ModifiedKey.Replace(text.dropLast(1))
        'p' -> ModifiedKey.HistoryPrevious
        'n' -> ModifiedKey.HistoryNext
        'j', 'm' -> ModifiedKey.Run
        'd' -> if (text.isEmpty()) {
            ModifiedKey.EndOfInput
        } else {
            ModifiedKey.Unsupported("CTRL+D only ends input on an empty line")
        }
        else -> unsupported(ComposerModifier.Ctrl, k)
    }

    private fun applyAlt(k: Char, text: String, lastCommand: String?): ModifiedKey = when (k) {
        '.', '_' -> {
            val word = lastWord(lastCommand)
            if (word == null) {
                ModifiedKey.Unsupported("No previous command to take an argument from")
            } else {
                val base = if (text.isEmpty() || text.last().isWhitespace()) text else "$text "
                ModifiedKey.Replace(base + word)
            }
        }
        'u' -> ModifiedKey.Replace(mapLastWord(text) { it.uppercase() })
        'l' -> ModifiedKey.Replace(mapLastWord(text) { it.lowercase() })
        'c' -> ModifiedKey.Replace(mapLastWord(text) { word -> word.lowercase().replaceFirstChar { c -> c.uppercase() } })
        else -> unsupported(ComposerModifier.Alt, k)
    }

    private fun unsupported(modifier: ComposerModifier, k: Char): ModifiedKey =
        ModifiedKey.Unsupported("${modifier.label}+${k.uppercaseChar()} is not available on this command line")

    /** Readline's unix-word-rubout: drops trailing whitespace, then the word before it. */
    fun deleteLastWord(text: String): String {
        val trimmed = text.trimEnd()
        val boundary = trimmed.indexOfLast { it.isWhitespace() }
        return if (boundary < 0) "" else trimmed.substring(0, boundary + 1)
    }

    /** The last whitespace-separated word of [command], or null for a blank or missing command. */
    fun lastWord(command: String?): String? =
        command?.trim()?.split(WHITESPACE)?.lastOrNull()?.takeIf { it.isNotEmpty() }

    private fun mapLastWord(text: String, transform: (String) -> String): String {
        val trimmedEnd = text.trimEnd()
        val tail = text.substring(trimmedEnd.length)
        val boundary = trimmedEnd.indexOfLast { it.isWhitespace() }
        val head = trimmedEnd.substring(0, boundary + 1)
        val word = trimmedEnd.substring(boundary + 1)
        return head + transform(word) + tail
    }

    private val WHITESPACE = Regex("\\s+")
}
