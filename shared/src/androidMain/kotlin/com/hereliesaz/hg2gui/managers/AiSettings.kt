package com.hereliesaz.hg2gui.managers

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "hg2gui_ai_settings"
private const val KEY_API_KEY = "api_key"

/**
 * The user-supplied Anthropic API key backing AI command search/chat, stored in plain
 * SharedPreferences - matching this app's existing security posture (no
 * EncryptedSharedPreferences is used anywhere else in the app either; SSH key paths and the MCP
 * pairing token get the same treatment). Flagged as a risk in docs, not hidden as a non-issue.
 */
object AiSettings {
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun apiKey(context: Context): String? = prefs(context).getString(KEY_API_KEY, null)?.ifBlank { null }

    fun setApiKey(context: Context, apiKey: String?) {
        prefs(context).edit {
            if (apiKey.isNullOrBlank()) remove(KEY_API_KEY) else putString(KEY_API_KEY, apiKey)
        }
    }
}
