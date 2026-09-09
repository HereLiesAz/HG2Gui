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

    fun migrateInstalled(context: Context) {
        val infoDir = infoDir(context)
        val launcher = launcher(context)
        if (!infoDir.isDirectory || !launcher.canExecute()) return
        infoDir.listFiles().orEmpty().forEach { entry ->
            val scriptName = entry.name.substringAfterLast('.', missingDelimiterValue = "")
            if (scriptName !in SCRIPT_NAMES) return@forEach
            if (Files.isSymbolicLink(entry.toPath())) return@forEach
            if (!entry.isFile) return@forEach
            installAtomic(entry, entry, launcher)
        }
    }

    fun stage(context: Context, packageName: String, destinationDir: File) {
        val info = infoDir(context)
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
        if (!scriptsDir.isDirectory) return
        val info = infoDir(context)
        val launcher = launcher(context)
        require(launcher.canExecute()) { "HG2Gui maintainer-script trampoline is unavailable" }
        info.mkdirs()
        SCRIPT_NAMES.forEach { scriptName ->
            val source = File(scriptsDir, scriptName)
            if (!source.isFile) return@forEach
            installAtomic(source, File(info, "$packageName.$scriptName"), launcher)
        }
    }

    fun removeEntries(context: Context, packageName: String) {
        val info = infoDir(context)
        SCRIPT_NAMES.forEach { scriptName ->
            val entry = File(info, "$packageName.$scriptName")
            Files.deleteIfExists(entry.toPath())
            Files.deleteIfExists(bodyFile(entry).toPath())
        }
    }

    fun hasEntry(context: Context, packageName: String, scriptName: String): Boolean {
        val entry = File(infoDir(context), "$packageName.$scriptName")
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

    private fun infoDir(context: Context) = File(DistroManager.prefixDir(context), "var/lib/dpkg/info")
    private fun launcher(context: Context) = File(context.applicationInfo.nativeLibraryDir, "libhg2gui_maintscript.so")
    private fun bodyFile(entry: File) = File(entry.parentFile, entry.name + ".hg2body")
}
