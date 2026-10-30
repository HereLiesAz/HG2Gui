package com.hereliesaz.hg2gui.terminal

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/**
 * Authenticated Termux repository access with mirror failover.
 *
 * Every candidate mirror must provide an InRelease whose OpenPGP signature verifies against the
 * Termux APT repository keyrings under `$PREFIX/share/termux-keyring`. The Packages.gz digest is
 * then matched against the SHA-256 entry inside that signed release before HG2Gui accepts the
 * package index. Mirrors provide availability only.
 *
 * Trust-root provenance: that directory lives inside the app-writable prefix, so by itself it is
 * not a trust root - anything that can write the prefix (a package, a maintainer script) could
 * drop in its own key. A keyring file is therefore only used when its SHA-256 is one of
 *  - the digests pinned in this build ([PINNED_KEYRING_SHA256], taken from the SHA-256-pinned
 *    bootstrap archive), or
 *  - the digests recorded in app-private storage by [recordKeyringProvenance] right after
 *    HG2Gui itself installed `termux-keyring` from an already-authenticated index (so a legitimate
 *    key rotation extends trust by induction, a file that merely appeared in the prefix does not).
 * This narrows the trust root to the pinned archive plus the signed update chain; it is not a
 * defence against code that already controls the whole app sandbox.
 *
 * Concurrency: every read/modify of the cached `Packages` index and its authenticity marker is
 * serialised by one process-wide lock ([INDEX_LOCK]), because any tab may construct a client (which
 * validates the cache) at any time, independent of the package-operation lock.
 */
