package com.hereliesaz.hg2gui.terminal

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercises Hg2Downloader against a real loopback HTTP server (JDK built-in, no extra deps). */
class Hg2DownloaderTest {

    private class TestServer(var body: ByteArray) {
        var etag: String? = "\"v1\""
        var lastModified: String? = null
        var rangeStartOverride: Long? = null
        var truncateAt: Int? = null
        val requests = CopyOnWriteArrayList<String>()
        private val http = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)

        init {
            http.createContext("/file") { handle(it) }
            http.start()
        }

        val url: String get() = "http://127.0.0.1:${http.address.port}/file"
        fun stop() = http.stop(0)

        private fun handle(ex: HttpExchange) {
            try {
                val range = ex.requestHeaders.getFirst("Range")
                val ifRange = ex.requestHeaders.getFirst("If-Range")
                requests += "${ex.requestMethod} range=$range ifRange=$ifRange"
                etag?.let { ex.responseHeaders.add("ETag", it) }
                lastModified?.let { ex.responseHeaders.add("Last-Modified", it) }
                if (ex.requestMethod == "HEAD") {
                    ex.responseHeaders.add("Content-Length", body.size.toString())
                    ex.sendResponseHeaders(200, -1)
                    return
                }
                val validatorOk = ifRange == null || ifRange == etag || ifRange == lastModified
                val start = range?.removePrefix("bytes=")?.substringBefore('-')?.toLongOrNull()
                if (start != null && validatorOk) {
                    if (start >= body.size) {
                        ex.sendResponseHeaders(416, -1)
                        return
                    }
                    val slice = body.copyOfRange(start.toInt(), body.size)
                    ex.responseHeaders.add("Content-Range", "bytes ${rangeStartOverride ?: start}-${body.size - 1}/${body.size}")
                    ex.sendResponseHeaders(206, slice.size.toLong())
                    ex.responseBody.use { it.write(slice) }
                } else {
                    val limit = truncateAt
                    ex.sendResponseHeaders(200, body.size.toLong())
                    ex.responseBody.use { out ->
                        if (limit != null) out.write(body, 0, limit) else out.write(body)
                    }
                }
            } catch (_: Exception) {
                // A deliberately truncated response ends in an IOException on close.
            } finally {
                ex.close()
            }
        }
    }

    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun bytes(seed: Int, size: Int = 300_000) = ByteArray(size) { ((it * 31 + seed) % 251).toByte() }

    private inline fun withServer(body: ByteArray, block: (TestServer, Hg2Downloader, File) -> Unit) {
        val server = TestServer(body)
        try {
            withHg2TempDir { dir ->
                block(server, Hg2Downloader(File(dir, "downloads"), OkHttpClient()), File(dir, "downloads/out.bin"))
            }
        } finally {
            server.stop()
        }
    }

    private fun Hg2Downloader.fetch(url: String, target: File, expected: String? = null) =
        runBlocking { downloadInternal(url, target, expected, allowHttp = true) { _, _ -> } }

    // --- basic ------------------------------------------------------------------------------

    @Test
    fun freshDownload_writesTheFileWithItsDigest() {
        val content = bytes(1)
        withServer(content) { server, downloader, target ->
            val result = downloader.fetch(server.url, target, sha(content))
            assertEquals(Hg2Downloader.Disposition.DOWNLOADED, result.disposition)
            assertEquals(sha(content), result.sha256)
            assertEquals(content.toList(), target.readBytes().toList())
            assertFalse(File(target.path + ".part").exists())
        }
    }

    @Test
    fun shaMismatchOnAFreshDownload_failsAndLeavesNoTarget() {
        withServer(bytes(1)) { server, downloader, target ->
            assertThrows(IllegalStateException::class.java) { downloader.fetch(server.url, target, "00".repeat(32)) }
            assertFalse(target.exists())
            assertFalse(File(target.path + ".part").exists())
        }
    }

    @Test
    fun aTruncatedBodyIsAFailureNotACompleteFile() {
        val content = bytes(1)
        withServer(content) { server, downloader, target ->
            server.truncateAt = 1000
            assertThrows(Exception::class.java) { downloader.fetch(server.url, target) }
            assertFalse("a short body must never be promoted", target.exists())
        }
    }

    // --- resume -----------------------------------------------------------------------------

    private fun seedPart(target: File, content: ByteArray, bytes: Int, etag: String?) {
        File(target.path + ".part").apply { parentFile.mkdirs() }.writeBytes(content.copyOf(bytes))
        if (etag != null) File(target.path + ".part.meta").writeText("etag=${etag.replace("\"", "\\\"")}\nlength=${content.size}\n")
    }

    @Test
    fun resumeWithAMatchingEtag_sendsIfRangeAndAppends() {
        val content = bytes(2)
        withServer(content) { server, downloader, target ->
            seedPart(target, content, 100_000, "\"v1\"")
            val result = downloader.fetch(server.url, target)
            assertEquals(Hg2Downloader.Disposition.RESUMED, result.disposition)
            assertEquals(100_000L, result.resumedFrom)
            assertEquals(content.toList(), target.readBytes().toList())
            assertTrue(server.requests.any { it.contains("range=bytes=100000-") && it.contains("ifRange=\"v1\"") })
        }
    }

    @Test
    fun resumeAfterTheRemoteObjectChanged_restartsInsteadOfSplicingTwoVersions() {
        val oldContent = bytes(3)
        val newContent = bytes(4)
        withServer(newContent) { server, downloader, target ->
            server.etag = "\"v2\""
            seedPart(target, oldContent, 100_000, "\"v1\"")
            val result = downloader.fetch(server.url, target)
            assertEquals(Hg2Downloader.Disposition.DOWNLOADED, result.disposition)
            assertEquals(newContent.toList(), target.readBytes().toList())
        }
    }

    @Test
    fun aSixOhSixWhoseContentRangeDoesNotStartAtOurOffset_isNotTrusted() {
        val content = bytes(5)
        withServer(content) { server, downloader, target ->
            server.rangeStartOverride = 7
            seedPart(target, content, 100_000, "\"v1\"")
            val result = downloader.fetch(server.url, target)
            assertEquals(Hg2Downloader.Disposition.DOWNLOADED, result.disposition)
            assertEquals(content.toList(), target.readBytes().toList())
        }
    }

    @Test
    fun aPartWithNoValidatorAndNoExpectedDigest_isNotResumed() {
        val content = bytes(6)
        withServer(content) { server, downloader, target ->
            seedPart(target, bytes(99), 100_000, null) // unrelated stale bytes, no meta
            val result = downloader.fetch(server.url, target)
            assertEquals(Hg2Downloader.Disposition.DOWNLOADED, result.disposition)
            assertEquals(content.toList(), target.readBytes().toList())
            assertTrue(server.requests.none { it.contains("range=bytes") })
        }
    }

    @Test
    fun aPartWithNoValidatorButAnExpectedDigest_isResumedAndVerified() {
        val content = bytes(7)
        withServer(content) { server, downloader, target ->
            seedPart(target, content, 50_000, null)
            val result = downloader.fetch(server.url, target, sha(content))
            assertEquals(Hg2Downloader.Disposition.RESUMED, result.disposition)
            assertEquals(content.toList(), target.readBytes().toList())
        }
    }

    @Test
    fun aDigestMismatchAfterResume_getsOneCleanRetry() {
        val content = bytes(8)
        withServer(content) { server, downloader, target ->
            // Stale bytes from a DIFFERENT object, resumed because the digest is the only proof.
            seedPart(target, bytes(77), 50_000, null)
            val result = downloader.fetch(server.url, target, sha(content))
            assertEquals(content.toList(), target.readBytes().toList())
            assertEquals(sha(content), result.sha256)
            assertEquals(Hg2Downloader.Disposition.DOWNLOADED, result.disposition)
        }
    }

    // --- reuse ------------------------------------------------------------------------------

    @Test
    fun anUnchangedRemote_isReusedWithoutRedownloading() {
        val content = bytes(9)
        withServer(content) { server, downloader, target ->
            downloader.fetch(server.url, target)
            server.requests.clear()
            val second = downloader.fetch(server.url, target)
            assertEquals(Hg2Downloader.Disposition.REUSED, second.disposition)
            assertTrue(server.requests.all { it.startsWith("HEAD") })
        }
    }

    @Test
    fun aRemoteWithTheSameLengthButANewEtag_isNotReused() {
        // The old check compared Content-Length only, so same-size different content was "Already downloaded".
        val first = bytes(10)
        val second = bytes(11)
        assertEquals(first.size, second.size)
        withServer(first) { server, downloader, target ->
            downloader.fetch(server.url, target)
            server.body = second
            server.etag = "\"v2\""
            val result = downloader.fetch(server.url, target)
            assertEquals(Hg2Downloader.Disposition.DOWNLOADED, result.disposition)
            assertEquals(second.toList(), target.readBytes().toList())
        }
    }

    @Test
    fun aRemoteWithNoValidators_isNeverReusedOnLengthAlone() {
        val first = bytes(12)
        val second = bytes(13)
        withServer(first) { server, downloader, target ->
            server.etag = null
            downloader.fetch(server.url, target)
            server.body = second
            val result = downloader.fetch(server.url, target)
            assertEquals(Hg2Downloader.Disposition.DOWNLOADED, result.disposition)
            assertEquals(second.toList(), target.readBytes().toList())
        }
    }

    @Test
    fun withAnExpectedDigest_aMatchingFileIsReusedAndAMismatchedOneReplaced() {
        val content = bytes(14)
        withServer(content) { server, downloader, target ->
            target.parentFile.mkdirs()
            target.writeBytes(content)
            assertEquals(Hg2Downloader.Disposition.REUSED, downloader.fetch(server.url, target, sha(content)).disposition)
            target.writeBytes(bytes(15))
            assertEquals(Hg2Downloader.Disposition.DOWNLOADED, downloader.fetch(server.url, target, sha(content)).disposition)
            assertEquals(content.toList(), target.readBytes().toList())
        }
    }

    // --- scheme and names -------------------------------------------------------------------

    @Test
    fun plainHttpIsRefusedByDefault_andOtherSchemesAlways() {
        withServer(bytes(1)) { server, downloader, target ->
            val http = assertThrows(IllegalStateException::class.java) {
                runBlocking { downloader.download(server.url, target) }
            }
            assertTrue(http.message, http.message!!.contains("http://"))
            assertTrue("no request may reach the server", server.requests.isEmpty())
            assertThrows(IllegalStateException::class.java) {
                runBlocking { downloader.downloadInternal("file:///etc/passwd", target, null, allowHttp = true) { _, _ -> } }
            }
            assertThrows(IllegalStateException::class.java) {
                runBlocking { downloader.downloadInternal("ftp://example.com/x", target, null, allowHttp = true) { _, _ -> } }
            }
        }
    }

    @Test
    fun sanitizeFileName_neverYieldsDotOrDotDot() {
        withHg2TempDir { dir ->
            val downloader = Hg2Downloader(dir, OkHttpClient())
            for (name in listOf(".", "..", "...", "", "   ", "a/..", "x/./..", "\\..\\..", "/")) {
                val clean = downloader.sanitizeFileName(name)
                assertTrue("'$name' -> '$clean'", clean != "." && clean != ".." && clean.isNotBlank() && !clean.all { it == '.' })
            }
            assertEquals("download.bin", downloader.sanitizeFileName(".."))
            assertEquals("report.tar.gz", downloader.sanitizeFileName("/tmp/evil/report.tar.gz"))
            assertEquals("a_b_c", downloader.sanitizeFileName("a b c"))
            assertEquals(".hidden", downloader.sanitizeFileName(".hidden"))
        }
    }

    @Test
    fun theDownloadCommand_refusesHttpWithoutTheFlagAndRejectsDotNames() {
        val content = bytes(16)
        withServer(content) { server, downloader, _ ->
            val refused = runBlocking {
                runCatching { downloader.run("download ${server.url}").toList() }
            }
            assertTrue(refused.isFailure)
            val allowed = runBlocking { downloader.run("download --allow-http ${server.url} ..").toList() }
            assertTrue(allowed.toString(), allowed.any { it.startsWith("Saved to") && it.endsWith("download.bin") })
        }
    }
}
