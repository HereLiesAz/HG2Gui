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
    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
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
        val url = "$REPOSITORY_BASE/packages/$id/versions/$version/download"
        val request = Request.Builder().url(url).get().build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            resp.body.bytes()
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