class TermuxRepositoryClient internal constructor(
    private val prefix: File,
    private val home: File,
    private val trustDir: File,
    private val client: OkHttpClient
) {
    constructor(context: Context, client: OkHttpClient) : this(
        DistroManager.prefixDir(context),
        DistroManager.homeDir(context),
        File(context.noBackupFilesDir, "hg2-trust"),
        client
    )

    data class IndexResult(
        val mirror: String,
        val packagesGz: File,
        val releaseText: String,
        /** The signed InRelease `Date:` in epoch millis, or null if it carried none. */
        val releaseDateMillis: Long? = null
    )

    private val gpgv = File(prefix, "bin/gpgv")
    private val keyringDir = File(prefix, "share/termux-keyring")
    private val repositoryState = File(prefix, "var/lib/hg2pkg")
    private val cachedPackages = File(repositoryState, "Packages")
    private val authenticatedMarker = File(repositoryState, "Packages.authenticated")
    // The newest signed InRelease Date ever accepted. Never deleted on refresh (unlike the marker),
    // so a mirror replaying an older - validly signed - InRelease is refused even after a crash.
    private val acceptedReleaseDate = File(repositoryState, "InRelease.date")
    private val recordedKeyringDigests = File(trustDir, "keyring-digests")

    init {
        // Builds predating signed-index enforcement may have left a perfectly parseable but
        // unauthenticated Packages cache behind. Never grandfather it into the new trust model -
        // and the marker's recorded digest must actually match the cache on disk, otherwise any
        // write to Packages after authentication would silently inherit the marker's trust.
        synchronized(INDEX_LOCK) {
            if (cachedPackages.exists()) {
                val recorded = authenticatedMarker.takeIf { it.isFile }?.readLines()?.getOrNull(2)?.trim()
                if (recorded == null || !recorded.equals(sha256(cachedPackages), ignoreCase = true)) {
                    cachedPackages.delete()
                    authenticatedMarker.delete()
                }
            }
        }
    }

    /**
     * Records that the decompressed index the caller just promoted to `Packages` came from an
     * authenticated [result]: the marker stores the SHA-256 of `Packages` itself, which the
     * constructor re-checks. Prefer [installAuthenticatedIndex], which does the promote and the
     * marker write under one lock acquisition.
     */
    fun markIndexAuthenticated(architecture: String, result: IndexResult) {
        synchronized(INDEX_LOCK) {
            writeMarker(architecture, result)
        }
    }

    /**
     * Decompresses the authenticated [result] into `Packages` and records its marker as ONE step
     * with respect to every other client in the process. Previously the caller promoted the file
     * and wrote the marker separately; another tab constructing a client in between saw a
     * markerless `Packages`, deleted the fresh index, and the marker write then threw.
     */
    fun installAuthenticatedIndex(architecture: String, result: IndexResult) {
        synchronized(INDEX_LOCK) {
            repositoryState.mkdirs()
            val tmp = File(repositoryState, "Packages.tmp")
            try {
                GZIPInputStream(result.packagesGz.inputStream().buffered()).use { gz ->
                    tmp.outputStream().buffered().use { out -> gz.copyTo(out) }
                }
                try {
                    Files.move(tmp.toPath(), cachedPackages.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(tmp.toPath(), cachedPackages.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                tmp.delete()
            }
            writeMarker(architecture, result)
        }
    }

    private fun writeMarker(architecture: String, result: IndexResult) {
        authenticatedMarker.parentFile?.mkdirs()
        authenticatedMarker.writeText("$architecture\n${result.mirror}\n${sha256(cachedPackages)}\n")
    }

    /**
     * Call after HG2Gui itself installed or upgraded `termux-keyring` from an authenticated index:
     * records the digests of the keyring files now on disk as trusted (see the class doc).
     */
    fun recordKeyringProvenance() {
        synchronized(INDEX_LOCK) {
            val digests = keyringDir.listFiles().orEmpty()
                .filter { it.isFile && it.extension == "gpg" }
                .map { sha256(it) }
            val merged = (readRecordedDigests() + digests).toSortedSet()
            trustDir.mkdirs()
            AtomicFiles.write(recordedKeyringDigests, merged.joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8))
        }
    }

    fun fetchAuthenticatedIndex(architecture: String, workDir: File): IndexResult {
        require(gpgv.canExecute()) { "HG2Gui gpgv is unavailable at ${gpgv.absolutePath}" }
        require(repositoryKeyrings().isNotEmpty()) {
            "No trusted Termux APT repository keyring at ${keyringDir.absolutePath}"
        }
        workDir.mkdirs()

        // A refresh invalidates the old cache before network work begins. If the process dies at
        // any point before the caller atomically promotes the newly authenticated index, the next
        // package operation sees no usable Packages file and performs authentication again.
        val lastAcceptedDate = synchronized(INDEX_LOCK) {
            authenticatedMarker.delete()
            cachedPackages.delete()
            acceptedReleaseDate.takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull()
        } ?: FLOOR_RELEASE_DATE_MILLIS
        val failures = mutableListOf<String>()
        for (mirror in MAIN_MIRRORS) {
            runCatching {
                val result = fetchFromMirror(mirror, architecture, workDir, lastAcceptedDate)
                result.releaseDateMillis?.let {
                    synchronized(INDEX_LOCK) {
                        acceptedReleaseDate.parentFile?.mkdirs()
                        // Never move the replay baseline backwards.
                        val stored = acceptedReleaseDate.takeIf { f -> f.isFile }?.readText()?.trim()?.toLongOrNull()
                        if (stored == null || it > stored) acceptedReleaseDate.writeText(it.toString())
                    }
                }
                return result
            }.onFailure { failures += "$mirror — ${it.message ?: it::class.java.simpleName}" }
        }
        error("No authenticated Termux repository mirror succeeded:\n${failures.joinToString("\n")}")
    }

    suspend fun downloadVerifiedPackage(
        downloader: Hg2Downloader,
        filename: String,
        target: File,
        expectedSha256: String,
        preferredMirror: String?,
        progress: suspend (done: Long, total: Long) -> Unit
    ): Hg2Downloader.Result {
        require(SHA256.matches(expectedSha256)) {
            "Authenticated package metadata is missing a valid SHA-256 for $filename"
        }
        val failures = mutableListOf<String>()
        for (url in packageUrls(filename, preferredMirror)) {
            runCatching { return downloader.download(url, target, expectedSha256, progress) }
                .onFailure { failures += "$url — ${it.message ?: it::class.java.simpleName}" }
        }
        error("Package download failed on every mirror:\n${failures.joinToString("\n")}")
    }

    fun packageUrls(filename: String, preferredMirror: String?): List<String> {
        if (filename.startsWith("https://")) return listOf(filename)
        if (filename.startsWith("http://")) error("Refusing insecure package URL: $filename")
        val ordered = buildList {
            preferredMirror?.takeIf { it in MAIN_MIRRORS }?.let(::add)
            MAIN_MIRRORS.filterNot { it == preferredMirror }.forEach(::add)
        }
        return ordered.map { "$it/${filename.trimStart('/')}" }
    }

    private fun fetchFromMirror(
        mirror: String,
        architecture: String,
        workDir: File,
        lastAcceptedDateMillis: Long?
    ): IndexResult {
        val safeMirror = mirror.substringAfter("https://").replace(Regex("[^A-Za-z0-9._-]"), "_")
        val inRelease = File(workDir, "InRelease-$safeMirror")
        val packagesGz = File(workDir, "Packages-$architecture-$safeMirror.gz")

        download("$mirror/dists/stable/InRelease", inRelease)
        verifyInRelease(inRelease)
        val releaseText = clearSignedPayload(inRelease.readText())
        val releaseDate = checkReleaseFreshness(
            releaseText,
            System.currentTimeMillis(),
            lastAcceptedDateMillis,
            maxAgeWithoutValidUntilMillis = MAX_UNBOUNDED_RELEASE_AGE_MILLIS
        )
        val relative = "main/binary-$architecture/Packages.gz"
        val expected = releaseSha256(releaseText, relative)
            ?: error("Signed InRelease from $mirror has no SHA256 entry for $relative")

        download("$mirror/dists/stable/$relative", packagesGz)
        val actual = sha256(packagesGz)
        if (!actual.equals(expected, ignoreCase = true)) {
            error("Packages.gz digest mismatch from $mirror: expected $expected, got $actual")
        }
        return IndexResult(mirror, packagesGz, releaseText, releaseDate)
    }

    private fun download(url: String, destination: File) {
        val request = Request.Builder().url(url).header("Cache-Control", "no-cache").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code} for $url")
            destination.parentFile?.mkdirs()
            val temp = File(destination.parentFile, destination.name + ".part")
            response.body.byteStream().use { input -> temp.outputStream().use(input::copyTo) }
            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = true)
                temp.delete()
            }
        }
    }

    private fun verifyInRelease(inRelease: File) {
        val keyrings = repositoryKeyrings()
        require(keyrings.isNotEmpty()) {
            "No trusted Termux APT repository keyring at ${keyringDir.absolutePath} " +
                "(a keyring file must match a digest pinned in this build or recorded after an authenticated termux-keyring install)"
        }
        val command = buildList {
            add(gpgv.absolutePath)
            keyrings.forEach { keyring ->
                add("--keyring")
                add(keyring.absolutePath)
            }
            add(inRelease.absolutePath)
        }
        val process = ProcessBuilder(command).directory(prefix).redirectErrorStream(true).apply {
            environment()["PREFIX"] = prefix.absolutePath
            environment()["HOME"] = home.absolutePath
            environment()["PATH"] = "${prefix.absolutePath}/bin:/system/bin"
            environment()["LD_LIBRARY_PATH"] = "${prefix.absolutePath}/lib"
        }.start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val code = process.waitFor()
        if (code != 0) {
            val names = keyrings.joinToString(", ") { it.name }
            error("InRelease signature verification failed using [$names]: ${output.takeLast(2000)}")
        }
    }

    /** Keyring files under the prefix whose digest is pinned or recorded (see the class doc). */
    internal fun repositoryKeyrings(): List<File> {
        val trusted = PINNED_KEYRING_SHA256 + synchronized(INDEX_LOCK) { readRecordedDigests() }
        return keyringDir.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && it.extension == "gpg" }
            // Upstream Termux deliberately excludes this pacman-only key from APT trust.
            .filterNot { it.name == PACMAN_KEYRING }
            .filter { sha256(it).lowercase() in trusted }
            .sortedBy { it.name }
            .toList()
    }

    private fun readRecordedDigests(): Set<String> =
        runCatching { recordedKeyringDigests.readLines().map { it.trim().lowercase() }.filter { SHA256.matches(it) }.toSet() }
            .getOrDefault(emptySet())

    private fun clearSignedPayload(text: String): String {
        val normalized = text.replace("\r\n", "\n")
        val marker = "-----BEGIN PGP SIGNED MESSAGE-----"
        val signature = "-----BEGIN PGP SIGNATURE-----"
        require(normalized.startsWith(marker)) { "InRelease is not an OpenPGP clear-signed message" }
        val bodyStart = normalized.indexOf("\n\n")
        require(bodyStart >= 0) { "InRelease clear-signed headers are malformed" }
        val signatureStart = normalized.indexOf(signature, bodyStart + 2)
        require(signatureStart > bodyStart) { "InRelease signature block is missing" }
        return normalized.substring(bodyStart + 2, signatureStart)
            .lineSequence()
            .joinToString("\n") { line -> if (line.startsWith("- ")) line.removePrefix("- ") else line }
            .trimEnd()
    }

    private fun releaseSha256(release: String, relativePath: String): String? {
        var inSha256 = false
        for (line in release.lineSequence()) {
            if (!line.startsWith(' ') && line.endsWith(':')) {
                inSha256 = line.trim() == "SHA256:"
                continue
            }
            if (!inSha256) continue
            val parts = line.trim().split(Regex("\\s+"), limit = 3)
            if (parts.size == 3 && parts[2] == relativePath && SHA256.matches(parts[0])) return parts[0]
        }
        return null
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val SHA256 = Regex("[0-9a-fA-F]{64}")
        private const val PACMAN_KEYRING = "termux-pacman.gpg"

        /** Process-wide: guards the cached index, its marker and the replay baseline. */
        private val INDEX_LOCK = Any()

        /**
         * SHA-256 of each `.gpg` file under `share/termux-keyring/` shipped in the pinned bootstrap archive
         * (bootstrap-2026.08.16-r1+apt.android-7; identical across aarch64/arm/x86_64). Bump with
         * the bootstrap tag in [DistroManager]. The pacman keyring is listed for completeness but
         * is never used for APT.
         */
        internal val PINNED_KEYRING_SHA256: Set<String> = setOf(
            "df8b62dfe4d160558a7f6f37f37c44c0f23b731e102f9400f79392a895473474", // thunder-coding
            "46502e5633a4ce767a9ffb7234e9c4d96bba8b9a749a3dc93e31f2eb2af71fc3", // 2096779623
            "21c385d5a30107453bd60582d64e2f6e5f5ce11e340ac05e57f943f9c0235420", // termux-autobuilds
            "98040a3411003720f8c541227382bffd8486be3075498959cbca88ba176b9748", // agnostic-apollo
            "a0d57f1458ff789b5b524159b56aa76439f995b5131fa643080084fff4e7ac5a", // termux-pacman
            "322284cca7927ac0d69368e74a712b14a3bdf10bde4ba8bde44f1817fd784a7b", // grimler
            "4e6473b5916ffd6a2b7082c0e81431cc2590a6e912be2bf5de6c734022e586b8", // landfillbaby
            "070762bc8630c00aa1f365bdc158024a5721099b7aef5070df9c9aec35686fe6", // kcubeterm
            "fba4cee6ecdc03f5ae2de08534bc23fedcfc05beb055a804c39ea10466b15141" // mradityaalok
        )

        /**
         * Replay floor for the very first fetch, before any InRelease has been accepted (so there
         * is no stored baseline): 2026-06-01 UTC, comfortably before the pinned bootstrap's
         * 2026-08-16 release. It cannot stop a replay of a newer-than-floor snapshot on a first
         * install - that remains an accepted limitation of a repository without Valid-Until.
         */
        internal const val FLOOR_RELEASE_DATE_MILLIS = 1_780_272_000_000L

        /**
         * Termux's InRelease normally carries no `Valid-Until`, so nothing else bounds how stale a
         * validly signed snapshot may be. Refuse one older than this (180 days) as a freeze bound.
         */
        internal const val MAX_UNBOUNDED_RELEASE_AGE_MILLIS = 180L * 24 * 60 * 60 * 1000

        // Primary/CDN plus mirrors listed by Termux's mirror rotation. All are still required to
        // pass the same signed InRelease verification before HG2Gui accepts their metadata.
        val MAIN_MIRRORS: List<String> = listOf(
            "https://packages.termux.dev/apt/termux-main",
            "https://packages-cf.termux.dev/apt/termux-main",
            "https://mirror.accum.se/mirror/termux.dev/termux-main",
            "https://ftp.agdsn.de/termux/termux-main"
        )
    }
}

