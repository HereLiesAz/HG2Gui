package com.hereliesaz.hg2gui.managers

import android.content.Context
import androidx.core.content.edit
import com.hereliesaz.hg2gui.ui.ssh.SshPreset

private const val PREFS_NAME = "hg2gui_ssh_presets"
private const val KEY_NAMES = "preset_names"

/**
 * Saved ssh connection presets, one flat SharedPreferences file rather than JSON - mirrors
 * TerminalActivity's own settings pattern (a handful of scalars, no serialization needed).
 * Preset names live in one Set<String>; each preset's fields are flat "$name.field" keys.
 */
object SshPresets {
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun list(context: Context): List<SshPreset> {
        val p = prefs(context)
        val names = p.getStringSet(KEY_NAMES, emptySet()).orEmpty()
        return names.mapNotNull { name ->
            val host = p.getString("$name.host", null) ?: return@mapNotNull null
            val user = p.getString("$name.user", "") ?: ""
            val port = p.getInt("$name.port", 22)
            val keyPath = p.getString("$name.key", null)
            SshPreset(name, user, host, port, keyPath)
        }.sortedBy { it.name }
    }

    fun save(context: Context, preset: SshPreset) {
        val p = prefs(context)
        val names = p.getStringSet(KEY_NAMES, emptySet()).orEmpty() + preset.name
        p.edit {
            putStringSet(KEY_NAMES, names)
            putString("${preset.name}.host", preset.host)
            putString("${preset.name}.user", preset.user)
            putInt("${preset.name}.port", preset.port)
            if (preset.keyPath != null) putString("${preset.name}.key", preset.keyPath)
            else remove("${preset.name}.key")
        }
    }

    fun delete(context: Context, name: String) {
        val p = prefs(context)
        val names = p.getStringSet(KEY_NAMES, emptySet()).orEmpty() - name
        p.edit {
            putStringSet(KEY_NAMES, names)
            remove("$name.host")
            remove("$name.user")
            remove("$name.port")
            remove("$name.key")
        }
    }
}
