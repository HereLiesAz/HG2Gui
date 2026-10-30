package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption

/**
 * Stores dpkg maintainer-script bodies separately from their executable entry points.
 *
 * Android can reject direct exec of writable app-data scripts. Dpkg still needs executable
 * preinst/postinst/prerm/postrm paths for trigger processing, so each dpkg-info entry is a symlink
 * to HG2Gui's APK-installed native trampoline while the original script body lives beside it with
 * a `.hg2body` suffix. The trampoline infers the body from argv[0] and invokes bundled Bash.
 */
object MaintainerScriptStore {
    private val SCRIPT_NAMES = setOf("preinst", "postinst", "prerm", "postrm")
    private val SHELLS = setOf("sh", "bash", "dash")
    private const val OLD_PREFIX = "/data/data/com.termux/files/usr"

    /**
     * Where the store reads and writes. [launcher] is the APK-installed trampoline every entry
     * points at - its path changes on every APK update, which is why entries are re-validated by
     * [migrateInstalled] rather than trusted once created. Production code uses [layout]; host
     * tests build one over temp directories.
     */
    internal class Layout(val infoDir: File, val launcher: File, val prefix: File)

    private fun layout(context: Context) = Layout(infoDir(context), launcher(context), DistroManager.prefixDir(context))

    /**
     * Brings every maintainer-script entry in dpkg's info directory to the current form:
     *  - a plain script file written by dpkg is converted to body + trampoline link;
     *  - a trampoline link that points at a DIFFERENT (or no longer existing) launcher path is
     *    re-pointed at the current one. Entries are absolute symlinks into `nativeLibraryDir`, which
     *    Android re-randomises on every APK update, so without this a direct `dpkg` run after an
     *    update would hit dangling `postinst`/`prerm` links. Only links that have a sibling
     *    `.hg2body` are treated as ours; any other symlink is left alone.
     */
    fun migrateInstalled(context: Context) {
        migrateInstalled(layout(context))
    }

    internal fun migrateInstalled(layout: Layout): Int {
        val infoDir = layout.infoDir
        val launcher = layout.launcher
        if (!infoDir.isDirectory || !launcher.canExecute()) return 0
        var changed = 0
        infoDir.listFiles().orEmpty().forEach { entry ->
            val scriptName = entry.name.substringAfterLast('.', missingDelimiterValue = "")
            if (scriptName !in SCRIPT_NAMES) return@forEach
            if (Files.isSymbolicLink(entry.toPath())) {
                if (healLink(entry, launcher)) changed++
                return@forEach
            }
            if (!entry.isFile) return@forEach
            normalizeBody(layout.prefix, entry)
            requireShellCompatible(entry)
            installAtomic(entry, entry, launcher)
            changed++
        }
        return changed
    }

    /** Re-points [entry] at [launcher] if it is one of our trampoline links and currently differs. */
    private fun healLink(entry: File, launcher: File): Boolean {
        if (!bodyFile(entry).isFile) return false
        val current = runCatching { Files.readSymbolicLink(entry.toPath()).toString() }.getOrNull()
        if (current == launcher.absolutePath && entry.exists()) return false
        linkAtomic(entry, launcher)
        return true
    }

    fun stage(context: Context, packageName: String, destinationDir: File) {
        stage(layout(context), packageName, destinationDir)
    }

    internal fun stage(layout: Layout, packageName: String, destinationDir: File) {
        val info = layout.infoDir
        destinationDir.mkdirs()
        SCRIPT_NAMES.forEach { scriptName ->
            val entry = File(info, "$packageName.$scriptName")
            val body = bodyFile(entry)
            val source = when {
                body.isFile -> body
                entry.isFile && !Files.isSymbolicLink(entry.toPath()) -> entry
                else -> null
            } ?: return@forEach
            source.copyTo(File(destinationDir, scriptName), overwrite = true)
            Files.deleteIfExists(entry.toPath())
            Files.deleteIfExists(body.toPath())
        }
    }

