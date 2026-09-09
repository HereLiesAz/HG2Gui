package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption

/**
 * Per-package rollback snapshot for dpkg mutations.
 *
 * The journal snapshots dpkg's status paragraph store, package-specific info files and the payload
 * paths recorded in <package>.list before HG2Gui mutates them. On failure it removes the failed
 * package's newly-recorded payload, restores the previous payload/symlinks, then restores dpkg
 * metadata. Maintainer scripts can deliberately touch arbitrary external state; those side effects
 * cannot be made transactional without syscall-level interception and are therefore not claimed as
 * part of this rollback boundary.
 */
class PackageTransactionJournal private constructor(
    private val prefix: File,
    private val packageName: String,
    private val transactionDir: File,
    private val statusFile: File,
    private val dpkgInfoDir: File,
    private val oldEntries: List<Entry>
) {
    private data class Entry(val type: Char, val relative: String, val linkTarget: String? = null)

    private var finished = false

    fun commit() {
        if (finished) return
        finished = true
        transactionDir.deleteRecursively()
    }

    fun rollback(): Result<Unit> = runCatching {
        if (finished) return@runCatching
        removeCurrentPayload()
        restorePayload()
        restoreDpkgInfo()
        restoreStatus()
        finished = true
        transactionDir.deleteRecursively()
    }

    private fun removeCurrentPayload() {
        val currentList = File(dpkgInfoDir, "$packageName.list")
        val current = readPackagePaths(currentList, prefix)
        val directories = mutableListOf<File>()
        current.forEach { path ->
            when {
                Files.isSymbolicLink(path.toPath()) -> Files.deleteIfExists(path.toPath())
                path.isFile -> Files.deleteIfExists(path.toPath())
                path.isDirectory -> directories += path
            }
        }
        directories.sortedByDescending { it.absolutePath.length }.forEach { dir ->
            runCatching { if (dir.listFiles().isNullOrEmpty()) Files.deleteIfExists(dir.toPath()) }
        }
    }

    private fun restorePayload() {
        val payloadRoot = File(transactionDir, "payload")
        oldEntries.filter { it.type == 'D' }.sortedBy { it.relative.length }.forEach { entry ->
            File(prefix, entry.relative).mkdirs()
        }
        oldEntries.filter { it.type == 'F' }.forEach { entry ->
            val source = File(payloadRoot, entry.relative)
            val destination = File(prefix, entry.relative)
            destination.parentFile?.mkdirs()
            Files.copy(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            )
        }
        oldEntries.filter { it.type == 'L' }.forEach { entry ->
            val destination = File(prefix, entry.relative).toPath()
            destination.parent?.let(Files::createDirectories)
            Files.deleteIfExists(destination)
            Files.createSymbolicLink(destination, java.nio.file.Paths.get(entry.linkTarget.orEmpty()))
        }
    }

    private fun restoreDpkgInfo() {
        dpkgInfoDir.mkdirs()
        dpkgInfoDir.listFiles().orEmpty()
            .filter { it.name == packageName || it.name.startsWith("$packageName.") }
            .forEach { file ->
                if (file.isDirectory) file.deleteRecursively() else Files.deleteIfExists(file.toPath())
            }
        val backup = File(transactionDir, "info")
        backup.listFiles().orEmpty().forEach { file ->
            Files.copy(
                file.toPath(),
                File(dpkgInfoDir, file.name).toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            )
        }
    }

    private fun restoreStatus() {
        val backup = File(transactionDir, "status")
        if (!backup.isFile) return
        statusFile.parentFile?.mkdirs()
        val temp = File(statusFile.parentFile, "status.hg2-rollback")
        Files.copy(backup.toPath(), temp.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
        runCatching {
            Files.move(temp.toPath(), statusFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(temp.toPath(), statusFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        fun begin(context: Context, packageName: String): PackageTransactionJournal {
            val prefix = DistroManager.prefixDir(context)
            val dpkgDir = File(prefix, "var/lib/dpkg")
            val infoDir = File(dpkgDir, "info")
            val status = File(dpkgDir, "status")
            val safe = packageName.replace(Regex("[^A-Za-z0-9._+-]"), "_")
            val transaction = File(context.cacheDir, "hg2-package-transactions/$safe-${System.nanoTime()}")
            val payload = File(transaction, "payload")
            val infoBackup = File(transaction, "info")
            transaction.mkdirs()
            payload.mkdirs()
            infoBackup.mkdirs()

            try {
                File(transaction, "package-name").writeText(packageName)
                if (status.isFile) {
                    Files.copy(status.toPath(), File(transaction, "status").toPath(), StandardCopyOption.COPY_ATTRIBUTES)
                }
                infoDir.listFiles().orEmpty()
                    .filter { it.isFile && (it.name == packageName || it.name.startsWith("$packageName.")) }
                    .forEach { file ->
                        Files.copy(file.toPath(), File(infoBackup, file.name).toPath(), StandardCopyOption.COPY_ATTRIBUTES)
                    }

                val entries = snapshotPayload(prefix, File(infoDir, "$packageName.list"), payload)
                writeManifest(File(transaction, "manifest"), entries)
                return PackageTransactionJournal(prefix, packageName, transaction, status, infoDir, entries)
            } catch (t: Throwable) {
                transaction.deleteRecursively()
                throw IllegalStateException("Could not create rollback snapshot for $packageName: ${t.message}", t)
            }
        }

        /**
         * A successful transaction deletes its journal. Any journal surviving process death is
         * therefore incomplete and is rolled back before another package mutation begins.
         */
        fun recoverAbandoned(context: Context): List<String> {
            val prefix = DistroManager.prefixDir(context)
            val dpkgDir = File(prefix, "var/lib/dpkg")
            val infoDir = File(dpkgDir, "info")
            val status = File(dpkgDir, "status")
            return File(context.cacheDir, "hg2-package-transactions")
                .listFiles().orEmpty()
                .filter(File::isDirectory)
                .map { transaction ->
                    val packageName = runCatching { File(transaction, "package-name").readText().trim() }.getOrDefault("")
                    if (packageName.isBlank()) {
                        transaction.deleteRecursively()
                        return@map "Discarded unreadable package transaction ${transaction.name}."
                    }
                    val entries = readManifest(File(transaction, "manifest"))
                    val journal = PackageTransactionJournal(prefix, packageName, transaction, status, infoDir, entries)
                    journal.rollback().fold(
                        onSuccess = { "Recovered interrupted package transaction for $packageName." },
                        onFailure = { "Could not recover interrupted package transaction for $packageName: ${it.message}" }
                    )
                }
        }

        private fun snapshotPayload(prefix: File, listFile: File, payloadRoot: File): List<Entry> {
            val entries = mutableListOf<Entry>()
            readPackagePaths(listFile, prefix).forEach { source ->
                val relative = prefix.toPath().normalize().relativize(source.toPath().normalize()).toString()
                when {
                    Files.isSymbolicLink(source.toPath()) -> {
                        val target = Files.readSymbolicLink(source.toPath()).toString()
                        entries += Entry('L', relative, target)
                    }
                    source.isDirectory -> entries += Entry('D', relative)
                    source.isFile -> {
                        val destination = File(payloadRoot, relative)
                        destination.parentFile?.mkdirs()
                        Files.copy(
                            source.toPath(),
                            destination.toPath(),
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES,
                            LinkOption.NOFOLLOW_LINKS
                        )
                        entries += Entry('F', relative)
                    }
                }
            }
            return entries
        }

        private fun readPackagePaths(listFile: File, prefix: File): List<File> {
            if (!listFile.isFile) return emptyList()
            val prefixPath = prefix.toPath().toAbsolutePath().normalize()
            return listFile.readLines().mapNotNull { raw ->
                val text = raw.trim()
                if (text.isBlank()) return@mapNotNull null
                val mapped = when {
                    text.startsWith(prefix.absolutePath) -> File(text)
                    text.startsWith(OLD_PREFIX) -> File(prefix, text.removePrefix(OLD_PREFIX).trimStart('/'))
                    text.startsWith('/') -> File(text)
                    else -> File(prefix, text)
                }
                val normalized = mapped.toPath().toAbsolutePath().normalize()
                if (normalized == prefixPath || normalized.startsWith(prefixPath)) normalized.toFile() else null
            }.distinctBy { it.path }
        }

        private fun writeManifest(file: File, entries: List<Entry>) {
            file.parentFile?.mkdirs()
            file.bufferedWriter().use { writer ->
                entries.forEach { entry ->
                    writer.append(entry.type).append('\t').append(entry.relative.replace("\t", ""))
                    entry.linkTarget?.let { writer.append('\t').append(it.replace("\t", "")) }
                    writer.appendLine()
                }
            }
        }

        private fun readManifest(file: File): List<Entry> {
            if (!file.isFile) return emptyList()
            return file.readLines().mapNotNull { line ->
                val parts = line.split('\t', limit = 3)
                val type = parts.getOrNull(0)?.singleOrNull() ?: return@mapNotNull null
                val relative = parts.getOrNull(1)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                Entry(type, relative, parts.getOrNull(2))
            }
        }

        private const val OLD_PREFIX = "/data/data/com.termux/files/usr"
    }
}
