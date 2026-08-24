package com.hereliesaz.hg2gui.terminal

/**
 * S1 (docs/HG2Gui Termux Coverage.dc.html): the commands that draw a full screen against a real
 * tty - a pager, an editor, a system monitor, a multiplexer, a bare REPL - rather than printing
 * line-by-line output a one-shot pipe can already handle. Dispatched to a dedicated pty-backed
 * surface instead of the normal flattened transcript; see FullScreenPtySession/
 * FullScreenTerminalScreen (androidMain) for the surface itself.
 */
private val FULLSCREEN_BASE_COMMANDS = setOf(
    "vim", "vi", "nvim", "nano", "pico",
    "htop", "top",
    "tmux", "screen",
    "less", "more", "man",
    "python3", "python", "node", "irb", "ghci",
    "ssh",
    "watch", "mc"
)

/** Matches "git rebase -i"/"--interactive" specifically - every other `git` invocation (status,
 *  log, diff, commit, ...) is line-based output a one-shot pipe already renders correctly. */
private fun isInteractiveGitRebase(tokens: List<String>): Boolean {
    if (tokens.getOrNull(0) != "git" || tokens.getOrNull(1) != "rebase") return false
    return tokens.drop(2).any { it == "-i" || it == "--interactive" }
}

/** The leading token of [commandLine] if it names a command that needs the full-screen pty
 *  surface, or null if it's plain one-shot output the existing flattened transcript already
 *  renders correctly. Pure text logic - no pty/session knowledge - so the dispatch decision is
 *  unit-testable without an Android runtime. */
fun fullScreenCommandOf(commandLine: String): String? {
    val tokens = commandLine.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val leading = tokens.firstOrNull()
    return when {
        leading == null -> null
        leading in FULLSCREEN_BASE_COMMANDS -> leading
        isInteractiveGitRebase(tokens) -> "git rebase -i"
        else -> null
    }
}
