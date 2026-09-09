package com.hereliesaz.hg2gui.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_UPDATE) return
        val versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: return
        val versionCode = intent.getLongExtra(EXTRA_VERSION_CODE, -1L)
        val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return
        val sha256 = intent.getStringExtra(EXTRA_SHA256)
        if (versionCode < 0L) return

        val pending = goAsync()
        try {
            AppUpdateChecker.downloadAndInstall(
                context,
                AppUpdateChecker.Update(versionName, versionCode, downloadUrl, sha256)
            )
        } finally {
            pending.finish()
        }
    }

    companion object {
        private const val ACTION_UPDATE = "com.hereliesaz.hg2gui.action.UPDATE_APP"
        private const val EXTRA_VERSION_NAME = "version_name"
        private const val EXTRA_VERSION_CODE = "version_code"
        private const val EXTRA_DOWNLOAD_URL = "download_url"
        private const val EXTRA_SHA256 = "sha256"

        fun intentFor(context: Context, update: AppUpdateChecker.Update): Intent =
            Intent(context, UpdateActionReceiver::class.java)
                .setAction(ACTION_UPDATE)
                .putExtra(EXTRA_VERSION_NAME, update.versionName)
                .putExtra(EXTRA_VERSION_CODE, update.versionCode)
                .putExtra(EXTRA_DOWNLOAD_URL, update.downloadUrl)
                .putExtra(EXTRA_SHA256, update.sha256)
    }
}
