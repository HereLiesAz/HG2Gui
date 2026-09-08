package com.hereliesaz.hg2gui.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hereliesaz.hg2gui.R
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Checks HG2Gui's rolling GitHub release and surfaces a newer APK automatically. */
object AppUpdateChecker {
    private const val RELEASES_API = "https://api.github.com/repos/HereLiesAz/HG2Gui/releases/tags/latest-debug-v0.7"
    private const val RELEASE_PAGE = "https://github.com/HereLiesAz/HG2Gui/releases/tag/latest-debug-v0.7"
    private const val CHANNEL_ID = "hg2gui_updates"
    private const val NOTIFICATION_ID = 2001
    private val VERSIONED_APK = Regex("""hg2gui-playstore-debug-([0-9]+(?:\.[0-9]+)*)\.apk""")

    data class Update(val versionName: String, val versionCode: Long, val downloadUrl: String)

    fun check(context: Context): Update? {
        val installedCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
        }
        val connection = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "HG2Gui-update-checker")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val release = connection.inputStream.bufferedReader().use { it.readText() }
            val assets = org.json.JSONObject(release).optJSONArray("assets") ?: JSONArray()
            var newest: Update? = null
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val match = VERSIONED_APK.matchEntire(asset.optString("name")) ?: continue
                val versionName = match.groupValues[1]
                val build = versionName.substringAfterLast('.').toLongOrNull() ?: continue
                val url = asset.optString("browser_download_url")
                if (url.isBlank()) continue
                if (newest == null || build > newest.versionCode) newest = Update(versionName, build, url)
            }
            newest?.takeIf { it.versionCode > installedCode }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    /** Runs off the UI thread. Failures are deliberately silent: being offline is not an error. */
    fun checkAndNotify(context: Context) {
        val appContext = context.applicationContext
        Thread {
            val update = check(appContext) ?: return@Thread
            if (canNotify(appContext)) {
                showNotification(appContext, update)
            } else {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(appContext, "HG2Gui ${update.versionName} is available", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun showNotification(context: Context, update: Update) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl))
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("HG2Gui update available")
            .setContentText("Version ${update.versionName} is ready. Tap to download.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun openUpdate(context: Context, update: Update?) {
        val url = update?.downloadUrl ?: RELEASE_PAGE
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
