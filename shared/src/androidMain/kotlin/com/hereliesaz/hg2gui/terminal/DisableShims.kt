package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicLong

/**
 * PATH-level enforcement of "Disable" (and the direct-`dpkg` guard) for the shells the engine starts.
 *
 * The line planner only sees command words that are written in the line. A package binary reached
 * through `$tool`, `$(which tool)`, a shell function or alias, or a script that calls it is invisible
 * to it. So for every package the user disabled, this object keeps one tiny `sh` script per binary
 * name in [directoryFor] (a directory beside the prefix, owned by no package) that prints
 * `<binary> is disabled (hg2package enable <manager> <package>)` to stderr and exits 126, and
 * [applyToPath] puts that directory FIRST on PATH for every shell session. Because the PATH string
 * is constant, the shims can be added and removed while a shell is running; only the shell's own
 * command-location cache needs a `hash -r` ([REHASH_COMMAND]) after a change.
 *
 * Rules:
 *  - a shim exists only for a binary whose owning packages are ALL disabled, so a binary shared with
 *    an enabled package, and every core tool of an enabled package, is never shadowed;
 *  - names that are not plain command names ([PrefixRuntimeConfig.isSafeScriptName]) get no shim;
 *  - one more permanent shim, `dpkg`, refuses mutating dpkg actions ([PrefixRuntimeConfig.dpkgGuardFunction])
 *    and passes everything else to the APK's dpkg launcher. A disabled `dpkg` package replaces it;
 *  - only files carrying [MARKER] are ever deleted from the directory.
 *
 * The engine itself never goes through these shims: [Hg2PackageManager] runs the dpkg launcher by
 * absolute path with `PATH=$PREFIX/bin:/system/bin`, and apt reaches dpkg through `Dir::Bin::dpkg`.
 * Absolute-path invocation (`$PREFIX/bin/tool`) is not covered.
 */
object DisableShims {
    const val DIRECTORY_NAME = "hg2gui-shims"

    /** Present on line 2 of every shim; the only files [syncDirectory] will delete. */
    const val MARKER = "# hg2gui-shim"

    /** Silently drops the shell's cached command locations so a new or removed shim takes effect. */
    const val REHASH_COMMAND = "type hash >/dev/null 2>&1 && hash -r"

    private const val INTERPRETER = "/system/bin/sh"
    private const val TEMP_SUFFIX = "~tmp"
    private const val EXIT_DISABLED = 126

    private val changeCounter = AtomicLong()
    private var lastApplied: Map<String, String>? = null

    /** Incremented every time a sync changed the directory; a shell that saw an older value must rehash. */
    fun generation(): Long = changeCounter.get()

    /** The shim directory for the prefix at [prefix]: a sibling of it, not inside any package-owned tree. */
    fun directoryFor(prefix: File): File {
        val absolute = prefix.absoluteFile
        return File(absolute.parentFile ?: absolute, DIRECTORY_NAME)
    }

    fun directory(context: Context): File = directoryFor(DistroManager.prefixDir(context))

    /**
     * [path] with the shim directory in front, but only for a PATH that starts with the prefix's
     * own `bin` (the shell environment ShellSession builds). Any other PATH is returned unchanged.
     */
    internal fun pathWithShims(path: String?, prefix: File, shimDir: File): String? {
        if (path == null) return null
        val first = path.substringBefore(':')
        if (first != File(prefix, "bin").absolutePath) return path
        return shimDir.absolutePath + ":" + path
    }

    /**
     * Called by [Hg2ExecEnvironment.apply]: prepends the shim directory to `env["PATH"]` (see
     * [pathWithShims]) and brings its contents up to date. Never throws.
     */
    fun applyToPath(context: Context, env: MutableMap<String, String>) {
        try {
            val prefix = DistroManager.prefixDir(context)
            val updated = pathWithShims(env["PATH"], prefix, directoryFor(prefix)) ?: return
            if (updated == env["PATH"]) return
            sync(context)
            env["PATH"] = updated
        } catch (e: Exception) {
            android.util.Log.w("DisableShims", "Could not add the shim directory to PATH", e)
        }
    }

    /**
     * Makes the shim directory match the packages installed now. Cheap when nothing changed (the
     * installed list is cached by [PackageLifecycleStore]). Returns true when files changed.
     * Never throws: a failure is logged and the previous shims stay in place.
     */
    @Synchronized
    fun sync(context: Context): Boolean {
        return try {
            val prefix = DistroManager.prefixDir(context)
            if (!prefix.isDirectory) return false
            val launcher = File(context.applicationInfo.nativeLibraryDir, PrefixRuntimeConfig.DPKG_LAUNCHER).absolutePath
            val desired = desiredShims(PackageLifecycleStore.installed(context), launcher)
            val dir = directoryFor(prefix)
            val applied = lastApplied
            if (applied == desired && dir.list()?.size == desired.size) return false
            val changed = syncDirectory(dir, desired)
            lastApplied = desired
            changed
        } catch (e: Exception) {
            android.util.Log.w("DisableShims", "Could not update the shim directory", e)
            lastApplied = null
            false
        }
    }

