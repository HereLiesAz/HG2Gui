package com.hereliesaz.hg2gui.terminal

/**
 * Commands that need an interactive PTY surface rather than flattened line-by-line output.
 * This list is only a launch hint. Once a program is running, the Adaptive TUI Wrapper decides
 * whether to replace the raw terminal grid from live terminal state, not from the command name.
 */
private val FULLSCREEN_BASE_COMMANDS = setOf(
    "vim", "vi", "nvim", "nano", "pico",
    "htop", "top",
    "tmux", "screen",
    "less", "more", "man",
    "python3", "python", "node", "irb", "ghci",
    "bash", "zsh", "fish",
    "ssh",
    "watch", "mc",
    // Modern interactive CLIs frequently render layered menus, prompts and alternate-screen
    // interfaces. Routing them to a PTY is a prerequisite for semantic wrapping.
    "claude", "gemini", "codex", "copilot", "gh"
)

/** Matches "git rebase -i"/"--interactive" specifically - every other `git` invocation (status,
 *  log, diff, commit, ...) is line-based output a one-shot pipe already renders correctly. */
private fun isInteractiveGitRebase(tokens: List<String>): Boolean {
    if (tokens.getOrNull(0) != "git" || tokens.getOrNull(1) != "rebase") return false
    return tokens.drop(2).any { it == "-i" || it == "--interactive" }
}

/** Matches GitHub Copilot's interactive suggestion/explain surfaces without forcing every `gh`
 * invocation into a full-screen PTY. */
private fun isInteractiveGh(tokens: List<String>): Boolean =
    tokens.getOrNull(0) == "gh" && tokens.getOrNull(1) == "copilot"

/** The leading token of [commandLine] if it names a command that needs the full-screen pty
 *  surface, or null if it's plain one-shot output the existing flattened transcript already
 *  renders correctly. Pure text logic - no pty/session knowledge - so the dispatch decision is
 *  unit-testable without an Android runtime. */
fun fullScreenCommandOf(commandLine: String): String? {
    val tokens = commandLine.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val leading = tokens.firstOrNull()
    return when {
        leading == null -> null
        leading == "gh" && !isInteractiveGh(tokens) -> null
        leading in FULLSCREEN_BASE_COMMANDS -> leading
        isInteractiveGitRebase(tokens) -> "git rebase -i"
        else -> null
    }
}
