package com.hereliesaz.hg2gui.terminal

import android.content.Context
import androidx.core.content.edit
import java.io.File

/**
 * HG2Gui's package lifecycle overlay.
 *
 * Package managers remain the source of truth for installation/update/removal. HG2Gui adds two
 * manager-independent lifecycle concepts on top:
 *  - disabled: the package stays installed/listed, but its owned commands are blocked by
 *    TerminalEngine until re-enabled;
 *  - reset: package-scoped runtime state and paths positively observed being created by the
 *    package are deleted without removing the installed payload.
 */
object PackageLifecycleStore {
    private const val PREFS = "hg2gui_package_lifecycle"
    private const val DISABLED_PREFIX = "disabled:"
    private const val TRACKED_PREFIX = "tracked:"

    data class InstalledPackage(
        val manager: String,
        val managerLabel: String,
        val name: String,
        val version: String,
        val binaries: List<String>,
        val disabled: Boolean
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
        val packages = mutableListOf<InstalledPackage>()

        packages += dpkgPackages(prefix, prefs)
        packages += pipPackages(prefix, prefs)
        packages += npmPackages(prefix, prefs)
        packages += gemPackages(prefix, prefs)

        return packages
            .distinctBy { it.key.lowercase() }
            .sortedWith(compareBy<InstalledPackage> { it.managerLabel.lowercase() }.thenBy { it.name.lowercase() })
    }

    fun ownerOfBinary(context: Context, binary: String): InstalledPackage? {
        val bare = binary.substringAfterLast('/')
        return installed(context).firstOrNull { pkg -> bare in pkg.binaries }
    }

    fun find(context: Context, manager: String, name: String): InstalledPackage? =
        installed(context).firstOrNull { it.manager == manager && it.name == name }

