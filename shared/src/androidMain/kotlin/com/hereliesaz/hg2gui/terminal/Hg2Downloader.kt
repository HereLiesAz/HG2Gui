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
import java.util.Properties

/**
 * Resumable, verifiable HTTP(S) downloader.
 *
 * Safety properties (all pinned by Hg2DownloaderTest):
 *  - https only by default; plain http needs an explicit opt-in (`download --allow-http`), and a
 *    redirect from https down to http is refused rather than followed.
 *  - the target file name can never be "." / ".." / a bare run of dots.
 *  - a partial download is only resumed when the server can prove it is the same object: a strong
 *    ETag or Last-Modified captured with the first bytes is replayed as `If-Range`, and a 206 reply
 *    must carry a `Content-Range` starting exactly at the bytes already on disk. Anything else
 *    restarts from zero. (With a caller-supplied SHA-256 the final digest is the proof, so resume
 *    is also allowed without a validator, and a mismatch after a resume gets one clean retry.)
 *  - "already downloaded" without a caller-supplied SHA-256 requires the server's current
 *    ETag/Last-Modified AND length to equal what was recorded when the file was saved; a matching
 *    Content-Length alone proves nothing.
 */
class Hg2Downloader internal constructor(
    private val downloadsDir: File,
    private val client: OkHttpClient
) {
    constructor(context: Context, client: OkHttpClient) : this(File(context.applicationContext.filesDir, "downloads"), client)

    init {
        downloadsDir.mkdirs()
    }

    enum class Disposition {
        REUSED,
        RESUMED,
        DOWNLOADED
    }

    data class Result(
        val file: File,
        val bytes: Long,
        val sha256: String,
        val disposition: Disposition,
        val resumedFrom: Long = 0L
    )

    /** What identifies one version of a remote object; persisted next to the file / part. */
    internal data class Validators(val etag: String?, val lastModified: String?, val length: Long?) {
        /** The value to send as `If-Range`, or null if the server gave us nothing to compare. */
        val ifRange: String? get() = etag ?: lastModified
        val isEmpty: Boolean get() = etag == null && lastModified == null
    }

    fun handles(line: String): Boolean = line.trim().substringBefore(' ') in setOf("download", "hg2download")

    /**
     * `download [--allow-http] <url> [filename]`. Plain-http URLs are refused unless
     * `--allow-http` is given.
     */
    fun run(line: String): Flow<String> = channelFlow {
        val args = words(line).drop(1)
        val allowHttp = "--allow-http" in args
        val positional = args.filterNot { it.startsWith("--") }
        val url = positional.firstOrNull()
        if (url.isNullOrBlank()) {
            send("usage: download [--allow-http] <url> [filename]")
            return@channelFlow
        }
        val requestedName = positional.getOrNull(1)
        val target = File(downloadsDir, sanitizeFileName(requestedName ?: fileNameFromUrl(url)))
        val result = downloadInternal(url, target, null, allowHttp) { done, total ->
            val percent = if (total > 0L) ((done * 100L) / total).coerceIn(0L, 100L) else 0L
            send("$percent% [${target.name} ${formatBytes(done)}/${if (total > 0L) formatBytes(total) else "?"}]")
        }
        when (result.disposition) {
            Disposition.REUSED -> send("Already downloaded: ${result.file.name} (${formatBytes(result.bytes)})")
            Disposition.RESUMED -> send("Resumed ${result.file.name} from ${formatBytes(result.resumedFrom)}; complete at ${formatBytes(result.bytes)}")
            Disposition.DOWNLOADED -> send("Downloaded ${result.file.name} (${formatBytes(result.bytes)})")
        }
        send("Saved to ${result.file.absolutePath}")
        send("SHA-256 ${result.sha256}")
    }.flowOn(Dispatchers.IO)

    suspend fun download(
        url: String,
        target: File,
        expectedSha256: String? = null,
        progress: suspend (done: Long, total: Long) -> Unit = { _, _ -> }
    ): Result = downloadInternal(url, target, expectedSha256, allowHttp = false, progress = progress)

    internal suspend fun downloadInternal(
        url: String,
        target: File,
        expectedSha256: String?,
        allowHttp: Boolean,
        progress: suspend (done: Long, total: Long) -> Unit
    ): Result {
        requireAllowedScheme(url, allowHttp)
        target.parentFile?.mkdirs()
        val expected = expectedSha256?.takeIf { it.isNotBlank() }
        val part = partFile(target)

        if (target.isFile) {
            val existingSha = sha256(target)
            val usable = if (expected != null) {
                existingSha.equals(expected, ignoreCase = true)
            } else {
                isUnchangedRemote(url, target, existingSha, allowHttp)
            }
            if (usable) {
                return Result(
                    file = target,
                    bytes = target.length(),
                    sha256 = existingSha,
                    disposition = Disposition.REUSED
                )
            }
            target.delete()
            finalMetaFile(target).delete()
        }

        var attempt = 0
        while (true) {
            val outcome = transfer(url, target, part, expected, allowHttp, progress)
            if (outcome.mismatch) {
                // The digest is the only proof of integrity. A mismatch after a RESUME may just mean
                // the remote object changed under the partial bytes: start over once.
                part.delete()
                partMetaFile(target).delete()
                if (outcome.resumedFrom > 0L && attempt == 0) {
                    attempt++
                    continue
                }
                error("SHA-256 mismatch for ${target.name}")
            }
            return outcome.result!!
        }
    }

    private class Outcome(val result: Result?, val mismatch: Boolean, val resumedFrom: Long)

    private suspend fun transfer(
        url: String,
        target: File,
        part: File,
        expected: String?,
        allowHttp: Boolean,
        progress: suspend (done: Long, total: Long) -> Unit
    ): Outcome {
        var existing = if (part.isFile) part.length() else 0L
        val savedValidators = readValidators(partMetaFile(target))
        // Resume only with proof of identity: a stored validator, or a digest to check at the end.
        if (existing > 0L && (savedValidators == null || savedValidators.isEmpty) && expected == null) {
            part.delete()
            partMetaFile(target).delete()
            existing = 0L
        }

        fun execute(rangeStart: Long): okhttp3.Response {
            val builder = Request.Builder().url(url)
            if (rangeStart > 0L) {
                builder.header("Range", "bytes=$rangeStart-")
                savedValidators?.ifRange?.let { builder.header("If-Range", it) }
            }
            return client.newCall(builder.build()).execute()
        }

        var response = execute(existing)
        if (existing > 0L && response.code == 416) {
            response.close()
            val partSha = sha256(part)
            if (expected != null && partSha.equals(expected, ignoreCase = true)) {
                promote(part, target)
                finishMeta(target, savedValidators, partSha)
                return Outcome(
                    Result(target, target.length(), partSha, Disposition.RESUMED, existing),
                    mismatch = false,
                    resumedFrom = existing
                )
            }
            part.delete()
            partMetaFile(target).delete()
            existing = 0L
            response = execute(0L)
        }
        if (existing > 0L && !isValidResumeReply(response, existing)) {
            // A 200 (validator mismatch / Range ignored) or a 206 whose Content-Range does not
            // start at our offset: the partial bytes cannot be trusted to belong to this body.
            response.close()
            part.delete()
            partMetaFile(target).delete()
            existing = 0L
            response = execute(0L)
        }
        checkRedirectScheme(response, allowHttp)

        response.use { resp ->
            if (!resp.isSuccessful) error("Download failed: HTTP ${resp.code}")
            val body = resp.body
            val resuming = existing > 0L
            val total: Long = if (resuming) {
                contentRangeTotal(resp.header("Content-Range")) ?: (existing + body.contentLength().coerceAtLeast(0L))
            } else {
                body.contentLength().coerceAtLeast(0L)
            }
            val validators = if (resuming) savedValidators else validatorsOf(resp)
            if (!resuming) writeValidators(partMetaFile(target), validators)
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
                progress(existing, total)
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

            // Content-Length / Content-Range say how much to expect; a short body is a failure, not
            // a "complete" file whose hash happens to get printed.
            if (total > 0L && done != total) {
                part.delete()
                partMetaFile(target).delete()
                error("Download of ${target.name} ended at $done of $total bytes")
            }

            val sha = digest.digest().joinToString("") { java.lang.String.format(Locale.US, "%02x", it.toInt() and 0xff) }
            if (expected != null && !sha.equals(expected, ignoreCase = true)) {
                return Outcome(null, mismatch = true, resumedFrom = if (resuming) existing else 0L)
            }

            promote(part, target)
            finishMeta(target, validators, sha)
            return Outcome(
                Result(
                    file = target,
                    bytes = done,
                    sha256 = sha,
                    disposition = if (resuming) Disposition.RESUMED else Disposition.DOWNLOADED,
                    resumedFrom = if (resuming) existing else 0L
                ),
                mismatch = false,
                resumedFrom = if (resuming) existing else 0L
            )
        }
    }

    /** True iff [response] is a 206 whose Content-Range begins exactly at [offset]. */
    private fun isValidResumeReply(response: okhttp3.Response, offset: Long): Boolean {
        if (response.code != 206) return false
        val range = response.header("Content-Range") ?: return false
        return CONTENT_RANGE.matchEntire(range.trim())?.groupValues?.get(1)?.toLongOrNull() == offset
    }

    private fun contentRangeTotal(header: String?): Long? =
        header?.let { CONTENT_RANGE.matchEntire(it.trim()) }?.groupValues?.get(3)?.toLongOrNull()

    private fun checkRedirectScheme(response: okhttp3.Response, allowHttp: Boolean) {
        if (!allowHttp && !response.request.url.isHttps) {
            response.close()
            error("Refusing insecure redirect to ${response.request.url}")
        }
    }

    private fun requireAllowedScheme(url: String, allowHttp: Boolean) {
        val scheme = runCatching { URI(url).scheme }.getOrNull()?.lowercase()
        when {
            scheme == "https" -> Unit
            scheme == "http" && allowHttp -> Unit
            scheme == "http" -> error("Refusing insecure http:// URL (use https, or pass --allow-http to the download command): $url")
            else -> error("Unsupported URL scheme '${scheme ?: "none"}' (only https is allowed): $url")
        }
    }

    /**
     * The saved file is reusable without a caller-supplied digest only if the server's CURRENT
     * validators and length equal what was recorded when it was saved, and the file on disk still
     * hashes to the recorded digest.
     */
    private fun isUnchangedRemote(url: String, target: File, currentSha: String, allowHttp: Boolean): Boolean {
        val saved = readProperties(finalMetaFile(target)) ?: return false
        if (!currentSha.equals(saved.getProperty("sha256"), ignoreCase = true)) return false
        val recorded = Validators(saved.getProperty("etag"), saved.getProperty("lastModified"), saved.getProperty("length")?.toLongOrNull())
        if (recorded.isEmpty || recorded.length != target.length()) return false
        val request = Request.Builder().url(url).head().build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                if (!allowHttp && !response.request.url.isHttps) return@use false
                val now = validatorsOf(response)
                now.length == recorded.length &&
                    ((recorded.etag != null && recorded.etag == now.etag) ||
                        (recorded.etag == null && recorded.lastModified != null && recorded.lastModified == now.lastModified))
            }
        }.getOrDefault(false)
    }

    private fun validatorsOf(response: okhttp3.Response): Validators {
        val etag = response.header("ETag")?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("W/") }
        val modified = response.header("Last-Modified")?.trim()?.takeIf { it.isNotEmpty() }
        val length = (response.header("Content-Range")?.let { contentRangeTotal(it) })
            ?: response.header("Content-Length")?.toLongOrNull()?.takeIf { it >= 0L }
        return Validators(etag, modified, length)
    }

    private fun partFile(target: File) = File(target.parentFile, target.name + ".part")
    private fun partMetaFile(target: File) = File(target.parentFile, target.name + ".part.meta")
    private fun finalMetaFile(target: File) = File(target.parentFile, target.name + ".meta")

    private fun readProperties(file: File): Properties? {
        if (!file.isFile) return null
        return runCatching { Properties().also { props -> file.inputStream().use(props::load) } }.getOrNull()
    }

    private fun readValidators(file: File): Validators? {
        val props = readProperties(file) ?: return null
        return Validators(props.getProperty("etag"), props.getProperty("lastModified"), props.getProperty("length")?.toLongOrNull())
    }

    private fun writeValidators(file: File, validators: Validators?, sha256: String? = null) {
        val props = Properties()
        validators?.etag?.let { props.setProperty("etag", it) }
        validators?.lastModified?.let { props.setProperty("lastModified", it) }
        validators?.length?.let { props.setProperty("length", it.toString()) }
        sha256?.let { props.setProperty("sha256", it) }
        runCatching { file.outputStream().use { props.store(it, null) } }
    }

    /** Moves the part's validators to the finished file's sidecar (adding the digest). */
    private fun finishMeta(target: File, validators: Validators?, sha256: String) {
        partMetaFile(target).delete()
        writeValidators(finalMetaFile(target), validators?.copy(length = target.length()), sha256)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (n > 0) digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { java.lang.String.format(Locale.US, "%02x", it.toInt() and 0xff) }
    }

    private fun promote(part: File, target: File) {
        if (target.exists() && !target.delete()) error("Cannot replace ${target.absolutePath}")
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            part.delete()
        }
    }

    private fun fileNameFromUrl(url: String): String {
        val path = runCatching { URI(url).path }.getOrNull().orEmpty()
        return path.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "download.bin"
    }

    /**
     * A single safe file name: directory parts stripped, anything outside `[A-Za-z0-9._-]`
     * replaced, and never blank, "." or "..". (A name of dots would resolve to the downloads
     * directory or its parent; saving over it only failed by luck because `File.delete()` refuses
     * non-empty directories.)
     */
    internal fun sanitizeFileName(name: String): String = name
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .let { if (it.isBlank() || it.all { c -> c == '.' }) "download.bin" else it }

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

        /** `bytes <start>-<end>/<total>` (total may be `*`). */
        private val CONTENT_RANGE = Regex("bytes (\\d+)-(\\d+)/(\\d+|\\*)")
    }
}
