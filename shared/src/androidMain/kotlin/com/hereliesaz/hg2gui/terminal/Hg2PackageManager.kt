package com.hereliesaz.hg2gui.terminal

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.GZIPInputStream

/**
 * HG2Gui-owned package path for the Termux repository.
 *
 * HG2Gui owns repository metadata, dependency resolution, downloads, integrity checks and the
 * install transaction. The bundled dpkg binary is retained only as the .deb unpack/configure
 * primitive because it already understands Debian archive semantics and Termux maintainer data.
 */
class Hg2PackageManager(
    private val context: Context,
    private val client: OkHttpClient
) {
    private val prefix = DistroManager.prefixDir(context)
    private val cacheDir = File(context.cacheDir, "hg2-packages").apply { mkdirs() }
    private val stateDir = File(prefix, "var/lib/hg2pkg").apply { mkdirs() }
    private val packagesFile = File(stateDir, "Packages")
    private val dpkgLauncher = File(context.applicationInfo.nativeLibraryDir, "libhg2gui_dpkg.so")

    data class PackageRecord(
        val name: String,
        val version: String,
        val architecture: String,
        val filename: String,
        val sha256: String,
        val size: Long,
        val depends: List<List<String>>
    )

    private data class Installed(val name: String, val version: String)

    fun handles(line: String): Boolean {
        val words = words(line)
        if (words.isEmpty()) return false
        return words.first() in setOf("pkg", "hg2pkg")
    }

    fun run(line: String): Flow<String> = flow {
        val args = words(line).drop(1)
        when (args.firstOrNull()) {
            "install", "in" -> {
                val requested = args.drop(1).filterNot { it.startsWith("-") }
                if (requested.isEmpty()) {
                    emit("usage: pkg install <package> [package…]")
                    return@flow
                }
                install(requested) { emit(it) }
            }
            "update", "up" -> updateIndex { emit(it) }
            "search" -> {
                val query = args.drop(1).joinToString(" ").trim()
                if (query.isEmpty()) {
                    emit("usage: pkg search <query>")
                    return@flow
                }
                ensureIndex { emit(it) }
                val records = parsePackages(packagesFile.readText())
                    .filter { it.name.contains(query, ignoreCase = true) }
                    .take(50)
                emit(if (records.isEmpty()) "No packages matching '$query'." else records.joinToString("\n") { "${it.name} ${it.version}" })
            }
            "list-installed" -> {
                val installed = readInstalled().values.sortedBy { it.name }
                emit(if (installed.isEmpty()) "No installed packages recorded by dpkg." else installed.joinToString("\n") { "${it.name} ${it.version}" })
            }
            null -> emit("HG2Gui package manager\nusage: pkg <install|update|search|list-installed> …")
            else -> emit("HG2Gui package manager: unsupported pkg operation '${args.first()}'. Use apt/dpkg directly for compatibility operations.")
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun install(requested: List<String>, emit: suspend (String) -> Unit) {
        ensureIndex(emit)
        val available = parsePackages(packagesFile.readText()).associateBy { it.name }
        val installed = readInstalled()
        val plan = resolve(requested, available, installed)
        if (plan.isEmpty()) {
            emit("Already satisfied: ${requested.joinToString(" ")}")
            return
        }

        val total = plan.sumOf { it.size.coerceAtLeast(0L) }
        emit("HG2Gui resolved ${plan.size} package${if (plan.size == 1) "" else "s"} (${formatBytes(total)}).")
        emit(plan.joinToString("\n") { "  ${it.name} ${it.version}" })

        val archives = ArrayList<File>(plan.size)
        var overallDone = 0L
        for (pkg in plan) {
            emit("Downloading ${pkg.name} ${pkg.version}…")
            val archive = downloadPackage(pkg) { packageDone ->
                val combined = overallDone + packageDone
                val percent = if (total > 0) ((combined * 100) / total).coerceIn(0, 100) else 0
                emit("$percent% [${pkg.name} ${formatBytes(packageDone)}/${formatBytes(pkg.size)}]")
            }
            archives += archive
            overallDone += pkg.size.coerceAtLeast(0L)
        }

        if (!dpkgLauncher.canExecute()) error("HG2Gui dpkg launcher is unavailable in ${context.applicationInfo.nativeLibraryDir}")

        emit("Installing ${archives.size} verified package archive${if (archives.size == 1) "" else "s"}…")
        runDpkg(listOf("--unpack") + archives.map { it.absolutePath }, emit)
        repairMaintainerScripts()
        runDpkg(listOf("--configure", "-a"), emit)
        emit("Installed: ${requested.joinToString(" ")}")
    }

    private suspend fun updateIndex(emit: suspend (String) -> Unit) {
        val url = "$REPO/dists/stable/main/binary-aarch64/Packages.gz"
        emit("Downloading HG2Gui package index…")
        val request = Request.Builder().url(url).header("Cache-Control", "no-cache").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Package index download failed: HTTP ${response.code}")
            val body = response.body ?: error("Package index response was empty")
            val tmp = File(stateDir, "Packages.tmp")
            GZIPInputStream(body.byteStream()).bufferedReader().use { reader -> tmp.writer().use { writer -> reader.copyTo(writer) } }
            if (!tmp.renameTo(packagesFile)) {
                tmp.copyTo(packagesFile, overwrite = true)
                tmp.delete()
            }
        }
        val count = parsePackages(packagesFile.readText()).size
        emit("HG2Gui package index ready: $count packages.")
    }

    private suspend fun ensureIndex(emit: suspend (String) -> Unit) {
        if (!packagesFile.isFile || packagesFile.length() == 0L) updateIndex(emit)
    }

    private fun resolve(
        requested: List<String>,
        available: Map<String, PackageRecord>,
        installed: Map<String, Installed>
    ): List<PackageRecord> {
        val visiting = HashSet<String>()
        val planned = LinkedHashMap<String, PackageRecord>()

        fun visit(name: String) {
            if (name in planned || name in installed) return
            if (!visiting.add(name)) return
            val pkg = available[name] ?: error("Package '$name' was not found in the Termux repository")
            for (alternatives in pkg.depends) {
                val satisfied = alternatives.any { it in installed || it in planned }
                if (satisfied) continue
                val choice = alternatives.firstOrNull { it in available }
                    ?: error("${pkg.name} depends on unavailable alternative: ${alternatives.joinToString(" | ")}")
                visit(choice)
            }
            visiting.remove(name)
            planned[name] = pkg
        }

        requested.forEach(::visit)
        return planned.values.toList()
    }

    private fun downloadPackage(pkg: PackageRecord, progress: suspend (Long) -> Unit): File {
        val output = File(cacheDir, "${pkg.name}_${pkg.version}_${pkg.architecture}.deb".replace('/', '_'))
        if (output.isFile && pkg.sha256.isNotBlank() && sha256(output).equals(pkg.sha256, ignoreCase = true)) return output

        val url = if (pkg.filename.startsWith("http://") || pkg.filename.startsWith("https://")) pkg.filename else "$REPO/${pkg.filename.trimStart('/')}"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Download failed for ${pkg.name}: HTTP ${response.code}")
            val body = response.body ?: error("Download response was empty for ${pkg.name}")
            val tmp = File(cacheDir, output.name + ".part")
            val digest = MessageDigest.getInstance("SHA-256")
            var done = 0L
            body.byteStream().use { input ->
                FileOutputStream(tmp).use { out ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        if (n == 0) continue
                        out.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        done += n
                        kotlinx.coroutines.runBlocking { progress(done) }
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(Locale.US, it.toInt() and 0xff) }
            if (pkg.sha256.isNotBlank() && !actual.equals(pkg.sha256, ignoreCase = true)) {
                tmp.delete()
                error("SHA-256 mismatch for ${pkg.name}")
            }
            if (!tmp.renameTo(output)) {
                tmp.copyTo(output, overwrite = true)
                tmp.delete()
            }
        }
        return output
    }

    private suspend fun runDpkg(args: List<String>, emit: suspend (String) -> Unit) {
        val command = ArrayList<String>(args.size + 1)
        command += dpkgLauncher.absolutePath
        command += args
        val process = ProcessBuilder(command)
            .directory(prefix)
            .redirectErrorStream(true)
            .apply {
                environment()["PREFIX"] = prefix.absolutePath
                environment()["HOME"] = DistroManager.homeDir(context).absolutePath
                environment()["PATH"] = "${prefix.absolutePath}/bin:/system/bin"
                environment()["LD_LIBRARY_PATH"] = "${prefix.absolutePath}/lib"
                environment()["TMPDIR"] = "${prefix.absolutePath}/tmp"
            }
            .start()
        process.inputStream.bufferedReader().useLines { lines -> lines.forEach { kotlinx.coroutines.runBlocking { emit(it) } } }
        val code = process.waitFor()
        if (code != 0) error("dpkg exited with code $code")
    }

    private fun repairMaintainerScripts() {
        val infoDir = File(prefix, "var/lib/dpkg/info")
        if (!infoDir.isDirectory) return
        infoDir.listFiles().orEmpty().forEach { file ->
            if (!file.isFile) return@forEach
            val text = runCatching { file.readText() }.getOrNull() ?: return@forEach
            if (OLD_PREFIX !in text) return@forEach
            runCatching { file.writeText(text.replace(OLD_PREFIX, prefix.absolutePath)) }
        }
    }

    private fun readInstalled(): Map<String, Installed> {
        val status = File(prefix, "var/lib/dpkg/status")
        if (!status.isFile) return emptyMap()
        return status.readText().split("\n\n").mapNotNull { paragraph ->
            val fields = paragraph.lineSequence().mapNotNull { line ->
                val i = line.indexOf(':')
                if (i <= 0) null else line.substring(0, i) to line.substring(i + 1).trim()
            }.toMap()
            val name = fields["Package"] ?: return@mapNotNull null
            if (fields["Status"] != "install ok installed") return@mapNotNull null
            Installed(name, fields["Version"].orEmpty())
        }.associateBy { it.name }
    }

    private fun parsePackages(text: String): List<PackageRecord> = text.split("\n\n").mapNotNull { paragraph ->
        val fields = LinkedHashMap<String, String>()
        var current: String? = null
        for (line in paragraph.lineSequence()) {
            if (line.startsWith(' ') && current != null) {
                fields[current] = fields.getValue(current) + " " + line.trim()
                continue
            }
            val i = line.indexOf(':')
            if (i <= 0) continue
            current = line.substring(0, i)
            fields[current] = line.substring(i + 1).trim()
        }
        val name = fields["Package"] ?: return@mapNotNull null
        val filename = fields["Filename"] ?: return@mapNotNull null
        PackageRecord(
            name = name,
            version = fields["Version"].orEmpty(),
            architecture = fields["Architecture"].orEmpty(),
            filename = filename,
            sha256 = fields["SHA256"].orEmpty(),
            size = fields["Size"]?.toLongOrNull() ?: 0L,
            depends = parseDepends(fields["Depends"].orEmpty())
        )
    }

    private fun parseDepends(raw: String): List<List<String>> {
        if (raw.isBlank()) return emptyList()
        return raw.split(',').mapNotNull { clause ->
            val alternatives = clause.split('|').mapNotNull { alt ->
                alt.trim().substringBefore(' ').substringBefore(':').takeIf { it.isNotBlank() }
            }
            alternatives.takeIf { it.isNotEmpty() }
        }
    }

    private fun words(line: String): List<String> = Regex("""(?:[^\s\"']+|\"[^\"]*\"|'[^']*')+""")
        .findAll(line)
        .map { it.value.trim().removeSurrounding("\"").removeSurrounding("'") }
        .toList()

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (n > 0) digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(Locale.US, it.toInt() and 0xff) }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> "%.1f GiB".format(Locale.US, bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> "%.1f MiB".format(Locale.US, bytes / (1024.0 * 1024))
        bytes >= 1024L -> "%.1f KiB".format(Locale.US, bytes / 1024.0)
        else -> "$bytes B"
    }

    companion object {
        private const val REPO = "https://packages.termux.dev/apt/termux-main"
        private const val OLD_PREFIX = "/data/data/com.termux/files/usr"
    }
}
