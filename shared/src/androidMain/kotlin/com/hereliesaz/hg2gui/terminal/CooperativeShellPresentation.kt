package com.hereliesaz.hg2gui.terminal

import org.json.JSONObject

/** Applies a fresh `shell/<family>` cooperative metadata channel over inferred prompt semantics. */
object CooperativeShellPresentation {
    fun forFamily(family: ShellFamily, fallback: ShellPresentation): ShellPresentation? {
        val key = family.name.lowercase()
        val entry = CooperativeMetadata.read("shell/$key") ?: CooperativeMetadata.read("shell/default") ?: return null
        return runCatching { parse(JSONObject(entry.payload), family, fallback) }.getOrNull()
    }

    private fun parse(root: JSONObject, family: ShellFamily, fallback: ShellPresentation): ShellPresentation =
        fallback.copy(
            shell = enumValue(root.optString("shell"), family),
            framework = enumValue(root.optString("framework"), fallback.framework),
            cwd = root.stringOrFallback("cwd", fallback.cwd),
            user = root.stringOrFallback("user", fallback.user),
            host = root.stringOrFallback("host", fallback.host),
            gitBranch = root.stringOrFallback("gitBranch", fallback.gitBranch),
            gitDirty = if (root.has("gitDirty")) root.optBoolean("gitDirty") else fallback.gitDirty,
            lastExitCode = if (root.has("lastExitCode") && !root.isNull("lastExitCode")) root.optInt("lastExitCode") else fallback.lastExitCode,
            rawPrompt = root.stringOrFallback("rawPrompt", fallback.rawPrompt),
            completionProvider = enumValue(root.optString("completionProvider"), fallback.completionProvider)
        )

    private fun JSONObject.stringOrFallback(name: String, fallback: String?): String? = when {
        !has(name) -> fallback
        isNull(name) -> null
        else -> optString(name).takeIf { it.isNotEmpty() }
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: fallback
}
