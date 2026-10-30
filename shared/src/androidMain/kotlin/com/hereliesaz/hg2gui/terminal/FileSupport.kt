package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

/**
 * Small, Android-runtime-free file helpers shared by the package/bootstrap layer. They exist as
 * plain `java.*` code (rather than `android.util.AtomicFile` / `android.util.Base64`) so the
 * logic built on them runs under ordinary host unit tests, where the android.jar stubs throw.
 */
object AtomicFiles {
    /**
     * Writes [bytes] to [file] through a sibling temp file that is fsync'd and then renamed over
     * the destination, so a reader (or a crash) sees either the old content or the new content,
     * never a torn file. Falls back to a plain replace on filesystems without atomic rename.
     */
    fun write(file: File, bytes: ByteArray) {
        val target = file.absoluteFile
        val parent = target.parentFile ?: error("No parent directory for ${target.path}")
        parent.mkdirs()
        val temp = File(parent, ".${target.name}.hg2tmp")
        try {
            FileOutputStream(temp).use { out ->
                out.write(bytes)
                out.flush()
                out.fd.sync()
            }
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            temp.delete()
        }
    }
}

/** Directory-tree helpers that never follow symbolic links. */
object FileTrees {
    /**
     * Deletes [root] and everything under it WITHOUT following symlinks. `File.deleteRecursively`
     * descends into a symlink that points at a directory (its walker asks `isDirectory`, which
     * resolves links), so a link inside a prefix aimed at, say, a user directory would have its
     * target's contents deleted. This walker removes the link itself instead. Returns true when
     * [root] no longer exists afterwards.
     */
    fun deleteRecursivelyNoFollow(root: File): Boolean {
        val path = root.toPath()
        if (!Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return true
        try {
            Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    Files.deleteIfExists(file)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    runCatching { Files.deleteIfExists(dir) }
                    return FileVisitResult.CONTINUE
                }
            })
        } catch (_: IOException) {
            // Fall through to the existence check: a partial delete is reported as failure.
        }
        return !Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)
    }

    /**
     * Total size in bytes of the regular files under [root], symlinks counted as zero and never
     * followed. Missing roots count as zero.
     */
    fun sizeNoFollow(root: File): Long {
        val path = root.toPath()
        if (!Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return 0L
        var total = 0L
        try {
            Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (attrs.isRegularFile) total += attrs.size()
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE
            })
        } catch (_: IOException) {
            // Best-effort estimate.
        }
        return total
    }
}

/**
 * RFC 4648 standard-alphabet Base64 without line wrapping - wire-compatible with
 * `android.util.Base64.NO_WRAP` output and with `Base64.DEFAULT` decoding (whitespace ignored,
 * padding optional) - so manifests written before this helper existed still decode.
 */
object PlainBase64 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val REVERSE = IntArray(128) { -1 }.also { table ->
        ALPHABET.forEachIndexed { index, c -> table[c.code] = index }
    }

    fun encode(bytes: ByteArray): String {
        val out = StringBuilder((bytes.size + 2) / 3 * 4)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xff
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xff else -1
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xff else -1
            out.append(ALPHABET[b0 shr 2])
            out.append(ALPHABET[((b0 and 0x03) shl 4) or (if (b1 >= 0) b1 shr 4 else 0)])
            out.append(if (b1 >= 0) ALPHABET[((b1 and 0x0f) shl 2) or (if (b2 >= 0) b2 shr 6 else 0)] else '=')
            out.append(if (b2 >= 0) ALPHABET[b2 and 0x3f] else '=')
            i += 3
        }
        return out.toString()
    }

    fun decode(text: String): ByteArray {
        val out = java.io.ByteArrayOutputStream(text.length * 3 / 4)
        var buffer = 0
        var bits = 0
        for (c in text) {
            if (c == '=' || c == ' ' || c == '\n' || c == '\r' || c == '\t') continue
            val value = if (c.code < REVERSE.size) REVERSE[c.code] else -1
            require(value >= 0) { "Illegal Base64 character: '$c'" }
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((buffer shr bits) and 0xff)
            }
        }
        return out.toByteArray()
    }

    fun encodeString(value: String): String = encode(value.toByteArray(Charsets.UTF_8))
    fun decodeString(value: String): String = String(decode(value), Charsets.UTF_8)
}

/**
 * Byte-level prefix rewriting for relocated Termux text (scripts, control files, dpkg info).
 *
 * Working on the raw bytes - never decoding through a charset and re-encoding - is what keeps
 * Latin-1 / other non-UTF-8 text intact: a decode/encode round trip replaces every undecodable
 * sequence with U+FFFD, silently corrupting a file that merely happens to mention the old prefix.
 * Files that contain a NUL byte are treated as binary and never touched, since a different-length
 * replacement would shift offsets inside an executable or archive.
 */
object PrefixRewriter {
    /** True if [content] has no NUL byte, i.e. is safe to treat as text. */
    fun looksLikeText(content: ByteArray): Boolean = content.none { it == 0.toByte() }

    /**
     * Returns [content] with every occurrence of [old] replaced by [replacement], or `null` when
     * [old] does not occur. Both strings are ASCII path prefixes in practice, but are matched as
     * their UTF-8 byte sequences so the rest of the content is carried through byte-for-byte.
     */
    fun replaceInBytes(content: ByteArray, old: String, replacement: String): ByteArray? {
        val needle = old.toByteArray(Charsets.UTF_8)
        if (needle.isEmpty() || content.size < needle.size) return null
        val with = replacement.toByteArray(Charsets.UTF_8)
        val out = java.io.ByteArrayOutputStream(content.size)
        var found = false
        var copyFrom = 0
        var i = 0
        val last = content.size - needle.size
        while (i <= last) {
            if (content[i] == needle[0] && matchesAt(content, i, needle)) {
                out.write(content, copyFrom, i - copyFrom)
                out.write(with)
                i += needle.size
                copyFrom = i
                found = true
            } else {
                i++
            }
        }
        if (!found) return null
        out.write(content, copyFrom, content.size - copyFrom)
        return out.toByteArray()
    }

    /**
     * Rewrites [old] to [replacement] inside [file] in place (keeping its mode). Returns true only
     * if the file was text, contained [old], and was rewritten; unreadable/binary files are left
     * alone and report false.
     */
    fun rewriteFile(file: File, old: String, replacement: String): Boolean {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return false
        if (!looksLikeText(bytes)) return false
        val rewritten = replaceInBytes(bytes, old, replacement) ?: return false
        return runCatching {
            file.writeBytes(rewritten)
            true
        }.getOrDefault(false)
    }

    /**
     * Applies [replacements] in order to [file] (text only, same byte-level rules as
     * [rewriteFile]). Returns true if any of them changed the file.
     */
    fun rewriteFile(file: File, replacements: List<Pair<String, String>>): Boolean {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return false
        if (!looksLikeText(bytes)) return false
        var current = bytes
        var changed = false
        for ((old, with) in replacements) {
            val next = replaceInBytes(current, old, with) ?: continue
            current = next
            changed = true
        }
        if (!changed) return false
        return runCatching {
            file.writeBytes(current)
            true
        }.getOrDefault(false)
    }

    private fun matchesAt(content: ByteArray, at: Int, needle: ByteArray): Boolean {
        for (j in needle.indices) {
            if (content[at + j] != needle[j]) return false
        }
        return true
    }
}
