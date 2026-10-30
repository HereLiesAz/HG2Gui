package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File

/** Shared Termux execution environment for HG2Gui's target-SDK-37 process. */
object Hg2ExecEnvironment {
    private const val HG2GUI_PRELOAD_LIBRARY = "libhg2gui_exec_preload.so"
    private const val TERMUX_PRELOAD_PATH = "lib/libtermux-exec-ld-preload.so"

    /**
     * Prefer the upstream Termux exec interceptor bundled with the bootstrap. It owns the complete
     * exec-family and system-linker workaround semantics needed by target-SDK-29+ apps. The small
     * HG2Gui shim remains an APK-local fallback for a damaged/legacy bootstrap.
     */
    fun preloadLibrary(context: Context): File {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val bundledTermuxName = BootstrapManifest.ENTRIES
            .firstOrNull { (path, _) -> path == TERMUX_PRELOAD_PATH }
            ?.second
        val bundledTermux = bundledTermuxName?.let { File(nativeDir, it) }
        if (bundledTermux?.isFile == true) return bundledTermux
        return File(nativeDir, HG2GUI_PRELOAD_LIBRARY)
    }

    /**
     * Fills [env] with the Termux exec environment. Throws [IllegalArgumentException] when the
     * exec preload or PRoot temp directory is unavailable: package operations surface that as
     * their error, while interactive session creation (ShellSession.bootstrapBashEnv) catches it
     * and falls back to the bare system shell instead of failing to open a tab.
     *
     * [home] becomes `HOME` and `TERMUX__HOME`: it is the user's home directory, never a working
     * directory. A process's start directory is passed to the spawn call, not through here.
     *
     * Callers that set `PATH` before calling this with the prefix's `bin` first (the interactive and
     * full-screen shells, PRoot compatibility runs) get [DisableShims]' directory prepended to it;
     * Hg2PackageManager sets PATH afterwards, so the engine's own dpkg/maintainer-script runs never
     * see the shims.
     */
    fun apply(
        context: Context,
        env: MutableMap<String, String>,
        home: File = DistroManager.homeDir(context)
    ) {
        val prefix = DistroManager.prefixDir(context)
        val preload = preloadLibrary(context)
        require(preload.isFile) {
            "Termux exec preload is unavailable at ${preload.absolutePath}"
        }

        // Termux's PRoot package is compiled with Termux's own /data/data/com.termux/.../tmp path
        // as its fallback. That directory cannot exist for HG2Gui, so every PRoot invocation must
        // inherit an explicit writable host-side temp directory before it attempts to build its
        // glue rootfs.
        val prootTmp = File(context.filesDir, "tmp/proot")
        require(prootTmp.isDirectory || prootTmp.mkdirs()) {
            "Cannot create PRoot temp directory at ${prootTmp.absolutePath}"
        }

        val dataDir = context.applicationInfo.dataDir
        env["HOME"] = home.absolutePath
        env["PREFIX"] = prefix.absolutePath
        env["TERMUX__PREFIX"] = prefix.absolutePath
        env["TERMUX__ROOTFS"] = context.filesDir.absolutePath
        env["TERMUX__HOME"] = home.absolutePath
        env["TERMUX_APP__PACKAGE_NAME"] = context.packageName
        env["TERMUX_APP__DATA_DIR"] = dataDir
        env["TERMUX_APP__LEGACY_DATA_DIR"] = "/data/data/${context.packageName}"
        env["TERMUX_EXEC__EXECVE_CALL__INTERCEPT"] = "enable"
        env["TERMUX_EXEC__SYSTEM_LINKER_EXEC__MODE"] = "force"
        env["PROOT_TMP_DIR"] = prootTmp.absolutePath
        env["LD_PRELOAD"] = preload.absolutePath
        // A shell environment (PATH starting with the prefix's bin) also gets the Disable /
        // dpkg-guard shim directory in front; any other PATH is left exactly as it is.
        DisableShims.applyToPath(context, env)
    }
}
