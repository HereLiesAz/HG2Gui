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
        if (savedInstanceState != null) return
        val directory = intent.getBooleanExtra(Hg2ApiReceiver.EXTRA_PICK_DIRECTORY, false)
        val picker = if (directory) Intent(Intent.ACTION_OPEN_DOCUMENT_TREE) else Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = intent.getStringExtra(Hg2ApiReceiver.EXTRA_MIME) ?: "*/*"
        }
        startActivityForResult(picker, REQUEST_PICK)
    }

    @Deprecated("Activity result bridge is intentionally local and self-contained")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK) return
        val uri = data?.data
        if (uri != null) {
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
        }
        val success = resultCode == RESULT_OK && uri != null
        callback()?.let { callback ->
            runCatching {
                callback.send(this, if (success) 0 else 1, Intent().apply {
                    putExtra(Hg2ApiReceiver.EXTRA_SUCCESS, success)
                    if (uri != null) putExtra(Hg2ApiReceiver.EXTRA_DATA, uri.toString())
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
