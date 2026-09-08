package com.hereliesaz.hg2gui.managers

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "hg2gui_pty_pref"
private const val KEY_ENABLED = "use_pty"
private const val KEY_DEFAULT_MIGRATED = "use_pty_default_v2"

/**
 * Whether [com.hereliesaz.hg2gui.terminal.ShellSession] should run commands over a real
 * pseudoterminal (the app's own bundled native pty bridge) instead of a plain OS pipe.
 *
 * PTY is now the default because package managers and other interactive CLI tools require real
 * terminal semantics for prompts and carriage-return progress. Existing installs may already
 * have persisted the old default-false value, so this migration flips that legacy value once.
 * After migration, an explicit user toggle remains authoritative.
 */
object PtyPreference {
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_DEFAULT_MIGRATED, false)) {
            prefs.edit {
                putBoolean(KEY_ENABLED, true)
                putBoolean(KEY_DEFAULT_MIGRATED, true)
            }
            return true
        }
        return prefs.getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit {
            putBoolean(KEY_ENABLED, enabled)
            putBoolean(KEY_DEFAULT_MIGRATED, true)
        }
    }
}
