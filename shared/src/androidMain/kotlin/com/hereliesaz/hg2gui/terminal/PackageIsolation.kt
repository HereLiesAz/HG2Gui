package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.util.AtomicFile
import android.util.Base64
import java.io.File
import java.nio.file.Files

/** Filesystem isolation and best-effort runtime observability for package-owned commands. */
object PackageIsolation {
    private const val TELEMETRY_PREFIX = "__HG2GUI_TELEMETRY__/"
    private const val AUDIT_RELATIVE = ".hg2gui/audit/latest.log"
    private const val LEGACY_SUMMARY_RELATIVE = ".hg2gui/audit/summary.tsv"
    private const val SUMMARY_RELATIVE = "audit/summary.tsv"
    private const val MAX_TELEMETRY_ROWS = 500

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

    class Snapshot internal constructor(
        internal val packageKey: String,
        internal val summaryFile: File,
        internal val files: Map<String, FileStamp>,
        internal val telemetry: List<String>
    )

    private fun packageBase(context: Context, pkg: PackageLifecycleStore.InstalledPackage): File =
        File(context.filesDir, "package-isolation/${safeKey(pkg.key)}")

    fun root(context: Context, pkg: PackageLifecycleStore.InstalledPackage): File =
        File(packageBase(context, pkg), "root")

    fun marker(context: Context, pkg: PackageLifecycleStore.InstalledPackage): File =
        File(root(context, pkg), ".hg2gui-seeded")

    private fun summaryFile(context: Context, pkg: PackageLifecycleStore.InstalledPackage): File =
        File(packageBase(context, pkg), SUMMARY_RELATIVE)

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
        val base = packageBase(context, pkg)
        val root = root(context, pkg)
        val hostPrefix = DistroManager.prefixDir(context)
        val hostHome = DistroManager.homeDir(context)
        val guestPrefix = File(root, hostPrefix.absolutePath.trimStart('/'))
        val guestHome = File(root, hostHome.absolutePath.trimStart('/'))
        val marker = marker(context, pkg)
        val denied = File(root, ".hg2gui/denied")
        val auditHost = File(root, AUDIT_RELATIVE)
        val plan = runCatching { IsolationSeedPlanner.prepare(context, pkg, base) }.getOrNull()

        val privilegedNames = listOf("adb", "su", "tsu", "magisk", "proot")
            .joinToString(" ") { q(File(guestPrefix, "bin/$it").absolutePath) }
        val seedCopy = if (plan?.manifest?.isFile == true) {
            val manifest = plan.manifest
            "hp=${q(hostPrefix.absolutePath)}; gp=${q(guestPrefix.absolutePath)}; " +
                "while IFS= read -r src; do " +
                "rel=\"\${src#\"\$hp\"/}\"; [ \"\$rel\" != \"\$src\" ] || continue; " +
                "dst=\"\$gp/\$rel\"; mkdir -p \"\$(dirname \"\$dst\")\"; " +
                "cp -a \"\$src\" \"\$dst\" || exit 1; done < ${q(manifest.absolutePath)}"
        } else {
            "cp -a ${q(hostPrefix.absolutePath + "/.")} ${q(guestPrefix.absolutePath + "/")}"
        }

