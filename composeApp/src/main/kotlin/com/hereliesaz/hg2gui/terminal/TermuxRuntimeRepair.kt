package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.system.Os
import java.io.File
import java.nio.file.Files

/**
 * Per-launch repair of an extracted Termux prefix. The generated `etc/apt/apt.conf` and the
 * `$HOME/.hg2gui-script-wrappers.sh` profile are NOT written here: [PrefixRuntimeConfig] (in
 * `:shared`) is their single source of truth, shared with [DistroManager], so the two callers can
 * no longer overwrite each other with different content.
 */
object TermuxRuntimeRepair {
    private const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
    private const val MAIN_REPO = "https://packages.termux.dev/apt/termux-main"
    private const val ROOT_REPO = "https://packages.termux.dev/apt/termux-root"
    private const val X11_REPO = "https://packages.termux.dev/apt/termux-x11"
    private const val NEEDS_APT_UPDATE = PrefixRuntimeConfig.NEEDS_APT_UPDATE
    private const val REPAIR_STAMP = ".hg2gui-repair-stamp"

    /**
     * True iff [shebangLine] (the script's first line) runs it with sh or bash - directly or via
     * `env` - the only scripts the wrapper may `source` into bash. A python/perl/node script
     * sourced into bash would just be parsed as shell. Pure, see TermuxRuntimeRepairTest.
     */
    internal fun isShellShebang(shebangLine: String): Boolean = PrefixRuntimeConfig.isShellShebang(shebangLine)

    fun repair(context: Context) {
        val prefix = DistroManager.prefixDir(context)
        val bin = File(prefix, "bin")
        if (!prefix.isDirectory || !bin.isDirectory) return
        // The full-tree walks (script prefix rewrite + ELF audit) run on every launch otherwise,
        // reading every file under the prefix. Skip them while nothing that could add or change
        // files there has happened since the last completed run.
        val stamp = repairStamp(context, prefix)
        val stampFile = File(prefix, REPAIR_STAMP)
        val upToDate = runCatching { stampFile.readText() == stamp }.getOrDefault(false)
        if (!upToDate) {
            listOf("bin", "libexec", "lib").forEach { subdir ->
                val root = File(prefix, subdir)
                if (root.exists()) root.walkTopDown().forEach { repairScript(it, prefix) }
            }
        }
        repairMainRepoKey(prefix)
        writeMainRepoSource(prefix)
        val nativeLibraryDir = context.applicationInfo.nativeLibraryDir
        PrefixRuntimeConfig.writeAptConf(prefix, prefix, nativeLibraryDir)
        PrefixRuntimeConfig.writeScriptWrappers(prefix, DistroManager.homeDir(context), nativeLibraryDir)
        if (!upToDate) {
            TermuxElfAudit.audit(context)
            runCatching { stampFile.writeText(stamp) }
        }
    }

    /** Changes whenever packages are installed/removed (dpkg status), top-level bin/lib/libexec
     *  entries change, or the app itself is updated (new launchers, new repair rules). */
    private fun repairStamp(context: Context, prefix: File): String {
        val appUpdated = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime }.getOrDefault(0L)
        val status = File(prefix, "var/lib/dpkg/status")
        return buildString {
            append("app=").append(appUpdated)
            append(";status=").append(status.lastModified()).append('/').append(status.length())
            listOf("bin", "lib", "libexec").forEach { append(';').append(it).append('=').append(File(prefix, it).lastModified()) }
        }
    }

    private fun repairScript(file: File, prefix: File) {
        if (!file.isFile || Files.isSymbolicLink(file.toPath()) || PrefixRuntimeConfig.isElf(file)) return
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return
        if (bytes.size < 2 || bytes[0] != '#'.code.toByte() || bytes[1] != '!'.code.toByte()) return
        // Byte-level (PrefixRewriter): re-encoding a non-UTF-8 script through a String would
        // corrupt every byte that is not valid UTF-8.
        val replaced = PrefixRewriter.replaceInBytes(bytes, TERMUX_PREFIX, prefix.absolutePath)
        if (replaced != null) runCatching { atomicWrite(file, replaced) }
        runCatching { file.setExecutable(true) }
    }

    private fun atomicWrite(target: File, content: ByteArray) {
        val tmp = File(target.parentFile, ".${target.name}.tmp")
        tmp.writeBytes(content)
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    private fun repairMainRepoKey(prefix: File) {
        val target = File(prefix, "share/termux-keyring/termux-pacman.gpg")
        if (!target.exists()) return
        val link = File(prefix, "etc/apt/trusted.gpg.d/termux-pacman.gpg")
        link.parentFile?.mkdirs()
        if (runCatching { link.canonicalPath == target.canonicalPath }.getOrDefault(false)) return
        runCatching { link.delete() }
        runCatching { Os.symlink(target.absolutePath, link.absolutePath) }
    }

    private fun writeMainRepoSource(prefix: File) {
        val aptDir = File(prefix, "etc/apt").apply { mkdirs() }
        val source = File(aptDir, "sources.list")
        val desiredSource = "deb $MAIN_REPO stable main\n"
        val sourceChanged = !source.exists() || source.readText() != desiredSource
        atomicWrite(source, desiredSource.toByteArray(Charsets.UTF_8))

        val termuxDir = File(prefix, "etc/termux").apply { mkdirs() }
        val mirrorDir = File(termuxDir, "mirrors/hg2gui").apply { mkdirs() }
        val descriptor = File(mirrorDir, "packages.termux.dev")
        descriptor.writeText(
            """
            # This file is sourced by pkg
            # HG2Gui pinned Termux primary host
            # Termux primary package host | https://packages.termux.dev
            # Managed by HG2Gui runtime repair.
            WEIGHT=10
            MAIN="$MAIN_REPO"
            ROOT="$ROOT_REPO"
            X11="$X11_REPO"
            """.trimIndent() + "\n"
        )

        val chosen = File(termuxDir, "chosen_mirrors")
        runCatching { chosen.deleteRecursively() }
        runCatching { Os.symlink(descriptor.absolutePath, chosen.absolutePath) }

        val listsDir = File(prefix, "var/lib/apt/lists").apply { mkdirs() }
        val hasPrimaryIndex = listsDir.listFiles().orEmpty().any {
            it.isFile && it.name.contains("packages.termux.dev")
        }
        if (sourceChanged || !hasPrimaryIndex) {
            File(listsDir, NEEDS_APT_UPDATE).writeText("1\n")
        }
    }
}
