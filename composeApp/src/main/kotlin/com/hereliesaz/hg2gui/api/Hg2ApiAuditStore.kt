package com.hereliesaz.hg2gui.api

import android.content.Context
import android.util.Log
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bounded, app-private audit history for same-signature external API calls.
 *
 * One API request leaves up to three rows, tied together by a shared `requestId`:
 *  - `request`  - written at the very top of Hg2ApiReceiver.dispatch, before any validation or
 *    side effect, so a request that is refused, crashes or is never answered still appears;
 *  - `decision` - written by Hg2ApiAuthorityActivity / Hg2ApiDialogActivity / Hg2ApiPickerActivity
 *    with what the user chose (approved, denied, dismissed, picked);
 *  - `result`   - what was replied to the caller (the only row this store used to write).
 * `callerPackage` is the creator of the caller's reply PendingIntent, which the system - not the
 * caller - records; it is null when the caller supplied no PendingIntent. Because only
 * same-signature apps can send to the receiver at all, this attributes a request among the user's
 * own apps; it is not a defence against an app that already holds the signing key.
 */
internal object Hg2ApiAuditStore {
    private const val TAG = "Hg2ApiAudit"
    private const val DIRECTORY = "api"
    private const val FILE_NAME = "audit.ndjson"
    private const val MAX_ENTRIES = 200

    /** Row written when the receiver first sees a request. */
    const val PHASE_REQUEST = "request"

    /** Row written when the user decides on a request in one of the API activities. */
    const val PHASE_DECISION = "decision"

    /** Row written when a reply (success or failure) goes back to the caller. */
    const val PHASE_RESULT = "result"

    /**
     * Appends one row. [success] is null for rows where it has no meaning yet (a `request`).
     * [command] is stored as a short digest plus its length, never verbatim; [detail] is stored
     * verbatim (callers pass a bounded [auditPreview] only for user-approved root/ADB commands,
     * which are exactly what a later review needs to see).
     * A write failure is logged (and the row is lost); it never propagates to the API call.
     */
    @Synchronized
    fun record(
        context: Context,
        action: String,
        callerPackage: String?,
        success: Boolean?,
        error: String?,
        phase: String = PHASE_RESULT,
        requestId: String? = null,
        command: String? = null,
        detail: String? = null
    ) {
        try {
            val file = file(context)
            file.parentFile?.mkdirs()
            val retained = if (file.isFile) {
                retainAuditLines(file.readLines(), MAX_ENTRIES - 1)
            } else {
                emptyList()
            }
            val entry = JSONObject()
                .put("timestampMillis", System.currentTimeMillis())
                .put("phase", phase)
                .put("action", action)
                .put("callerPackage", callerPackage ?: JSONObject.NULL)
                .apply {
                    requestId?.let { put("requestId", it) }
                    success?.let { put("success", it) }
                    error?.let { put("error", it) }
                    command?.let { put("commandDigest", commandDigest(it)).put("commandLength", it.length) }
                    detail?.let { put("detail", it) }
                }
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
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // An audit trail that silently stops recording is worse than none: say so in logcat.
            Log.w(TAG, "could not write API audit row ($phase $action)", e)
        }
    }

    @Synchronized
    fun history(context: Context): String {
        val result = JSONArray()
        val file = file(context)
        if (!file.isFile) return result.toString()
        try {
            retainAuditLines(file.readLines(), MAX_ENTRIES).forEach { line ->
                runCatching { JSONObject(line) }.getOrNull()?.let(result::put)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.w(TAG, "could not read API audit history", e)
        }
        return result.toString()
    }

    private fun file(context: Context): File = File(File(context.filesDir, DIRECTORY), FILE_NAME)
}
