package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.util.Base64
import java.io.File

/** Filesystem isolation and best-effort runtime observability for package-owned commands. */
object PackageIsolation {
    private const val TELEMETRY_PREFIX = "__HG2GUI_TELEMETRY__/"
    private const val AUDIT_RELATIVE = ".hg2gui/audit/latest.log"
    private const val SUMMARY_RELATIVE = ".hg2gui/audit/summary.tsv"

    data class FileStamp(val size: Long, val modified: Long, val directory: Boolean)

    data class Audit(
        val created: List<String>,
        val modified: List<String>,
        val deleted: List<String>,
        val telemetry: List<String> = emptyList()
    ) {
        val changedCount: Int get() = created.size + modified.size + deleted.size
    }

    data class SavedAudit(val timestampMillis: Long, val audit: Audit)

    fun root(context: Context, pkg: PackageLifecycleStore.InstalledPackage): File =
        File(context.filesDir, "package-isolation/${safeKey(pkg.key)}/root")

    fun marker(context: Context, pkg: PackageLifecycleStore.InstalledPackage): File =
        File(root(context, pkg), ".hg2gui-seeded")

    private fun summaryFile(context: Context, pkg: PackageLifecycleStore.InstalledPackage): File =
        File(root(context, pkg), SUMMARY_RELATIVE)

    fun engine(context: Context): File? {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val prefix = DistroManager.prefixDir(context)
        return listOf(
            File(nativeDir, "libhg2gui_proot.so"),
            File(nativeDir, "libbin_proot.so"),
            File(prefix, "bin/proot")
        ).firstOrNull { file ->
            try { file.isFile && file.canExecute() } catch (_: SecurityException) { false }
        }
    }

    fun isSeeded(context: Context, pkg: PackageLifecycleStore.InstalledPackage): Boolean {
        val marker = marker(context, pkg)
        if (!marker.isFile) return false
        return runCatching { marker.readText().trim() == pkg.version.trim() }.getOrDefault(false)
    }

    fun seedCommand(context: Context, pkg: PackageLifecycleStore.InstalledPackage): String {
        val root = root(context, pkg)
        val hostPrefix = DistroManager.prefixDir(context)
        val hostHome = DistroManager.homeDir(context)
        val guestPrefix = File(root, hostPrefix.absolutePath.trimStart('/'))
        val guestHome = File(root, hostHome.absolutePath.trimStart('/'))
        val marker = marker(context, pkg)
        val denied = File(root, ".hg2gui/denied")
        val auditHost = File(root, AUDIT_RELATIVE)

        val privilegedNames = listOf("adb", "su", "tsu", "magisk", "proot")
            .joinToString(" ") { q(File(guestPrefix, "bin/$it").absolutePath) }

        return listOf(
            "rm -rf ${q(root.absolutePath)}",
            "mkdir -p ${q(guestPrefix.absolutePath)} ${q(guestHome.absolutePath)} ${q(denied.parentFile!!.absolutePath)} ${q(auditHost.parentFile!!.absolutePath)}",
            "cp -a ${q(hostPrefix.absolutePath + "/.")} ${q(guestPrefix.absolutePath + "/")}",
            "rm -f $privilegedNames",
            ": > ${q(denied.absolutePath)}",
            ": > ${q(auditHost.absolutePath)}",
            "mkdir -p ${q(File(guestHome, ".cache").absolutePath)} ${q(File(guestHome, ".config").absolutePath)} ${q(File(guestHome, ".local/share").absolutePath)} ${q(File(guestHome, ".local/state").absolutePath)} ${q(File(guestHome, ".tmp").absolutePath)}",
            "printf '%s\\n' ${q(pkg.version)} > ${q(marker.absolutePath)}"
        ).joinToString(" && ")
    }

    fun command(context: Context, pkg: PackageLifecycleStore.InstalledPackage, original: String): String {
        val proot = engine(context)
            ?: error("Isolation engine is unavailable. Install the proot package before isolating packages.")
        val root = root(context, pkg)
        val denied = File(root, ".hg2gui/denied")
        val prefix = DistroManager.prefixDir(context).absolutePath
        val home = DistroManager.homeDir(context).absolutePath
        val bash = "$prefix/bin/bash"
        val guestAudit = "/$AUDIT_RELATIVE"
        val monitored = monitorScript(original, guestAudit)

        return buildString {
            append(q(proot.absolutePath))
            append(" -r ").append(q(root.absolutePath))
            if (File("/system").exists()) append(" -b /system")
            if (File("/apex").exists()) append(" -b /apex")
            if (File("/proc").exists()) append(" -b /proc")
            for (path in listOf("/system/bin/su", "/system/xbin/su")) {
                if (File(path).exists()) append(" -b ").append(q("${denied.absolutePath}:$path"))
            }
            append(" -w ").append(q(home))
            append(" /system/bin/env")
            append(" HOME=").append(q(home))
            append(" PREFIX=").append(q(prefix))
            append(" PATH=").append(q("$prefix/bin:/system/bin"))
            append(" LD_LIBRARY_PATH=").append(q("$prefix/lib"))
            append(" TMPDIR=").append(q("$home/.tmp"))
            append(" XDG_CONFIG_HOME=").append(q("$home/.config"))
            append(" XDG_CACHE_HOME=").append(q("$home/.cache"))
            append(" XDG_DATA_HOME=").append(q("$home/.local/share"))
            append(" XDG_STATE_HOME=").append(q("$home/.local/state"))
            append(' ').append(q(bash)).append(" -lc ").append(q(monitored))
        }
    }