    /**
     * The shims that should exist: binary name -> script text. Pure.
     *
     * A binary is shimmed only when every package listing it is disabled. The message names the
     * alphabetically first owner (manager, then name) so it is stable.
     */
    internal fun desiredShims(
        packages: List<PackageLifecycleStore.InstalledPackage>,
        dpkgLauncher: String
    ): Map<String, String> {
        val owners = HashMap<String, MutableList<PackageLifecycleStore.InstalledPackage>>()
        for (pkg in packages) {
            for (binary in pkg.binaries) {
                val bare = binary.substringAfterLast('/')
                if (!PrefixRuntimeConfig.isSafeScriptName(bare)) continue
                owners.getOrPut(bare) { mutableListOf() }.add(pkg)
            }
        }
        val result = java.util.TreeMap<String, String>()
        result["dpkg"] = dpkgGuardScript(dpkgLauncher)
        for ((binary, list) in owners) {
            if (list.isEmpty() || !list.all { it.disabled }) continue
            val first = list.sortedWith(compareBy<PackageLifecycleStore.InstalledPackage>({ it.manager }, { it.name })).first()
            result[binary] = disabledScript(binary, first)
        }
        return result
    }

    internal fun disabledScript(binary: String, owner: PackageLifecycleStore.InstalledPackage): String {
        val message = "$binary is disabled (hg2package enable ${owner.manager} ${owner.name})"
        return buildString {
            append("#!").append(INTERPRETER).append('\n')
            append(MARKER).append(" disabled\n")
            append("printf '%s\\n' ").append(ShellCommandProtocol.posixQuote(printable(message))).append(" >&2\n")
            append("exit ").append(EXIT_DISABLED).append('\n')
        }
    }

    internal fun dpkgGuardScript(dpkgLauncher: String): String = buildString {
        append("#!").append(INTERPRETER).append('\n')
        append(MARKER).append(" dpkg-guard\n")
        append(PrefixRuntimeConfig.dpkgGuardFunction())
        append("_hg2gui_dpkg_guard \"\$@\" || exit \$?\n")
        append("exec ").append(PrefixRuntimeConfig.shellQuoted(dpkgLauncher)).append(" \"\$@\"\n")
    }

    /** [text] with control characters replaced by spaces, so a hostile package name cannot add lines. */
    private fun printable(text: String): String =
        text.map { if (it.code < 0x20 || it.code == 0x7f) ' ' else it }.joinToString("")

    /**
     * Makes [dir] contain exactly [desired] among the files this object owns: writes missing or
     * changed shims (executable, atomically), deletes marked files that are no longer wanted and
     * leftover temp files. Files without [MARKER] are left alone. Returns true if anything changed;
     * the first I/O failure is rethrown after every file was attempted.
     */
    internal fun syncDirectory(dir: File, desired: Map<String, String>): Boolean {
        dir.mkdirs()
        var changed = false
        var failure: IOException? = null

        for (file in dir.listFiles().orEmpty()) {
            if (file.name in desired) continue
            if (file.name.endsWith(TEMP_SUFFIX) || isOurs(file)) {
                if (file.delete()) changed = true else failure = failure ?: IOException("Cannot delete ${file.path}")
            }
        }
        for ((name, body) in desired) {
            val target = File(dir, name)
            val bytes = body.toByteArray(Charsets.UTF_8)
            try {
                if (Files.isSymbolicLink(target.toPath())) target.delete()
                if (target.isFile && target.canExecute() && target.readBytes().contentEquals(bytes)) continue
                writeExecutable(target, bytes)
                changed = true
            } catch (e: IOException) {
                failure = failure ?: e
            }
        }
        if (changed) changeCounter.incrementAndGet()
        failure?.let { throw it }
        return changed
    }

    private fun isOurs(file: File): Boolean {
        if (!file.isFile || Files.isSymbolicLink(file.toPath()) || file.length() > MAX_SHIM_BYTES) return false
        return try {
            file.readText(Charsets.UTF_8).lineSequence().drop(1).firstOrNull()?.startsWith(MARKER) == true
        } catch (_: IOException) {
            false
        }
    }

    private const val MAX_SHIM_BYTES = 16 * 1024L

    private fun writeExecutable(target: File, bytes: ByteArray) {
        val temp = File(target.parentFile, target.name + TEMP_SUFFIX)
        try {
            FileOutputStream(temp).use { out ->
                out.write(bytes)
                out.flush()
                out.fd.sync()
            }
            temp.setReadable(true, false)
            temp.setExecutable(true, false)
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
