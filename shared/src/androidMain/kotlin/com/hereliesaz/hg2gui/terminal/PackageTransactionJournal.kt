package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Comparator

/**
 * Per-package rollback snapshot for dpkg mutations.
 *
 * The journal snapshots dpkg's status paragraph store, package-specific info files, package-owned
 * payload paths, and dpkg's shared trigger/alternatives/diversion state before HG2Gui mutates a
 * package. On failure all of that state is restored. Maintainer scripts can still deliberately
 * touch arbitrary files outside package/dpkg-managed state; those side effects require syscall
 * interception and are deliberately outside this rollback boundary.
 */
class PackageTransactionJournal private constructor(
    private val prefix: File,
    private val packageName: String,
    private val transactionDir: File,
    private val statusFile: File,
    private val dpkgInfoDir: File,
    private val oldEntries: List<Entry>,
    private val mechanismEntries: List<Entry>
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
        restoreMechanismState()
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
        restoreEntries(oldEntries, File(transactionDir, "payload"))
    }

    private fun restoreDpkgInfo() {
        dpkgInfoDir.mkdirs()
        dpkgInfoDir.listFiles().orEmpty()
            .filter { it.name == packageName || it.name.startsWith("$packageName.") }
            .forEach { file -> deleteNoFollow(file.toPath()) }
        val backup = File(transactionDir, "info")
        backup.listFiles().orEmpty().forEach { file ->
            Files.copy(
                file.toPath(),
                File(dpkgInfoDir, file.name).toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES,
                LinkOption.NOFOLLOW_LINKS
            )
        }
    }

    private fun restoreMechanismState() {
        MANAGED_DPKG_STATE.forEach { relative -> deleteNoFollow(File(prefix, relative).toPath()) }
        restoreEntries(mechanismEntries, File(transactionDir, "mechanisms"))
    }

    private fun restoreEntries(entries: List<Entry>, backupRoot: File) {
        entries.filter { it.type == 'D' }.sortedBy { it.relative.length }.forEach { entry ->
            File(prefix, entry.relative).mkdirs()
        }
        entries.filter { it.type == 'F' }.forEach { entry ->
            val source = File(backupRoot, entry.relative)
            val destination = File(prefix, entry.relative)
            destination.parentFile?.mkdirs()
            Files.copy(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            )
        }
        entries.filter { it.type == 'L' }.forEach { entry ->
            val destination = File(prefix, entry.relative).toPath()
            destination.parent?.let(Files::createDirectories)
            Files.deleteIfExists(destination)
            Files.createSymbolicLink(destination, java.nio.file.Paths.get(entry.linkTarget.orEmpty()))
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
            val mechanisms = File(transaction, "mechanisms")
            val infoBackup = File(transaction, "info")
            transaction.mkdirs()
            payload.mkdirs()
            mechanisms.mkdirs()
            infoBackup.mkdirs()

            try {
                File(transaction, "package-name").writeText(packageName)
                File(transaction, "created-at").writeText(System.currentTimeMillis().toString())
                if (status.isFile) {
                    Files.copy(status.toPath(), File(transaction, "status").toPath(), StandardCopyOption.COPY_ATTRIBUTES)
                }
                infoDir.listFiles().orEmpty()
                    .filter {
                        (it.name == packageName || it.name.startsWith("$packageName.")) &&
                            (Files.isRegularFile(it.toPath(), LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(it.toPath()))
                    }
                    .forEach { file ->
                        Files.copy(
                            file.toPath(),
                            File(infoBackup, file.name).toPath(),
                            StandardCopyOption.COPY_ATTRIBUTES,
                            LinkOption.NOFOLLOW_LINKS
                        )
                    }

                val entries = snapshotPayload(prefix, File(infoDir, "$packageName.list"), payload)
                val mechanismEntries = snapshotMechanismState(prefix, mechanisms)
                writeManifest(File(transaction, "manifest"), entries)
                writeManifest(File(transaction, "mechanisms.manifest"), mechanismEntries)
                return PackageTransactionJournal(
                    prefix,
                    packageName,
                    transaction,
                    status,
                    infoDir,
                    entries,
                    mechanismEntries
                )
            } catch (t: Throwable) {
                transaction.deleteRecursively()
                throw IllegalStateException("Could not create rollback snapshot for $packageName: ${t.message}", t)
            }
        }

        /**
         * A successful transaction deletes its journal. Any journal surviving process death is
         * therefore incomplete. Newer snapshots must be replayed first so the full dpkg status
         * and shared mechanism state walk backward to the original pre-plan state.
         */
        fun recoverAbandoned(context: Context): List<String> {
            val prefix = DistroManager.prefixDir(context)
            val dpkgDir = File(prefix, "var/lib/dpkg")
            val infoDir = File(dpkgDir, "info")
            val status = File(dpkgDir, "status")
            return File(context.cacheDir, "hg2-package-transactions")
                .listFiles().orEmpty()
                .filter(File::isDirectory)
                .sortedByDescending(::transactionCreatedAt)
                .map { transaction ->
                    val packageName = runCatching { File(transaction, "package-name").readText().trim() }.getOrDefault("")
                    if (packageName.isBlank()) {
                        transaction.deleteRecursively()
                        return@map "Discarded unreadable package transaction ${transaction.name}."
                    }
                    val entries = readManifest(File(transaction, "manifest"))
                    val mechanismEntries = readManifest(File(transaction, "mechanisms.manifest"))
                    val journal = PackageTransactionJournal(
                        prefix,
                        packageName,
                        transaction,
                        status,
                        infoDir,
                        entries,
                        mechanismEntries
                    )
                    journal.rollback().fold(
                        onSuccess = { "Recovered interrupted package transaction for $packageName." },
                        onFailure = { "Could not recover interrupted package transaction for $packageName: ${it.message}" }
                    )
                }
        }

        private fun transactionCreatedAt(transaction: File): Long =
            runCatching { File(transaction, "created-at").readText().trim().toLong() }
                .getOrElse { transaction.lastModified() }

        private fun snapshotPayload(prefix: File, listFile: File, payloadRoot: File): List<Entry> {
            val entries = mutableListOf<Entry>()
            readPackagePaths(listFile, prefix).forEach { source ->
                snapshotPath(prefix, source.toPath(), payloadRoot, entries)
            }
            return entries
        }

        private fun snapshotMechanismState(prefix: File, backupRoot: File): List<Entry> {
            val entries = mutableListOf<Entry>()
            MANAGED_DPKG_STATE.forEach { relative ->
                val root = File(prefix, relative).toPath()
                if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return@forEach
                if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root)) {
                    snapshotPath(prefix, root, backupRoot, entries)
                } else {
                    Files.walk(root).use { stream ->
                        stream.forEach { path -> snapshotPath(prefix, path, backupRoot, entries) }
                    }
                }
            }
            return entries.distinctBy { it.type to it.relative }
        }

        private fun snapshotPath(prefix: File, path: Path, backupRoot: File, entries: MutableList<Entry>) {
            val prefixPath = prefix.toPath().toAbsolutePath().normalize()
            val normalized = path.toAbsolutePath().normalize()
            if (!normalized.startsWith(prefixPath)) return
            val relative = prefixPath.relativize(normalized).toString()
            if (relative.isBlank()) return
            when {
                Files.isSymbolicLink(path) -> {
                    entries += Entry('L', relative, Files.readSymbolicLink(path).toString())
                }
                Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) -> entries += Entry('D', relative)
                Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) -> {
                    val destination = File(backupRoot, relative)
                    destination.parentFile?.mkdirs()
                    Files.copy(
                        path,
                        destination.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES,
                        LinkOption.NOFOLLOW_LINKS
                    )
                    entries += Entry('F', relative)
                }
            }
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

        private fun deleteNoFollow(path: Path) {
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return
            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path)) {
                Files.walk(path).use { stream ->
                    stream.sorted(Comparator.reverseOrder()).forEach { entry -> Files.deleteIfExists(entry) }
                }
            } else {
                Files.deleteIfExists(path)
            }
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

        private val MANAGED_DPKG_STATE = listOf(
            "var/lib/dpkg/diversions",
            "var/lib/dpkg/diversions-old",
            "var/lib/dpkg/alternatives",
            "etc/alternatives",
            "var/lib/dpkg/triggers"
        )
        private const val OLD_PREFIX = "/data/data/com.termux/files/usr"
    }
}
