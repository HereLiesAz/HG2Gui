package com.hereliesaz.hg2gui.api

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle

/** Transient result bridge for file/directory API requests. */
class Hg2ApiPickerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            // Activity was recreated (e.g. rotation) while the system picker was open.
            // Android preserves the activity-result relationship across recreation, so
            // onActivityResult will still fire when the picker returns. Nothing to do here.
            return
        }
        val directory = intent.getBooleanExtra(Hg2ApiReceiver.EXTRA_PICK_DIRECTORY, false)
        val picker = if (directory) Intent(Intent.ACTION_OPEN_DOCUMENT_TREE) else Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = intent.getStringExtra(Hg2ApiReceiver.EXTRA_MIME) ?: "*/*"
        }
        // Ask for a persistable read/write grant up front - without these the provider is free to
        // hand back a one-shot grant, and takePersistableUriPermission below then fails.
        picker.addFlags(
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        startActivityForResult(picker, REQUEST_PICK)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK) return
        val uri = data?.data
        val flags = (data?.flags ?: 0) and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (uri != null) {
            val granted = runCatching { contentResolver.takePersistableUriPermission(uri, flags); true }.getOrDefault(false)
            if (!granted) {
                callback()?.let { runCatching {
                    it.send(this, 1, android.content.Intent().apply {
                        putExtra(Hg2ApiReceiver.EXTRA_SUCCESS, false)
                        putExtra(Hg2ApiReceiver.EXTRA_ERROR, "failed to persist URI permission")
                    })
                } }
                finish()
                return
            }
        }
        val success = resultCode == RESULT_OK && uri != null
        callback()?.let { callback ->
            runCatching {
                callback.send(this, if (success) 0 else 1, Intent().apply {
                    putExtra(Hg2ApiReceiver.EXTRA_SUCCESS, success)
                    if (uri != null) {
                        putExtra(Hg2ApiReceiver.EXTRA_DATA, uri.toString())
                        // The caller only gets to open the Uri if it travels with a grant: as the
                        // reply's data and ClipData (where fill-in honours it), plus the same
                        // read/write grant flags this app was given.
                        setData(uri)
                        clipData = android.content.ClipData.newRawUri("", uri)
                        addFlags(flags)
                    }
                    if (!success) putExtra(Hg2ApiReceiver.EXTRA_ERROR, "picker cancelled")
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

    companion object { private const val REQUEST_PICK = 4102 }
}
