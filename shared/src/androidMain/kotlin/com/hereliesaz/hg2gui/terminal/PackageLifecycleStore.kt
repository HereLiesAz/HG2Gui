package com.hereliesaz.hg2gui.terminal

import android.content.Context
import androidx.core.content.edit
import java.io.File

/** HG2Gui lifecycle state layered over whatever package manager installed the package. */
object PackageLifecycleStore {
    private const val PREFS = "hg2gui_package_lifecycle"
    private const val DISABLED_PREFIX = "disabled:"
    private const val ISOLATED_PREFIX = "isolated:"
    private const val TRACKED_PREFIX = "tracked:"

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

    data class RunSnapshot(
        val packageKey: String,
        val directory: File,
        val children: Set<String>
    )

    fun installed(context: Context): List<InstalledPackage> {
        val prefix = DistroManager.prefixDir(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return buildList {
            addAll(dpkgPackages(prefix, prefs))
            addAll(pipPackages(prefix, prefs))
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
        val children = try {
            dir.listFiles().orEmpty().map { it.name }.toSet()
        } catch (_: SecurityException) {
            return null
        }
        return RunSnapshot(pkg.key, dir, children)
    }

    fun finishRun(context: Context, snapshot: RunSnapshot?) {
        if (snapshot == null || !snapshot.directory.isDirectory) return
        val now = try {
            snapshot.directory.listFiles().orEmpty()
        } catch (_: SecurityException) {
            return
        }
        val created = now.filter { it.name !in snapshot.children }.mapNotNull { safeCanonicalPath(it) }.toSet()
        if (created.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = trackedKey(snapshot.packageKey)
        prefs.edit { putStringSet(key, prefs.getStringSet(key, emptySet()).orEmpty() + created) }
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

        val candidates = linkedSetOf<File>()
        candidates += conventionalStatePaths(context, pkg)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getStringSet(trackedKey(pkg.key), emptySet()).orEmpty().forEach { path ->
            val file = File(path)
            if (isResettablePath(context, file)) candidates += file
        }

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
        "npm" -> "npm update -g ${quote(pkg.name)}"
        "gem" -> "gem update ${quote(pkg.name)}"
        else -> error("Unsupported package manager '${pkg.manager}'")
    }

    fun removeCommand(pkg: InstalledPackage, purge: Boolean = false): String = when (pkg.manager) {
        "pkg" -> "pkg ${if (purge) "purge" else "remove"} ${quote(pkg.name)}"
        "pip" -> "python -m pip uninstall -y ${quote(pkg.name)}"
        "npm" -> "npm uninstall -g ${quote(pkg.name)}"
        "gem" -> "gem uninstall ${quote(pkg.name)} -a -x"
        else -> error("Unsupported package manager '${pkg.manager}'")
    }

    fun infoCommand(pkg: InstalledPackage): String = when (pkg.manager) {
        "pkg" -> "pkg show ${quote(pkg.name)}"
        "pip" -> "python -m pip show ${quote(pkg.name)}"
        "npm" -> "npm list -g ${quote(pkg.name)} --depth=0"
        "gem" -> "gem info ${quote(pkg.name)}"
        else -> error("Unsupported package manager '${pkg.manager}'")
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

    private fun npmPackages(prefix: File, prefs: android.content.SharedPreferences): List<InstalledPackage> {
        val root = File(prefix, "lib/node_modules")
        if (!root.isDirectory) return emptyList()
        return root.listFiles().orEmpty().flatMap { child ->
            if (child.isDirectory && child.name.startsWith("@")) child.listFiles().orEmpty().toList() else listOf(child)
        }.filter { it.isDirectory && File(it, "package.json").isFile }
            .mapNotNull { dir ->
                val json = runCatching { File(dir, "package.json").readText() }.getOrNull() ?: return@mapNotNull null
                val name = JSON_NAME.find(json)?.groupValues?.getOrNull(1) ?: return@mapNotNull null
                packageRecord(
                    prefs,
                    "npm",
                    "Node / npm",
                    name,
                    JSON_VERSION.find(json)?.groupValues?.getOrNull(1).orEmpty(),
                    parseNpmBins(json, name)
                )
            }
    }

    private fun gemPackages(prefix: File, prefs: android.content.SharedPreferences): List<InstalledPackage> {
        val root = File(prefix, "lib/ruby/gems")
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown().filter { it.isFile && it.parentFile?.name == "specifications" && it.extension == "gemspec" }
            .mapNotNull { spec ->
                val text = runCatching { spec.readText() }.getOrNull() ?: return@mapNotNull null
                val name = GEM_NAME.find(text)?.groupValues?.getOrNull(1) ?: return@mapNotNull null
                val executables = GEM_EXECUTABLES.find(text)?.groupValues?.getOrNull(1)
                    ?.let { body -> QUOTED.findAll(body).map { it.groupValues[1] }.toList() }
                    .orEmpty()
                packageRecord(
                    prefs,
                    "gem",
                    "Ruby / gem",
                    name,
                    GEM_VERSION.find(text)?.groupValues?.getOrNull(1).orEmpty(),
                    executables
                )
            }.toList()
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

    private fun parseNpmBins(json: String, packageName: String): List<String> {
        val body = JSON_BIN_OBJECT.find(json)?.groupValues?.getOrNull(1)
        if (body != null) return JSON_KEY.findAll(body).map { it.groupValues[1] }.distinct().toList()
        return if (JSON_BIN_STRING.containsMatchIn(json)) listOf(packageName.substringAfterLast('/')) else emptyList()
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
    private fun quote(value: String): String = "'${value.replace("'", "'\\''")}'"

    private val JSON_NAME = Regex("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
    private val JSON_VERSION = Regex("\\\"version\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
    private val JSON_BIN_OBJECT = Regex("\\\"bin\\\"\\s*:\\s*\\{([^}]*)}", RegexOption.DOT_MATCHES_ALL)
    private val JSON_BIN_STRING = Regex("\\\"bin\\\"\\s*:\\s*\\\"[^\\\"]+\\\"")
    private val JSON_KEY = Regex("\\\"([^\\\"]+)\\\"\\s*:")
    private val GEM_NAME = Regex("\\.name\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']")
    private val GEM_VERSION = Regex("\\.version\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']")
    private val GEM_EXECUTABLES = Regex("\\.executables\\s*=\\s*\\[([^]]*)]", RegexOption.DOT_MATCHES_ALL)
    private val QUOTED = Regex("[\\\"']([^\\\"']+)[\\\"']")
    private val STATE_NAME_DENYLIST = setOf("sh", "env", "test", "true", "false", "yes", "no")
}
