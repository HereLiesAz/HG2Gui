package com.hereliesaz.hg2gui.managers

import android.content.Context
import androidx.core.content.edit
import com.hereliesaz.hg2gui.azp.AzpInstalled
import com.hereliesaz.hg2gui.azp.AzpInstaller
import com.hereliesaz.hg2gui.azp.AzpTrust

private const val PREFS_NAME = "hg2gui_azp"
private const val KEY_IDS = "azp_ids"

/** Total budget for skill text folded into the AI system prompt - keeps a handful of installed
 *  skills from ballooning every AI request's token count. */
private const val SKILL_TEXT_BUDGET_CHARS = 6000

/**
 * Locally installed azphalt packages - one flat SharedPreferences file, mirrors WorkflowStore's
 * shape. "Installed" means AzpInstaller already unpacked the `.azp` into `filesDir/azp/...`;
 * this store just remembers which packages are present so the store screen can show
 * INSTALLED instead of INSTALL, and so [installedSkillTexts] knows where to look.
 */
object AzpLibrary {
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun installed(context: Context): List<AzpInstalled> {
        val p = prefs(context)
        val ids = p.getStringSet(KEY_IDS, emptySet()).orEmpty()
        return ids.mapNotNull { id ->
            val name = p.getString("$id.name", null) ?: return@mapNotNull null
            val version = p.getString("$id.version", "") ?: ""
            val kind = p.getString("$id.kind", "asset") ?: "asset"
            val trust = p.getString("$id.trust", null)?.let {
                runCatching { AzpTrust.valueOf(it) }.getOrDefault(AzpTrust.UNSIGNED)
            } ?: AzpTrust.UNSIGNED
            AzpInstalled(id, name, version, kind, trust)
        }.sortedBy { it.name }
    }

    fun isInstalled(context: Context, id: String): Boolean =
        prefs(context).getStringSet(KEY_IDS, emptySet()).orEmpty().contains(id)

    fun record(
        context: Context,
        id: String,
        name: String,
        version: String,
        kind: String,
        skillIds: List<String>,
        trust: AzpTrust = AzpTrust.UNSIGNED,
    ) {
        val p = prefs(context)
        val ids = p.getStringSet(KEY_IDS, emptySet()).orEmpty() + id
        p.edit {
            putStringSet(KEY_IDS, ids)
            putString("$id.name", name)
            putString("$id.version", version)
            putString("$id.kind", kind)
            putString("$id.skillIds", skillIds.joinToString(","))
            putString("$id.trust", trust.name)
        }
    }

    fun remove(context: Context, id: String) {
        val p = prefs(context)
        val ids = p.getStringSet(KEY_IDS, emptySet()).orEmpty() - id
        p.edit {
            putStringSet(KEY_IDS, ids)
            remove("$id.name"); remove("$id.version"); remove("$id.kind"); remove("$id.skillIds"); remove("$id.trust")
        }
    }

    /** Reads every installed skill package's SKILL.md files back off disk, as (skillId, text)
     *  pairs, truncated to a total character budget so AiClient's system prompt stays bounded. */
    fun installedSkillTexts(context: Context): List<Pair<String, String>> {
        val p = prefs(context)
        val ids = p.getStringSet(KEY_IDS, emptySet()).orEmpty()
        val out = mutableListOf<Pair<String, String>>()
        var remaining = SKILL_TEXT_BUDGET_CHARS
        for (id in ids) {
            if (remaining <= 0) break
            if (p.getString("$id.kind", "asset") != "skill") continue
            val version = p.getString("$id.version", "") ?: continue
            val skillIds = p.getString("$id.skillIds", "")?.split(",")?.filter { it.isNotBlank() } ?: continue
            val root = AzpInstaller.rootDir(context, id, version)
            for (skillId in skillIds) {
                if (remaining <= 0) break
                val skillMd = root.resolve("skills/$skillId/SKILL.md")
                if (!skillMd.exists()) continue
                val text = skillMd.readText().take(remaining)
                remaining -= text.length
                out += skillId to text
            }
        }
        return out
    }
}
