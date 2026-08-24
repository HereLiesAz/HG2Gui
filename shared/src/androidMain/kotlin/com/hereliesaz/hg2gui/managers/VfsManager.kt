package com.hereliesaz.hg2gui.managers

import android.content.Context
import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A sandboxed filesystem rooted at the app's private storage, so `mkdir`/`touch`/editing
 * never touches real Android storage unless explicitly mounted (see `vfs mount`, which
 * requires root). Confined by construction: every path resolves through [resolve], which
 * refuses anything that canonicalizes outside the root.
 *
 * Global and shared across the whole app rather than per-session — this is the same "one
 * Files app" model a phone's real file manager uses, not a per-terminal-tab concept.
 */
object VfsManager {
    // `root` and `relativePath` are global/shared across every terminal tab (see the class doc
    // above), each of which runs on its own Dispatchers.IO coroutine. A `vfs cd` in one tab
    // writes `relativePath`; a `pwd`/`ls`/resolve() in another tab, on another thread, reads it.
    // Without @Volatile there is no cross-thread visibility guarantee for a plain var, so a
    // reader on another core could observe a stale value even after the write has genuinely
    // completed. Both fields hold plain immutable references (File, String), so a single
    // @Volatile fully covers this - there's no in-place mutation hazard to guard against here.
    @Volatile
    private var root: File? = null
    @Volatile
    private var relativePath: String = ""

    fun init(context: Context): File {
        val existing = root
        if (existing != null) return existing
        val created = File(context.filesDir, "vfs").apply { mkdirs() }
        root = created
        return created
    }

    /** The directory currently open in the explorer / used by relative `vfs` commands. */
    fun currentDir(context: Context): File {
        val r = init(context)
        val dir = File(r, relativePath)
        return if (dir.isDirectory) dir else r
    }

    fun currentPath(): String = if (relativePath.isEmpty()) "/" else "/$relativePath"

    /** [file]'s own path expressed relative to the sandbox root, e.g. "/Downloads/photo.png" -
     *  the stable, platform-agnostic identifier the file-manager UI (commonMain, no `File`
     *  access) keys everything off. */
    fun pathOf(context: Context, file: File): String {
        val r = init(context)
        val relative = file.canonicalPath.removePrefix(r.canonicalPath).trim(File.separatorChar)
        return if (relative.isEmpty()) "/" else "/$relative"
    }

    /** Resolves [path] against the root (absolute) or the current directory (relative). Returns
     *  null if it would escape the sandbox root. */
    fun resolve(context: Context, path: String): File? {
        val r = init(context)
        val base = if (path.startsWith("/")) r else currentDir(context)
        val candidate = File(base, path.removePrefix("/")).canonicalFile
        val rootCanonical = r.canonicalFile
        return if (candidate == rootCanonical || candidate.path.startsWith(rootCanonical.path + File.separator)) {
            candidate
        } else {
            null
        }
    }

    // TRASH-2: this used to be private logic inlined into list() below, reachable only through
    // currentDir(context) - the shell's own cwd concept. The GUI browser's nested-accordion view
    // lists whatever File it already has open at each level, which is a different directory from
    // the shell cwd almost all the time, so it never went through this filter at all: it called
    // dir.listFiles() directly (TerminalActivity.kt's vfsListDir), and .trash - along with
    // whatever a person moved into it while Show Hidden was on - was fully browsable, movable,
    // and deletable like any other folder. Exposed here so every listing surface, not just the
    // shell-cwd one, can share the one true filtered view instead of each reimplementing it (and
    // silently omitting the filter, the way vfsListDir did).
    fun listChildren(dir: File): List<File> =
        dir.listFiles()
            ?.filterNot { it.name == TRASH_DIR_NAME }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()

    fun list(context: Context): List<File> = listChildren(currentDir(context))

    fun cd(context: Context, name: String): Boolean {
        val r = init(context)
        val target = if (name == "..") currentDir(context).parentFile else resolve(context, name)
        if (target == null || !target.isDirectory) return false
        val targetCanonical = target.canonicalFile
        val rootCanonical = r.canonicalFile
        if (targetCanonical != rootCanonical && !targetCanonical.path.startsWith(rootCanonical.path + File.separator)) {
            return false
        }
        relativePath = targetCanonical.path.removePrefix(rootCanonical.path).trim(File.separatorChar)
        return true
    }

