package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File

/**
 * Filesystem STATE SEPARATION and BEST-EFFORT OBSERVATION for package-owned commands.
 *
 * This is deliberately not described as a security boundary. The engine is PRoot: a ptrace-based
 * path translator running as the same Android UID as HG2Gui, not a chroot, namespace or sandbox.
 * What it provides is (a) a private, per-package copy of the prefix and home so a package's writes
 * land in its own tree instead of the shared one, and (b) an after-the-fact record of what changed
 * and what was observed while it ran. It does NOT confine a hostile process: network access is
 * unconfined, and a same-UID process may find ways around path translation. The guest therefore
 * gets no `/proc` bind (raw `/proc` exposes kernel-resolved `/proc/<pid>/root`, `cwd`, `fd` and
 * `environ` links that bypass translation) - only a handful of static informational files.
 *
 * Observation is done by a watcher that runs OUTSIDE the PRoot guest and writes its log outside the
 * guest root, so the observed process cannot rewrite it through normal path access; it samples
 * `/proc` at ~20 Hz, so short-lived opens can still be missed.
 */
object PackageIsolation {
    private const val LEGACY_AUDIT_RELATIVE = ".hg2gui/audit/latest.log"
    private const val LEGACY_SUMMARY_RELATIVE = ".hg2gui/audit/summary.tsv"
    private const val SUMMARY_RELATIVE = "audit/summary.tsv"
    private const val LIVE_LOG_RELATIVE = "audit/live.log"
    private const val MAX_TELEMETRY_ROWS = 500
    private const val MAX_TELEMETRY_ROW_CHARS = 1024

    /** Static, non-PID `/proc` files bound into the guest instead of the whole `/proc`. */
    internal val PROC_ALLOWLIST = listOf(
        "/proc/cpuinfo",
        "/proc/meminfo",
        "/proc/stat",
        "/proc/uptime",
        "/proc/loadavg",
        "/proc/version",
        "/proc/filesystems"
    )

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

