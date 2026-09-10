package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.util.AtomicFile
import android.util.Base64
import java.io.File
import java.nio.file.Files
import java.util.UUID

/** Explicit, bounded restore points for HG2Gui-owned package runtime state. */
object PackageRestorePoints {
    private const val DIRECTORY = "package-restore-points"
    private const val MANIFEST = "manifest.tsv"
    private const val DATA = "data"
    private const val MAX_POINTS_PER_PACKAGE = 3
    private const val MAX_FILES = 20_000
    private const val MAX_BYTES = 512L * 1024L * 1024L

    data class RestorePoint(
        val id: String,
        val timestampMillis: Long,
        val packageVersion: String,
        val pathCount: Int,
        val bytes: Long
    )

    data class RestoreResult(
        val restoredPaths: Int,
        val restoredBytes: Long,
        val failed: List<String>
    )

    private data class Entry(val index: Int, val target: String, val bytes: Long)
    private data class Manifest(
        val timestampMillis: Long,
        val packageVersion: String,
        val isolated: Boolean,
        val entries: List<Entry>
    )

    fun create(context: Context, pkg: PackageLifecycleStore.InstalledPackage): RestorePoint {
        val preview = PackageLifecycleStore.previewReset(context, pkg)
        val targets = preview.paths.map(::File).filter { it.exists() }
        require(targets.isNotEmpty()) { "No managed runtime state exists to snapshot for ${pkg.name}." }

        val parent = packageDirectory(context, pkg).apply { mkdirs() }
        val id = "${System.currentTimeMillis().toString(36)}-${UUID.randomUUID().toString().take(8)}"
        val temporary = File(parent, ".$id.tmp")
        val final = File(parent, id)
        require(temporary.mkdirs()) { "Could not create restore-point staging directory." }

        var files = 0
        var bytes = 0L
        val entries = mutableListOf<Entry>()
        try {
            val data = File(temporary, DATA).apply { mkdirs() }
            targets.forEachIndexed { index, target ->
                require(isAllowedTarget(context, pkg, target)) { "Refusing unmanaged snapshot target: ${target.absolutePath}" }
                val destination = File(data, index.toString())
                val copied = copyTree(target, destination) { size ->
                    files++
                    bytes += size
                    require(files <= MAX_FILES) { "Restore point exceeds $MAX_FILES files." }
                    require(bytes <= MAX_BYTES) { "Restore point exceeds 512 MiB." }
                }
                entries += Entry(index, target.canonicalPath, copied)
            }
            writeManifest(
                File(temporary, MANIFEST),
                Manifest(System.currentTimeMillis(), pkg.version, pkg.isolated, entries)
            )
            require(temporary.renameTo(final)) { "Could not commit restore point." }
        } catch (error: Exception) {
            deleteTree(temporary)
            throw error
        }

        prune(context, pkg)
        return readPoint(final) ?: error("Restore point was committed but could not be read.")
    }

    fun list(context: Context, pkg: PackageLifecycleStore.InstalledPackage): List<RestorePoint> =
        packageDirectory(context, pkg).listFiles().orEmpty()
            .asSequence()
            .filter { it.isDirectory && !it.name.startsWith('.') }
            .mapNotNull(::readPoint)
            .sortedByDescending { it.timestampMillis }
            .toList()

    fun restore(context: Context, pkg: PackageLifecycleStore.InstalledPackage, id: String): RestoreResult {
        require(SAFE_ID.matches(id)) { "Invalid restore-point id." }
        val point = File(packageDirectory(context, pkg), id)
        val manifest = readManifest(File(point, MANIFEST)) ?: error("Restore point '$id' is missing or corrupt.")
        require(manifest.isolated == pkg.isolated) {
            "Restore point isolation mode no longer matches ${pkg.name}; restore it in the same isolation mode."
        }

        var restoredPaths = 0
        var restoredBytes = 0L
        val failed = mutableListOf<String>()
        manifest.entries.forEach { entry ->
            val source = File(File(point, DATA), entry.index.toString())
            val target = File(entry.target)
            if (!source.exists() || !isAllowedTarget(context, pkg, target)) {
                failed += entry.target
                return@forEach
            }
            val staging = File(target.parentFile, ".${target.name}.hg2restore-${id.takeLast(8)}")
            try {
                deleteTree(staging)
                copyTree(source, staging) { }
                deleteTree(target)
                target.parentFile?.mkdirs()
                require(staging.renameTo(target)) { "Could not install restored path" }
                restoredPaths++
                restoredBytes += entry.bytes
            } catch (_: Exception) {
                deleteTree(staging)
                failed += entry.target
            }
        }
        return RestoreResult(restoredPaths, restoredBytes, failed)
    }

