package com.hereliesaz.hg2gui.terminal

import android.content.Context
import androidx.core.content.edit
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import org.json.JSONObject

/** HG2Gui lifecycle state layered over whatever package manager installed the package. */
object PackageLifecycleStore {
    private const val PREFS = "hg2gui_package_lifecycle"
    private const val DISABLED_PREFIX = "disabled:"
    private const val ISOLATED_PREFIX = "isolated:"
    private const val TRACKED_PREFIX = "tracked:"
    private const val OBSERVED_PREFIX = "observed:"
    private const val MAX_PROVENANCE_FILES = 2_000
    private const val MAX_PROVENANCE_ROWS = 1_000

    /** Most paths a package may accumulate in its tracked "created files" set. */
    internal const val MAX_TRACKED_PATHS = 1_000

    /** A cwd snapshot that takes longer than this is abandoned: tracking must never slow a command noticeably. */
    private const val SNAPSHOT_BUDGET_NANOS = 200_000_000L

    data class InstalledPackage(
        val manager: String,
        val managerLabel: String,
        val name: String,
        val version: String,
        val binaries: List<String>,
        val disabled: Boolean,
        val isolated: Boolean
    ) {
        val key: String get() = "$manager:$name"
    }

    data class ResetResult(
        val deletedPaths: Int,
        val deletedBytes: Long,
        val failed: List<String>
    )

    data class ResetPreview(
        val paths: List<String>,
        val bytes: Long,
        val isolated: Boolean
    )

    data class ProvenanceEntry(
        val path: String,
        val change: String
    )

    internal data class RunFileStamp(val size: Long, val modified: Long, val directory: Boolean)

    data class RunSnapshot internal constructor(
        val packageKey: String,
        val directory: File,
        internal val entries: Map<String, RunFileStamp>
    )

    private class InstalledCache(val fingerprint: List<Long>, val packages: List<InstalledPackage>)

    @Volatile
    private var installedCache: InstalledCache? = null

    /**
     * Every installed package across the supported managers. TerminalEngine calls this (via
     * [ownerOfBinary]) for every command line, so the full scan is cached and reused until the
     * dpkg status file or one of the other managers' install roots changes, or a disabled/
     * isolated flag is flipped through this store.
     */
    fun installed(context: Context): List<InstalledPackage> {
        val fingerprint = installFingerprint(context)
        installedCache?.takeIf { it.fingerprint == fingerprint }?.let { return it.packages }
        return scanInstalled(context).also { installedCache = InstalledCache(fingerprint, it) }
    }

    /** mtime of dpkg's status file plus each manager root a package install adds an entry to. */
    private fun installFingerprint(context: Context): List<Long> {
        val prefix = DistroManager.prefixDir(context)
        val home = DistroManager.homeDir(context)
        val roots = buildList {
            add(File(prefix, "var/lib/dpkg/status"))
            File(prefix, "lib").listFiles().orEmpty()
                .filter { it.isDirectory && it.name.startsWith("python") }
                .forEach { add(File(it, "site-packages")) }
            add(File(home, ".local/share/pipx/venvs"))
            add(File(home, ".local/pipx/venvs"))
            val npm = File(prefix, "lib/node_modules")
            add(npm)
            npm.listFiles().orEmpty().filter { it.isDirectory && it.name.startsWith("@") }.forEach(::add)
            File(prefix, "lib/ruby/gems").listFiles().orEmpty().forEach { add(File(it, "specifications")) }
        }
        return roots.flatMap { listOf(it.lastModified(), if (it.isFile) it.length() else 0L) }
    }

