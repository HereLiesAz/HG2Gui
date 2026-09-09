package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File

/** Shared Termux execution environment for HG2Gui's target-SDK-37 process. */
object Hg2ExecEnvironment {
    private const val PRELOAD_LIBRARY = "libhg2gui_exec_preload.so"

    fun preloadLibrary(context: Context): File =
        File(context.applicationInfo.nativeLibraryDir, PRELOAD_LIBRARY)

    fun apply(
        context: Context,
        env: MutableMap<String, String>,
        home: File = DistroManager.homeDir(context)
    ) {
        val prefix = DistroManager.prefixDir(context)
        val preload = preloadLibrary(context)
        require(preload.isFile) {
            "HG2Gui exec preload is unavailable at ${preload.absolutePath}"
        }

        env["HOME"] = home.absolutePath
        env["PREFIX"] = prefix.absolutePath
        env["TERMUX__PREFIX"] = prefix.absolutePath
        env["TERMUX__ROOTFS"] = context.filesDir.absolutePath
        env["TERMUX__HOME"] = home.absolutePath
        env["TERMUX_APP__PACKAGE_NAME"] = context.packageName
        env["LD_PRELOAD"] = preload.absolutePath
    }
}
