package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.os.Build
import android.system.Os
import com.hereliesaz.hg2gui.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Downloads and installs a real Termux bootstrap - the same rootfs archive upstream Termux
 * ships - giving this app a genuine `usr/` prefix with `apt`/`pkg` and real coreutils, rather
 * than the bare, toybox-only `/system/bin/sh` Android provides on its own.
 */
object DistroManager {

    // /releases/latest/download/<asset> always redirects to whatever the current release is,
    // so this doesn't go stale the way pinning one specific release tag does.
    private const val BOOTSTRAP_BASE_URL = "https://github.com/termux/termux-packages/releases/latest/download"
    private const val BOOTSTRAP_FILE = "bootstrap.zip"
    private const val SYMLINKS_ENTRY = "SYMLINKS.txt"
    // U+2190 LEFTWARDS ARROW - the field separator Termux's own bootstrap builder puts between
    // a symlink's target and the path it should be created at.
    private const val SYMLINK_SEPARATOR = "←"

    private fun bootstrapArch(): String? = when (Build.SUPPORTED_ABIS.firstOrNull()) {
        "arm64-v8a" -> "aarch64"
        "armeabi-v7a" -> "arm"
        "x86_64" -> "x86_64"
        "x86" -> "i686"
        else -> null
    }

    fun prefixDir(context: Context): File = File(context.filesDir, "usr")
    fun homeDir(context: Context): File = File(context.filesDir, "home")

    fun isInstalled(context: Context): Boolean {
        val usrDir = prefixDir(context)
        return usrDir.isDirectory && File(usrDir, "bin").isDirectory
    }

    fun bootstrap(context: Context, client: OkHttpClient): Flow<String> = flow {
        val arch = bootstrapArch()
        if (arch == null) {
            emit("Error: no Termux bootstrap is published for this device's ABI (${Build.SUPPORTED_ABIS.joinToString()}).")
            return@flow
        }

        val url = "$BOOTSTRAP_BASE_URL/bootstrap-$arch.zip"
        emit("Starting bootstrap process...")

        val destFile = File(context.cacheDir, BOOTSTRAP_FILE)
        if (destFile.exists()) destFile.delete()

        emit("Downloading rootfs from $url...")

        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Download failed: HTTP ${response.code}")

                val body = response.body ?: throw Exception("Empty response body")
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            // Progress update every ~1MB
                            if (downloadedBytes % (1024 * 1024) < 8192) {
                                val total = if (totalBytes > 0) "${totalBytes / (1024 * 1024)}MB" else "?"
                                emit("Downloaded: ${downloadedBytes / (1024 * 1024)}MB / $total")
                            }
                        }
                    }
                }
            }

            emit("Download complete. Extracting...")

            val prefix = prefixDir(context)
            extractBootstrap(destFile, prefix)
            homeDir(context).mkdirs()

            emit("Extraction complete. Fixing permissions...")
            makeBinariesExecutable(prefix)

            emit("Bootstrap successful! You can now use 'apt', 'pkg', and real coreutils.")
        } catch (e: Exception) {
            emit("Error during bootstrap: ${e.message}")
            Utils.log(e)
        } finally {
            if (destFile.exists()) destFile.delete()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Extracts a Termux bootstrap zip into [prefix]. Regular files unpack directly; a zip can't
     * carry real symlinks, so Termux's bootstrap builder lists them instead in a root-level
     * SYMLINKS.txt (one per line, "<target><SEPARATOR><link path>") and they're created here as
     * an explicit second pass, once every real file the symlinks might point at already exists.
     */
    private fun extractBootstrap(zipFile: File, prefix: File) {
        prefix.mkdirs()
        val symlinksText = StringBuilder()

        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                when {
                    entry.name == SYMLINKS_ENTRY -> {
                        symlinksText.append(zis.bufferedReader(Charsets.UTF_8).readText())
                    }
                    entry.isDirectory -> {
                        File(prefix, entry.name).mkdirs()
                    }
                    else -> {
                        val outFile = File(prefix, entry.name)
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().buffered().use { out -> zis.copyTo(out) }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        symlinksText.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val separatorIndex = line.indexOf(SYMLINK_SEPARATOR)
            if (separatorIndex < 0) return@forEach
            val target = line.substring(0, separatorIndex)
            val linkPath = line.substring(separatorIndex + SYMLINK_SEPARATOR.length)
            val linkFile = File(prefix, linkPath)
            linkFile.parentFile?.mkdirs()
            // unlink()s the path itself rather than whatever it points to - a no-op if nothing
            // was there yet, and clears a stale symlink left by a prior failed bootstrap attempt.
            linkFile.delete()
            try {
                Os.symlink(target, linkFile.absolutePath)
            } catch (e: Exception) {
                Utils.log(e)
            }
        }
    }

    private fun makeBinariesExecutable(prefix: File) {
        listOf("bin", "libexec", "lib").forEach { sub ->
            File(prefix, sub).walkTopDown().forEach { f ->
                if (f.isFile) f.setExecutable(true)
            }
        }
    }

    private const val OSINT_SCRIPT_NAME = "osint-lookup"

    /** Idempotent: a no-op before a bootstrap exists, and only writes once the script is
     *  actually missing - called both right after a fresh bootstrap and on every launch
     *  thereafter, so an install from before this script existed picks it up too. */
    fun ensureOsintTool(context: Context) {
        if (!isInstalled(context)) return
        val prefix = prefixDir(context)
        if (!File(prefix, "bin/$OSINT_SCRIPT_NAME").exists()) installOsintTool(prefix)
    }

    private fun installOsintTool(prefix: File) {
        val script = File(prefix, "bin/$OSINT_SCRIPT_NAME")
        val shebang = "#!${File(prefix, "bin/bash").absolutePath}\n"
        script.writeText(shebang + OSINT_SCRIPT_BODY)
        script.setExecutable(true)
    }

    // Self-scoped, read-only recon for a domain YOU name - your own domain, or one you have a
    // legitimate reason to research (due diligence, checking your own exposure). Whois, DNS
    // records, and certificate-transparency logs (crt.sh) are all passive lookups against
    // publicly published data; nothing here scans, brute-forces, or contacts the target's own
    // infrastructure. Not a tool for surveilling arbitrary third parties.
    private const val OSINT_SCRIPT_BODY = """
set -u
target="${'$'}{1:-}"
if [ -z "${'$'}target" ]; then
  echo "usage: osint-lookup <domain>" >&2
  exit 1
fi

echo "== whois: ${'$'}target =="
if command -v whois >/dev/null 2>&1; then
  whois "${'$'}target" 2>&1 | head -n 40
else
  echo "(whois not installed - pkg install whois)"
fi

echo
echo "== DNS records: ${'$'}target =="
if command -v dig >/dev/null 2>&1; then
  for rtype in A AAAA MX TXT NS; do
    echo "-- ${'$'}rtype --"
    dig +short "${'$'}target" "${'$'}rtype"
  done
else
  echo "(dig not installed - pkg install dnsutils)"
fi

echo
echo "== certificate transparency (crt.sh): ${'$'}target =="
if command -v curl >/dev/null 2>&1; then
  if command -v jq >/dev/null 2>&1; then
    curl -s "https://crt.sh/?q=%25.${'$'}target&output=json" | jq -r '.[].name_value' | sort -u | head -n 40
  else
    curl -s "https://crt.sh/?q=%25.${'$'}target&output=json" | head -c 2000
    echo
    echo "(install jq for readable output - pkg install jq)"
  fi
else
  echo "(curl not installed - pkg install curl)"
fi
"""
}
