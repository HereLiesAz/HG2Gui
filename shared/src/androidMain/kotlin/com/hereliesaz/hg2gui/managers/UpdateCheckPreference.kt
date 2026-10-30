package com.hereliesaz.hg2gui.managers

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "hg2gui_updates"
private const val KEY_AUTO_CHECK = "auto_check_enabled"

/**
 * Whether HG2Gui may ask GitHub, on its own, whether a newer build exists. On by default (the
 * behaviour before this switch existed); the Settings screen flips it.
 *
 * It lives in the same `hg2gui_updates` preferences file as the checker's own last-check
 * timestamp, so the app-module `AppUpdateChecker.checkAndNotify` can gate on it with one line,
 * before it touches the timestamp or the network:
 *
 *     if (!UpdateCheckPreference.isEnabled(appContext)) return
 *
 * Turning it off stops the background check and its notification only; nothing here blocks an
 * update the user starts by hand.
 */
object UpdateCheckPreference {
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO_CHECK, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AUTO_CHECK, enabled) }
    }
}