    fun cdInto(context: Context, absoluteDir: File): Boolean {
        val r = init(context)
        val rootCanonical = r.canonicalFile
        val targetCanonical = absoluteDir.canonicalFile
        if (!targetCanonical.isDirectory) return false
        if (targetCanonical != rootCanonical && !targetCanonical.path.startsWith(rootCanonical.path + File.separator)) {
            return false
        }
        relativePath = targetCanonical.path.removePrefix(rootCanonical.path).trim(File.separatorChar)
        return true
    }

    fun mkdir(context: Context, name: String): Boolean = resolve(context, name)?.mkdirs() ?: false

    fun touch(context: Context, name: String): Boolean {
        val f = resolve(context, name) ?: return false
        return f.exists() || f.createNewFile()
    }

    // MCP-11: the MCP vfs.read tool routes straight through this with no cap of its own - a
    // paired agent (or the in-app editor) asking to read a multi-gigabyte file would otherwise
    // materialize the whole thing as one String in memory before anything gets a chance to reject
    // it.
    private const val MAX_READ_TEXT_BYTES = 16L * 1024 * 1024

    fun readText(context: Context, name: String): String? =
        resolve(context, name)
            ?.takeIf { it.isFile && it.length() <= MAX_READ_TEXT_BYTES }
            ?.readText()

