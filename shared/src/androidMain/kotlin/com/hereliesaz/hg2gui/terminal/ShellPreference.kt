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

    fun available(context: Context): List<String> = buildList {
        val nativeBash = File(context.applicationInfo.nativeLibraryDir, "libbin_bash.so")
        if (nativeBash.canExecute()) add(BASH)
        val bin = File(DistroManager.prefixDir(context), "bin")
        if (File(bin, ZSH).canExecute()) add(ZSH)
        if (File(bin, FISH).canExecute()) add(FISH)
    }.ifEmpty { listOf(BASH) }

    fun executable(context: Context, shell: String): File? = when (shell) {
        BASH -> File(context.applicationInfo.nativeLibraryDir, "libbin_bash.so").takeIf(File::canExecute)
        ZSH, FISH -> File(DistroManager.prefixDir(context), "bin/$shell").takeIf(File::canExecute)
        else -> null
    }
}
