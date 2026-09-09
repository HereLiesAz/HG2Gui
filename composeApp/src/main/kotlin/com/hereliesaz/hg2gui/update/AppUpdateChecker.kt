package com.hereliesaz.hg2gui.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hereliesaz.hg2gui.R
import com.hereliesaz.hg2gui.terminal.Hg2Downloader
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** Checks HG2Gui's rolling GitHub release, downloads newer APKs itself, verifies them and hands
 * them to Android's package installer. The browser is no longer part of the update path. */
object AppUpdateChecker {
    private const val RELEASES_API = "https://api.github.com/repos/HereLiesAz/HG2Gui/releases/tags/latest-debug-v0.7"
    private const val CHANNEL_ID = "hg2gui_updates"
    private const val NOTIFICATION_ID = 2001
    private const val DOWNLOAD_NOTIFICATION_ID = 2002
    private val VERSIONED_APK = Regex("""hg2gui-playstore-debug-([0-9]+(?:\.[0-9]+)*)\.apk""")

    data class Update(
        val versionName: String,
        val versionCode: Long,
        val downloadUrl: String,
        val sha256: String?
    )

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun check(context: Context): Update? {
        val installedCode = installedVersionCode(context)
        val request = Request.Builder()
            .url(RELEASES_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "HG2Gui-update-checker")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val assets = JSONObject(response.body.string()).optJSONArray("assets") ?: JSONArray()
                var newest: Update? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val match = VERSIONED_APK.matchEntire(asset.optString("name")) ?: continue
                    val versionName = match.groupValues[1]
                    val build = versionName.substringAfterLast('.').toLongOrNull() ?: continue
                    val url = asset.optString("browser_download_url")
                    if (url.isBlank()) continue
                    val digest = asset.optString("digest")
                        .takeIf { it.startsWith("sha256:", ignoreCase = true) }
                        ?.substringAfter(':')
                        ?.takeIf { it.length == 64 }
                    val candidate = Update(versionName, build, url, digest)
                    if (newest == null || candidate.versionCode > newest!!.versionCode) newest = candidate
                }
                newest?.takeIf { it.versionCode > installedCode }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun checkAndNotify(context: Context) {
        val appContext = context.applicationContext
        Thread {
            val update = check(appContext) ?: return@Thread
            if (canNotify(appContext)) {
                showAvailableNotification(appContext, update)
            } else {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(appContext, "HG2Gui ${update.versionName} is available", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    fun downloadAndInstall(context: Context, update: Update) {
        val appContext = context.applicationContext
        Thread {
            try {
                ensureNotificationChannel(appContext)
                val target = File(appContext.cacheDir, "updates/hg2gui-${update.versionName}.apk")
                val downloader = Hg2Downloader(appContext, client)
                val result = downloader.download(update.downloadUrl, target, update.sha256) { done, total ->
                    if (canNotify(appContext)) showDownloadProgress(appContext, update, done, total)
                }
                verifyDownloadedApk(appContext, result.file, update)
                if (canNotify(appContext)) {
                    appContext.getSystemService(NotificationManager::class.java).cancel(DOWNLOAD_NOTIFICATION_ID)
                }
                launchInstaller(appContext, result.file)
            } catch (e: Exception) {
                notifyFailure(appContext, e.message ?: "Update failed")
            }
        }.start()
    }

    private fun verifyDownloadedApk(context: Context, apk: File, update: Update) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0
        @Suppress("DEPRECATION")
        val archive = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: error("Downloaded file is not a valid APK")
        if (archive.packageName != context.packageName) {
            error("Update APK belongs to ${archive.packageName}, not ${context.packageName}")
        }
        val archiveCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) archive.longVersionCode else {
            @Suppress("DEPRECATION") archive.versionCode.toLong()
        }
        if (archiveCode != update.versionCode) {
            error("Update APK version code $archiveCode does not match release ${update.versionCode}")
        }
        if (archiveCode <= installedVersionCode(context)) error("Downloaded APK is not newer than the installed app")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val installed = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val installedSigners = installed.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet().orEmpty()
            val archiveSigners = archive.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet().orEmpty()
            if (installedSigners.isNotEmpty() && archiveSigners.isNotEmpty() && installedSigners != archiveSigners) {
                error("Update APK signature does not match the installed app")
            }
        }
    }

    private fun launchInstaller(context: Context, apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            notifyFailure(context, "Allow HG2Gui to install updates, then tap the update notification again")
            return
        }

        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("hg2gui-update.apk", 0, apk.length()).use { output ->
                    input.copyTo(output, 256 * 1024)
                    session.fsync(output)
                }
            }
            val callback = Intent(context, UpdateInstallReceiver::class.java)
                .setAction(UpdateInstallReceiver.ACTION_INSTALL_STATUS)
            val pending = PendingIntent.getBroadcast(
                context,
                sessionId,
                callback,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            session.commit(pending.intentSender)
        }
    }

    private fun installedVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else {
            @Suppress("DEPRECATION") info.versionCode.toLong()
        }
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    private fun showAvailableNotification(context: Context, update: Update) {
        ensureNotificationChannel(context)
        val pending = PendingIntent.getBroadcast(
            context,
            update.versionCode.toInt(),
            UpdateActionReceiver.intentFor(context, update),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("HG2Gui update available")
            .setContentText("Version ${update.versionName} is ready. Tap to update.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun showDownloadProgress(context: Context, update: Update, done: Long, total: Long) {
        ensureNotificationChannel(context)
        val percent = if (total > 0) ((done * 100) / total).toInt().coerceIn(0, 100) else 0
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Updating HG2Gui ${update.versionName}")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
        if (total > 0) builder.setProgress(100, percent, false).setContentText("$percent% downloaded")
        else builder.setProgress(0, 0, true).setContentText("Downloading…")
        context.getSystemService(NotificationManager::class.java).notify(DOWNLOAD_NOTIFICATION_ID, builder.build())
    }

    private fun notifyFailure(context: Context, message: String) {
        if (canNotify(context)) {
            ensureNotificationChannel(context)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("HG2Gui update")
                .setContentText(message)
                .setAutoCancel(true)
                .build()
            context.getSystemService(NotificationManager::class.java).notify(DOWNLOAD_NOTIFICATION_ID, notification)
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun openUpdate(context: Context, update: Update?) {
        val resolved = update ?: check(context)
        if (resolved == null) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "HG2Gui is up to date", Toast.LENGTH_SHORT).show()
            }
            return
        }
        downloadAndInstall(context, resolved)
    }
}
