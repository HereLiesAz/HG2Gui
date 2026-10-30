package com.hereliesaz.hg2gui.terminal

/** Shell-specific framing used by ShellSession's long-lived command channel. */
internal object ShellCommandProtocol {
    fun family(selected: String): ShellFamily = when (selected) {
        ShellPreference.BASH -> ShellFamily.BASH
        ShellPreference.ZSH -> ShellFamily.ZSH
        ShellPreference.FISH -> ShellFamily.FISH
        else -> ShellFamily.UNKNOWN
    }

    fun loginArgs(family: ShellFamily, executable: String): Array<String> = when (family) {
        ShellFamily.FISH -> arrayOf(executable, "-l", "-i")
        ShellFamily.BASH, ShellFamily.ZSH -> arrayOf(executable, "-l")
        else -> arrayOf(executable)
    }

    /**
     * Wraps [command] between a start marker ([startHead]+[sentinelTail]+`:<shell pid>`) and an end
     * marker ([sentinelHead]+[sentinelTail]+status:pwd). Each marker is printed as two quoted
     * halves, so a PTY's echo of the frame itself never contains a joined marker: in PTY mode
     * everything before the joined start marker is echo/prompt noise and is discarded.
     *
     * The user's text is never parsed as part of the frame. It travels as one quoted string that is
     * handed to `eval`, so a parse error in it (`echo (`, an unbalanced quote, a dangling `if`)
     * becomes an ordinary non-zero status for that one command. Pasted inline inside `{ ... }` it
     * used to take the whole frame down with it: a non-interactive shell exits on the syntax error
     * and an interactive one never reaches the end marker. POSIX-family shells (dash, mksh,
     * `bash --posix`) additionally exit on a syntax error inside the special built-in `eval`, hence
     * `command eval` there; zsh is excluded because its `command` modifier skips built-ins.
     * The end marker is therefore printed for every command the shell survives to finish, and the
     * only text that can still end the shell is one that asks to (`exit`, `exec`).
     */
    fun frame(family: ShellFamily, command: String, startHead: String, sentinelHead: String, sentinelTail: String): String = when (family) {
        ShellFamily.FISH -> buildString {
            append("begin\n")
            append(startMarker(startHead, sentinelTail, "\$fish_pid"))
            append("set -l __hg2gui_cmd ").append(fishQuote(command + "\n")).append('\n')
            append("eval \$__hg2gui_cmd\n")
            append("set -l __hg2gui_status \$status\n")
            append("printf '%s%s%d:%s\\n' '").append(sentinelHead).append("' '")
                .append(sentinelTail).append("' \$__hg2gui_status \$PWD\n")
            append("end\n")
        }
        else -> buildString {
            val eval = if (family == ShellFamily.ZSH) "eval" else "command eval"
            append("{\n")
            append(startMarker(startHead, sentinelTail, "\"\$\$\""))
            append("__hg2gui_cmd=").append(posixQuote(command + "\n")).append('\n')
            append(eval).append(" \"\$__hg2gui_cmd\"\n")
            append("__hg2gui_status=\$?\n")
            append("printf '%s%s%d:%s\\n' '").append(sentinelHead).append("' '")
                .append(sentinelTail).append("' \"\$__hg2gui_status\" \"\$PWD\"\n")
            append("unset __hg2gui_cmd __hg2gui_status\n")
            append("}\n")
        }
    }

    /**
     * Reads the shell pid that [frame]'s start marker appends after the joined marker
     * (`:1234`); null when it is absent or malformed. ShellSession uses it to find the foreground
     * children of a pipe-mode shell, which it cannot otherwise signal.
     */
    fun parseShellPid(markerSuffix: String): Int? {
        val trimmed = markerSuffix.trim()
        if (!trimmed.startsWith(":")) return null
        return trimmed.substring(1).toIntOrNull()?.takeIf { it > 1 }
    }

    /** [value] as one POSIX single-quoted word; the only character needing care is the quote itself. */
    fun posixQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

    /** [value] as one fish single-quoted word: inside them fish recognises only `\\` and `\'`. */
    fun fishQuote(value: String): String = "'${value.replace("\\", "\\\\").replace("'", "\\'")}'"

    private fun startMarker(startHead: String, sentinelTail: String, pidExpression: String): String =
        "printf '%s%s:%s\\n' '$startHead' '$sentinelTail' $pidExpression\n"
}