    private fun scanInstalled(context: Context): List<InstalledPackage> {
        val prefix = DistroManager.prefixDir(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return buildList {
            addAll(dpkgPackages(prefix, prefs))
            addAll(pipPackages(prefix, prefs))
            addAll(pipxPackages(context, prefs))
            addAll(npmPackages(prefix, prefs))
            addAll(gemPackages(prefix, prefs))
        }.distinctBy { it.key.lowercase() }
            .sortedWith(compareBy<InstalledPackage> { it.managerLabel.lowercase() }.thenBy { it.name.lowercase() })
    }

    fun ownerOfBinary(context: Context, binary: String): InstalledPackage? {
        val bare = binary.substringAfterLast('/')
        return installed(context).firstOrNull { bare in it.binaries }
    }

    fun find(context: Context, manager: String, name: String): InstalledPackage? =
        installed(context).firstOrNull { it.manager == manager && it.name == name }

    fun setDisabled(context: Context, manager: String, name: String, disabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(disabledKey(manager, name), disabled)
        }
        installedCache = null
    }

    fun setIsolated(context: Context, manager: String, name: String, isolated: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(isolatedKey(manager, name), isolated)
        }
        installedCache = null
    }

    fun beginRun(context: Context, pkg: InstalledPackage, cwd: String): RunSnapshot? {
        if (pkg.isolated || cwd.isBlank()) return null
        if (!shouldTrack(pkg, cwd, DistroManager.homeDir(context), DistroManager.prefixDir(context))) return null
        val dir = File(cwd)
        if (!dir.isDirectory) return null
        // A capped walk can't tell "created by this run" from "already there but past the cap",
        // so an incomplete snapshot tracks nothing rather than marking pre-existing files as the
        // package's own (and later offering them to a reset).
        val entries = runSnapshot(dir) ?: return null
        return RunSnapshot(pkg.key, dir, entries)
    }

    /**
     * Whether a run of [pkg] in [cwd] is worth walking the directory for. Base utilities (ls, cat,
     * cp, grep… from coreutils and friends) run constantly and create files on the user's behalf,
     * not the package's, so tracking them only costs two directory walks per command and fills the
     * package's reset set with the user's own files. A cwd of HOME itself or of the runtime prefix
     * is skipped for the same reason: it is the largest tree and the least attributable.
     */
    internal fun shouldTrack(pkg: InstalledPackage, cwd: String, home: File, prefix: File): Boolean {
        if (pkg.manager == "pkg" && pkg.name in UNTRACKED_PACKAGES) return false
        val dir = safeCanonicalPath(File(cwd)) ?: return false
        val homePath = safeCanonicalPath(home)
        val prefixPath = safeCanonicalPath(prefix)
        if (dir == homePath || dir == "/") return false
        if (prefixPath != null && (dir == prefixPath || dir.startsWith("$prefixPath/"))) return false
        return true
    }

    fun finishRun(context: Context, snapshot: RunSnapshot?) {
        if (snapshot == null || !snapshot.directory.isDirectory) return
        val now = runSnapshot(snapshot.directory) ?: return
        val created = now.keys.filter { it !in snapshot.entries }
        val modified = now.keys.filter { path ->
            val before = snapshot.entries[path]
            before != null && before != now[path]
        }
        if (created.isEmpty() && modified.isEmpty()) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val home = DistroManager.homeDir(context)
        val prefix = DistroManager.prefixDir(context)
        // Only the roots of what a run created: a new directory already covers its children.
        val resettableCreated = topLevelOnly(created).map { File(snapshot.directory, it) }
            .filter { isResettablePath(home, prefix, it) }
            .mapNotNull(::safeCanonicalPath)
            .toSet()
        val trackedKey = trackedKey(snapshot.packageKey)
        val observedKey = observedKey(snapshot.packageKey)
        val observed = buildSet {
            created.forEach { add("created\t${File(snapshot.directory, it).absolutePath}") }
            modified.forEach { add("modified\t${File(snapshot.directory, it).absolutePath}") }
        }.toList().takeLast(MAX_PROVENANCE_ROWS).toSet()

        prefs.edit {
            if (resettableCreated.isNotEmpty()) {
                val existing = prefs.getStringSet(trackedKey, emptySet()).orEmpty()
                putStringSet(trackedKey, capTracked(existing, resettableCreated))
            }
            val existing = prefs.getStringSet(observedKey, emptySet()).orEmpty().toList()
            putStringSet(observedKey, (existing + observed).takeLast(MAX_PROVENANCE_ROWS).toSet())
        }
    }

    /** [existing] plus as many of [added] as fit under [MAX_TRACKED_PATHS]; the set never shrinks or overflows. */
    internal fun capTracked(existing: Set<String>, added: Set<String>): Set<String> {
        if (existing.size >= MAX_TRACKED_PATHS) return existing
        val room = MAX_TRACKED_PATHS - existing.size
        return existing + added.filter { it !in existing }.sorted().take(room)
    }

    /** Drops every path that lies inside another path of the list (`a/b` when `a` is present). */
    internal fun topLevelOnly(relativePaths: List<String>): List<String> {
        val kept = mutableListOf<String>()
        for (path in relativePaths.distinct().sortedBy { it.length }) {
            if (kept.none { path.startsWith("$it/") }) kept += path
        }
        return kept
    }

    fun provenance(context: Context, pkg: InstalledPackage): List<ProvenanceEntry> {
        val rows = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(observedKey(pkg.key), emptySet()).orEmpty()
        return rows.mapNotNull { row ->
            val split = row.indexOf('\t')
            if (split <= 0) null else ProvenanceEntry(row.substring(0, split), row.substring(split + 1))
        }.sortedWith(compareBy<ProvenanceEntry> { it.change }.thenBy { it.path })
    }

    fun previewReset(context: Context, pkg: InstalledPackage): ResetPreview {
        if (pkg.isolated) {
            val root = PackageIsolation.root(context, pkg).parentFile
            val paths = root?.takeIf { it.exists() }?.let { listOf(it.absolutePath) }.orEmpty()
            return ResetPreview(paths, root?.takeIf { it.exists() }?.let(::sizeOf) ?: 0L, isolated = true)
        }
        val candidates = resetCandidates(context, pkg).filter { it.exists() }.sortedBy { it.absolutePath }
        return ResetPreview(candidates.map { it.absolutePath }, candidates.sumOf { sizeOf(it) }, isolated = false)
    }

    fun reset(context: Context, pkg: InstalledPackage): ResetResult {
        if (pkg.isolated) {
            val root = PackageIsolation.root(context, pkg).parentFile
            val bytes = root?.takeIf { it.exists() }?.let(::sizeOf) ?: 0L
            val deleted = root == null || !root.exists() || PackageIsolation.wipe(context, pkg)
            return ResetResult(
                deletedPaths = if (deleted && bytes > 0L) 1 else 0,
                deletedBytes = if (deleted) bytes else 0L,
                failed = if (deleted || root == null) emptyList() else listOf(root.absolutePath)
            )
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val home = DistroManager.homeDir(context)
        val prefix = DistroManager.prefixDir(context)
        val candidates = resetCandidates(context, pkg)
        var deletedPaths = 0
        var deletedBytes = 0L
        val failed = mutableListOf<String>()
        candidates.filter { it.exists() || Files.isSymbolicLink(it.toPath()) }
            .sortedByDescending { it.absolutePath.length }
            .forEach { file ->
                // Re-validate against the live filesystem: the preview may be minutes old and a
                // symlink can have been swapped in since. Nothing outside the allowed state dirs is
                // ever deleted, whatever name a package chose.
                if (!isResettablePath(home, prefix, file)) {
                    failed += file.absolutePath
                    return@forEach
                }
                val bytes = sizeOf(file)
                if (deleteTreeNoFollow(file)) {
                    deletedPaths++
                    deletedBytes += bytes
                } else {
                    failed += file.absolutePath
                }
            }
        prefs.edit { remove(trackedKey(pkg.key)) }
        return ResetResult(deletedPaths, deletedBytes, failed)
    }

    /**
     * Deletes [root] and everything under it without ever following a symlink: a link is removed
     * as a link, so a package-created `cache/link -> /storage/emulated/0` cannot make Reset delete
     * the link's target.
     */
    internal fun deleteTreeNoFollow(root: File): Boolean = try {
        Files.walkFileTree(root.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (exc != null) throw exc
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })
        true
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }

    fun updateCommand(pkg: InstalledPackage): String = when (pkg.manager) {
        // `pkg install <name>` is a no-op for an installed package (any installed version
        // satisfies it), so Update must go through the upgrade path, which is the only one that
        // really replaces an older version with the newest in the index.
        "pkg" -> "pkg upgrade ${quote(pkg.name)}"
        "pip" -> "python -m pip install --upgrade ${quote(pkg.name)}"
        "pipx" -> "pipx upgrade ${quote(pkg.name)}"
        "npm" -> "npm update -g ${quote(pkg.name)}"
        "gem" -> "gem update ${quote(pkg.name)}"
        else -> error("Unsupported package manager '${pkg.manager}'")
    }

    fun removeCommand(pkg: InstalledPackage, purge: Boolean = false): String = when (pkg.manager) {
        "pkg" -> "pkg ${if (purge) "purge" else "remove"} ${quote(pkg.name)}"
        "pip" -> "python -m pip uninstall -y ${quote(pkg.name)}"
        "pipx" -> "pipx uninstall ${quote(pkg.name)}"
        "npm" -> "npm uninstall -g ${quote(pkg.name)}"
        "gem" -> "gem uninstall ${quote(pkg.name)} -a -x"
        else -> error("Unsupported package manager '${pkg.manager}'")
    }

    fun infoCommand(pkg: InstalledPackage): String = when (pkg.manager) {
        "pkg" -> "pkg show ${quote(pkg.name)}"
        "pip" -> "python -m pip show ${quote(pkg.name)}"
        "pipx" -> "pipx list --output json"
        "npm" -> "npm list -g ${quote(pkg.name)} --depth=0"
        "gem" -> "gem info ${quote(pkg.name)}"
        else -> error("Unsupported package manager '${pkg.manager}'")
    }

    private fun resetCandidates(context: Context, pkg: InstalledPackage): Set<File> {
        val home = DistroManager.homeDir(context)
        val prefix = DistroManager.prefixDir(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return resetTargets(
            home,
            prefix,
            listOf(pkg.name) + pkg.binaries,
            prefs.getStringSet(trackedKey(pkg.key), emptySet()).orEmpty()
        )
    }

    /**
     * Every path a Reset may delete: the conventional per-name state directories plus tracked
     * paths, each re-checked with [isResettablePath]. Names come from package metadata (npm `bin`
     * keys, pip console scripts, the package name itself) and are untrusted, so a name that is
     * `.`/`..`, holds a separator, or resolves anywhere but directly inside its state directory
     * is dropped here rather than trusted.
     */
    internal fun resetTargets(home: File, prefix: File, rawNames: List<String>, tracked: Set<String>): Set<File> {
        val candidates = linkedSetOf<File>()
        for ((base, file) in conventionalStatePaths(home, prefix, rawNames)) {
            if (isDirectChildOf(base, file) && isResettablePath(home, prefix, file)) candidates += file
        }
        tracked.forEach { path ->
            val file = File(path)
            if (isResettablePath(home, prefix, file)) candidates += file
        }
        return candidates
    }

    /** Stamps of everything under [root], or null when the tree exceeds [MAX_PROVENANCE_FILES]
     *  or can't be fully read - a partial snapshot is not a usable before/after baseline. */
    private fun runSnapshot(root: File): Map<String, RunFileStamp>? {
        val result = linkedMapOf<String, RunFileStamp>()
        val deadline = System.nanoTime() + SNAPSHOT_BUDGET_NANOS
        try {
            var count = 0
            root.walkTopDown().drop(1).forEach { file ->
                if (++count > MAX_PROVENANCE_FILES) return null
                if (count % 64 == 0 && System.nanoTime() > deadline) return null
                val relative = runCatching { file.relativeTo(root).path }.getOrNull() ?: return@forEach
                result[relative] = RunFileStamp(
                    size = if (file.isFile) file.length() else 0L,
                    modified = file.lastModified(),
                    directory = file.isDirectory
                )
            }
        } catch (_: SecurityException) {
            return null
        }
        return result
    }

    private fun dpkgPackages(prefix: File, prefs: android.content.SharedPreferences): List<InstalledPackage> {
        val binaries = DpkgCatalog.binariesByPackage(prefix)
        return DpkgCatalog.installedVersions(prefix).map { (name, version) ->
            packageRecord(prefs, "pkg", "Termux / pkg", name, version, binaries[name].orEmpty().distinct().sorted())
        }
    }

    private fun pipPackages(prefix: File, prefs: android.content.SharedPreferences): List<InstalledPackage> {
        val result = mutableListOf<InstalledPackage>()
        File(prefix, "lib").listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("python") }
            .map { File(it, "site-packages") }
            .filter { it.isDirectory }
            .forEach { site ->
                site.listFiles().orEmpty().filter { it.isDirectory && it.name.endsWith(".dist-info") }.forEach { dist ->
                    val fields = readHeaderFields(File(dist, "METADATA"))
                    val name = fields["Name"] ?: return@forEach
                    result += packageRecord(
                        prefs,
                        "pip",
                        "Python / pip",
                        name,
                        fields["Version"].orEmpty(),
                        parseConsoleScripts(File(dist, "entry_points.txt"))
                    )
                }
            }
        return result
    }

    private fun pipxPackages(context: Context, prefs: android.content.SharedPreferences): List<InstalledPackage> {
        val home = DistroManager.homeDir(context)
        val roots = listOf(
            File(home, ".local/share/pipx/venvs"),
            File(home, ".local/pipx/venvs")
        ).filter { it.isDirectory }

        return roots.flatMap { root ->
            root.listFiles().orEmpty().mapNotNull { venv ->
                val metadata = File(venv, "pipx_metadata.json")
                if (!venv.isDirectory || !metadata.isFile) return@mapNotNull null
                val rootJson = runCatching { JSONObject(metadata.readText()) }.getOrNull() ?: return@mapNotNull null
                val main = rootJson.optJSONObject("main_package") ?: rootJson
                val name = main.optString("package").takeIf { it.isNotBlank() } ?: venv.name
                val version = main.optString("package_version")
                val appsJson = main.optJSONArray("apps")
                val apps = buildList {
                    if (appsJson != null) {
                        for (index in 0 until appsJson.length()) {
                            appsJson.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                }.distinct()
                packageRecord(prefs, "pipx", "Python apps / pipx", name, version, apps)
            }
        }.distinctBy { it.name.lowercase() }
    }

    private fun npmPackages(prefix: File, prefs: android.content.SharedPreferences): List<InstalledPackage> {
        val root = File(prefix, "lib/node_modules")
        if (!root.isDirectory) return emptyList()
        return root.listFiles().orEmpty().flatMap { child ->
            if (child.isDirectory && child.name.startsWith("@")) child.listFiles().orEmpty().toList() else listOf(child)
        }.filter { it.isDirectory && File(it, "package.json").isFile }
            .mapNotNull { dir ->
                val json = runCatching { JSONObject(File(dir, "package.json").readText()) }.getOrNull() ?: return@mapNotNull null
                val name = json.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val bin = json.opt("bin")
                val binaries = when (bin) {
                    is JSONObject -> buildList {
                        val keys = bin.keys()
                        while (keys.hasNext()) add(keys.next())
                    }.distinct().sorted()
                    is String -> listOf(name.substringAfterLast('/'))
                    else -> emptyList()
                }
                packageRecord(
                    prefs,
                    "npm",
                    "Node / npm",
                    name,
                    json.optString("version"),
                    binaries
                )
            }
    }

    private fun gemPackages(prefix: File, prefs: android.content.SharedPreferences): List<InstalledPackage> {
        val root = File(prefix, "lib/ruby/gems")
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown()
            .filter { it.isFile && it.parentFile?.name == "specifications" && it.extension == "gemspec" }
            .mapNotNull { spec ->
                val text = runCatching { spec.readText() }.getOrNull() ?: return@mapNotNull null
                val name = gemAssignment(text, "name") ?: return@mapNotNull null
                val version = gemAssignment(text, "version").orEmpty()
                val executables = gemArray(text, "executables")
                packageRecord(prefs, "gem", "Ruby / gem", name, version, executables)
            }
            .toList()
    }

    private fun packageRecord(
        prefs: android.content.SharedPreferences,
        manager: String,
        managerLabel: String,
        name: String,
        version: String,
        binaries: List<String>
    ) = InstalledPackage(
        manager = manager,
        managerLabel = managerLabel,
        name = name,
        version = version,
        binaries = binaries,
        disabled = prefs.getBoolean(disabledKey(manager, name), false),
        isolated = prefs.getBoolean(isolatedKey(manager, name), false)
    )

    /** True for a name that is safe to use as one path component under a state directory. */
    internal fun isSafeStateName(name: String): Boolean =
        name.length >= 2 && name != "." && name != ".." &&
            name.none { it == '/' || it == '\\' || it == '\u0000' } &&
            name !in STATE_NAME_DENYLIST

    /** (state directory, candidate) pairs for each usable name, lowercased as before. */
    internal fun conventionalStatePaths(home: File, prefix: File, rawNames: List<String>): List<Pair<File, File>> {
        val names = rawNames
            .map { it.substringAfterLast('/').lowercase() }
            .filter(::isSafeStateName)
            .distinct()
        val bases = listOf(
            File(home, ".cache"),
            File(home, ".config"),
            File(home, ".local/share"),
            File(home, ".local/state"),
            File(prefix, "var/cache"),
            File(prefix, "var/log"),
            File(prefix, "var/tmp")
        )
        return buildList {
            for (name in names) for (base in bases) add(base to File(base, name))
        }
    }

    /** [file] sits directly inside [base] once symlinks and `..` are resolved. */
    internal fun isDirectChildOf(base: File, file: File): Boolean {
        val baseCanonical = safeCanonicalPath(base) ?: return false
        val parent = safeCanonicalPath(file.absoluteFile.parentFile ?: return false) ?: return false
        return parent == baseCanonical
    }

    /**
     * Whether Reset may delete [file]: its canonical path must lie strictly inside HOME or inside
     * the prefix's var/cache, var/log or var/tmp - never HOME or those directories themselves -
     * and not inside a directory that holds credentials.
     */
    internal fun isResettablePath(home: File, prefix: File, file: File): Boolean {
        val path = safeCanonicalPath(file) ?: return false
        val homePath = safeCanonicalPath(home) ?: return false
        val prefixPath = safeCanonicalPath(prefix) ?: return false
        if (path.startsWith("$homePath/")) {
            val firstComponent = path.removePrefix("$homePath/").substringBefore('/')
            return firstComponent !in PROTECTED_HOME_DIRS
        }
        return path.startsWith("$prefixPath/var/cache/") ||
            path.startsWith("$prefixPath/var/log/") ||
            path.startsWith("$prefixPath/var/tmp/")
    }

    private fun readHeaderFields(file: File): Map<String, String> {
        if (!file.isFile) return emptyMap()
        return runCatching {
            file.useLines { lines ->
                lines.takeWhile { it.isNotBlank() }.mapNotNull { line ->
                    val index = line.indexOf(':')
                    if (index <= 0) null else line.substring(0, index) to line.substring(index + 1).trim()
                }.toMap()
            }
        }.getOrDefault(emptyMap())
    }

    private fun parseConsoleScripts(file: File): List<String> {
        if (!file.isFile) return emptyList()
        var inConsole = false
        return runCatching {
            buildList {
                file.forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        inConsole = trimmed.equals("[console_scripts]", ignoreCase = true)
                    } else if (inConsole && '=' in trimmed) {
                        trimmed.substringBefore('=').trim().takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun gemAssignment(text: String, field: String): String? {
        val marker = ".$field"
        return text.lineSequence().map(String::trim).firstNotNullOfOrNull { line ->
            val markerIndex = line.indexOf(marker)
            if (markerIndex < 0) return@firstNotNullOfOrNull null
            val equalsIndex = line.indexOf('=', markerIndex + marker.length)
            if (equalsIndex < 0) return@firstNotNullOfOrNull null
            quotedValues(line.substring(equalsIndex + 1)).firstOrNull()
        }
    }

    private fun gemArray(text: String, field: String): List<String> {
        val marker = ".$field"
        val lines = text.lines()
        for (index in lines.indices) {
            val line = lines[index]
            val markerIndex = line.indexOf(marker)
            if (markerIndex < 0) continue
            val equalsIndex = line.indexOf('=', markerIndex + marker.length)
            if (equalsIndex < 0) continue
            val buffer = StringBuilder(line.substring(equalsIndex + 1))
            var cursor = index + 1
            while (']' !in buffer && cursor < lines.size) {
                buffer.append(' ').append(lines[cursor])
                cursor++
            }
            return quotedValues(buffer.toString()).distinct()
        }
        return emptyList()
    }

    private fun quotedValues(text: String): List<String> {
        val result = mutableListOf<String>()
        var index = 0
        while (index < text.length) {
            val quote = text[index]
            if (quote != '\'' && quote != '"') {
                index++
                continue
            }
            val value = StringBuilder()
            index++
            var escaped = false
            while (index < text.length) {
                val ch = text[index++]
                if (escaped) {
                    value.append(ch)
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == quote) {
                    result += value.toString()
                    break
                } else {
                    value.append(ch)
                }
            }
        }
        return result
    }

    private fun sizeOf(file: File): Long = try {
        if (file.isFile) file.length() else file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    } catch (_: SecurityException) {
        0L
    }

    private fun safeCanonicalPath(file: File): String? = try { file.canonicalPath } catch (_: Exception) { null }
    private fun disabledKey(manager: String, name: String) = "$DISABLED_PREFIX$manager:$name"
    private fun isolatedKey(manager: String, name: String) = "$ISOLATED_PREFIX$manager:$name"
    private fun trackedKey(packageKey: String) = "$TRACKED_PREFIX$packageKey"
    private fun observedKey(packageKey: String) = "$OBSERVED_PREFIX$packageKey"
    private fun quote(value: String): String = "'${value.replace("'", "'\\''")}'"

    private val STATE_NAME_DENYLIST = setOf("sh", "env", "test", "true", "false", "yes", "no")

    /** Credentials and HG2Gui's own state are never Reset targets, even if a package created them. */
    private val PROTECTED_HOME_DIRS = setOf(".ssh", ".gnupg")

    /**
     * Base utilities whose commands are never attributed file creation: they run constantly, on
     * the user's behalf, in whatever directory the user is in.
     */
    private val UNTRACKED_PACKAGES = setOf(
        "coreutils", "findutils", "grep", "sed", "gawk", "diffutils", "tar", "gzip", "bzip2", "xz-utils",
        "less", "bash", "dash", "procps", "ncurses-utils", "util-linux", "file", "which", "busybox",
        "termux-tools", "termux-exec", "apt", "dpkg", "zsh", "nano", "vim", "unzip", "zip"
    )
}