    fun setDisabled(context: Context, manager: String, name: String, disabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(disabledKey(manager, name), disabled)
        }
    }

    fun beginRun(context: Context, pkg: InstalledPackage, cwd: String): RunSnapshot? {
        if (cwd.isBlank()) return null
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
        val existing = prefs.getStringSet(key, emptySet()).orEmpty()
        prefs.edit { putStringSet(key, existing + created) }
    }

    fun reset(context: Context, pkg: InstalledPackage): ResetResult {
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
        candidates
            .filter { it.exists() }
            .sortedByDescending { it.absolutePath.length }
            .forEach { file ->
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
            InstalledPackage(
                manager = "pkg",
                managerLabel = "Termux / pkg",
                name = name,
                version = version,
                binaries = binaries[name].orEmpty().distinct().sorted(),
                disabled = prefs.getBoolean(disabledKey("pkg", name), false)
            )
        }
    }

    private fun pipPackages(prefix: File, prefs: android.content.SharedPreferences): List<InstalledPackage> {
        val result = mutableListOf<InstalledPackage>()
        val lib = File(prefix, "lib")
        lib.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("python") }
            .map { File(it, "site-packages") }
            .filter { it.isDirectory }
            .forEach { site ->
                site.listFiles().orEmpty().filter { it.isDirectory && it.name.endsWith(".dist-info") }.forEach { dist ->
                    val metadata = File(dist, "METADATA")
                    val fields = readHeaderFields(metadata)
                    val name = fields["Name"] ?: return@forEach
                    val version = fields["Version"].orEmpty()
                    val entryPoints = File(dist, "entry_points.txt")
                    val binaries = parseConsoleScripts(entryPoints)
                    result += InstalledPackage(
                        manager = "pip",
                        managerLabel = "Python / pip",
                        name = name,
                        version = version,
                        binaries = binaries,
                        disabled = prefs.getBoolean(disabledKey("pip", name), false)
                    )
                }
            }
        return result
    }

    private fun npmPackages(prefix: File, prefs: android.content.SharedPreferences): List<InstalledPackage> {
        val root = File(prefix, "lib/node_modules")
        if (!root.isDirectory) return emptyList()
        val packageDirs = root.listFiles().orEmpty().flatMap { child ->
            if (child.isDirectory && child.name.startsWith("@")) child.listFiles().orEmpty().toList() else listOf(child)
        }.filter { it.isDirectory && File(it, "package.json").isFile }

        return packageDirs.mapNotNull { dir ->
            val json = runCatching { File(dir, "package.json").readText() }.getOrNull() ?: return@mapNotNull null
            val name = JSON_NAME.find(json)?.groupValues?.getOrNull(1) ?: return@mapNotNull null
            val version = JSON_VERSION.find(json)?.groupValues?.getOrNull(1).orEmpty()
            val binaries = parseNpmBins(json, name)
            InstalledPackage(
                manager = "npm",
                managerLabel = "Node / npm",
                name = name,
                version = version,
                binaries = binaries,
                disabled = prefs.getBoolean(disabledKey("npm", name), false)
            )
        }
    }

    private fun gemPackages(prefix: File, prefs: android.content.SharedPreferences): List<InstalledPackage> {
        val gemsRoot = File(prefix, "lib/ruby/gems")
        if (!gemsRoot.isDirectory) return emptyList()
        val specs = gemsRoot.walkTopDown().filter { it.isFile && it.parentFile?.name == "specifications" && it.extension == "gemspec" }
        return specs.mapNotNull { spec ->
            val text = runCatching { spec.readText() }.getOrNull() ?: return@mapNotNull null
            val name = GEM_NAME.find(text)?.groupValues?.getOrNull(1) ?: return@mapNotNull null
            val version = GEM_VERSION.find(text)?.groupValues?.getOrNull(1).orEmpty()
            val executables = GEM_EXECUTABLES.find(text)?.groupValues?.getOrNull(1)
                ?.let { body -> QUOTED.findAll(body).map { it.groupValues[1] }.toList() }
                .orEmpty()
            InstalledPackage(
                manager = "gem",
                managerLabel = "Ruby / gem",
                name = name,
                version = version,
                binaries = executables,
                disabled = prefs.getBoolean(disabledKey("gem", name), false)
            )
        }.toList()
    }

    private fun conventionalStatePaths(context: Context, pkg: InstalledPackage): Set<File> {
        val home = DistroManager.homeDir(context)
        val prefix = DistroManager.prefixDir(context)
        val names = (listOf(pkg.name) + pkg.binaries)
            .map { it.substringAfterLast('/').lowercase() }
            .filter { it.length >= 2 && it !in STATE_NAME_DENYLIST }
            .distinct()

        val result = linkedSetOf<File>()
        for (name in names) {
            result += File(home, ".cache/$name")
            result += File(home, ".config/$name")
            result += File(home, ".local/share/$name")
            result += File(home, ".local/state/$name")
            result += File(prefix, "var/cache/$name")
            result += File(prefix, "var/log/$name")
            result += File(prefix, "var/tmp/$name")
        }
        return result
    }

    private fun isResettablePath(context: Context, file: File): Boolean {
        val path = safeCanonicalPath(file) ?: return false
        val home = safeCanonicalPath(DistroManager.homeDir(context)) ?: return false
        val prefix = safeCanonicalPath(DistroManager.prefixDir(context)) ?: return false
        return path.startsWith("$home/") ||
            path.startsWith("$prefix/var/cache/") ||
            path.startsWith("$prefix/var/log/") ||
            path.startsWith("$prefix/var/tmp/")
    }

    private fun readHeaderFields(file: File): Map<String, String> {
        if (!file.isFile) return emptyMap()
        return runCatching {
            file.useLines { lines ->
                lines.takeWhile { it.isNotBlank() }
                    .mapNotNull { line ->
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
        val objectBody = JSON_BIN_OBJECT.find(json)?.groupValues?.getOrNull(1)
        if (objectBody != null) {
            return JSON_KEY.findAll(objectBody).map { it.groupValues[1] }.distinct().toList()
        }
        return if (JSON_BIN_STRING.containsMatchIn(json)) listOf(packageName.substringAfterLast('/')) else emptyList()
    }

    private fun sizeOf(file: File): Long = try {
        if (file.isFile) file.length() else file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    } catch (_: SecurityException) {
        0L
    }

    private fun safeCanonicalPath(file: File): String? = try {
        file.canonicalPath
    } catch (_: Exception) {
        null
    }

    private fun disabledKey(manager: String, name: String) = "$DISABLED_PREFIX$manager:$name"
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