/** A top-level `Field: value` from a Debian Release/InRelease payload, or null if absent. */
internal fun releaseField(release: String, name: String): String? =
    release.lineSequence()
        .firstOrNull { !it.startsWith(' ') && it.startsWith("$name:") }
        ?.substringAfter(':')
        ?.trim()

/** Parses a Debian Release date ("Sat, 16 Aug 2026 12:00:00 UTC"), or null if unparseable. */
internal fun parseReleaseDate(value: String): Long? = listOf(
    "EEE, dd MMM yyyy HH:mm:ss zzz",
    "EEE, dd MMM yyyy HH:mm:ss Z",
).firstNotNullOfOrNull { pattern ->
    try {
        java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply { isLenient = false }.parse(value)?.time
    } catch (_: java.text.ParseException) {
        null
    }
}

/**
 * Freeze/replay protection for an already signature-verified InRelease payload: rejects it when
 * its `Valid-Until` has passed, or its `Date` is older than [lastAcceptedDateMillis] (the newest
 * one previously accepted - a mirror serving an old, validly signed snapshot to hold back
 * security updates). A present-but-unparseable field is rejected too. When the payload has no
 * `Valid-Until` and [maxAgeWithoutValidUntilMillis] is set, a `Date` older than that bound is
 * rejected as a frozen snapshot (without a `Valid-Until` nothing else limits staleness). Returns
 * the payload's `Date` in epoch millis (null if it has none). Pure, see TermuxReleaseFreshnessTest.
 */
internal fun checkReleaseFreshness(
    release: String,
    nowMillis: Long,
    lastAcceptedDateMillis: Long?,
    maxAgeWithoutValidUntilMillis: Long? = null
): Long? {
    val validUntilField = releaseField(release, "Valid-Until")
    validUntilField?.let { raw ->
        val validUntil = parseReleaseDate(raw) ?: error("InRelease has an unparseable Valid-Until: $raw")
        if (nowMillis > validUntil) error("InRelease expired (Valid-Until $raw)")
    }
    val date = releaseField(release, "Date")?.let { raw ->
        parseReleaseDate(raw) ?: error("InRelease has an unparseable Date: $raw")
    }
    if (date != null && lastAcceptedDateMillis != null && date < lastAcceptedDateMillis) {
        error("InRelease Date is older than the last accepted index (possible replay)")
    }
    if (validUntilField == null && date != null && maxAgeWithoutValidUntilMillis != null &&
        nowMillis - date > maxAgeWithoutValidUntilMillis
    ) {
        error("InRelease has no Valid-Until and its Date is older than ${maxAgeWithoutValidUntilMillis / 86_400_000L} days (possible frozen mirror)")
    }
    return date
}