    fun writeText(context: Context, name: String, text: String): Boolean {
        val f = resolve(context, name) ?: return false
        return try {
            f.writeText(text)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun delete(context: Context, name: String): Boolean = resolve(context, name)?.deleteRecursively() ?: false

    fun move(context: Context, from: String, to: String): Boolean {
        val f = resolve(context, from) ?: return false
        val t = resolve(context, to) ?: return false
        return f.renameTo(t)
    }

    fun copy(context: Context, from: String, to: String): Boolean {
        val f = resolve(context, from) ?: return false
        val t = resolve(context, to) ?: return false
        return try {
            if (f.isDirectory) f.copyRecursively(t, overwrite = true) else { f.copyTo(t, overwrite = true); true }
        } catch (e: Exception) {
            false
        }
    }

    /** True if [file] is a real path inside the sandbox root - the safety check every File-based
     *  operation below needs, since these bypass [resolve]'s own name-based containment check. */
    private fun contains(file: File): Boolean {
        val r = root ?: return false
        val rootCanonical = r.canonicalFile
        val fileCanonical = file.canonicalFile
        return fileCanonical == rootCanonical || fileCanonical.path.startsWith(rootCanonical.path + File.separator)
    }

    // --- File-based operations -------------------------------------------------------------
    // The nested-accordion browser holds real File references for whatever is expanded at each
    // level, independent of any single "current directory" - so these work directly against a
    // File rather than resolving a name relative to it.

    // mkdir/touch/rename all join a user-typed name onto an already-checked File - checking only
    // that File (the parent, or the file being renamed) and not the *joined result* is exactly
    // how a name like "../../evil" walks back out of the sandbox despite the parent itself being
    // legitimate: contains() has to run again against the actual candidate target, the same way
    // resolve() already checks the joined path for the string-based API above.

    fun mkdir(dir: File, name: String): Boolean {
        if (!contains(dir)) return false
        val target = File(dir, name)
        return contains(target) && target.mkdirs()
    }

    // VFS-13: this backs the Files screen's own "+ New File" - unlike the string-based touch()
    // above (a real Unix `touch`, correctly idempotent for the vfs command line), an existing
    // target here is reported as a failure rather than silently doing nothing while the UI acts
    // as though a fresh file was created.
    fun touch(dir: File, name: String): Boolean {
        if (!contains(dir)) return false
        val target = File(dir, name)
        if (!contains(target) || target.exists()) return false
        return target.createNewFile()
    }

    fun delete(file: File): Boolean = contains(file) && file.deleteRecursively()

    // --- Trash -------------------------------------------------------------------------------
    // TRASH-1: HG2Gui is a touch-native interface over Termux, whose own `rm` is correctly,
    // brutally final - that's the right behaviour for a shell. The graphical file manager isn't
    // a shell, and delete-with-only-a-confirm-dialog is a worse safety net than delete-with-an-
    // undo: a dialog only ever catches a mis-tap noticed in the same second, never "I didn't
    // realize I needed that until tomorrow." Trashed items live in a reserved ".trash" folder at
    // the sandbox root (filtered out of list()/walk()/searchWalk() above, so it never appears as
    // a browsable folder) and are purged for good after 30 days - the same retention convention
    // most desktop and photo-library trash cans use - or immediately, via purgeTrash/emptyTrash.
    //
    // No locking here, same as move/copy/moveInto/copyInto above: an exists-check followed by a
    // renameTo is a TOCTOU race if two tabs restore to the same path at the same instant. This
    // isn't new to trash/restore - it's the pre-existing shape of every write in this object -
    // and fixing it means locking the whole manager, not just the two functions added here.

    private const val TRASH_DIR_NAME = ".trash"
    private const val TRASH_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000

    private val trashJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class TrashEntry(
        val id: String,
        /** Where [restore] puts this back - the vfs-relative path it was deleted from. */
        val originalPath: String,
        val name: String,
        val isDirectory: Boolean,
        val sizeBytes: Long,
        val deletedAtMillis: Long
    )

    private fun trashRoot(context: Context): File = File(init(context), TRASH_DIR_NAME).apply { mkdirs() }
    private fun trashSlot(context: Context, id: String): File = File(trashRoot(context), id)
    private fun trashPayload(slot: File): File = File(slot, "payload")
    private fun trashMeta(slot: File): File = File(slot, "meta.json")

    /** [path]'s own parent, string-only - a local copy of ui/files/VfsEntry.kt's vfsParentPath
     *  rather than an import across the managers/ui boundary for one two-line op. */
    private fun parentOfVfsPath(path: String): String {
        val trimmed = path.trimEnd('/')
        val slash = trimmed.lastIndexOf('/')
        return if (slash <= 0) "/" else trimmed.substring(0, slash)
    }

    private fun dirSize(file: File): Long =
        if (file.isFile) file.length() else file.listFiles()?.sumOf { dirSize(it) } ?: 0L

    /** [name] with a " (restored)" / " (restored 2)" / ... suffix before the extension - [attempt]
     *  1 is bare "(restored)", matching how most file managers number their own collision suffix. */
    private fun restoredName(name: String, attempt: Int): String {
        val dot = name.lastIndexOf('.')
        val suffix = if (attempt <= 1) " (restored)" else " (restored $attempt)"
        return if (dot > 0) "${name.substring(0, dot)}$suffix${name.substring(dot)}" else "$name$suffix"
    }

    /** Bails a restore loop that's somehow still colliding after this many suffixed attempts -
     *  never expected to matter, since it would mean 1000 same-named restores already landed in
     *  one folder, but an unbounded while(true) has no business existing over real I/O. */
    private const val MAX_RESTORE_ATTEMPTS = 1000

    /**
     * Moves [file] into the trash instead of deleting it outright. Returns the entry [restore]
     * needs to put it back, or null if [file] isn't inside the sandbox or the move failed - the
     * same null-means-refused shape [delete] and every other operation here already use.
     */
    fun trash(context: Context, file: File): TrashEntry? {
        if (!contains(file)) return null
        val originalPath = pathOf(context, file)
        // Captured before the move below: File.isDirectory() is a live stat, and once file has
        // been renamed away, that same File reference stats nothing - it read as false for every
        // trashed folder, understating a purge confirmation's own "and everything inside it".
        val wasDirectory = file.isDirectory
        val size = dirSize(file)
        val slot = trashSlot(context, UUID.randomUUID().toString())
        if (!slot.mkdirs()) return null
        val payload = trashPayload(slot)
        if (!file.renameTo(payload)) {
            // Nothing moved yet - the slot is empty and safe to just drop.
            slot.deleteRecursively()
            return null
        }
        val entry = TrashEntry(slot.name, originalPath, file.name, wasDirectory, size, System.currentTimeMillis())
        return try {
            trashMeta(slot).writeText(trashJson.encodeToString(TrashEntry.serializer(), entry))
            entry
        } catch (e: Exception) {
            // The payload really did move - writing its own receipt is what failed. Move it back
            // rather than deleting it: a missing "moved to trash" confirmation is a far smaller
            // failure than silently erasing the file trash exists to protect. Only clean up the
            // slot if that move-back actually succeeded - if it didn't too (the same unlocked
            // TOCTOU class noted above), deleting the slot now would destroy the payload a second
            // failure was never supposed to cost. Left alone, purgeExpiredTrash's own meta-first
            // check above never touches a slot it can't read a receipt for, so the payload
            // outlives the automatic 30-day sweep - but not an explicit Empty Trash, which nukes
            // every slot on purpose, orphan or not, because "empty" is not supposed to leave
            // anything behind. That's the correct behaviour for the action a person actually
            // asked for; this comment used to promise more durability than that leaves room for.
            if (payload.renameTo(file)) slot.deleteRecursively()
            null
        }
    }

    /** Puts [entry]'s payload back at [TrashEntry.originalPath]. If something now occupies that
     *  exact spot - a new file created there since the delete, or another trashed copy already
     *  restored under a suffix - the restored item gets its own unique " (restored [n])" suffix
     *  rather than silently clobbering whatever's there now. */
    fun restore(context: Context, entry: TrashEntry): Boolean {
        val slot = trashSlot(context, entry.id)
        val payload = trashPayload(slot)
        if (!payload.exists()) return false
        val parent = resolve(context, parentOfVfsPath(entry.originalPath)) ?: return false
        if (!parent.exists() && !parent.mkdirs()) return false
        var target = File(parent, entry.name)
        if (!contains(target)) return false
        var attempt = 1
        while (target.exists()) {
            if (attempt > MAX_RESTORE_ATTEMPTS) return false
            target = File(parent, restoredName(entry.name, attempt))
            if (!contains(target)) return false
            attempt++
        }
        if (!payload.renameTo(target)) return false
        slot.deleteRecursively()
        return true
    }

    /** Permanently removes [entry] - the trash's own DELETE, one step past [trash]. */
    fun purgeTrash(context: Context, entry: TrashEntry): Boolean = trashSlot(context, entry.id).deleteRecursively()

    /** Empties the whole trash at once. Returns how many entries were removed. */
    fun emptyTrash(context: Context): Int =
        trashRoot(context).listFiles()?.count { it.deleteRecursively() } ?: 0

    /** Trash older than [TRASH_RETENTION_MILLIS] is purged for good. Called from [trashedItems]
     *  rather than on a timer - there's no background task infrastructure in this app to run it
     *  on one, and every real read of the trash is a fine time to sweep it first. */
    private fun purgeExpiredTrash(context: Context) {
        val now = System.currentTimeMillis()
        trashRoot(context).listFiles()?.forEach { slot ->
            // A slot with no readable meta.json is left alone, not purged. It's more likely a
            // trash() call caught mid-failure (payload moved, receipt never written or since
            // moved back) than a genuinely expired entry - the one thing this function must never
            // do is destroy something it can't positively identify as actually past its window.
            val entry = readTrashMeta(slot) ?: return@forEach
            if (now - entry.deletedAtMillis > TRASH_RETENTION_MILLIS) {
                slot.deleteRecursively()
            }
        }
    }

    private fun readTrashMeta(slot: File): TrashEntry? {
        val metaFile = trashMeta(slot)
        if (!metaFile.exists()) return null
        return try {
            trashJson.decodeFromString(TrashEntry.serializer(), metaFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    /** Every entry currently in the trash, most recently deleted first. */
    fun trashedItems(context: Context): List<TrashEntry> {
        purgeExpiredTrash(context)
        return trashRoot(context).listFiles()
            ?.mapNotNull { readTrashMeta(it) }
            ?.sortedByDescending { it.deletedAtMillis }
            ?: emptyList()
    }

    fun rename(file: File, newName: String): Boolean {
        if (!contains(file)) return false
        val parent = file.parentFile ?: return false
        val target = File(parent, newName)
        return contains(target) && file.renameTo(target)
    }

    /** True iff [target] is [ancestor] itself or sits somewhere underneath it - the check
     *  moveInto/copyInto need before touching a folder, since neither `renameTo` nor
     *  `copyRecursively` reject a destination that's actually inside the source they're reading
     *  from; the folder picker offers exactly that target with nothing else stopping it. */
    private fun isSelfOrDescendant(ancestor: File, target: File): Boolean {
        val a = ancestor.canonicalFile
        val t = target.canonicalFile
        return t == a || t.path.startsWith(a.path + File.separator)
    }

    fun moveInto(file: File, targetDir: File): Boolean {
        if (!contains(file) || !contains(targetDir)) return false
        if (file.isDirectory && isSelfOrDescendant(file, targetDir)) return false
        return file.renameTo(File(targetDir, file.name))
    }

    fun copyInto(file: File, targetDir: File): Boolean {
        if (!contains(file) || !contains(targetDir)) return false
        if (file.isDirectory && isSelfOrDescendant(file, targetDir)) return false
        val target = File(targetDir, file.name)
        return try {
            if (file.isDirectory) file.copyRecursively(target, overwrite = true) else { file.copyTo(target, overwrite = true); true }
        } catch (e: Exception) {
            false
        }
    }

    // A guard shared by every recursive walk below: [seen] catches a symlink cycle (an inward-
    // pointing link resolves to a canonical path already on this walk's own stack, so it's
    // skipped rather than recursed into forever) and MAX_WALK_DEPTH backstops any cycle [seen]
    // doesn't - a real filesystem is never this deep, so hitting it means something's wrong, not
    // that there's more to see.
    private const val MAX_WALK_DEPTH = 64

    /** Every file and directory under [dir] (or the whole sandbox, by default), depth-first,
     *  directories before their own contents - the walk every recursive feature below shares. */
    private fun walk(dir: File, into: MutableList<File>, seen: MutableSet<String> = mutableSetOf(), depth: Int = 0) {
        if (depth >= MAX_WALK_DEPTH || !seen.add(dir.canonicalPath)) return
        val children = dir.listFiles()
            ?.filterNot { it.name == TRASH_DIR_NAME } // TRASH-1, see list() above
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: return
        for (child in children) {
            into.add(child)
            if (child.isDirectory) walk(child, into, seen, depth + 1)
        }
    }

    /** Every name under [dir] (defaulting to the sandbox root) containing [query], case
     *  insensitive - real recursive search, not just the one open directory. Stops as soon as
     *  [limit] matches are found rather than materializing the whole tree first and truncating
     *  after - a query that matches broadly (a single common letter) shouldn't have to finish
     *  walking an unbounded tree before the first [limit] results can come back. */
    fun search(context: Context, query: String, dir: File = init(context), limit: Int = 200): List<File> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<File>()
        searchWalk(dir, query.lowercase(), results, limit)
        return results
    }

    private fun searchWalk(
        dir: File, q: String, into: MutableList<File>, limit: Int,
        seen: MutableSet<String> = mutableSetOf(), depth: Int = 0
    ) {
        if (into.size >= limit || depth >= MAX_WALK_DEPTH || !seen.add(dir.canonicalPath)) return
        val children = dir.listFiles()
            ?.filterNot { it.name == TRASH_DIR_NAME } // TRASH-1, see list() above
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: return
        for (child in children) {
            if (into.size >= limit) return
            if (child.name.lowercase().contains(q)) into.add(child)
            if (child.isDirectory) searchWalk(child, q, into, limit, seen, depth + 1)
        }
    }

    enum class StorageCategory(val label: String) {
        IMAGES("Images"), DOCUMENTS("Documents"), CODE("Code"), ARCHIVES("Archives"), OTHER("Other")
    }

    private val IMAGE_EXT = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "heic")
    private val DOCUMENT_EXT = setOf("pdf", "doc", "docx", "txt", "md", "odt", "xls", "xlsx", "ppt", "pptx", "csv")
    private val CODE_EXT = setOf("kt", "java", "py", "js", "ts", "kts", "json", "xml", "html", "css", "c", "cpp", "h", "go", "rs", "sh")
    private val ARCHIVE_EXT = setOf("zip", "tar", "gz", "7z", "rar", "bz2", "xz")

    fun isImage(file: File): Boolean = file.extension.lowercase() in IMAGE_EXT

    private fun categoryOf(file: File): StorageCategory = when (file.extension.lowercase()) {
        in IMAGE_EXT -> StorageCategory.IMAGES
        in DOCUMENT_EXT -> StorageCategory.DOCUMENTS
        in CODE_EXT -> StorageCategory.CODE
        in ARCHIVE_EXT -> StorageCategory.ARCHIVES
        else -> StorageCategory.OTHER
    }

    data class StorageBreakdown(
        val totalBytes: Long,
        val byCategory: Map<StorageCategory, Long>,
        val largestFiles: List<File>
    )

    /** Recursively sizes the whole sandbox by [StorageCategory] - this is storage used inside
     *  HG2Gui's own private sandbox, not the device's, since that's the only filesystem [vfs]
     *  actually models; a device-wide figure would be claiming knowledge this sandbox doesn't
     *  have. */
    fun storageByType(context: Context): StorageBreakdown {
        val all = mutableListOf<File>()
        walk(init(context), all)
        val files = all.filter { it.isFile }
        val byCategory = files.groupingBy { categoryOf(it) }.fold(0L) { acc, f -> acc + f.length() }
        val total = files.sumOf { it.length() }
        val largest = files.sortedByDescending { it.length() }.take(10)
        return StorageBreakdown(total, byCategory, largest)
    }
}
