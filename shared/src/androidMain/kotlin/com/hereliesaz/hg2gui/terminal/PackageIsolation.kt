package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File

/**
 * Filesystem isolation for package-owned commands.
 *
 * The private root contains a copy of HG2Gui's runtime at the same absolute guest prefix path,
 * which keeps Termux's absolute paths working while PRoot redirects every write into the copy.
 * Only Android system/APEX/proc paths are exposed from the host; HG2Gui's real prefix and home
 * are never bind-mounted into the guest.
 */
object PackageIsolation {
    data class FileStamp(val size: Long, val modified: Long, val directory: Boolean)

    data class Audit(
        val created: List<String>,
        val modified: List<String>,
        val deleted: List<String>
    ) {
        val changedCount: Int get() = created.size + modified.size + deleted.size
    }

    fun root(context: Context, pkg: PackageLifecycleStore.InstalledPackage): File =
        File(context.filesDir, "package-isolation/${safeKey(pkg.key)}/root")

    fun marker(context: Context, pkg: PackageLifecycleStore.InstalledPackage): File =
        File(root(context, pkg), ".hg2gui-seeded")

    fun isSeeded(context: Context, pkg: PackageLifecycleStore.InstalledPackage): Boolean {
        val marker = marker(context, pkg)
        if (!marker.isFile) return false
        return runCatching { marker.readText().trim() == pkg.version.trim() }.getOrDefault(false)
    }

    fun seedCommand(context: Context, pkg: PackageLifecycleStore.InstalledPackage): String {
        val root = root(context, pkg)
        val hostPrefix = DistroManager.prefixDir(context)
        val hostHome = DistroManager.homeDir(context)
        val guestPrefixHostPath = File(root, hostPrefix.absolutePath.trimStart('/'))
        val guestHomeHostPath = File(root, hostHome.absolutePath.trimStart('/'))
        val marker = marker(context, pkg)

        return listOf(
            "rm -rf ${q(root.absolutePath)}",
            "mkdir -p ${q(guestPrefixHostPath.absolutePath)} ${q(guestHomeHostPath.absolutePath)}",
            "cp -a ${q(hostPrefix.absolutePath + "/.")} ${q(guestPrefixHostPath.absolutePath + "/")}",
            "mkdir -p ${q(File(guestHomeHostPath, ".cache").absolutePath)} ${q(File(guestHomeHostPath, ".config").absolutePath)} ${q(File(guestHomeHostPath, ".local/share").absolutePath)} ${q(File(guestHomeHostPath, ".local/state").absolutePath)} ${q(File(guestHomeHostPath, ".tmp").absolutePath)}",
            "printf '%s\\n' ${q(pkg.version)} > ${q(marker.absolutePath)}"
        ).joinToString(" && ")
    }

    fun command(context: Context, pkg: PackageLifecycleStore.InstalledPackage, original: String): String {
        val proot = File(DistroManager.prefixDir(context), "bin/proot")
        val root = root(context, pkg)
        val prefix = DistroManager.prefixDir(context).absolutePath
        val home = DistroManager.homeDir(context).absolutePath
        val bash = "$prefix/bin/bash"
        val guestTmp = "$home/.tmp"
        val guestConfig = "$home/.config"
        val guestCache = "$home/.cache"
        val guestData = "$home/.local/share"
        val guestState = "$home/.local/state"

        return buildString {
            append(q(proot.absolutePath))
            append(" -r ").append(q(root.absolutePath))
            if (File("/system").exists()) append(" -b /system")
            if (File("/apex").exists()) append(" -b /apex")
            if (File("/proc").exists()) append(" -b /proc")
            append(" -w ").append(q(home))
            append(" /system/bin/env")
            append(" HOME=").append(q(home))
            append(" PREFIX=").append(q(prefix))
            append(" PATH=").append(q("$prefix/bin:/system/bin"))
            append(" LD_LIBRARY_PATH=").append(q("$prefix/lib"))
            append(" TMPDIR=").append(q(guestTmp))
            append(" XDG_CONFIG_HOME=").append(q(guestConfig))
            append(" XDG_CACHE_HOME=").append(q(guestCache))
            append(" XDG_DATA_HOME=").append(q(guestData))
            append(" XDG_STATE_HOME=").append(q(guestState))
            append(' ').append(q(bash)).append(" -lc ").append(q(original))
        }
    }

    fun snapshot(context: Context, pkg: PackageLifecycleStore.InstalledPackage): Map<String, FileStamp> {
        val root = root(context, pkg)
        if (!root.isDirectory) return emptyMap()
        val result = LinkedHashMap<String, FileStamp>()
        try {
            root.walkTopDown().forEach { file ->
                if (file == root) return@forEach
                val relative = file.relativeTo(root).path
                result[relative] = FileStamp(
                    size = if (file.isFile) file.length() else 0L,
                    modified = file.lastModified(),
                    directory = file.isDirectory
                )
            }
        } catch (_: Exception) {
            return result
        }
        return result
    }

    fun audit(before: Map<String, FileStamp>, after: Map<String, FileStamp>): Audit {
        val created = (after.keys - before.keys).sorted()
        val deleted = (before.keys - after.keys).sorted()
        val modified = (before.keys intersect after.keys).filter { before[it] != after[it] }.sorted()
        return Audit(created, modified, deleted)
    }

    fun wipe(context: Context, pkg: PackageLifecycleStore.InstalledPackage): Boolean {
        val base = root(context, pkg).parentFile ?: return true
        return !base.exists() || base.deleteRecursively()
    }

    fun formatAudit(audit: Audit, maxPaths: Int = 40): String {
        if (audit.changedCount == 0) return "Isolation audit: no filesystem changes."
        val lines = mutableListOf<String>()
        lines += "Isolation audit: ${audit.created.size} created, ${audit.modified.size} modified, ${audit.deleted.size} deleted."
        fun add(label: String, paths: List<String>) {
            if (paths.isEmpty() || lines.size >= maxPaths + 1) return
            paths.take((maxPaths + 1 - lines.size).coerceAtLeast(0)).forEach { lines += "$label $it" }
        }
        add("+", audit.created)
        add("~", audit.modified)
        add("-", audit.deleted)
        val shown = lines.size - 1
        if (shown < audit.changedCount) lines += "… ${audit.changedCount - shown} more changed paths"
        return lines.joinToString("\n")
    }

    private fun safeKey(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun q(value: String): String = "'${value.replace("'", "'\\''")}'"
}
