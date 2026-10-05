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
     * Wraps [command] between a start marker ([startHead]+[sentinelTail]) and an end marker
     * ([sentinelHead]+[sentinelTail]+status:pwd). Each marker is printed as two quoted halves, so
     * a PTY's echo of the frame itself never contains a joined marker: in PTY mode everything
     * before the joined start marker is echo/prompt noise and is discarded.
     */
    fun frame(family: ShellFamily, command: String, startHead: String, sentinelHead: String, sentinelTail: String): String = when (family) {
        ShellFamily.FISH -> buildString {
            append("begin\n")
            append(startMarker(startHead, sentinelTail))
            append(command).append('\n')
            append("set -l __hg2gui_status \$status\n")
            append("printf '%s%s%d:%s\\n' '").append(sentinelHead).append("' '")
                .append(sentinelTail).append("' \$__hg2gui_status \$PWD\n")
            append("end\n")
        }
        else -> buildString {
            append("{\n")
            append(startMarker(startHead, sentinelTail))
            append(command).append('\n')
            append("__hg2gui_status=\$?\n")
            append("printf '%s%s%d:%s\\n' '").append(sentinelHead).append("' '")
                .append(sentinelTail).append("' \"\$__hg2gui_status\" \"\$PWD\"\n")
            append("}\n")
        }
    }

    private fun startMarker(startHead: String, sentinelTail: String): String =
        "printf '%s%s\\n' '$startHead' '$sentinelTail'\n"
}
