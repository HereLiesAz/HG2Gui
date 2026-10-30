package com.hereliesaz.hg2gui.api

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Reply plumbing shared by Hg2ApiReceiver and the three API activities, so every reply to a
 * caller carries the same envelope (success / api_version / result_type / data / error) and every
 * outcome - including the ones decided inside an activity - lands in the audit log.
 */
internal object Hg2ApiReplies {

    /** The caller's reply PendingIntent (`EXTRA_REPLY`), or null when the request carried none. */
    @Suppress("DEPRECATION")
    fun callbackOf(intent: Intent): PendingIntent? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Hg2ApiReceiver.EXTRA_REPLY, PendingIntent::class.java)
        else intent.getParcelableExtra(Hg2ApiReceiver.EXTRA_REPLY) as? PendingIntent

    /** Package that created [callback]; recorded by the system at creation, null if there is no callback. */
    fun callerPackageOf(callback: PendingIntent?): String? = runCatching { callback?.creatorPackage }.getOrNull()

    /**
     * Sends the standard result envelope to [callback] (when there is one). [extra] can add the
     * uri / grant flags the picker needs. Returns whether the PendingIntent accepted it.
     */
    fun send(
        context: Context,
        callback: PendingIntent?,
        resultType: String,
        success: Boolean,
        data: String? = null,
        error: String? = null,
        extra: (Intent.() -> Unit)? = null
    ): Boolean {
        callback ?: return false
        return runCatching {
            callback.send(context, if (success) 0 else 1, Intent().apply {
                putExtra(Hg2ApiReceiver.EXTRA_SUCCESS, success)
                putExtra(Hg2ApiReceiver.EXTRA_API_VERSION, Hg2ApiReceiver.API_VERSION)
                putExtra(Hg2ApiReceiver.EXTRA_RESULT_TYPE, resultType)
                data?.let { putExtra(Hg2ApiReceiver.EXTRA_DATA, it) }
                error?.let { putExtra(Hg2ApiReceiver.EXTRA_ERROR, it) }
                extra?.invoke(this)
            })
        }.isSuccess
    }
}
