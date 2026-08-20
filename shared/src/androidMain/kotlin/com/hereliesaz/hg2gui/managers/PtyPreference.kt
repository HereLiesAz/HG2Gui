package com.hereliesaz.hg2gui.managers

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "hg2gui_pty_pref"
private const val KEY_ENABLED = "use_pty"

/**
 * Whether [com.hereliesaz.hg2gui.terminal.ShellSession] should run commands over a real
 * pseudoterminal (the app's own bundled native pty bridge) instead of a plain OS pipe. Off by
 * default - a pipe is what every command in the app has always run through, and there's no
 * physical device in the environment this was built in to have verified the pty path on. A
 * flipped toggle only takes effect for a session created after the flip (a new tab, or the next
 * app launch) - it never touches a shell that's already running.
 */
object PtyPreference {
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }
}
