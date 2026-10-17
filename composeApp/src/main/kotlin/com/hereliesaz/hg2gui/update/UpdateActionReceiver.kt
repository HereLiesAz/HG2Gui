package com.hereliesaz.hg2gui.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hereliesaz.hg2gui.LongTaskService

class UpdateActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_UPDATE) return
        val versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: return
        val versionCode = intent.getLongExtra(EXTRA_VERSION_CODE, -1L)
        val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return
        val sha256 = intent.getStringExtra(EXTRA_SHA256)
        if (versionCode < 0L) return

        val update = AppUpdateChecker.Update(versionName, versionCode, downloadUrl, sha256)
        val appContext = context.applicationContext
        // A download easily outlives a receiver - run it in a foreground service so the process
        // isn't reaped mid-download. Tapping the notification that sent this broadcast is a
        // foreground-service start exemption, so this normally succeeds; if the platform still
        // refuses, hold the broadcast open until the work finishes rather than dropping it at once.
        if (LongTaskService.launch(appContext, "Updating HG2Gui ${update.versionName}") {
                AppUpdateChecker.downloadAndInstallBlocking(appContext, update)
            }) return
        val pending = goAsync()
        Thread {
            try {
                AppUpdateChecker.downloadAndInstallBlocking(appContext, update)
            } finally {
                pending.finish()
            }
        }.start()
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
