package com.hereliesaz.hg2gui.managers

import android.content.Context
import java.io.File

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
    private var root: File? = null
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

    fun list(context: Context): List<File> =
        currentDir(context).listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()

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

    fun readText(context: Context, name: String): String? =
        resolve(context, name)?.takeIf { it.isFile }?.readText()

    fun writeText(context: Context, name: String, text: String): Boolean {
        val f = resolve(context, name) ?: return false
        f.writeText(text)
        return true
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

    fun mkdir(dir: File, name: String): Boolean = contains(dir) && File(dir, name).mkdirs()

    fun touch(dir: File, name: String): Boolean {
        if (!contains(dir)) return false
        val f = File(dir, name)
        return f.exists() || f.createNewFile()
    }

    fun delete(file: File): Boolean = contains(file) && file.deleteRecursively()

    fun rename(file: File, newName: String): Boolean {
        if (!contains(file)) return false
        val parent = file.parentFile ?: return false
        return file.renameTo(File(parent, newName))
    }

    fun moveInto(file: File, targetDir: File): Boolean {
        if (!contains(file) || !contains(targetDir)) return false
        return file.renameTo(File(targetDir, file.name))
    }

    fun copyInto(file: File, targetDir: File): Boolean {
        if (!contains(file) || !contains(targetDir)) return false
        val target = File(targetDir, file.name)
        return try {
            if (file.isDirectory) file.copyRecursively(target, overwrite = true) else { file.copyTo(target, overwrite = true); true }
        } catch (e: Exception) {
            false
        }
    }

    /** Every file and directory under [dir] (or the whole sandbox, by default), depth-first,
     *  directories before their own contents - the walk every recursive feature below shares. */
    private fun walk(dir: File, into: MutableList<File>) {
        val children = dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: return
        for (child in children) {
            into.add(child)
            if (child.isDirectory) walk(child, into)
        }
    }

    /** Every name under [dir] (defaulting to the sandbox root) containing [query], case
     *  insensitive - real recursive search, not just the one open directory. Capped at [limit]
     *  since a query that matches broadly (a single common letter) shouldn't hang the UI walking
     *  an unbounded tree. */
    fun search(context: Context, query: String, dir: File = init(context), limit: Int = 200): List<File> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        val all = mutableListOf<File>()
        walk(dir, all)
        return all.filter { it.name.lowercase().contains(q) }.take(limit)
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
