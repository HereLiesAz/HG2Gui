package com.hereliesaz.hg2gui.terminal

import android.content.Context
import androidx.core.content.edit

/** Last known successful ADB connection endpoints. Pairing secrets are deliberately never stored. */
object AdbEndpointStore {
    private const val PREFS = "hg2gui_adb_endpoints"
    private const val ENDPOINTS = "endpoints"
    private val endpointPattern = Regex("(?:[A-Za-z0-9._:-]+|\\[[0-9A-Fa-f:]+]):[0-9]{1,5}")

    fun list(context: Context): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(ENDPOINTS, emptySet()).orEmpty()
            .filter(endpointPattern::matches)
            .sorted()

    fun remember(context: Context, endpoint: String) {
        if (!endpointPattern.matches(endpoint)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit { putStringSet(ENDPOINTS, prefs.getStringSet(ENDPOINTS, emptySet()).orEmpty() + endpoint) }
    }

    fun forget(context: Context, endpoint: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit { putStringSet(ENDPOINTS, prefs.getStringSet(ENDPOINTS, emptySet()).orEmpty() - endpoint) }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { remove(ENDPOINTS) }
    }
}
