package com.hereliesaz.hg2gui.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_NAME = "hg2gui_ai_settings"
private const val SECURE_PREFS_NAME = "hg2gui_ai_settings_secure"
private const val KEY_API_KEY = "api_key"
private const val TAG = "hg2gui"

/**
 * The user-supplied Anthropic API key backing AI command search/chat, kept in
 * [EncryptedSharedPreferences] (AES-256 values, keyset wrapped by an Android Keystore master key).
 * The other app settings (SSH key paths, MCP pairing token) still use plain SharedPreferences.
 *
 * Two failure modes are handled rather than crashing the AI screens:
 * - A keystore/keyset that can no longer decrypt the file (restored device data, wiped keystore,
 *   hardware fault) throws on open or read. The unreadable file is deleted and recreated empty,
 *   so the user just re-enters the key.
 * - Installs that stored the key in the old plain `hg2gui_ai_settings` file are migrated once: the
 *   value is copied into the encrypted store and the plain value is then removed.
 */
object AiSettings {
    private fun plainPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun openSecure(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    @Volatile
    private var cachedSecure: SharedPreferences? = null

    /** Opens the encrypted store, or deletes and recreates it if it cannot be opened or read. */
    private fun securePrefs(context: Context): SharedPreferences? = cachedSecure ?: synchronized(this) {
        cachedSecure ?: openOrRecreate(context.applicationContext).also { cachedSecure = it }
    }

    private fun openOrRecreate(app: Context): SharedPreferences? {
        return try {
            openSecure(app).also { it.getString(KEY_API_KEY, null) } // force decryption of the entry
        } catch (e: Exception) {
            Log.w(TAG, "AI key store unreadable; recreating it", e)
            try {
                app.deleteSharedPreferences(SECURE_PREFS_NAME)
                openSecure(app)
            } catch (e2: Exception) {
                Log.e(TAG, "AI key store could not be recreated; the key will not be saved", e2)
                null
            }
        }
    }

    /** Moves a key left in the old plain file into [secure], then deletes the plain value. */
    private fun migrateLegacy(context: Context, secure: SharedPreferences) {
        val plain = plainPrefs(context)
        val legacy = plain.getString(KEY_API_KEY, null) ?: return
        if (legacy.isNotBlank() && secure.getString(KEY_API_KEY, null).isNullOrBlank()) {
            // commit(), not apply(): only drop the plain copy once the encrypted write has landed.
            if (!secure.edit().putString(KEY_API_KEY, legacy).commit()) return
        }
        plain.edit(commit = true) { remove(KEY_API_KEY) }
    }

    fun apiKey(context: Context): String? {
        val secure = securePrefs(context) ?: return null
        migrateLegacy(context, secure)
        return try {
            secure.getString(KEY_API_KEY, null)?.ifBlank { null }
        } catch (e: Exception) {
            Log.w(TAG, "AI key unreadable", e)
            null
        }
    }

    fun setApiKey(context: Context, apiKey: String?) {
        val secure = securePrefs(context) ?: return
        secure.edit(commit = true) {
            if (apiKey.isNullOrBlank()) remove(KEY_API_KEY) else putString(KEY_API_KEY, apiKey)
        }
        // A key set before migration ran must not linger in plain text.
        plainPrefs(context).edit(commit = true) { remove(KEY_API_KEY) }
    }
}
