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

    fun frame(family: ShellFamily, command: String, sentinelHead: String, sentinelTail: String): String = when (family) {
        ShellFamily.FISH -> buildString {
            append("begin\n")
            append(command).append('\n')
            append("set -l __hg2gui_status $status\n")
            append("printf '%s%s%d:%s\\n' '").append(sentinelHead).append("' '")
                .append(sentinelTail).append("' $__hg2gui_status $PWD\n")
            append("end\n")
        }
        else -> buildString {
            append("{\n")
            append(command).append('\n')
            append("__hg2gui_status=$?\n")
            append("printf '%s%s%d:%s\\n' '").append(sentinelHead).append("' '")
                .append(sentinelTail).append("' \"$__hg2gui_status\" \"$PWD\"\n")
            append("}\n")
        }
    }
}