    /**
     * Wraps the child with a same-UID /proc sampler. This does not pretend to be kernel audit or
     * syscall tracing: very short-lived opens can escape a sample. It does, however, report real
     * observed process descendants, their live file descriptors/open modes, socket endpoints and
     * privilege-tool attempts without granting the child any extra authority.
     */
    private fun monitorScript(original: String, audit: String): String = listOf(
        "audit=${q(audit)}",
        "mkdir -p \"\$(dirname \"\$audit\")\"",
        ": > \"\$audit\"",
        "seen=\"\$audit.seen\"",
        ": > \"\$seen\"",
        "hg2_log() { grep -Fqx -- \"\$1\" \"\$seen\" 2>/dev/null || { printf '%s\\n' \"\$1\" >> \"\$seen\"; printf '%s\\n' \"\$1\" >> \"\$audit\"; }; }",
        "hg2_scan_pid() {",
        "  local p=\"\$1\" child fd target flags mode inode proto row",
        "  [ -r \"/proc/\$p/cmdline\" ] || return",
        "  local cmd=\"\$(tr '\\000' ' ' < \"/proc/\$p/cmdline\" 2>/dev/null)\"",
        "  local ppid=\"\$(awk '/^PPid:/ {print \$2}' \"/proc/\$p/status\" 2>/dev/null)\"",
        "  hg2_log \"process:\$p:\$ppid:\$cmd\"",
        "  case \"\$cmd\" in *\"/su \"*|*\" su \"*|*\"adb\"*|*\"magisk\"*|*\"tsu\"*) hg2_log \"authority-attempt:\$p:\$cmd\" ;; esac",
        "  for fd in /proc/\$p/fd/*; do",
        "    [ -e \"\$fd\" ] || continue",
        "    target=\"\$(readlink \"\$fd\" 2>/dev/null)\"",
        "    [ -n \"\$target\" ] || continue",
        "    flags=\"\$(awk '/^flags:/ {print \$2}' \"/proc/\$p/fdinfo/\${fd##*/}\" 2>/dev/null)\"",
        "    if [ \"\${target#socket:[}\" != \"\$target\" ]; then",
        "      inode=\"\${target#socket:[}\"; inode=\"\${inode%]}\"",
        "      for proto in tcp tcp6 udp udp6; do",
        "        [ -r \"/proc/\$p/net/\$proto\" ] || continue",
        "        row=\"\$(awk -v i=\"\$inode\" '\$10==i {print \$2 \":\" \$3 \":\" \$4 \":\" \$10; exit}' \"/proc/\$p/net/\$proto\" 2>/dev/null)\"",
        "        [ -n \"\$row\" ] && hg2_log \"network:\$proto:\$row\"",
        "      done",
        "    elif [ \"\${target#/}\" != \"\$target\" ]; then",
        "      mode=unknown",
        "      case \"\$flags\" in *1|*100001|*1000001) mode=write ;; *2|*100002|*1000002) mode=readwrite ;; *) mode=read ;; esac",
        "      hg2_log \"file-\$mode:\$p:\$target\"",
        "      case \"\$target\" in *\" (deleted)\") hg2_log \"file-deleted-open:\$p:\$target\" ;; esac",
        "    fi",
        "  done",
        "  if [ -r \"/proc/\$p/task/\$p/children\" ]; then",
        "    for child in \$(cat \"/proc/\$p/task/\$p/children\" 2>/dev/null); do hg2_scan_pid \"\$child\"; done",
        "  fi",
        "}",
        "( eval ${q(original)} ) &",
        "main=\$!",
        "hg2_log \"root-process:\$main\"",
        "while kill -0 \"\$main\" 2>/dev/null; do hg2_scan_pid \"\$main\"; sleep 0.05; done",
        "hg2_scan_pid \"\$main\"",
        "wait \"\$main\"",
        "code=\$?",
        "rm -f \"\$seen\"",
        "exit \"\$code\""
    ).joinToString("\n")

