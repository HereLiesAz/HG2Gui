package com.hereliesaz.hg2gui.managers

import java.io.File

/**
 * A filesystem view pinned to ONE directory, independent of everything [VfsManager]'s global
 * state does. [VfsManager] has a single mutable `root` that the Files UI flips between the
 * private sandbox and real device storage, and a shared `relativePath` cwd; anything resolving
 * through it follows those switches. The MCP server must not: a paired agent that was promised
 * "HG2Gui's app-private sandboxed filesystem" would silently gain all of shared storage the moment
 * the user opened Files in device-storage mode. So MCP builds one [VfsSandbox] (via
 * [VfsManager.sandbox]) and every vfs.* call goes through it.
 *
 * Every path is treated as root-absolute (a missing leading "/" is added), so there is no
 * working-directory state to race against, and every path is canonicalized - symlinks included - and
 * must land inside the root or the operation is refused. Plain java.io only, so it is unit-tested on
 * the JVM (see VfsSandboxTest).
 */
class VfsSandbox(rootDir: File) {
    private val root: File = rootDir.canonicalFile

    /** [path] resolved inside the root, or null if it would escape it (".." or a symlink out). */
    fun resolve(path: String): File? {
        val candidate = File(root, path.removePrefix("/").trimStart('/')).canonicalFile
        return if (candidate == root || candidate.path.startsWith(root.path + File.separator)) candidate else null
    }

    /** [file]'s root-relative path, e.g. "/Downloads/photo.png" ("/" for the root itself). */
    fun pathOf(file: File): String {
        val canonical = file.canonicalPath
        if (canonical != root.path && !canonical.startsWith(root.path + File.separator)) return "/"
        val relative = canonical.removePrefix(root.path).trim(File.separatorChar)
        return if (relative.isEmpty()) "/" else "/$relative"
    }

    /** Children of [dir], folders first, without the trash folder (same view as [VfsManager.listChildren]). */
    fun listChildren(dir: File): List<File> =
        dir.listFiles()
            ?.filterNot { it.name == TRASH_DIR_NAME }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()

    /** [path]'s text, or null if it is not a regular file inside the root, is over the size cap, or cannot be read. */
    fun readText(path: String): String? =
        resolve(path)
            ?.takeIf { it.isFile && it.length() <= MAX_READ_TEXT_BYTES }
            ?.let { f -> try { f.readText() } catch (e: Exception) { null } }

    fun writeText(path: String, text: String): Boolean {
        val f = resolve(path) ?: return false
        return try {
            f.writeText(text)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun mkdir(path: String): Boolean = resolve(path)?.mkdirs() ?: false

    fun delete(path: String): Boolean {
        val f = resolve(path) ?: return false
        return f != root && f.deleteRecursively()
    }

    fun move(from: String, to: String): Boolean {
        val f = resolve(from) ?: return false
        val t = resolve(to) ?: return false
        return f != root && f.renameTo(t)
    }

    fun copy(from: String, to: String): Boolean {
        val f = resolve(from) ?: return false
        val t = resolve(to) ?: return false
        if (isSelfOrDescendant(f, t)) return false // copying a path onto itself would truncate it
        return try {
            if (f.isDirectory) f.copyRecursively(t, overwrite = true) else { f.copyTo(t, overwrite = true); true }
        } catch (e: Exception) {
            false
        }
    }

    private fun isSelfOrDescendant(ancestor: File, target: File): Boolean =
        target == ancestor || target.path.startsWith(ancestor.path + File.separator)

    private companion object {
        const val TRASH_DIR_NAME = ".trash"
        const val MAX_READ_TEXT_BYTES = 16L * 1024 * 1024
    }
}
