package com.hereliesaz.hg2gui.terminal

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.ArrayDeque
import java.util.Locale
import java.util.zip.GZIPInputStream

class Hg2PackageManager(
    private val context: Context,
    private val client: OkHttpClient,
    private val downloader: Hg2Downloader
) {
    private val prefix = DistroManager.prefixDir(context)
    private val cacheDir = File(context.cacheDir, "hg2-packages").apply { mkdirs() }
    private val stateDir = File(prefix, "var/lib/hg2pkg").apply { mkdirs() }
    private val packagesFile = File(stateDir, "Packages")
    private val dpkgLauncher = File(context.applicationInfo.nativeLibraryDir, "libhg2gui_dpkg.so")
    private val dpkgDeb = File(prefix, "bin/dpkg-deb")

    data class PackageRecord(
        val name: String,
        val version: String,
        val architecture: String,
        val filename: String,
        val sha256: String,
        val size: Long,
        val depends: List<List<String>>,
        val description: String
    )

    private data class Installed(val name: String, val version: String)

    fun handles(line: String): Boolean = words(line).firstOrNull() in setOf("pkg", "hg2pkg")

    fun run(line: String): Flow<String> = channelFlow {
        val output: suspend (String) -> Unit = { send(it) }
        val args = words(line).drop(1)
        when (args.firstOrNull()) {
            "install", "in" -> install(requirePackages(args, "install"), forceRequested = false, emit = output)
            "update" -> updateIndex(output)
            "upgrade", "up" -> upgrade(output)
            "remove", "rm", "uninstall" -> remove(requirePackages(args, "remove"), purge = false, emit = output)
            "purge" -> remove(requirePackages(args, "purge"), purge = true, emit = output)
            "clean" -> clean(output)
            "search" -> search(args.drop(1).joinToString(" "), output)
            "show", "info" -> show(requirePackages(args, "show"), output)
            "list-installed" -> {
                val installed = readInstalled().values.sortedBy { it.name }
                send(if (installed.isEmpty()) "No installed packages recorded by dpkg." else installed.joinToString("\n") { "${it.name} ${it.version}" })
            }
            null -> send(USAGE)
            else -> send("HG2Gui package manager: unsupported operation '${args.first()}'.\n$USAGE")
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun clean(emit: suspend (String) -> Unit) {
        val files = cacheDir.listFiles().orEmpty()
        val removable = files.filter {
            it.isFile && (it.extension == "deb" || it.name.endsWith(".part")) ||
                it.isDirectory && it.name.startsWith("prepare-")
        }
        removable.forEach {
            if (it.isDirectory) it.deleteRecursively() else it.delete()
        }
        emit("Removed ${removable.size} cached package item${if (removable.size == 1) "" else "s"}.")
    }

    private fun requirePackages(args: List<String>, operation: String): List<String> {
        val packages = args.drop(1).filterNot { it.startsWith("-") }
        if (packages.isEmpty()) error("usage: pkg $operation <package> [package…]")
        return packages
    }

    private suspend fun upgrade(emit: suspend (String) -> Unit) {
        updateIndex(emit)
        val available = parsePackages(packagesFile.readText()).associateBy { it.name }
        val installed = readInstalled()
        val upgrades = installed.values.mapNotNull { current ->
            val candidate = available[current.name] ?: return@mapNotNull null
            candidate.takeIf { isVersionLessThan(current.version, candidate.version) }
        }
        if (upgrades.isEmpty()) {
            emit("All installed packages are up to date.")
            return
        }
        emit("Upgrading ${upgrades.size} package${if (upgrades.size == 1) "" else "s"}:")
        upgrades.forEach { emit("  ${it.name}: ${installed.getValue(it.name).version} → ${it.version}") }
        install(upgrades.map { it.name }, forceRequested = true, emit = emit)
    }

    private suspend fun search(query: String, emit: suspend (String) -> Unit) {
        if (query.isBlank()) error("usage: pkg search <query>")
        ensureIndex(emit)
        val records = parsePackages(packagesFile.readText())
            .filter { it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
            .take(50)
        emit(if (records.isEmpty()) "No packages matching '$query'." else records.joinToString("\n") { "${it.name} ${it.version} — ${it.description.lineSequence().firstOrNull().orEmpty()}" })
    }

    private suspend fun show(names: List<String>, emit: suspend (String) -> Unit) {
        ensureIndex(emit)
        val available = parsePackages(packagesFile.readText()).associateBy { it.name }
        val installed = readInstalled()
        names.forEachIndexed { index, name ->
            val pkg = available[name] ?: error("Package '$name' was not found in the Termux repository")
            if (index > 0) emit("")
            emit(buildString {
                appendLine("Package: ${pkg.name}")
                appendLine("Version: ${pkg.version}")
                appendLine("Architecture: ${pkg.architecture}")
                appendLine("Installed: ${installed[name]?.version ?: "no"}")
                appendLine("Download size: ${formatBytes(pkg.size)}")
                if (pkg.depends.isNotEmpty()) appendLine("Depends: ${pkg.depends.joinToString(", ") { it.joinToString(" | ") }}")
                append("Description: ${pkg.description}")
            })
        }
    }

    private suspend fun remove(names: List<String>, purge: Boolean, emit: suspend (String) -> Unit) {
        val installed = readInstalled()
        val present = names.filter { it in installed }
        val missing = names.filterNot { it in installed }
        if (missing.isNotEmpty()) emit("Not installed: ${missing.joinToString(" ")}")
        if (present.isEmpty()) return
        emit("${if (purge) "Purging" else "Removing"}: ${present.joinToString(" ")}")
        runDpkg(listOf(if (purge) "--purge" else "--remove") + present, emit)
        emit("Done: ${present.joinToString(" ")}")
    }

    private suspend fun install(requested: List<String>, forceRequested: Boolean, emit: suspend (String) -> Unit) {
        ensureIndex(emit)
        val available = parsePackages(packagesFile.readText()).associateBy { it.name }
        val installed = readInstalled()
        val plan = resolve(requested, available, installed, forceRequested)
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
            val output = File(cacheDir, "${pkg.name}_${pkg.version}_${pkg.architecture}.deb".replace('/', '_'))
            val url = if (pkg.filename.startsWith("http://") || pkg.filename.startsWith("https://")) pkg.filename else "$REPO/${pkg.filename.trimStart('/')}"
            val result = downloader.download(url, output, pkg.sha256) { packageDone, _ ->
                val combined = overallDone + packageDone
                val percent = if (total > 0L) ((combined * 100L) / total).coerceIn(0L, 100L) else 0L
                emit("$percent% [${pkg.name} ${formatBytes(packageDone)}/${formatBytes(pkg.size)}]")
            }
            emit("Preparing ${pkg.name} for HG2Gui prefix…")
            archives += prepareArchive(result.file, pkg, emit)
            overallDone += pkg.size.coerceAtLeast(0L)
        }

        if (!dpkgLauncher.canExecute()) error("HG2Gui dpkg launcher is unavailable in ${context.applicationInfo.nativeLibraryDir}")
        emit("Installing ${archives.size} verified package archive${if (archives.size == 1) "" else "s"}…")
        runDpkg(listOf("--unpack") + archives.map { it.absolutePath }, emit)
        repairMaintainerScripts()
        runDpkg(listOf("--configure", "-a"), emit)
        emit("Installed: ${requested.joinToString(" ")}")
    }

    private suspend fun prepareArchive(
        archive: File,
        pkg: PackageRecord,
        emit: suspend (String) -> Unit
    ): File {
        if (!dpkgDeb.canExecute()) {
            error("HG2Gui dpkg-deb is unavailable at ${dpkgDeb.absolutePath}")
        }

        val safeStem = "${pkg.name}_${pkg.version}_${pkg.architecture}".replace(Regex("[^A-Za-z0-9._-]"), "_")
        val workDir = File(cacheDir, "prepare-$safeStem")
        val patched = File(cacheDir, "$safeStem.hg2.deb")
        workDir.deleteRecursively()
        patched.delete()
        workDir.mkdirs()

        runTool(listOf(dpkgDeb.absolutePath, "-R", archive.absolutePath, workDir.absolutePath), "dpkg-deb extract")

        var rewritten = 0
        workDir.walkTopDown().filter { it.isFile }.forEach { file ->
            if (rewriteTextPrefix(file)) rewritten++
        }
        emit("Rewrote $rewritten file${if (rewritten == 1) "" else "s"} for ${pkg.name}.")

        runTool(listOf(dpkgDeb.absolutePath, "-b", workDir.absolutePath, patched.absolutePath), "dpkg-deb build")
        if (!patched.isFile || patched.length() == 0L) error("Failed to rebuild ${pkg.name}")
        workDir.deleteRecursively()
        return patched
    }

    private fun rewriteTextPrefix(file: File): Boolean {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return false
        if (bytes.any { it == 0.toByte() }) return false
        val text = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull() ?: return false
        if (!text.contains(OLD_PREFIX)) return false
        return runCatching {
            file.writeText(text.replace(OLD_PREFIX, prefix.absolutePath))
            true
        }.getOrDefault(false)
    }

    private fun runTool(command: List<String>, label: String) {
        val process = ProcessBuilder(command)
            .directory(prefix)
            .redirectErrorStream(true)
            .apply { applyPackageEnvironment(environment()) }
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val code = process.waitFor()
        if (code != 0) {
            val detail = output.trim().takeLast(4000)
            error("$label exited with code $code${if (detail.isBlank()) "" else ":\n$detail"}")
        }
    }

    private suspend fun updateIndex(emit: suspend (String) -> Unit) {
        val url = "$REPO/dists/stable/main/binary-aarch64/Packages.gz"
        emit("Downloading HG2Gui package index…")
        val request = Request.Builder().url(url).header("Cache-Control", "no-cache").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Package index download failed: HTTP ${response.code}")
            val tmp = File(stateDir, "Packages.tmp")
            GZIPInputStream(response.body.byteStream()).bufferedReader().use { reader ->
                tmp.writer().use { writer -> reader.copyTo(writer) }
            }
            if (!tmp.renameTo(packagesFile)) {
                tmp.copyTo(packagesFile, overwrite = true)
                tmp.delete()
            }
        }
        emit("HG2Gui package index ready: ${parsePackages(packagesFile.readText()).size} packages.")
    }

    private suspend fun ensureIndex(emit: suspend (String) -> Unit) {
        if (!packagesFile.isFile || packagesFile.length() == 0L) updateIndex(emit)
    }

    private fun resolve(
        requested: List<String>,
        available: Map<String, PackageRecord>,
        installed: Map<String, Installed>,
        forceRequested: Boolean
    ): List<PackageRecord> {
        val requestedSet = requested.toSet()
        val visiting = HashSet<String>()
        val planned = LinkedHashMap<String, PackageRecord>()

        fun visit(name: String) {
            if (name in planned) return
            if (name in installed && !(forceRequested && name in requestedSet)) return
            if (!visiting.add(name)) return
            val pkg = available[name] ?: error("Package '$name' was not found in the Termux repository")
            for (alternatives in pkg.depends) {
                if (alternatives.any { it in installed || it in planned }) continue
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

    private fun isVersionLessThan(installed: String, available: String): Boolean {
        if (installed == available) return false
        if (!dpkgLauncher.canExecute()) return false
        val process = ProcessBuilder(dpkgLauncher.absolutePath, "--compare-versions", installed, "lt", available)
            .directory(prefix)
            .redirectErrorStream(true)
            .apply { applyPackageEnvironment(environment()) }
            .start()
        process.inputStream.close()
        return process.waitFor() == 0
    }

    private suspend fun runDpkg(args: List<String>, emit: suspend (String) -> Unit) {
        val process = ProcessBuilder(listOf(dpkgLauncher.absolutePath) + args)
            .directory(prefix)
            .redirectErrorStream(true)
            .apply { applyPackageEnvironment(environment()) }
            .start()

        val tail = ArrayDeque<String>(DPKG_ERROR_TAIL_LINES)
        process.inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (tail.size == DPKG_ERROR_TAIL_LINES) tail.removeFirst()
                tail.addLast(line)
                emit(line)
            }
        }
        val code = process.waitFor()
        if (code != 0) {
            val detail = tail.joinToString("\n").trim()
            error("dpkg exited with code $code${if (detail.isBlank()) "" else ":\n$detail"}")
        }
    }

    private fun applyPackageEnvironment(env: MutableMap<String, String>) {
        env["PREFIX"] = prefix.absolutePath
        env["HOME"] = DistroManager.homeDir(context).absolutePath
        env["PATH"] = "${prefix.absolutePath}/bin:/system/bin"
        env["LD_LIBRARY_PATH"] = "${prefix.absolutePath}/lib"
        env["TMPDIR"] = "${prefix.absolutePath}/tmp"
        env["DPKG_ROOT"] = prefix.absolutePath
        env["DPKG_ADMINDIR"] = File(prefix, "var/lib/dpkg").absolutePath
    }

    private fun repairMaintainerScripts() {
        val infoDir = File(prefix, "var/lib/dpkg/info")
        if (!infoDir.isDirectory) return
        infoDir.listFiles().orEmpty().forEach { file ->
            if (file.isFile) rewriteTextPrefix(file)
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
                fields[current] = fields.getValue(current) + "\n" + line.trim()
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
            depends = parseDepends(fields["Depends"].orEmpty()),
            description = fields["Description"].orEmpty()
        )
    }

    private fun parseDepends(raw: String): List<List<String>> {
        if (raw.isBlank()) return emptyList()
        return raw.split(',').mapNotNull { clause ->
            clause.split('|').mapNotNull { alt ->
                alt.trim().substringBefore(' ').substringBefore(':').takeIf { it.isNotBlank() }
            }.takeIf { it.isNotEmpty() }
        }
    }

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
        private const val REPO = "https://packages.termux.dev/apt/termux-main"
        private const val OLD_PREFIX = "/data/data/com.termux/files/usr"
        private const val DPKG_ERROR_TAIL_LINES = 40
        private const val USAGE = "HG2Gui package manager\nusage: pkg <install|update|upgrade|remove|purge|search|show|list-installed|clean> …"
    }
}
