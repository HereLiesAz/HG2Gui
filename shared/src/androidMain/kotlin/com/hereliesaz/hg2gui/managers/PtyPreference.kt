package com.hereliesaz.hg2gui.managers

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "hg2gui_pty_pref"
private const val KEY_ENABLED = "use_pty"

/**
 * Whether [com.hereliesaz.hg2gui.terminal.ShellSession] should run commands over a real
 * pseudoterminal (the app's own bundled native pty bridge) instead of a plain OS pipe.
 *
 * Enabled by default. Package managers and other interactive CLI programs need a controlling
 * terminal so confirmation prompts can actually wait for and receive user input. The old pipe
 * default let the persistent shell consume/buffer stdin ahead of the child, so prompts such as
 * `Do you want to continue? [Y/n]` saw EOF and aborted before HG2Gui could answer them.
 *
 * A flipped toggle only takes effect for a session created after the flip (a new tab, or the next
 * app launch) - it never touches a shell that's already running.
 */
object PtyPreference {
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }
}