    /** Host-side watcher log: next to, not inside, the guest root. */
    internal fun liveLogFile(base: File): File = File(base, LIVE_LOG_RELATIVE)

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
        val hostPrefix = DistroManager.prefixDir(context)
        val hostHome = DistroManager.homeDir(context)
        val plan = runCatching { IsolationSeedPlanner.prepare(hostPrefix, pkg, base) }.getOrNull()
        return buildSeedCommand(base, hostPrefix, hostHome, pkg.version, plan?.manifest)
    }

    /** Pure builder behind [seedCommand]; see PackageIsolationTest. */
    internal fun buildSeedCommand(
        base: File,
        hostPrefix: File,
        hostHome: File,
        version: String,
        manifest: File?
    ): String {
        val root = File(base, "root")
        val guestPrefix = File(root, hostPrefix.absolutePath.trimStart('/'))
        val guestHome = File(root, hostHome.absolutePath.trimStart('/'))
        val marker = File(root, ".hg2gui-seeded")
        val denied = File(root, ".hg2gui/denied")
        val liveLog = liveLogFile(base)

        val privilegedNames = listOf("adb", "su", "tsu", "magisk", "proot")
            .joinToString(" ") { q(File(guestPrefix, "bin/$it").absolutePath) }
        val seedCopy = if (manifest?.isFile == true) {
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
            "mkdir -p ${q(guestPrefix.absolutePath)} ${q(guestHome.absolutePath)} ${q(denied.parentFile!!.absolutePath)} ${q(liveLog.parentFile!!.absolutePath)}",
            seedCopy,
            "rm -f $privilegedNames",
            ": > ${q(denied.absolutePath)}",
            ": > ${q(liveLog.absolutePath)}",
            "mkdir -p ${q(File(guestHome, ".cache").absolutePath)} ${q(File(guestHome, ".config").absolutePath)} ${q(File(guestHome, ".local/share").absolutePath)} ${q(File(guestHome, ".local/state").absolutePath)} ${q(File(guestHome, ".tmp").absolutePath)}",
            "printf '%s\\n' ${q(version)} > ${q(marker.absolutePath)}"
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
        val base = packageBase(context, pkg)
        val prefix = DistroManager.prefixDir(context).absolutePath
        return buildCommand(
            LaunchSpec(
                proot = proot,
                root = root(context, pkg),
                liveLog = liveLogFile(base),
                hostPrefix = prefix,
                hostHome = DistroManager.homeDir(context).absolutePath,
                hostBash = "$prefix/bin/bash",
                original = original,
                bindSystem = File("/system").exists(),
                bindApex = File("/apex").exists(),
                procFiles = PROC_ALLOWLIST.filter { File(it).exists() },
                suPaths = listOf("/system/bin/su", "/system/xbin/su").filter { File(it).exists() }
            )
        )
    }

    /** Everything [buildCommand] needs, so the command shape can be asserted without a device. */
    internal class LaunchSpec(
        val proot: File,
        val root: File,
        val liveLog: File,
        val hostPrefix: String,
        val hostHome: String,
        val hostBash: String,
        val original: String,
        val bindSystem: Boolean,
        val bindApex: Boolean,
        val procFiles: List<String>,
        val suPaths: List<String>,
        /** The in-guest `env` used to set the guest environment; overridable only for host tests. */
        val guestEnv: String = "/system/bin/env",
        /** Pause between watcher samples, seconds. */
        val sampleInterval: String = "0.05"
    )

    /**
     * Builds the single shell line that runs [LaunchSpec.original] inside PRoot.
     *
     * The line is `bash -c '<watcher>'`: the watcher script runs on the HOST side, starts PRoot in
     * the background and samples its process tree through the host's own `/proc`, appending rows to
     * [LaunchSpec.liveLog], which lives outside the guest root and is not bound into the guest. The
     * guest never sees `/proc` itself (only the static files in [PROC_ALLOWLIST]).
     */
    internal fun buildCommand(spec: LaunchSpec): String {
        val denied = File(spec.root, ".hg2gui/denied")
        val prootCommand = buildString {
            append(q(spec.proot.absolutePath))
            append(" -r ").append(q(spec.root.absolutePath))
            if (spec.bindSystem) append(" -b /system")
            if (spec.bindApex) append(" -b /apex")
            for (file in spec.procFiles) append(" -b ").append(q(file))
            for (path in spec.suPaths) append(" -b ").append(q("${denied.absolutePath}:$path"))
            append(" -w ").append(q(spec.hostHome))
            append(' ').append(spec.guestEnv)
            append(" HOME=").append(q(spec.hostHome))
            append(" PREFIX=").append(q(spec.hostPrefix))
            append(" PATH=").append(q("${spec.hostPrefix}/bin:/system/bin"))
            append(" LD_LIBRARY_PATH=").append(q("${spec.hostPrefix}/lib"))
            append(" TMPDIR=").append(q("${spec.hostHome}/.tmp"))
            append(" XDG_CONFIG_HOME=").append(q("${spec.hostHome}/.config"))
            append(" XDG_CACHE_HOME=").append(q("${spec.hostHome}/.cache"))
            append(" XDG_DATA_HOME=").append(q("${spec.hostHome}/.local/share"))
            append(" XDG_STATE_HOME=").append(q("${spec.hostHome}/.local/state"))
            append(' ').append(q(spec.hostBash)).append(" -lc ").append(q(spec.original))
        }
        val watcher = watcherScript(prootCommand, spec.proot.absolutePath, spec.root.absolutePath, spec.liveLog.absolutePath, spec.sampleInterval)
        return q(spec.hostBash) + " -c " + q(watcher)
    }

    /**
     * Host-side watcher: runs [prootCommand] in the background and samples its process tree.
     * This is not kernel audit or syscall tracing: very short-lived opens can escape a sample.
     * Every observation is normalized to one physical log row (control characters removed, length
     * capped) before the hard row limit is enforced. File paths under the guest root are shown
     * relative to it, as the guest sees them.
     */
    private fun watcherScript(prootCommand: String, prootBin: String, root: String, audit: String, interval: String): String = listOf(
        "audit=${q(audit)}",
        "root=${q(root)}",
        "proot_bin=${q(prootBin)}",
        "mkdir -p \"\$(dirname \"\$audit\")\"",
        ": > \"\$audit\"",
        "limit=$MAX_TELEMETRY_ROWS",
        "count=0",
        "truncated=0",
        "hg2_log() {",
        "  local safe",
        "  safe=\"\$(printf '%s' \"\$1\" | tr -c '[:print:]' '?')\"",
        "  safe=\"\${safe:0:$MAX_TELEMETRY_ROW_CHARS}\"",
        "  grep -Fqx -- \"\$safe\" \"\$audit\" 2>/dev/null && return",
        "  if [ \"\$count\" -ge \"\$((limit - 1))\" ]; then",
        "    if [ \"\$truncated\" -eq 0 ]; then printf 'telemetry-limit:%s\\n' \"\$limit\" >> \"\$audit\"; truncated=1; fi",
        "    return",
        "  fi",
        "  printf '%s\\n' \"\$safe\" >> \"\$audit\"",
        "  count=\$((count + 1))",
        "}",
        "hg2_scan_pid() {",
        "  local p=\"\$1\" child fd target flags mode inode proto row shown tracer=0",
        "  [ -r \"/proc/\$p/cmdline\" ] || return",
        "  local cmd=\"\$(tr '\\000' ' ' < \"/proc/\$p/cmdline\" 2>/dev/null)\"",
        "  case \"\$cmd\" in \"\$proot_bin \"*) tracer=1 ;; esac",
        // The tracer itself is infrastructure: its fds are PRoot's own bookkeeping, not the package's.
        "  if [ \"\$tracer\" -eq 0 ]; then",
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
        "      shown=\"\$target\"",
        "      case \"\$target\" in \"\$root\"/*) shown=\"\${target#\"\$root\"}\" ;; esac",
        "      hg2_log \"file-\$mode:\$p:\$shown\"",
        "      case \"\$target\" in *\" (deleted)\") hg2_log \"file-deleted-open:\$p:\$shown\" ;; esac",
        "    fi",
        "  done",
        "  fi",
        "  if [ -r \"/proc/\$p/task/\$p/children\" ]; then",
        "    for child in \$(cat \"/proc/\$p/task/\$p/children\" 2>/dev/null); do hg2_scan_pid \"\$child\"; done",
        "  fi",
        "}",
        // A non-interactive shell runs an `&` job with stdin from /dev/null; hand the isolated
        // command the watcher's real stdin (tty/pipe) explicitly so interactive packages and
        // piped input still work while the sampling loop watches it.
        "exec 3<&0",
        "( eval ${q(prootCommand)} ) <&3 3<&- &",
        "main=\$!",
        "exec 3<&-",
        "hg2_log \"root-process:\$main\"",
        "while kill -0 \"\$main\" 2>/dev/null; do hg2_scan_pid \"\$main\"; sleep $interval; done",
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
                if (relative == LEGACY_AUDIT_RELATIVE || relative == LEGACY_SUMMARY_RELATIVE) return@forEach
                result[relative] = FileStamp(
                    size = if (file.isFile) file.length() else 0L,
                    modified = file.lastModified(),
                    directory = file.isDirectory
                )
            }
        } catch (_: Exception) {
            // Keep the partial snapshot; an incomplete audit is preferable to losing the run.
        }
        val telemetry = readObservedTelemetry(liveLogFile(packageBase(context, pkg)))
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
            AtomicFiles.write(file, serialized)
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
        lines += "Isolation audit: ${audit.created.size} created, ${audit.modified.size} modified, ${audit.deleted.size} deleted (host-diffed); " +
            "${audit.telemetry.size} sampled runtime observations (best-effort, not tamper-proof)."
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

    /**
     * Reads the host-side watcher's log. The file sits outside the guest root, but its content is
     * still treated as untrusted display text: control characters (terminal escape injection) are
     * replaced, each row is length-capped and the row count is bounded.
     */
    internal fun readObservedTelemetry(log: File): List<String> {
        if (!log.isFile) return emptyList()
        return try {
            log.useLines { lines ->
                lines.filter(String::isNotBlank)
                    .take(MAX_TELEMETRY_ROWS)
                    .map { sanitizeTelemetryRow(it) }
                    .toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun sanitizeTelemetryRow(row: String): String =
        buildString {
            for (c in row.take(MAX_TELEMETRY_ROW_CHARS)) append(if (c.isISOControl()) '?' else c)
        }

    private fun encode(value: String): String = PlainBase64.encodeString(value)
    private fun decode(value: String): String = PlainBase64.decodeString(value)
    private fun safeKey(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun q(value: String): String = "'${value.replace("'", "'\\''")}'"
}
