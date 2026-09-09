package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File

/**
 * Detects shell/prompt frameworks from the installed runtime and home-directory configuration.
 * The result describes presentation/completion capabilities only; it never changes which shell
 * HG2Gui executes and never grants a theme control over HG2Gui's native UI.
 */
object ShellAdapterRegistry {
    fun detect(context: Context, cwd: String? = null): ShellPresentation {
        val home = DistroManager.homeDir(context)
        val prefix = DistroManager.prefixDir(context)
        val shell = detectShell(home, prefix)
        val framework = detectFramework(home)
        return ShellPresentation(
            shell = shell,
            framework = framework,
            cwd = cwd ?: home.absolutePath,
            completionProvider = when (shell) {
                ShellFamily.BASH -> ShellCompletionProvider.BASH_COMPLETION
                ShellFamily.ZSH -> ShellCompletionProvider.ZSH_COMPLETION
                ShellFamily.FISH -> ShellCompletionProvider.FISH_COMPLETION
                else -> ShellCompletionProvider.NONE
            }
        )
    }

    fun mergePrompt(base: ShellPresentation, rawPrompt: String, fallbackCwd: String?): ShellPresentation {
        val parsed = ShellPromptParser.parse(rawPrompt, base.shell, base.framework, fallbackCwd)
        return parsed.copy(
            completionProvider = base.completionProvider,
            shell = base.shell,
            framework = base.framework
        )
    }

    private fun detectShell(home: File, prefix: File): ShellFamily {
        val zshConfigured = File(home, ".zshrc").exists() || File(home, ".oh-my-zsh").isDirectory
        val fishConfigured = File(home, ".config/fish/config.fish").exists()
        return when {
            zshConfigured && File(prefix, "bin/zsh").exists() -> ShellFamily.ZSH
            fishConfigured && File(prefix, "bin/fish").exists() -> ShellFamily.FISH
            File(prefix, "bin/bash").exists() || File(home, ".bashrc").exists() -> ShellFamily.BASH
            else -> ShellFamily.SH
        }
    }

    private fun detectFramework(home: File): PromptFramework {
        val zshrc = readSmall(File(home, ".zshrc"))
        val bashrc = readSmall(File(home, ".bashrc"))
        val fish = readSmall(File(home, ".config/fish/config.fish"))
        val combined = listOfNotNull(zshrc, bashrc, fish).joinToString("\n")
        return when {
            File(home, ".oh-my-zsh/custom/themes/powerlevel10k").exists() ||
                combined.contains("powerlevel10k", ignoreCase = true) ||
                combined.contains("p10k", ignoreCase = true) -> PromptFramework.POWERLEVEL10K
            File(home, ".oh-my-zsh").isDirectory || combined.contains("ZSH=", ignoreCase = false) ->
                PromptFramework.OH_MY_ZSH
            combined.contains("starship init", ignoreCase = true) || File(home, ".config/starship.toml").exists() ->
                PromptFramework.STARSHIP
            combined.contains("pure", ignoreCase = true) && combined.contains("prompt", ignoreCase = true) ->
                PromptFramework.PURE
            combined.isNotBlank() -> PromptFramework.CUSTOM
            else -> PromptFramework.NONE
        }
    }

    private fun readSmall(file: File): String? = try {
        if (!file.isFile || file.length() > MAX_CONFIG_BYTES) null else file.readText()
    } catch (_: Exception) {
        null
    }

    private const val MAX_CONFIG_BYTES = 256 * 1024L
}
