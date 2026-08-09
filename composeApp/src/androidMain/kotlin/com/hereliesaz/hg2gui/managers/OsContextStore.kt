package com.hereliesaz.hg2gui.managers

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "hg2gui_os_context"
private const val KEY_OS = "os"

/** The OS a session is mentally "in" - "local" (the real Termux prefix, the default) or a
 *  reference-only context (ubuntu/macos/windows) offered alongside it. Persisted, not
 *  session-scoped, since a device is usually SSH'd into the same kind of host repeatedly. */
object OsContextStore {
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun current(context: Context): String = prefs(context).getString(KEY_OS, "local") ?: "local"

    fun set(context: Context, os: String) {
        prefs(context).edit { putString(KEY_OS, os) }
    }
}
