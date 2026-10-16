package com.hereliesaz.hg2gui.azp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private const val REPOSITORY_BASE = "https://www.azphalt.store"

/**
 * A thin client for the azphalt Repository API (spec/repository-api.md) - the same standard
 * interface `pkg`/`apt` play for Debian packages, but for `.azp` extensions: search, then
 * download the raw archive. HG2Gui never executes a `.azp`'s sandboxed code or WASM payload (it
 * has no such runtime) - see AzpInstaller for what "install" actually means here.
 */
object AzpClient {
    private const val MAX_DOWNLOAD_BYTES = 256L * 1024 * 1024
    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // AZP-10: OkHttp's own defaults follow both same-scheme and HTTPS->HTTP redirects, which
        // would let a compromised or misconfigured REPOSITORY_BASE silently hand a `.azp`
        // download (or the discovery/search responses that steer it) off to an arbitrary,
        // possibly cleartext host. The repository's own domain is fixed and never legitimately
        // redirects a download, so a 3xx here is treated as a failure (resp.isSuccessful is
        // false for 3xx, which every call site below already handles) rather than followed.
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun search(query: String, kind: String?, page: Int = 1): AzpSearchResponse =
        withContext(Dispatchers.IO) {
            val url = "$REPOSITORY_BASE/packages".toHttpUrl().newBuilder()
                .apply {
                    if (query.isNotBlank()) addQueryParameter("q", query)
                    if (!kind.isNullOrBlank()) addQueryParameter("kind", kind)
                    addQueryParameter("page", page.toString())
                }
                .build()
            val request = Request.Builder().url(url).get().build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext AzpSearchResponse()
                val body = resp.body.string()
                json.decodeFromString(AzpSearchResponse.serializer(), body)
            }
        }

    /** Downloads the raw `.azp` (a ZIP archive) for the given package/version. Free packages
     *  only - paid packages need a Bearer entitlement HG2Gui does not yet implement. */
    suspend fun download(id: String, version: String): ByteArray? = withContext(Dispatchers.IO) {
        // id/version come from search results (untrusted) - never let them add path segments
        // or query strings to the request, and refuse the same values AzpInstaller would.
        if (!isValidAzpPathSegment(id) || !isValidAzpPathSegment(version)) return@withContext null
        val url = REPOSITORY_BASE.toHttpUrl().newBuilder()
            .addPathSegment("packages").addPathSegment(id)
            .addPathSegment("versions").addPathSegment(version)
            .addPathSegment("download")
            .build()
        val request = Request.Builder().url(url).get().build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body
            if (body.contentLength() > MAX_DOWNLOAD_BYTES) return@withContext null
            // Streamed with a hard cap rather than body.bytes(), which would buffer an unbounded
            // (or lying-Content-Length) response entirely into memory.
            val out = java.io.ByteArrayOutputStream()
            body.byteStream().use { input ->
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > MAX_DOWNLOAD_BYTES) return@withContext null
                    out.write(buffer, 0, n)
                }
            }
            out.toByteArray()
        }
    }

    /** The registry's trust anchor - `signingKeys`, used to tell a [AzpTrust.TRUSTED] package
     *  from one that's merely internally-consistent ([AzpTrust.VALID]). */
    suspend fun discovery(): AzpDiscovery? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$REPOSITORY_BASE/.well-known/azphalt-repository.json").get().build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            json.decodeFromString(AzpDiscovery.serializer(), resp.body.string())
        }
    }
}
