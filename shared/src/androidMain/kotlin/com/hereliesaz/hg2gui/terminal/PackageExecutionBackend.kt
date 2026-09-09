package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Selects the execution boundary for package-owned commands.
 *
 * DIRECT_LINKER is the normal HG2Gui/Termux path and relies on the HG2Gui exec preload for
 * dynamically-linked ELF files installed under app data. PROOT_COMPAT keeps the host prefix/home
 * visible but executes through the same PRoot engine used by isolation. PROOT_ISOLATED is a hard
 * boundary: an isolated package is never downgraded to either non-isolated backend.
 */
object PackageExecutionBackend {
    enum class Backend {
        DIRECT_LINKER,
        PROOT_COMPAT,
        PROOT_ISOLATED
    }

    data class Decision(
        val backend: Backend,
        val executable: File? = null,
        val reason: String
    )

    fun select(
        context: Context,
        pkg: PackageLifecycleStore.InstalledPackage,
        verb: String
    ): Decision {
        if (pkg.isolated) {
            return Decision(Backend.PROOT_ISOLATED, reason = "package isolation policy")
        }

        val executable = resolveExecutable(context, verb)
            ?: return Decision(Backend.DIRECT_LINKER, reason = "command is resolved by the shell")
        val elf = inspectElf(executable)
            ?: return Decision(Backend.DIRECT_LINKER, executable, "script or non-ELF executable")

        return if (elf.hasInterpreter) {
            Decision(Backend.DIRECT_LINKER, executable, "dynamically-linked ELF")
        } else {
            Decision(Backend.PROOT_COMPAT, executable, "ELF has no PT_INTERP entry")
        }
    }

    /**
     * Compatibility mode deliberately does not create a private root. It gives PRoot the host root
     * and current working directory, then re-establishes HG2Gui's Termux environment. This is an
     * execution compatibility layer, not a security boundary.
     */
    fun compatibilityCommand(context: Context, original: String, workingDirectory: String): String {
        val proot = PackageIsolation.engine(context)
            ?: error("PRoot compatibility backend is unavailable")
        val prefix = DistroManager.prefixDir(context)
        val home = DistroManager.homeDir(context)
        val cwd = File(workingDirectory).takeIf { it.isDirectory }?.absolutePath ?: home.absolutePath
        val bash = File(prefix, "bin/bash").absolutePath
        val environment = linkedMapOf(
            "PATH" to "${prefix.absolutePath}/bin:/system/bin",
            "LD_LIBRARY_PATH" to "${prefix.absolutePath}/lib",
            "TMPDIR" to "${prefix.absolutePath}/tmp",
            "LANG" to "en_US.UTF-8",
            "TERM" to "xterm-256color",
            "TERMINFO" to "${prefix.absolutePath}/share/terminfo"
        )
        Hg2ExecEnvironment.apply(context, environment, home)

        return buildString {
            append(q(proot.absolutePath))
            append(" -r /")
            append(" -w ").append(q(cwd))
            append(" /system/bin/env")
            environment.forEach { (key, value) ->
                append(' ').append(key).append('=').append(q(value))
            }
            append(' ').append(q(bash)).append(" -lc ").append(q(original))
        }
    }

    private data class ElfInfo(val hasInterpreter: Boolean)

    private fun resolveExecutable(context: Context, verb: String): File? {
        if (verb.contains('/')) return File(verb).takeIf { it.isFile }
        val prefix = DistroManager.prefixDir(context)
        val candidate = File(prefix, "bin/$verb")
        if (!candidate.exists()) return null
        return runCatching { candidate.canonicalFile }.getOrDefault(candidate).takeIf { it.isFile }
    }

    private fun inspectElf(file: File): ElfInfo? = runCatching {
        RandomAccessFile(file, "r").use { raf ->
            if (raf.length() < 52L) return null
            val ident = ByteArray(16)
            raf.readFully(ident)
            if (ident[0] != 0x7f.toByte() || ident[1] != 'E'.code.toByte() ||
                ident[2] != 'L'.code.toByte() || ident[3] != 'F'.code.toByte()
            ) return null

            val elfClass = ident[4].toInt() and 0xff
            val endian = when (ident[5].toInt() and 0xff) {
                1 -> ByteOrder.LITTLE_ENDIAN
                2 -> ByteOrder.BIG_ENDIAN
                else -> return null
            }
            val headerSize = if (elfClass == 2) 64 else if (elfClass == 1) 52 else return null
            raf.seek(0)
            val header = ByteArray(headerSize)
            raf.readFully(header)
            val buffer = ByteBuffer.wrap(header).order(endian)
            val phoff: Long
            val phentsize: Int
            val phnum: Int
            if (elfClass == 2) {
                phoff = buffer.getLong(32)
                phentsize = buffer.getShort(54).toInt() and 0xffff
                phnum = buffer.getShort(56).toInt() and 0xffff
            } else {
                phoff = buffer.getInt(28).toLong() and 0xffffffffL
                phentsize = buffer.getShort(42).toInt() and 0xffff
                phnum = buffer.getShort(44).toInt() and 0xffff
            }
            if (phoff <= 0L || phentsize < 4 || phnum <= 0 || phnum > 4096) {
                return ElfInfo(hasInterpreter = false)
            }
            if (phoff + phentsize.toLong() * phnum > raf.length()) return null

            val entry = ByteArray(phentsize)
            repeat(phnum) { index ->
                raf.seek(phoff + index.toLong() * phentsize)
                raf.readFully(entry)
                val type = ByteBuffer.wrap(entry, 0, 4).order(endian).int
                if (type == PT_INTERP) return ElfInfo(hasInterpreter = true)
            }
            ElfInfo(hasInterpreter = false)
        }
    }.getOrNull()

    private fun q(value: String): String = "'${value.replace("'", "'\\''")}'"
    private const val PT_INTERP = 3
}
