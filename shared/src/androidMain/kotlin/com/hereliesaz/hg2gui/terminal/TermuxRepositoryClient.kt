package com.hereliesaz.hg2gui.terminal

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/**
 * Authenticated Termux repository access with mirror failover.
 *
 * Every candidate mirror must provide an InRelease whose OpenPGP signature verifies against the
 * official APT repository keys already shipped in the pinned Termux bootstrap. The Packages.gz
 * digest is then matched against the SHA-256 entry inside that signed release before HG2Gui accepts
 * the package index. Mirrors provide availability only; the pinned Termux keyring remains the trust
 * root.
 */
class TermuxRepositoryClient(
    private val context: Context,
    private val client: OkHttpClient
) {
    data class IndexResult(
        val mirror: String,
        val packagesGz: File,
        val releaseText: String
    )

    private val prefix = DistroManager.prefixDir(context)
    private val gpgv = File(prefix, "bin/gpgv")
    private val keyringDir = File(prefix, "share/termux-keyring")
    private val repositoryState = File(prefix, "var/lib/hg2pkg")
    private val cachedPackages = File(repositoryState, "Packages")
    private val authenticatedMarker = File(repositoryState, "Packages.authenticated")

    init {
        // Builds predating signed-index enforcement may have left a perfectly parseable but
        // unauthenticated Packages cache behind. Never grandfather it into the new trust model.
        if (cachedPackages.exists() && !authenticatedMarker.isFile) cachedPackages.delete()
    }

    fun fetchAuthenticatedIndex(architecture: String, workDir: File): IndexResult {
        require(gpgv.canExecute()) { "HG2Gui gpgv is unavailable at ${gpgv.absolutePath}" }
        require(repositoryKeyrings().isNotEmpty()) {
            "Termux APT repository keyrings are unavailable at ${keyringDir.absolutePath}"
        }
        workDir.mkdirs()

        // A refresh invalidates the old cache before network work begins. If the process dies at
        // any point before the caller atomically promotes the newly authenticated index, the next
        // package operation sees no usable Packages file and performs authentication again.
        authenticatedMarker.delete()
        cachedPackages.delete()

        val failures = mutableListOf<String>()
        for (mirror in MAIN_MIRRORS) {
            runCatching {
                val result = fetchFromMirror(mirror, architecture, workDir)
                authenticatedMarker.parentFile?.mkdirs()
                authenticatedMarker.writeText("$architecture\n${result.mirror}\n${sha256(result.packagesGz)}\n")
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

    private fun fetchFromMirror(mirror: String, architecture: String, workDir: File): IndexResult {
        val safeMirror = mirror.substringAfter("https://").replace(Regex("[^A-Za-z0-9._-]"), "_")
        val inRelease = File(workDir, "InRelease-$safeMirror")
        val packagesGz = File(workDir, "Packages-$architecture-$safeMirror.gz")

        download("$mirror/dists/stable/InRelease", inRelease)
        verifyInRelease(inRelease)
        val releaseText = clearSignedPayload(inRelease.readText())
        val relative = "main/binary-$architecture/Packages.gz"
        val expected = releaseSha256(releaseText, relative)
            ?: error("Signed InRelease from $mirror has no SHA256 entry for $relative")

        download("$mirror/dists/stable/$relative", packagesGz)
        val actual = sha256(packagesGz)
        if (!actual.equals(expected, ignoreCase = true)) {
            error("Packages.gz digest mismatch from $mirror: expected $expected, got $actual")
        }
        return IndexResult(mirror, packagesGz, releaseText)
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
            "Termux APT repository keyrings are unavailable at ${keyringDir.absolutePath}"
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
            environment()["HOME"] = DistroManager.homeDir(context).absolutePath
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

    private fun repositoryKeyrings(): List<File> =
        keyringDir.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && it.extension == "gpg" }
            // Upstream Termux deliberately excludes this pacman-only key from APT trust.
            .filterNot { it.name == PACMAN_KEYRING }
            .sortedBy { it.name }
            .toList()

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
