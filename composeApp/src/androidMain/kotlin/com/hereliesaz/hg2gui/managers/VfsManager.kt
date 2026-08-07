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
}
