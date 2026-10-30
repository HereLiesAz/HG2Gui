package com.hereliesaz.hg2gui.api

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground user interaction for the API's dialog capability.
 *
 * The caller always hears back exactly once: OK, CANCEL, back/tap-away and - because this activity
 * is `noHistory`, so leaving the screen destroys it with no button pressed - being dismissed by the
 * system each produce a reply, and each is recorded as a `decision` row in the API audit log.
 */
class Hg2ApiDialogActivity : Activity() {
    private val replied = AtomicBoolean(false)
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra(Hg2ApiReceiver.EXTRA_TITLE) ?: "HG2Gui"
        val message = intent.getStringExtra(Hg2ApiReceiver.EXTRA_TEXT).orEmpty()
        dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> complete(true, "ok") }
            .setNegativeButton("CANCEL") { _, _ -> complete(false, "cancel") }
            .setOnCancelListener { complete(false, "cancel") }
            .show()
    }

    private fun complete(success: Boolean, value: String, error: String? = null) {
        if (replied.compareAndSet(false, true)) {
            val callback = Hg2ApiReplies.callbackOf(intent)
            Hg2ApiAuditStore.record(
                context = applicationContext,
                action = Hg2ApiReceiver.ACTION_DIALOG,
                callerPackage = Hg2ApiReplies.callerPackageOf(callback),
                success = success,
                error = error,
                phase = Hg2ApiAuditStore.PHASE_DECISION,
                requestId = intent.getStringExtra(Hg2ApiReceiver.EXTRA_REQUEST_ID),
                detail = "choice=$value"
            )
            Hg2ApiReplies.send(applicationContext, callback, "ack", success, data = value, error = error)
        }
        finish()
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
        // Destroyed with no button pressed (noHistory, process housekeeping): answer rather than leave the caller waiting.
        // A rotation is not a dismissal - the recreated activity shows the dialog again.
        if (!isChangingConfigurations) complete(false, "cancel", error = "dialog dismissed")
        super.onDestroy()
    }
}
