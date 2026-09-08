package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files

/** Audits persistent Termux ELFs for relocation hazards without ever modifying binary bytes. */
object TermuxElfAudit {
    private const val TAG = "HG2Gui-ElfAudit"
    private const val OLD_PREFIX = "/data/data/com.termux/files/usr"
    private const val REPORT = ".hg2gui-elf-audit.txt"

    data class Finding(val path: String, val manifestManaged: Boolean, val oldPrefixEmbedded: Boolean, val interpreter: String?, val interpreterUsesOldPrefix: Boolean)

    fun audit(context: Context): List<Finding> {
        val prefix = DistroManager.prefixDir(context)
        if (!prefix.isDirectory) return emptyList()
        val managed = BootstrapManifest.ENTRIES.mapTo(HashSet()) { (path, _) -> File(prefix, path).absolutePath }
        val findings = mutableListOf<Finding>()
        listOf("bin", "libexec", "lib").forEach { subdir ->
            val root = File(prefix, subdir)
            if (!root.exists()) return@forEach
            root.walkTopDown().forEach { file ->
                if (!file.isFile || Files.isSymbolicLink(file.toPath())) return@forEach
                val header = runCatching { file.inputStream().use { it.readNBytes(64) } }.getOrNull() ?: return@forEach
                if (!isElf(header)) return@forEach
                val interpreter = readInterpreter(file, header)
                val embedded = containsAscii(file, OLD_PREFIX.toByteArray(Charsets.UTF_8))
                if (embedded || interpreter != null) findings += Finding(file.absolutePath.removePrefix(prefix.absolutePath + "/"), file.absolutePath in managed, embedded, interpreter, interpreter?.startsWith(OLD_PREFIX) == true)
            }
        }
        runCatching { File(prefix, REPORT).writeText(buildString {
            appendLine("HG2Gui Termux ELF relocation audit")
            appendLine("prefix=${prefix.absolutePath}")
            appendLine("oldPrefix=$OLD_PREFIX")
            appendLine("findings=${findings.size}")
            appendLine()
            findings.forEach { f -> appendLine("${f.path}\tmanaged=${f.manifestManaged}\toldPrefix=${f.oldPrefixEmbedded}\tinterpOldPrefix=${f.interpreterUsesOldPrefix}\tinterp=${f.interpreter ?: "-"}") }
        }) }
        Log.i(TAG, "ELF relocation audit: ${findings.count { it.oldPrefixEmbedded || it.interpreterUsesOldPrefix }} hazardous references; report=${File(prefix, REPORT)}")
        return findings
    }

    private fun isElf(h: ByteArray) = h.size >= 16 && h[0].toInt() == 0x7f && h[1] == 'E'.code.toByte() && h[2] == 'L'.code.toByte() && h[3] == 'F'.code.toByte()

    private fun readInterpreter(file: File, h: ByteArray): String? = runCatching {
        val cls = h[4].toInt() and 0xff
        val order = when (h[5].toInt() and 0xff) { 1 -> ByteOrder.LITTLE_ENDIAN; 2 -> ByteOrder.BIG_ENDIAN; else -> return null }
        val bb = ByteBuffer.wrap(h).order(order)
        val phoff: Long; val phentsize: Int; val phnum: Int
        if (cls == 1) { phoff = bb.getInt(28).toLong() and 0xffffffffL; phentsize = bb.getShort(42).toInt() and 0xffff; phnum = bb.getShort(44).toInt() and 0xffff }
        else if (cls == 2) { phoff = bb.getLong(32); phentsize = bb.getShort(54).toInt() and 0xffff; phnum = bb.getShort(56).toInt() and 0xffff }
        else return null
        if (phoff < 0 || phentsize <= 0 || phnum <= 0 || phnum > 4096) return null
        file.inputStream().buffered().use { input ->
            var skipped = 0L
            while (skipped < phoff) { val n = input.skip(phoff - skipped); if (n <= 0) return null; skipped += n }
            repeat(phnum) {
                val ph = input.readNBytes(phentsize); if (ph.size != phentsize) return null
                val p = ByteBuffer.wrap(ph).order(order)
                if (p.getInt(0) == 3) {
                    val offset = if (cls == 1) p.getInt(4).toLong() and 0xffffffffL else p.getLong(8)
                    val size = if (cls == 1) p.getInt(16).toLong() and 0xffffffffL else p.getLong(32)
                    if (offset < 0 || size <= 0 || size > 4096) return null
                    file.inputStream().use { ii ->
                        var s = 0L; while (s < offset) { val n = ii.skip(offset - s); if (n <= 0) return null; s += n }
                        return String(ii.readNBytes(size.toInt()), Charsets.UTF_8).trimEnd('\u0000')
                    }
                }
            }
        }
        null
    }.getOrNull()

    private fun containsAscii(file: File, needle: ByteArray): Boolean = runCatching {
        file.inputStream().buffered().use { input ->
            var matched = 0
            while (true) {
                val b = input.read(); if (b < 0) return false
                if (b.toByte() == needle[matched]) { matched++; if (matched == needle.size) return true }
                else matched = if (b.toByte() == needle[0]) 1 else 0
            }
        }; false
    }.getOrDefault(false)
}
