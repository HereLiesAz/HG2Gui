package com.hereliesaz.hg2gui.terminal

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

class Hg2Downloader(
    context: Context,
    private val client: OkHttpClient
) {
    private val appContext = context.applicationContext
    private val downloadsDir = File(appContext.filesDir, "downloads").apply { mkdirs() }

    data class Result(
        val file: File,
        val bytes: Long,
        val sha256: String
    )

    fun handles(line: String): Boolean = line.trim().substringBefore(' ') in setOf("download", "hg2download")

    fun run(line: String): Flow<String> = channelFlow {
        val args = words(line).drop(1)
        val url = args.firstOrNull()
        if (url.isNullOrBlank()) {
            send("usage: download <url> [filename]")
            return@channelFlow
        }
        val requestedName = args.getOrNull(1)
        val target = File(downloadsDir, sanitizeFileName(requestedName ?: fileNameFromUrl(url)))
        val result = download(url, target) { done, total ->
            val percent = if (total > 0L) ((done * 100L) / total).coerceIn(0L, 100L) else 0L
            send("$percent% [${target.name} ${formatBytes(done)}/${if (total > 0L) formatBytes(total) else "?"}]")
        }
        send("Downloaded ${result.file.name} (${formatBytes(result.bytes)})")
        send("Saved to ${result.file.absolutePath}")
        send("SHA-256 ${result.sha256}")
    }.flowOn(Dispatchers.IO)

    suspend fun download(
        url: String,
        target: File,
        expectedSha256: String? = null,
        progress: suspend (done: Long, total: Long) -> Unit = { _, _ -> }
    ): Result {
        target.parentFile?.mkdirs()
        val part = File(target.parentFile, target.name + ".part")
        var existing = if (part.isFile) part.length() else 0L

        fun execute(rangeStart: Long): okhttp3.Response {
            val builder = Request.Builder().url(url)
            if (rangeStart > 0L) builder.header("Range", "bytes=$rangeStart-")
            return client.newCall(builder.build()).execute()
        }

        var response = execute(existing)
        if (existing > 0L && response.code != 206) {
            response.close()
            part.delete()
            existing = 0L
            response = execute(0L)
        }

        response.use { resp ->
            if (!resp.isSuccessful) error("Download failed: HTTP ${resp.code}")
            val body = resp.body
            val responseLength = body.contentLength().coerceAtLeast(0L)
            val total = if (existing > 0L && resp.code == 206) existing + responseLength else responseLength
            val digest = MessageDigest.getInstance("SHA-256")

            if (existing > 0L) {
                part.inputStream().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        if (n > 0) digest.update(buffer, 0, n)
                    }
                }
            }

            var done = existing
            FileOutputStream(part, existing > 0L).use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        if (n == 0) continue
                        out.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        done += n
                        progress(done, total)
                    }
                }
            }

            val sha = digest.digest().joinToString("") { "%02x".format(Locale.US, it.toInt() and 0xff) }
            if (!expectedSha256.isNullOrBlank() && !sha.equals(expectedSha256, ignoreCase = true)) {
                part.delete()
                error("SHA-256 mismatch for ${target.name}")
            }

            if (target.exists() && !target.delete()) error("Cannot replace ${target.absolutePath}")
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            return Result(target, done, sha)
        }
    }

    private fun fileNameFromUrl(url: String): String {
        val path = runCatching { URI(url).path }.getOrNull().orEmpty()
        return path.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "download.bin"
    }

    private fun sanitizeFileName(name: String): String = name
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .ifBlank { "download.bin" }

    private fun words(line: String): List<String> = Regex("""(?:[^\s\"']+|\"[^\"]*\"|'[^']*')+""")
        .findAll(line)
        .map { it.value.trim().removeSurrounding("\"").removeSurrounding("'") }
        .toList()

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> "%.1f GiB".format(Locale.US, bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> "%.1f MiB".format(Locale.US, bytes / (1024.0 * 1024))
        bytes >= 1024L -> "%.1f KiB".format(Locale.US, bytes / 1024.0)
        else -> "$bytes B"
    }

    companion object {
        private const val BUFFER_SIZE = 128 * 1024
    }
}
