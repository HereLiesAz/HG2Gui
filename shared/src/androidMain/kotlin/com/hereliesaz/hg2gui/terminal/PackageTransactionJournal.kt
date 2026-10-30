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
 *
 * Durability: journals live under `noBackupFilesDir`, not `cacheDir`. The OS (low storage) and the
 * user ("Clear cache") may delete cacheDir at any moment - including between a crash and the next
 * recovery, which would leave dpkg half-mutated with nothing to roll back to. Journals written by
 * older builds under cacheDir are migrated on the next recovery.
 *
 * Layout: each logical package-manager transaction is a GROUP directory holding one numbered
 * journal directory per package. A group is committed by ONE atomic directory rename
 * (`<group>` -> `.committed-<group>`); the old per-journal delete loop could be interrupted
 * between deletes, after which recovery would roll back the surviving journals of a plan that had
 * in fact completed. Recovery ignores and deletes `.committed-*` leftovers.
 */
class PackageTransactionJournal private constructor(
    private val env: Env,
    private val prefix: File,
    private val packageName: String,
    private val transactionDir: File,
    private val statusFile: File,
    private val dpkgInfoDir: File,
    private val oldEntries: List<Entry>,
    private val mechanismEntries: List<Entry>,
    /** Non-null only for a standalone journal that owns its single-journal group directory. */
    private val ownedGroupDir: File? = null
) {
    /**
     * Everything environment-specific, so the journal logic runs against plain temp directories in
     * host tests. Production code builds one with [forContext].
     */
    internal class Env(
        val prefix: File,
        val journalRoot: File,
        /** Directories earlier builds wrote journals to (cacheDir); migrated, then scanned as a fallback. */
        val legacyRoots: List<File> = emptyList(),
        val pid: Int,
        val isProcessAlive: (Int) -> Boolean = { File("/proc/$it").exists() },
        val usableSpace: (File) -> Long = { it.usableSpace },
        val processToken: String = PROCESS_TOKEN
    ) {
        companion object {
            fun forContext(context: Context): Env = Env(
                prefix = DistroManager.prefixDir(context),
                journalRoot = File(context.noBackupFilesDir, JOURNAL_DIR),
                legacyRoots = listOf(File(context.cacheDir, JOURNAL_DIR)),
                pid = android.os.Process.myPid()
            )
        }
    }

    private data class Entry(val type: Char, val relative: String, val linkTarget: String? = null)

    private var finished = false

    /**
     * Discards this journal. For a journal created through [begin] on its own this is the atomic
     * commit; journals inside a [PackageTransactionGroup] are committed by the group, which calls
     * [markCommitted] after its single rename.
     */
    fun commit() {
        if (finished) return
        val group = ownedGroupDir
        if (group != null) commitDirectory(group) else FileTrees.deleteRecursivelyNoFollow(transactionDir)
        finished = true
    }

    internal fun markCommitted() {
        finished = true
    }

    fun rollback(): Result<Unit> = runCatching {
        if (finished) return@runCatching
        removeCurrentPayload()
        restorePayload()
        restoreDpkgInfo()
        restoreMechanismState()
        restoreStatus()
        finished = true
        FileTrees.deleteRecursivelyNoFollow(transactionDir)
        ownedGroupDir?.let { FileTrees.deleteRecursivelyNoFollow(it) }
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
        internal const val JOURNAL_DIR = "hg2-package-transactions"
        private const val COMMITTED_PREFIX = ".committed-"
        private const val PACKAGE_NAME_FILE = "package-name"

        /** Slack kept free on top of the estimated snapshot size so a snapshot cannot fill the disk. */
        private const val FREE_SPACE_RESERVE_BYTES = 16L * 1024L * 1024L

        /** Distinguishes this process from an earlier one that happened to get the same PID. */
        private val PROCESS_TOKEN: String = java.util.UUID.randomUUID().toString()
        private const val OWNER_FILE = "owner"

        /**
         * Starts a standalone single-package journal. Prefer [PackageTransactionGroup] for a
         * multi-package plan so the whole plan commits atomically.
         */
        fun begin(context: Context, packageName: String): PackageTransactionJournal =
            begin(Env.forContext(context), packageName, groupDir = null, index = 1)

        /** Creates a new, empty, owner-stamped group directory under the journal root. */
        internal fun newGroupDir(env: Env): File {
            env.journalRoot.mkdirs()
            val dir = File(env.journalRoot, "g-${System.currentTimeMillis()}-${System.nanoTime()}")
            check(dir.mkdirs()) { "Could not create transaction directory ${dir.path}" }
            File(dir, "created-at").writeText(System.currentTimeMillis().toString())
            File(dir, OWNER_FILE).writeText("${env.pid} ${env.processToken}")
            return dir
        }

        /**
         * Snapshots [packageName] into a numbered journal inside [groupDir] (a fresh standalone
         * group when null). Fails - leaving nothing behind - if the volume cannot hold the
         * snapshot.
         */
        internal fun begin(env: Env, packageName: String, groupDir: File?, index: Int): PackageTransactionJournal {
            val prefix = env.prefix
            val dpkgDir = File(prefix, "var/lib/dpkg")
            val infoDir = File(dpkgDir, "info")
            val status = File(dpkgDir, "status")
            val safe = packageName.replace(Regex("[^A-Za-z0-9._+-]"), "_")
            val standalone = groupDir == null
            val group = groupDir ?: newGroupDir(env)
            val transaction = File(group, "%04d-%s".format(java.util.Locale.US, index, safe))
            val payload = File(transaction, "payload")
            val mechanisms = File(transaction, "mechanisms")
            val infoBackup = File(transaction, "info")
            transaction.mkdirs()
            payload.mkdirs()
            mechanisms.mkdirs()
            infoBackup.mkdirs()

            try {
                val listFile = File(infoDir, "$packageName.list")
                requireFreeSpace(env, packageName, estimateSnapshotBytes(prefix, packageName, listFile, status, infoDir))
                File(transaction, PACKAGE_NAME_FILE).writeText(packageName)
                File(transaction, "created-at").writeText(System.currentTimeMillis().toString())
                File(transaction, OWNER_FILE).writeText("${env.pid} ${env.processToken}")
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

                val entries = snapshotPayload(prefix, listFile, payload)
                val mechanismEntries = snapshotMechanismState(prefix, mechanisms)
                writeManifest(File(transaction, "manifest"), entries)
                writeManifest(File(transaction, "mechanisms.manifest"), mechanismEntries)
                return PackageTransactionJournal(
                    env,
                    prefix,
                    packageName,
                    transaction,
                    status,
                    infoDir,
                    entries,
                    mechanismEntries,
                    ownedGroupDir = if (standalone) group else null
                )
            } catch (t: Throwable) {
                FileTrees.deleteRecursivelyNoFollow(transaction)
                if (standalone) FileTrees.deleteRecursivelyNoFollow(group)
                throw IllegalStateException("Could not create rollback snapshot for $packageName: ${t.message}", t)
            }
        }

        /**
         * Bytes a snapshot of [packageName] will occupy: its payload files, dpkg's status file,
         * its info files and the shared mechanism state. Symlinks and directories cost nothing.
         */
        internal fun estimateSnapshotBytes(
            prefix: File,
            packageName: String,
            listFile: File,
            status: File,
            infoDir: File
        ): Long {
            var total = if (status.isFile) status.length() else 0L
            total += infoDir.listFiles().orEmpty()
                .filter { (it.name == packageName || it.name.startsWith("$packageName.")) && Files.isRegularFile(it.toPath(), LinkOption.NOFOLLOW_LINKS) }
                .sumOf { it.length() }
            readPackagePaths(listFile, prefix).forEach { path ->
                if (Files.isRegularFile(path.toPath(), LinkOption.NOFOLLOW_LINKS)) total += path.length()
            }
            MANAGED_DPKG_STATE.forEach { relative -> total += FileTrees.sizeNoFollow(File(prefix, relative)) }
            return total
        }

        private fun requireFreeSpace(env: Env, packageName: String, estimatedBytes: Long) {
            val needed = estimatedBytes + estimatedBytes / 10 + FREE_SPACE_RESERVE_BYTES
            val available = env.usableSpace(env.journalRoot)
            check(available >= needed) {
                "not enough free storage to snapshot $packageName for rollback " +
                    "(need about ${needed / (1024 * 1024)} MiB, ${available / (1024 * 1024)} MiB available). " +
                    "Free some space and retry; nothing was changed."
            }
        }

        /**
         * Commits a whole plan with ONE atomic directory rename. After the rename the group is no
         * longer an active transaction, so a crash during the (best-effort) cleanup that follows
         * cannot make recovery roll back a plan that completed. Throws if the rename fails, in
         * which case the group is still intact and the caller should roll the plan back.
         */
        internal fun commitDirectory(groupDir: File) {
            val committed = File(groupDir.parentFile, COMMITTED_PREFIX + groupDir.name)
            Files.move(groupDir.toPath(), committed.toPath(), StandardCopyOption.ATOMIC_MOVE)
            FileTrees.deleteRecursivelyNoFollow(committed)
        }

        /**
         * A successful transaction removes its journal. Any journal surviving process death is
         * therefore incomplete. Newer snapshots must be replayed first so the full dpkg status
         * and shared mechanism state walk backward to the original pre-plan state.
         *
         * Callers hold the package-operation lock, so a journal owned by this process is never
         * in flight here. A journal whose owner marker names a different, still-running process
         * is left untouched and reported as a recovery failure, which blocks the new mutation.
         */
        fun recoverAbandoned(context: Context): List<String> {
            return recoverAbandoned(Env.forContext(context))
        }

        internal fun recoverAbandoned(env: Env): List<String> {
            migrateLegacyJournals(env)
            val dpkgDir = File(env.prefix, "var/lib/dpkg")
            val infoDir = File(dpkgDir, "info")
            val status = File(dpkgDir, "status")
            val messages = mutableListOf<String>()

            val roots = (listOf(env.journalRoot) + env.legacyRoots).filter { it.isDirectory }
            val units = roots.flatMap { root ->
                root.listFiles().orEmpty().filter(File::isDirectory)
            }.filter { dir ->
                if (dir.name.startsWith(COMMITTED_PREFIX)) {
                    // A group whose atomic commit rename succeeded; only cleanup was interrupted.
                    FileTrees.deleteRecursivelyNoFollow(dir)
                    false
                } else {
                    true
                }
            }.sortedByDescending(::transactionCreatedAt)

            for (unit in units) {
                val isFlatJournal = File(unit, PACKAGE_NAME_FILE).isFile
                val foreignPid = if (isFlatJournal) null else liveForeignOwner(env, unit)
                if (foreignPid != null) {
                    messages += "Could not recover interrupted package transaction ${unit.name}: still owned by running process $foreignPid."
                    continue
                }
                val journalDirs = if (isFlatJournal) {
                    listOf(unit)
                } else {
                    unit.listFiles().orEmpty().filter(File::isDirectory).sortedByDescending { it.name }
                }
                var allRecovered = true
                for (transaction in journalDirs) {
                    val message = recoverJournal(env, transaction, status, infoDir)
                    if (message.startsWith("Could not recover")) allRecovered = false
                    messages += message
                }
                if (!isFlatJournal && allRecovered) FileTrees.deleteRecursivelyNoFollow(unit)
            }
            return messages
        }

        private fun recoverJournal(env: Env, transaction: File, status: File, infoDir: File): String {
            val packageName = runCatching { File(transaction, PACKAGE_NAME_FILE).readText().trim() }.getOrDefault("")
            if (packageName.isBlank()) {
                FileTrees.deleteRecursivelyNoFollow(transaction)
                return "Discarded unreadable package transaction ${transaction.name}."
            }
            liveForeignOwner(env, transaction)?.let { pid ->
                return "Could not recover interrupted package transaction for $packageName: still owned by running process $pid."
            }
            val entries = readManifest(File(transaction, "manifest"))
            val mechanismEntries = readManifest(File(transaction, "mechanisms.manifest"))
            val journal = PackageTransactionJournal(
                env,
                env.prefix,
                packageName,
                transaction,
                status,
                infoDir,
                entries,
                mechanismEntries
            )
            return journal.rollback().fold(
                onSuccess = { "Recovered interrupted package transaction for $packageName." },
                onFailure = { "Could not recover interrupted package transaction for $packageName: ${it.message}" }
            )
        }

        /**
         * Moves journals written by older builds (under cacheDir) into the durable journal root so
         * a later cache clear cannot delete them. Anything that cannot be moved is still scanned
         * in place by [recoverAbandoned].
         */
        internal fun migrateLegacyJournals(env: Env) {
            for (legacy in env.legacyRoots) {
                if (!legacy.isDirectory) continue
                env.journalRoot.mkdirs()
                for (child in legacy.listFiles().orEmpty().filter(File::isDirectory)) {
                    var target = File(env.journalRoot, child.name)
                    if (target.exists()) target = File(env.journalRoot, "${child.name}-migrated-${System.nanoTime()}")
                    runCatching { Files.move(child.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE) }
                }
                legacy.delete() // only succeeds when empty
            }
        }

        /**
         * Deletes every journal. Only for when the whole prefix has just been replaced (a
         * bootstrap): the journals describe state that no longer exists and replaying them over
         * the new prefix would corrupt it.
         */
        fun discardAll(context: Context) {
            discardAll(Env.forContext(context))
        }

        internal fun discardAll(env: Env): Unit {
            (listOf(env.journalRoot) + env.legacyRoots).filter { it.isDirectory }.forEach { root ->
                root.listFiles().orEmpty().forEach { FileTrees.deleteRecursivelyNoFollow(it) }
            }
        }

        /** The owner PID when the journal belongs to another process that is still alive. */
        private fun liveForeignOwner(env: Env, transaction: File): Int? {
            val owner = runCatching { File(transaction, OWNER_FILE).readText().trim() }.getOrNull() ?: return null
            val pid = owner.substringBefore(' ').toIntOrNull() ?: return null
            val token = owner.substringAfter(' ', "")
            if (token == env.processToken) return null
            if (pid == env.pid) return null // PID reused by this new process
            return pid.takeIf { env.isProcessAlive(it) }
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
