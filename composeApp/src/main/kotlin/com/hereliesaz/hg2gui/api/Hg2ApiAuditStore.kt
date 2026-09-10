package com.hereliesaz.hg2gui.api

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Bounded, app-private audit history for same-signature external API calls. */
internal object Hg2ApiAuditStore {
    private const val DIRECTORY = "api"
    private const val FILE_NAME = "audit.ndjson"
    private const val MAX_ENTRIES = 200

    @Synchronized
    fun record(
        context: Context,
        action: String,
        callerPackage: String?,
        success: Boolean,
        error: String?
    ) {
        runCatching {
            val file = file(context)
            file.parentFile?.mkdirs()
            val retained = if (file.isFile) {
                file.readLines().asSequence().filter(String::isNotBlank).takeLast(MAX_ENTRIES - 1).toList()
            } else {
                emptyList()
            }
            val entry = JSONObject()
                .put("timestampMillis", System.currentTimeMillis())
                .put("action", action)
                .put("callerPackage", callerPackage ?: JSONObject.NULL)
                .put("success", success)
                .apply { error?.let { put("error", it) } }
                .toString()
            val temporary = File(file.parentFile, "$FILE_NAME.tmp")
            temporary.bufferedWriter().use { out ->
                retained.forEach(out::appendLine)
                out.appendLine(entry)
            }
            if (!temporary.renameTo(file)) {
                temporary.copyTo(file, overwrite = true)
                temporary.delete()
            }
        }
    }

    @Synchronized
    fun history(context: Context): String {
        val result = JSONArray()
        val file = file(context)
        if (!file.isFile) return result.toString()
        runCatching {
            file.useLines { lines ->
                lines.filter(String::isNotBlank).takeLast(MAX_ENTRIES).forEach { line ->
                    runCatching { JSONObject(line) }.getOrNull()?.let(result::put)
                }
            }
        }
        return result.toString()
    }

    private fun file(context: Context): File = File(File(context.filesDir, DIRECTORY), FILE_NAME)
}
