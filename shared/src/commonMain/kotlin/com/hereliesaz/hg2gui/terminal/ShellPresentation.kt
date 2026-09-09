package com.hereliesaz.hg2gui.terminal

enum class ShellFamily {
    BASH,
    ZSH,
    FISH,
    SH,
    UNKNOWN
}

enum class PromptFramework {
    NONE,
    STARSHIP,
    OH_MY_ZSH,
    POWERLEVEL10K,
    PURE,
    CUSTOM
}

data class ShellPresentation(
    val shell: ShellFamily = ShellFamily.UNKNOWN,
    val framework: PromptFramework = PromptFramework.NONE,
    val cwd: String? = null,
    val user: String? = null,
    val host: String? = null,
    val gitBranch: String? = null,
    val gitDirty: Boolean = false,
    val lastExitCode: Int? = null,
    val rawPrompt: String? = null,
    val completionProvider: ShellCompletionProvider = ShellCompletionProvider.NONE
) {
    val hasStructuredStatus: Boolean
        get() = cwd != null || gitBranch != null || user != null || host != null || lastExitCode != null
}

enum class ShellCompletionProvider {
    NONE,
    BASH_COMPLETION,
    ZSH_COMPLETION,
    FISH_COMPLETION
}

/**
 * Conservative parser for prompt text emitted by shell/theme frameworks. This is deliberately
 * advisory: if a field cannot be inferred confidently it stays null and HG2Gui continues to
 * preserve the raw prompt as the source of truth.
 */
object ShellPromptParser {
    private val ansi = Regex("\u001B\\[[;?0-9]*[ -/]*[@-~]")
    private val gitParen = Regex("(?:git:)?\\(([^)]+)\\)")
    private val gitBranch = Regex("(?:|branch[: ]+|git[: ]+)([^\\s)]+)", RegexOption.IGNORE_CASE)
    private val userHost = Regex("(?:^|\\s)([A-Za-z0-9._-]+)@([A-Za-z0-9._-]+)(?:\\s|$)")
    private val pathLike = Regex("(?:^|\\s)(~(?:/[^\\s]*)?|/(?:[^\\s]+/?)*)(?:\\s|$)")
    private val exitCode = Regex("(?:exit|status)[:= ]+(\\d+)", RegexOption.IGNORE_CASE)

    fun parse(
        rawPrompt: String,
        shell: ShellFamily = ShellFamily.UNKNOWN,
        framework: PromptFramework = PromptFramework.NONE,
        fallbackCwd: String? = null
    ): ShellPresentation {
        val clean = rawPrompt.replace(ansi, "").replace('\r', ' ').replace('\n', ' ').trim()
        val userHostMatch = userHost.find(clean)
        val branch = gitBranch.find(clean)?.groupValues?.getOrNull(1)
            ?: gitParen.find(clean)?.groupValues?.getOrNull(1)?.takeUnless { it.contains(' ') }
        val dirty = clean.contains('✗') || clean.contains('*') || clean.contains("dirty", ignoreCase = true) || clean.contains('!')
        val cwd = pathLike.find(clean)?.groupValues?.getOrNull(1) ?: fallbackCwd
        return ShellPresentation(
            shell = shell,
            framework = framework,
            cwd = cwd,
            user = userHostMatch?.groupValues?.getOrNull(1),
            host = userHostMatch?.groupValues?.getOrNull(2),
            gitBranch = branch,
            gitDirty = dirty,
            lastExitCode = exitCode.find(clean)?.groupValues?.getOrNull(1)?.toIntOrNull(),
            rawPrompt = rawPrompt,
            completionProvider = when (shell) {
                ShellFamily.BASH -> ShellCompletionProvider.BASH_COMPLETION
                ShellFamily.ZSH -> ShellCompletionProvider.ZSH_COMPLETION
                ShellFamily.FISH -> ShellCompletionProvider.FISH_COMPLETION
                else -> ShellCompletionProvider.NONE
            }
        )
    }
}
