package com.hereliesaz.hg2gui.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hereliesaz.hg2gui.ai.AiConfig
import com.hereliesaz.hg2gui.ai.AiProvider

private const val PREFS_NAME = "hg2gui_ai_settings"
private const val SECURE_PREFS_NAME = "hg2gui_ai_settings_secure"
private const val KEY_API_KEY = "api_key"
private const val PREF_PROVIDER = "provider"
private const val PREF_MODEL = "model"
private const val PREF_BASE_URL = "base_url"
private const val TAG = "hg2gui"

/**
 * The AI provider choice and the user-supplied API key for it, backing AI command search/chat. Keys
 * are kept per provider (Anthropic's under the original `api_key` name, so an existing install keeps
 * working); [apiKey]/[saveApiKey] act on the SELECTED provider. Provider, model and endpoint are not
 * secret and live in the plain preferences file. Keys are kept in
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

    /** The secure-store name of [provider]'s key; Anthropic keeps the pre-multi-provider name. */
    internal fun keyName(provider: AiProvider): String =
        if (provider == AiProvider.ANTHROPIC) KEY_API_KEY else "${KEY_API_KEY}_${provider.id}"

    /** The provider, model and endpoint currently selected (blank model/endpoint = provider default). */
    fun config(context: Context): AiConfig {
        val plain = plainPrefs(context)
        return AiConfig(
            provider = AiProvider.fromId(plain.getString(PREF_PROVIDER, null)),
            model = plain.getString(PREF_MODEL, null).orEmpty(),
            baseUrl = plain.getString(PREF_BASE_URL, null).orEmpty()
        )
    }

    /** Stores the selection (model and endpoint trimmed; blank means "use the default"). False when the write did not land. */
    fun saveConfig(context: Context, config: AiConfig): Boolean = plainPrefs(context).edit()
        .putString(PREF_PROVIDER, config.provider.id)
        .putString(PREF_MODEL, config.model.trim())
        .putString(PREF_BASE_URL, config.baseUrl.trim())
        .commit()

    /** The SELECTED provider's stored key. */
    fun apiKey(context: Context): String? = apiKey(context, config(context).provider)

    /** [provider]'s stored key, or null when none is set (or the secure store is unreadable). */
    fun apiKey(context: Context, provider: AiProvider): String? {
        val secure = securePrefs(context) ?: return null
        migrateLegacy(context, secure)
        return try {
            secure.getString(keyName(provider), null)?.ifBlank { null }
        } catch (e: Exception) {
            Log.w(TAG, "AI key unreadable", e)
            null
        }
    }

    /** What [saveApiKey] managed to do. Only [Saved] and [Cleared] mean the new state will survive a restart. */
    enum class SaveResult {
        /** The key is stored (encrypted) and will be read back after a restart. */
        Saved,

        /** A blank key was given and the stored key was removed. */
        Cleared,

        /** The encrypted store could not be opened or recreated; nothing was written. */
        StoreUnavailable,

        /** The store opened but the write did not land; the previous value is unchanged. */
        WriteFailed;

        /** True when the requested state is now what is persisted. */
        val persisted: Boolean get() = this == Saved || this == Cleared

        companion object {
            /** The outcome for a write of a [blank] (or non-blank) key; [committed] is the commit() result. */
            internal fun of(storeOpen: Boolean, committed: Boolean, blank: Boolean): SaveResult = when {
                !storeOpen -> StoreUnavailable
                !committed -> WriteFailed
                blank -> Cleared
                else -> Saved
            }
        }
    }

    /**
     * Stores [apiKey] (trimmed; blank removes it) and reports whether it was really persisted, so
     * the UI can say so instead of showing "saved" for a key that is gone after a restart.
     */
    fun saveApiKey(context: Context, apiKey: String?): SaveResult =
        saveApiKey(context, config(context).provider, apiKey)

    /** [saveApiKey] for an explicit [provider] rather than the selected one. */
    fun saveApiKey(context: Context, provider: AiProvider, apiKey: String?): SaveResult {
        val key = apiKey?.trim()
        val blank = key.isNullOrEmpty()
        val name = keyName(provider)
        val secure = securePrefs(context)
            ?: return SaveResult.of(storeOpen = false, committed = false, blank = blank)
        val committed = secure.edit().apply {
            if (blank) remove(name) else putString(name, key)
        }.commit()
        // A key set before migration ran must not linger in plain text - but only drop the plain
        // copy once the encrypted write has landed.
        if (committed && provider == AiProvider.ANTHROPIC) plainPrefs(context).edit(commit = true) { remove(KEY_API_KEY) }
        return SaveResult.of(storeOpen = true, committed = committed, blank = blank)
    }

    /** Source-compatible form of [saveApiKey] for callers that do not report the outcome. */
    fun setApiKey(context: Context, apiKey: String?) {
        saveApiKey(context, apiKey)
    }
}
