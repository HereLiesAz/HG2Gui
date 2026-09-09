package com.hereliesaz.hg2gui.api

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle

/** Foreground user interaction for the API's dialog capability. */
class Hg2ApiDialogActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra(Hg2ApiReceiver.EXTRA_TITLE) ?: "HG2Gui"
        val message = intent.getStringExtra(Hg2ApiReceiver.EXTRA_TEXT).orEmpty()
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> complete(true, "ok") }
            .setNegativeButton("CANCEL") { _, _ -> complete(false, "cancel") }
            .setOnCancelListener { complete(false, "cancel") }
            .show()
    }

    private fun complete(success: Boolean, value: String) {
        callback()?.let { callback ->
            runCatching {
                callback.send(this, if (success) 0 else 1, Intent().apply {
                    putExtra(Hg2ApiReceiver.EXTRA_SUCCESS, success)
                    putExtra(Hg2ApiReceiver.EXTRA_DATA, value)
                })
            }
        }
        finish()
    }

    @Suppress("DEPRECATION")
    private fun callback(): PendingIntent? = if (Build.VERSION.SDK_INT >= 33) {
        intent.getParcelableExtra(Hg2ApiReceiver.EXTRA_REPLY, PendingIntent::class.java)
    } else {
        intent.getParcelableExtra(Hg2ApiReceiver.EXTRA_REPLY) as? PendingIntent
    }
}
