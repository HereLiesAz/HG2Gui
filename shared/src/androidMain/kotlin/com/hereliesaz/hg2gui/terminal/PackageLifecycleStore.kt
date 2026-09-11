package com.hereliesaz.hg2gui.terminal

import android.content.Context
import androidx.core.content.edit
import java.io.File
import org.json.JSONObject

/** HG2Gui lifecycle state layered over whatever package manager installed the package. */
object PackageLifecycleStore {
    private const val PREFS = "hg2gui_package_lifecycle"
    private const val DISABLED_PREFIX = "disabled:"
    private const val ISOLATED_PREFIX = "isolated:"
    private const val TRACKED_PREFIX = "tracked:"
    private const val OBSERVED_PREFIX = "observed:"
    private const val MAX_PROVENANCE_FILES = 10_000
    private const val MAX_PROVENANCE_ROWS = 1_000

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

    fun installed(context: Context): List<InstalledPackage> {
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
    }

    fun setIsolated(context: Context, manager: String, name: String, isolated: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(isolatedKey(manager, name), isolated)
        }
    }

    fun beginRun(context: Context, pkg: InstalledPackage, cwd: String): RunSnapshot? {
        if (pkg.isolated || cwd.isBlank()) return null
        val dir = File(cwd)
        if (!dir.isDirectory) return null
        return RunSnapshot(pkg.key, dir, runSnapshot(dir))
    }

    fun finishRun(context: Context, snapshot: RunSnapshot?) {
        if (snapshot == null || !snapshot.directory.isDirectory) return
        val now = runSnapshot(snapshot.directory)
        val created = now.keys.filter { it !in snapshot.entries }
        val modified = now.keys.filter { path ->
            val before = snapshot.entries[path]
            before != null && before != now[path]
        }
        if (created.isEmpty() && modified.isEmpty()) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val resettableCreated = created.map { File(snapshot.directory, it) }
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
                putStringSet(trackedKey, prefs.getStringSet(trackedKey, emptySet()).orEmpty() + resettableCreated)
            }
            val existing = prefs.getStringSet(observedKey, emptySet()).orEmpty().toList()
            putStringSet(observedKey, (existing + observed).takeLast(MAX_PROVENANCE_ROWS).toSet())
        }
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
        val candidates = resetCandidates(context, pkg)
        var deletedPaths = 0
        var deletedBytes = 0L
        val failed = mutableListOf<String>()
        candidates.filter { it.exists() }.sortedByDescending { it.absolutePath.length }.forEach { file ->
            val bytes = sizeOf(file)
            val deleted = try {
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            } catch (_: SecurityException) {
                false
            }
            if (deleted) {
                deletedPaths++
                deletedBytes += bytes
            } else {
                failed += file.absolutePath
            }
        }
        prefs.edit { remove(trackedKey(pkg.key)) }
        return ResetResult(deletedPaths, deletedBytes, failed)
    }

    fun updateCommand(pkg: InstalledPackage): String = when (pkg.manager) {
        "pkg" -> "pkg install ${quote(pkg.name)}"
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
        val candidates = linkedSetOf<File>()
        candidates += conventionalStatePaths(context, pkg)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getStringSet(trackedKey(pkg.key), emptySet()).orEmpty().forEach { path ->
            val file = File(path)
            if (isResettablePath(context, file)) candidates += file
        }
        return candidates
    }

    private fun runSnapshot(root: File): Map<String, RunFileStamp> {
        val result = linkedMapOf<String, RunFileStamp>()
        try {
            root.walkTopDown().drop(1).take(MAX_PROVENANCE_FILES).forEach { file ->
                val relative = runCatching { file.relativeTo(root).path }.getOrNull() ?: return@forEach
                result[relative] = RunFileStamp(
                    size = if (file.isFile) file.length() else 0L,
                    modified = file.lastModified(),
                    directory = file.isDirectory
                )
            }
        } catch (_: SecurityException) {
            return result
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

    private fun conventionalStatePaths(context: Context, pkg: InstalledPackage): Set<File> {
        val home = DistroManager.homeDir(context)
        val prefix = DistroManager.prefixDir(context)
        val names = (listOf(pkg.name) + pkg.binaries)
            .map { it.substringAfterLast('/').lowercase() }
            .filter { it.length >= 2 && it !in STATE_NAME_DENYLIST }
            .distinct()
        return buildSet {
            for (name in names) {
                add(File(home, ".cache/$name"))
                add(File(home, ".config/$name"))
                add(File(home, ".local/share/$name"))
                add(File(home, ".local/state/$name"))
                add(File(prefix, "var/cache/$name"))
                add(File(prefix, "var/log/$name"))
                add(File(prefix, "var/tmp/$name"))
            }
        }
    }

    private fun isResettablePath(context: Context, file: File): Boolean {
        val path = safeCanonicalPath(file) ?: return false
        val home = safeCanonicalPath(DistroManager.homeDir(context)) ?: return false
        val prefix = safeCanonicalPath(DistroManager.prefixDir(context)) ?: return false
        return path.startsWith("$home/") || path.startsWith("$prefix/var/cache/") ||
            path.startsWith("$prefix/var/log/") || path.startsWith("$prefix/var/tmp/")
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
}