    fun snapshot(context: Context, pkg: PackageLifecycleStore.InstalledPackage): Map<String, FileStamp> {
        val root = root(context, pkg)
        if (!root.isDirectory) return emptyMap()
        val result = LinkedHashMap<String, FileStamp>()
        try {
            root.walkTopDown().forEach { file ->
                if (file == root) return@forEach
                val relative = file.relativeTo(root).path
                if (relative == AUDIT_RELATIVE || relative == "$AUDIT_RELATIVE.seen" || relative == SUMMARY_RELATIVE) return@forEach
                result[relative] = FileStamp(
                    size = if (file.isFile) file.length() else 0L,
                    modified = file.lastModified(),
                    directory = file.isDirectory
                )
            }
            val audit = File(root, AUDIT_RELATIVE)
            if (audit.isFile) {
                audit.useLines { lines ->
                    lines.filter(String::isNotBlank).take(500).forEachIndexed { index, line ->
                        result[TELEMETRY_PREFIX + index.toString().padStart(4, '0') + "/" + line] = FileStamp(0, 0, false)
                    }
                }
            }
        } catch (_: Exception) {
            return result
        }
        return result
    }

    fun audit(before: Map<String, FileStamp>, after: Map<String, FileStamp>): Audit {
        val telemetry = after.keys.filter { it.startsWith(TELEMETRY_PREFIX) }
            .map { it.substringAfter('/', "") }.sorted()
        val beforeFiles = before.filterKeys { !it.startsWith(TELEMETRY_PREFIX) }
        val afterFiles = after.filterKeys { !it.startsWith(TELEMETRY_PREFIX) }
        val created = (afterFiles.keys - beforeFiles.keys).sorted()
        val deleted = (beforeFiles.keys - afterFiles.keys).sorted()
        val modified = (beforeFiles.keys intersect afterFiles.keys).filter { beforeFiles[it] != afterFiles[it] }.sorted()
        return Audit(created, modified, deleted, telemetry)
    }

    fun saveLatestAudit(context: Context, pkg: PackageLifecycleStore.InstalledPackage, audit: Audit) {
        val file = summaryFile(context, pkg)
        runCatching {
            file.parentFile?.mkdirs()
            file.bufferedWriter().use { out ->
                out.appendLine("timestamp\t${System.currentTimeMillis()}")
                fun write(kind: String, values: List<String>) {
                    values.forEach { value ->
                        out.append(kind).append('\t').appendLine(encode(value))
                    }
                }
                write("created", audit.created)
                write("modified", audit.modified)
                write("deleted", audit.deleted)
                write("telemetry", audit.telemetry)
            }
        }
    }

    fun latestAudit(context: Context, pkg: PackageLifecycleStore.InstalledPackage): SavedAudit? {
        val file = summaryFile(context, pkg)
        if (!file.isFile) return null
        return runCatching {
            var timestamp = 0L
            val created = mutableListOf<String>()
            val modified = mutableListOf<String>()
            val deleted = mutableListOf<String>()
            val telemetry = mutableListOf<String>()
            file.useLines { lines ->
                lines.forEach { line ->
                    val split = line.indexOf('\t')
                    if (split <= 0) return@forEach
                    val kind = line.substring(0, split)
                    val value = line.substring(split + 1)
                    when (kind) {
                        "timestamp" -> timestamp = value.toLongOrNull() ?: 0L
                        "created" -> created += decode(value)
                        "modified" -> modified += decode(value)
                        "deleted" -> deleted += decode(value)
                        "telemetry" -> telemetry += decode(value)
                    }
                }
            }
            SavedAudit(timestamp, Audit(created, modified, deleted, telemetry))
        }.getOrNull()
    }

    fun wipe(context: Context, pkg: PackageLifecycleStore.InstalledPackage): Boolean {
        val base = root(context, pkg).parentFile ?: return true
        return !base.exists() || base.deleteRecursively()
    }

    fun formatAudit(audit: Audit, maxPaths: Int = 40): String {
        val lines = mutableListOf<String>()
        lines += "Isolation audit: ${audit.created.size} created, ${audit.modified.size} modified, ${audit.deleted.size} deleted; ${audit.telemetry.size} runtime observations."
        fun add(label: String, paths: List<String>) {
            if (paths.isEmpty() || lines.size >= maxPaths + 1) return
            paths.take((maxPaths + 1 - lines.size).coerceAtLeast(0)).forEach { lines += "$label $it" }
        }
        add("+", audit.created)
        add("~", audit.modified)
        add("-", audit.deleted)
        add("•", audit.telemetry)
        val total = audit.changedCount + audit.telemetry.size
        val shown = lines.size - 1
        if (shown < total) lines += "… ${total - shown} more audit observations"
        return lines.joinToString("\n")
    }

    private fun encode(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    private fun decode(value: String): String = String(Base64.decode(value, Base64.DEFAULT), Charsets.UTF_8)
    private fun safeKey(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun q(value: String): String = "'${value.replace("'", "'\\''")}'"
}