    fun restore(context: Context, packageName: String, scriptsDir: File) {
        restore(layout(context), packageName, scriptsDir)
    }

    internal fun restore(layout: Layout, packageName: String, scriptsDir: File) {
        if (!scriptsDir.isDirectory) return
        val launcher = layout.launcher
        require(launcher.canExecute()) { "HG2Gui maintainer-script trampoline is unavailable" }
        layout.infoDir.mkdirs()
        SCRIPT_NAMES.forEach { scriptName ->
            val source = File(scriptsDir, scriptName)
            if (!source.isFile) return@forEach
            normalizeBody(layout.prefix, source)
            requireShellCompatible(source)
            installAtomic(source, File(layout.infoDir, "$packageName.$scriptName"), launcher)
        }
    }

    fun removeEntries(context: Context, packageName: String) {
        removeEntries(layout(context), packageName)
    }

    internal fun removeEntries(layout: Layout, packageName: String) {
        SCRIPT_NAMES.forEach { scriptName ->
            val entry = File(layout.infoDir, "$packageName.$scriptName")
            Files.deleteIfExists(entry.toPath())
            Files.deleteIfExists(bodyFile(entry).toPath())
        }
    }

    fun hasEntry(context: Context, packageName: String, scriptName: String): Boolean =
        hasEntry(layout(context), packageName, scriptName)

    internal fun hasEntry(layout: Layout, packageName: String, scriptName: String): Boolean {
        val entry = File(layout.infoDir, "$packageName.$scriptName")
        return Files.exists(entry.toPath(), LinkOption.NOFOLLOW_LINKS) || bodyFile(entry).exists()
    }

    private fun installAtomic(source: File, entry: File, launcher: File) {
        val body = bodyFile(entry)
        body.parentFile?.mkdirs()
        val bodyTemp = File(body.parentFile, body.name + ".tmp")
        source.copyTo(bodyTemp, overwrite = true)
        if (!bodyTemp.renameTo(body)) {
            bodyTemp.copyTo(body, overwrite = true)
            bodyTemp.delete()
        }
        linkAtomic(entry, launcher)
    }

    /** Replaces [entry] with a symlink to [launcher] without ever leaving it missing. */
    private fun linkAtomic(entry: File, launcher: File) {
        val linkTemp = File(entry.parentFile, entry.name + ".hg2link")
        Files.deleteIfExists(linkTemp.toPath())
        Files.createSymbolicLink(linkTemp.toPath(), launcher.toPath())
        runCatching {
            Files.move(
                linkTemp.toPath(),
                entry.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }.getOrElse {
            Files.move(linkTemp.toPath(), entry.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun normalizeBody(prefix: File, file: File) {
        // Byte-level (see PrefixRewriter): re-encoding through UTF-8 would corrupt a non-UTF-8
        // script that merely mentions the old Termux prefix.
        PrefixRewriter.rewriteFile(file, OLD_PREFIX, prefix.absolutePath)
    }

    private fun requireShellCompatible(file: File) {
        val first = runCatching { file.bufferedReader().use { it.readLine().orEmpty() } }.getOrDefault("")
        if (!first.startsWith("#!")) return
        val words = first.removePrefix("#!").trim().split(Regex("\\s+")).filter(String::isNotBlank)
        val interpreter = if (words.firstOrNull()?.substringAfterLast('/') == "env") {
            words.drop(1).firstOrNull { !it.startsWith("-") }?.substringAfterLast('/')
        } else {
            words.firstOrNull()?.substringAfterLast('/')
        }
        require(interpreter.isNullOrBlank() || interpreter in SHELLS) {
            "Unsupported stored maintainer-script interpreter '$interpreter' in ${file.name}"
        }
    }

    private fun infoDir(context: Context) = File(DistroManager.prefixDir(context), "var/lib/dpkg/info")
    private fun launcher(context: Context) = File(context.applicationInfo.nativeLibraryDir, "libhg2gui_maintscript.so")
    private fun bodyFile(entry: File) = File(entry.parentFile, entry.name + ".hg2body")
}