        return listOf(
            "rm -rf ${q(root.absolutePath)}",
            "mkdir -p ${q(guestPrefix.absolutePath)} ${q(guestHome.absolutePath)} ${q(denied.parentFile!!.absolutePath)} ${q(auditHost.parentFile!!.absolutePath)}",
            seedCopy,
            "rm -f $privilegedNames",
            ": > ${q(denied.absolutePath)}",
            ": > ${q(auditHost.absolutePath)}",
            "mkdir -p ${q(File(guestHome, ".cache").absolutePath)} ${q(File(guestHome, ".config").absolutePath)} ${q(File(guestHome, ".local/share").absolutePath)} ${q(File(guestHome, ".local/state").absolutePath)} ${q(File(guestHome, ".tmp").absolutePath)}",
            "printf '%s\\n' ${q(pkg.version)} > ${q(marker.absolutePath)}"
        ).joinToString(" && ")
    }

    fun command(context: Context, pkg: PackageLifecycleStore.InstalledPackage, original: String): String {
        val network = PackageCapabilityPolicy.decide(
            context,
            pkg.key,
            PackageCapabilityPolicy.Capability.NETWORK
        )
        if (!network.allowed) {
            error(
                "Network policy blocked ${pkg.name}'s isolated launch (${network.reason}). " +
                    "This Android/PRoot boundary cannot run a process in a truthful offline network namespace, " +
                    "so HG2Gui fails closed. Arm Authority → Package policies → ${pkg.name} → network → Allow once, then run again."
            )
        }

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
     * Wraps the child with a same-UID /proc sampler. This is not kernel audit or syscall tracing:
     * very short-lived opens can escape a sample. Every observation is normalized to one physical
     * log row before the hard row limit is enforced.
     */
    private fun monitorScript(original: String, audit: String): String = listOf(
        "audit=${q(audit)}",
        "mkdir -p \"\$(dirname \"\$audit\")\"",
        ": > \"\$audit\"",
        "limit=$MAX_TELEMETRY_ROWS",
        "count=0",
        "truncated=0",
        "hg2_log() {",
        "  local safe",
        "  safe=\"\$(printf '%s' \"\$1\" | tr '\\r\\n' '  ')\"",
        "  grep -Fqx -- \"\$safe\" \"\$audit\" 2>/dev/null && return",
        "  if [ \"\$count\" -ge \"\$((limit - 1))\" ]; then",
        "    if [ \"\$truncated\" -eq 0 ]; then printf 'telemetry-limit:%s\\n' \"\$limit\" >> \"\$audit\"; truncated=1; fi",
        "    return",
        "  fi",
        "  printf '%s\\n' \"\$safe\" >> \"\$audit\"",
        "  count=\$((count + 1))",
        "}",
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
        "exit \"\$code\""
    ).joinToString("\n")

    fun snapshot(context: Context, pkg: PackageLifecycleStore.InstalledPackage): Snapshot {
        val root = root(context, pkg)
        val summary = summaryFile(context, pkg)
        if (!root.isDirectory) return Snapshot(pkg.key, summary, emptyMap(), emptyList())
        val result = LinkedHashMap<String, FileStamp>()
        try {
            root.walkTopDown().forEach { file ->
                if (file == root) return@forEach
                val relative = file.relativeTo(root).path
                if (relative == AUDIT_RELATIVE || relative == LEGACY_SUMMARY_RELATIVE) return@forEach
                result[relative] = FileStamp(
                    size = if (file.isFile) file.length() else 0L,
                    modified = file.lastModified(),
                    directory = file.isDirectory
                )
            }
        } catch (_: Exception) {
            // Keep the partial snapshot; an incomplete audit is preferable to losing the run.
        }
        val telemetry = readGuestTelemetry(root)
        return Snapshot(pkg.key, summary, result, telemetry)
    }

    /** Computes the before/after diff and atomically persists the package's completed-run audit. */
    fun audit(before: Snapshot, after: Snapshot): Audit {
        require(before.packageKey == after.packageKey) { "Cannot compare isolation snapshots from different packages" }
        val created = (after.files.keys - before.files.keys).sorted()
        val deleted = (before.files.keys - after.files.keys).sorted()
        val modified = (before.files.keys intersect after.files.keys).filter { before.files[it] != after.files[it] }.sorted()
        return Audit(created, modified, deleted, after.telemetry).also { saved ->
            saveLatestAudit(after.summaryFile, saved)
        }
    }

    fun saveLatestAudit(context: Context, pkg: PackageLifecycleStore.InstalledPackage, audit: Audit) {
        saveLatestAudit(summaryFile(context, pkg), audit)
    }

    private fun saveLatestAudit(file: File, audit: Audit) {
        val serialized = buildString {
            append("timestamp\t").append(System.currentTimeMillis()).append('\n')
            fun write(kind: String, values: List<String>) {
                values.forEach { value -> append(kind).append('\t').append(encode(value)).append('\n') }
            }
            write("created", audit.created)
            write("modified", audit.modified)
            write("deleted", audit.deleted)
            write("telemetry", audit.telemetry)
        }.toByteArray(Charsets.UTF_8)

        try {
            file.parentFile?.mkdirs()
            val atomic = AtomicFile(file)
            val stream = atomic.startWrite()
            try {
                stream.write(serialized)
                atomic.finishWrite(stream)
            } catch (error: Exception) {
                atomic.failWrite(stream)
                throw error
            }
        } catch (_: Exception) {
            // Audit persistence is observability only; it must never change command exit behavior.
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
        val base = packageBase(context, pkg)
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

    private fun readGuestTelemetry(root: File): List<String> {
        val audit = File(root, AUDIT_RELATIVE)
        if (!audit.isFile) return emptyList()
        return try {
            if (Files.isSymbolicLink(audit.toPath())) return emptyList()
            val rootPath = root.canonicalFile.toPath()
            val auditPath = audit.canonicalFile.toPath()
            if (!auditPath.startsWith(rootPath)) return emptyList()
            audit.useLines { lines -> lines.filter(String::isNotBlank).take(MAX_TELEMETRY_ROWS).toList() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encode(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    private fun decode(value: String): String = String(Base64.decode(value, Base64.DEFAULT), Charsets.UTF_8)
    private fun safeKey(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun q(value: String): String = "'${value.replace("'", "'\\''")}'"
}
