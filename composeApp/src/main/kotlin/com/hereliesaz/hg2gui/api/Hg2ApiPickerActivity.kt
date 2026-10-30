package com.hereliesaz.hg2gui.api

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Transient result bridge for file/directory API requests.
 *
 * Grants are one-shot: the picked Uri reaches the caller only as a temporary grant that rides on
 * the reply (data + ClipData + the read/write flags), never as a persisted permission. This
 * activity used to call takePersistableUriPermission for every pick and never release it; Android
 * caps persisted grants per app (128 before Android 11, 512 after), after which every pick failed.
 * HG2Gui itself never reopens a picked Uri, so it has no use for a persisted grant.
 */
class Hg2ApiPickerActivity : Activity() {
    private val replied = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            // Activity was recreated (e.g. rotation) while the system picker was open.
            // Android preserves the activity-result relationship across recreation, so
            // onActivityResult will still fire when the picker returns. Nothing to do here.
            return
        }
        releaseLegacyGrants()
        val directory = intent.getBooleanExtra(Hg2ApiReceiver.EXTRA_PICK_DIRECTORY, false)
        val picker = if (directory) Intent(Intent.ACTION_OPEN_DOCUMENT_TREE) else Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = intent.getStringExtra(Hg2ApiReceiver.EXTRA_MIME) ?: "*/*"
        }
        // Read/write grant requested up front so the provider hands this app something it can pass on;
        // deliberately NOT FLAG_GRANT_PERSISTABLE_URI_PERMISSION (see the class doc).
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        startActivityForResult(picker, REQUEST_PICK)
    }

    /**
     * Earlier versions persisted a grant for every pick. Drop whatever is still held so an install
     * that already saturated the cap recovers; nothing else in the app persists Uri permissions.
     */
    private fun releaseLegacyGrants() {
        runCatching {
            contentResolver.persistedUriPermissions.forEach { held ->
                val flags = (if (held.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                    (if (held.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
                runCatching { contentResolver.releasePersistableUriPermission(held.uri, flags) }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK) return
        val uri = data?.data
        val grantFlags = (data?.flags ?: 0) and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val success = resultCode == RESULT_OK && uri != null
        reply(success, uri?.toString(), if (success) null else "picker cancelled") {
            if (uri != null) {
                // The caller only gets to open the Uri if it travels with a grant: as the reply's data
                // and ClipData (where fill-in honours it), plus the same read/write grant flags this
                // app was given. HG2Gui still holds its own transient grant while it sends this.
                setData(uri)
                clipData = ClipData.newRawUri("", uri)
                addFlags(grantFlags)
            }
        }
        finish()
    }

    override fun onDestroy() {
        // Destroyed before the picker returned anything (not a rotation): answer instead of leaving the caller waiting.
        if (!isChangingConfigurations) reply(false, null, "picker dismissed") {}
        super.onDestroy()
    }

    /** Sends the single reply (and its audit `decision` row); later calls are no-ops. */
    private fun reply(success: Boolean, uri: String?, error: String?, extra: Intent.() -> Unit) {
        if (!replied.compareAndSet(false, true)) return
        val callback = Hg2ApiReplies.callbackOf(intent)
        Hg2ApiAuditStore.record(
            context = applicationContext,
            action = if (intent.getBooleanExtra(Hg2ApiReceiver.EXTRA_PICK_DIRECTORY, false)) {
                Hg2ApiReceiver.ACTION_PICK_DIRECTORY
            } else {
                Hg2ApiReceiver.ACTION_PICK_FILE
            },
            callerPackage = Hg2ApiReplies.callerPackageOf(callback),
            success = success,
            error = error,
            phase = Hg2ApiAuditStore.PHASE_DECISION,
            requestId = intent.getStringExtra(Hg2ApiReceiver.EXTRA_REQUEST_ID)
        )
        Hg2ApiReplies.send(applicationContext, callback, "uri", success, data = uri, error = error, extra = extra)
    }

    companion object { private const val REQUEST_PICK = 4102 }
}
