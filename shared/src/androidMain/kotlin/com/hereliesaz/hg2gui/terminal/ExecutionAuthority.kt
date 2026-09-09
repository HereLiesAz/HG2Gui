package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File

/**
 * Explicit privilege backends for commands that are intentionally run outside HG2Gui's normal
 * application authority. Merely having adb or su on the device never elevates ordinary commands:
 * callers must opt into this object through the hg2auth command surface.
 */
object ExecutionAuthority {
    data class Availability(
        val adbClient: File?,
        val rootClient: File?
    ) {
        val adbAvailable: Boolean get() = adbClient != null
        val rootAvailable: Boolean get() = rootClient != null
    }

    fun availability(context: Context): Availability = Availability(
        adbClient = findAdb(context),
        rootClient = findSu(context)
    )

    fun adbCommand(context: Context, args: List<String>): String {
        val adb = findAdb(context)
            ?: error("ADB client is not installed. Install the android-tools package first.")
        return (listOf(q(adb.absolutePath)) + args.map(::q)).joinToString(" ")
    }

    fun adbShellCommand(context: Context, command: String): String =
        adbCommand(context, listOf("shell", "sh", "-c", command))

    fun rootCommand(context: Context, command: String): String {
        val su = findSu(context) ?: error("No executable su provider was found on this device.")
        return "${q(su.absolutePath)} -c ${q(command)}"
    }

    fun summary(context: Context): String {
        val availability = availability(context)
        return buildString {
            appendLine("Execution authority")
            appendLine("App: available (default)")
            appendLine("Isolated package: available for packages when PRoot is available")
            appendLine(
                if (availability.adbAvailable) {
                    "ADB shell: client available at ${availability.adbClient!!.absolutePath}"
                } else {
                    "ADB shell: client unavailable — install android-tools, then pair/connect to Wireless Debugging"
                }
            )
            append(
                if (availability.rootAvailable) {
                    "Root: su provider detected at ${availability.rootClient!!.absolutePath} (authorization still requires the device's root manager)"
                } else {
                    "Root: unavailable — no executable su provider detected"
                }
            )
        }
    }

    private fun findAdb(context: Context): File? {
        val prefix = DistroManager.prefixDir(context)
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        return listOf(
            File(nativeDir, "libhg2gui_adb.so"),
            File(nativeDir, "libbin_adb.so"),
            File(prefix, "bin/adb")
        ).firstOrNull(::isExecutableFile)
    }

    private fun findSu(context: Context): File? {
        val prefix = DistroManager.prefixDir(context)
        return listOf(
            File(prefix, "bin/su"),
            File("/system/bin/su"),
            File("/system/xbin/su"),
            File("/sbin/su"),
            File("/debug_ramdisk/su")
        ).firstOrNull(::isExecutableFile)
    }

    private fun isExecutableFile(file: File): Boolean = try {
        file.isFile && file.canExecute()
    } catch (_: SecurityException) {
        false
    }

    private fun q(value: String): String = "'${value.replace("'", "'\\''")}'"
}
