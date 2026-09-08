package com.hereliesaz.hg2gui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight update checker for HG2Gui's rolling GitHub debug release.
 *
 * The workflow keeps many APKs attached to the same release, so release timestamps and tag names
 * are not useful version signals. Instead we inspect every APK asset, parse the version encoded in
 * its filename, and choose the greatest versionCode/build number.
 */
object AppUpdateChecker {
    private const val RELEASES_API = "https://api.github.com/repos/HereLiesAz/HG2Gui/releases/tags/latest-debug-v0.7"
    private const val RELEASE_PAGE = "https://github.com/HereLiesAz/HG2Gui/releases/tag/latest-debug-v0.7"
    private val VERSIONED_APK = Regex("""hg2gui-playstore-debug-([0-9]+(?:\.[0-9]+)*)\.apk""")

    data class Update(
        val versionName: String,
        val versionCode: Long,
        val downloadUrl: String
    )

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
                val name = asset.optString("name")
                val match = VERSIONED_APK.matchEntire(name) ?: continue
                val versionName = match.groupValues[1]
                val build = versionName.substringAfterLast('.').toLongOrNull() ?: continue
                val url = asset.optString("browser_download_url")
                if (url.isBlank()) continue
                if (newest == null || build > newest.versionCode) {
                    newest = Update(versionName, build, url)
                }
            }
            newest?.takeIf { it.versionCode > installedCode }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    fun openUpdate(context: Context, update: Update?) {
        val url = update?.downloadUrl ?: RELEASE_PAGE
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
