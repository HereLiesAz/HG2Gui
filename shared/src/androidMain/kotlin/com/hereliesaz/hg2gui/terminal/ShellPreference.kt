package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File

/** Persistent default shell selection for new HG2Gui sessions. */
object ShellPreference {
    private const val PREFS = "hg2gui_shell"
    private const val KEY_DEFAULT = "default_shell"
    const val BASH = "bash"
    const val ZSH = "zsh"
    const val FISH = "fish"

    fun selected(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DEFAULT, BASH) ?: BASH

    fun setSelected(context: Context, shell: String) {
        require(shell in available(context)) { "Shell is not installed: $shell" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DEFAULT, shell).apply()
    }

    /**
     * The shells that can actually be launched right now, bash first. Empty when none is
     * executable: it used to fall back to `[BASH]`, which made the Settings picker offer (and
     * highlight) a shell that was not installed. Callers show an explicit "no shell" state instead.
     */
    fun available(context: Context): List<String> {
        val nativeBash = File(context.applicationInfo.nativeLibraryDir, "libbin_bash.so")
        val bin = File(DistroManager.prefixDir(context), "bin")
        return availableFrom(
            bash = nativeBash.canExecute(),
            zsh = File(bin, ZSH).canExecute(),
            fish = File(bin, FISH).canExecute()
        )
    }

    /** The pure part of [available]: which of the known shells are present, in picker order. */
    internal fun availableFrom(bash: Boolean, zsh: Boolean, fish: Boolean): List<String> = buildList {
        if (bash) add(BASH)
        if (zsh) add(ZSH)
        if (fish) add(FISH)
    }

    /**
     * The shell the picker should show as chosen: the stored one when it is still installed,
     * otherwise the first available shell (what a new session falls back to), or null when
     * nothing is installed. [selected] alone can name a shell that has since been removed.
     */
    internal fun effectiveSelection(stored: String, available: List<String>): String? =
        stored.takeIf { it in available } ?: available.firstOrNull()

    fun executable(context: Context, shell: String): File? = when (shell) {
        BASH -> File(context.applicationInfo.nativeLibraryDir, "libbin_bash.so").takeIf(File::canExecute)
        ZSH, FISH -> File(DistroManager.prefixDir(context), "bin/$shell").takeIf(File::canExecute)
        else -> null
    }
}