    fun delete(context: Context, pkg: PackageLifecycleStore.InstalledPackage, id: String): Boolean {
        if (!SAFE_ID.matches(id)) return false
        val point = File(packageDirectory(context, pkg), id)
        return !point.exists() || deleteTree(point)
    }

    private fun readPoint(directory: File): RestorePoint? {
        val manifest = readManifest(File(directory, MANIFEST)) ?: return null
        return RestorePoint(
            id = directory.name,
            timestampMillis = manifest.timestampMillis,
            packageVersion = manifest.packageVersion,
            pathCount = manifest.entries.size,
            bytes = manifest.entries.sumOf { it.bytes }
        )
    }

    private fun writeManifest(file: File, manifest: Manifest) {
        val text = buildString {
            append("timestamp\t").append(manifest.timestampMillis).append('\n')
            append("version\t").append(encode(manifest.packageVersion)).append('\n')
            append("isolated\t").append(manifest.isolated).append('\n')
            manifest.entries.forEach { entry ->
                append("entry\t")
                    .append(entry.index).append('\t')
                    .append(entry.bytes).append('\t')
                    .append(encode(entry.target)).append('\n')
            }
        }.toByteArray(Charsets.UTF_8)
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            stream.write(text)
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    }

    private fun readManifest(file: File): Manifest? = runCatching {
        var timestamp = 0L
        var version = ""
        var isolated = false
        val entries = mutableListOf<Entry>()
        file.forEachLine { line ->
            val parts = line.split('\t')
            when (parts.firstOrNull()) {
                "timestamp" -> timestamp = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                "version" -> version = parts.getOrNull(1)?.let(::decode).orEmpty()
                "isolated" -> isolated = parts.getOrNull(1).toBoolean()
                "entry" -> if (parts.size >= 4) {
                    val index = parts[1].toIntOrNull() ?: return@forEachLine
                    val bytes = parts[2].toLongOrNull() ?: 0L
                    entries += Entry(index, decode(parts[3]), bytes)
                }
            }
        }
        require(timestamp > 0L)
        Manifest(timestamp, version, isolated, entries)
    }.getOrNull()

    private fun copyTree(source: File, destination: File, onFile: (Long) -> Unit): Long {
        require(!Files.isSymbolicLink(source.toPath())) { "Symbolic links are not snapshot-safe: ${source.absolutePath}" }
        if (source.isDirectory) {
            require(destination.mkdirs() || destination.isDirectory) { "Could not create ${destination.absolutePath}" }
            var total = 0L
            source.listFiles().orEmpty().forEach { child ->
                total += copyTree(child, File(destination, child.name), onFile)
            }
            return total
        }
        destination.parentFile?.mkdirs()
        source.inputStream().use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
        val size = destination.length()
        onFile(size)
        return size
    }

    private fun deleteTree(file: File): Boolean {
        if (!file.exists() && !Files.isSymbolicLink(file.toPath())) return true
        if (Files.isSymbolicLink(file.toPath()) || file.isFile) return file.delete()
        val childrenDeleted = file.listFiles().orEmpty().all(::deleteTree)
        return childrenDeleted && file.delete()
    }

    private fun isAllowedTarget(context: Context, pkg: PackageLifecycleStore.InstalledPackage, file: File): Boolean {
        val target = runCatching { file.canonicalPath }.getOrNull() ?: return false
        if (pkg.isolated) {
            val base = PackageIsolation.root(context, pkg).parentFile?.canonicalPath ?: return false
            return target == base
        }
        val home = runCatching { DistroManager.homeDir(context).canonicalPath }.getOrNull() ?: return false
        val prefix = runCatching { DistroManager.prefixDir(context).canonicalPath }.getOrNull() ?: return false
        return target.startsWith("$home/") ||
            target.startsWith("$prefix/var/cache/") ||
            target.startsWith("$prefix/var/log/") ||
            target.startsWith("$prefix/var/tmp/")
    }

    private fun prune(context: Context, pkg: PackageLifecycleStore.InstalledPackage) {
        list(context, pkg).drop(MAX_POINTS_PER_PACKAGE).forEach { delete(context, pkg, it.id) }
    }

    private fun packageDirectory(context: Context, pkg: PackageLifecycleStore.InstalledPackage): File =
        File(File(context.filesDir, DIRECTORY), safeKey(pkg.key))

    private fun encode(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    private fun decode(value: String): String = String(Base64.decode(value, Base64.DEFAULT), Charsets.UTF_8)
    private fun safeKey(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private val SAFE_ID = Regex("[a-z0-9-]{4,64}")
}
