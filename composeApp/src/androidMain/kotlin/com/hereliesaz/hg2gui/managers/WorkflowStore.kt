package com.hereliesaz.hg2gui.managers

import android.content.Context
import androidx.core.content.edit
import com.hereliesaz.hg2gui.ui.Workflow

private const val PREFS_NAME = "hg2gui_workflows"
private const val KEY_NAMES = "workflow_names"

/**
 * Saved command-template workflows, one flat SharedPreferences file - mirrors SshPresets.kt's
 * shape exactly (a name Set<String>, one "$name.field" scalar per field).
 */
object WorkflowStore {
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun list(context: Context): List<Workflow> {
        val p = prefs(context)
        val names = p.getStringSet(KEY_NAMES, emptySet()).orEmpty()
        return names.mapNotNull { name ->
            val template = p.getString("$name.template", null) ?: return@mapNotNull null
            Workflow(name, template)
        }.sortedBy { it.name }
    }

    fun save(context: Context, workflow: Workflow) {
        val p = prefs(context)
        val names = p.getStringSet(KEY_NAMES, emptySet()).orEmpty() + workflow.name
        p.edit {
            putStringSet(KEY_NAMES, names)
            putString("${workflow.name}.template", workflow.template)
        }
    }

    fun delete(context: Context, name: String) {
        val p = prefs(context)
        val names = p.getStringSet(KEY_NAMES, emptySet()).orEmpty() - name
        p.edit {
            putStringSet(KEY_NAMES, names)
            remove("$name.template")
        }
    }
}
