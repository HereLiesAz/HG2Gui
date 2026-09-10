package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.util.AtomicFile
import java.io.File

/**
 * Produces a bounded, package-owned seed manifest for dpkg packages instead of cloning the whole
 * prefix. Non-dpkg managers deliberately fall back to whole-prefix seeding until they expose a
 * trustworthy file-ownership inventory.
 */
object IsolationSeedPlanner {
    data class Plan(
        val manifest: File?,
        val packages: List<String>,
        val fileCount: Int,
        val strategy: String
    )

    private const val DIRECTORY = "seed"
    private const val MANIFEST = "files.txt"
    private const val MAX_FILES = 50_000
    private val runtimePackages = listOf(
        "bash",
        "coreutils",
        "libandroid-support",
        "libc++",
        "ncurses",
        "readline",
        "termux-core",
        "termux-exec",
        "termux-tools"
    )

    fun prepare(
        context: Context,
        pkg: PackageLifecycleStore.InstalledPackage,
        packageBase: File
    ): Plan {
        if (pkg.manager != "pkg") return Plan(null, emptyList(), 0, "installed-prefix-copy")
        val prefix = DistroManager.prefixDir(context)
        val metadata = DpkgCatalog.installedMetadata(prefix)
        if (pkg.name !in metadata) return Plan(null, emptyList(), 0, "installed-prefix-copy")

        val closure = DpkgCatalog.dependencyView(prefix, pkg.name).dependencyClosure
        val packages = (listOf(pkg.name) + closure + runtimePackages.filter(metadata::containsKey)).distinct().sorted()
        val info = File(prefix, "var/lib/dpkg/info")
        val paths = linkedSetOf<String>()
        var overflowed = false
        for (name in packages) {
            matchingLists(info, name).forEach { listFile ->
                runCatching {
                    listFile.forEachLine { raw ->
                        val source = raw.trim()
                        if (source.isBlank() || !source.startsWith(prefix.absolutePath + "/")) return@forEachLine
                        val file = File(source)
                        if (!file.exists() || file.isDirectory || source in paths) return@forEachLine
                        if (paths.size >= MAX_FILES) {
                            overflowed = true
                            return@forEachLine
                        }
                        paths += file.absolutePath
                    }
                }
            }
            if (overflowed) break
        }
        if (overflowed || paths.isEmpty()) {
            return Plan(null, packages, paths.size, "installed-prefix-copy")
        }

        val manifest = File(File(packageBase, DIRECTORY), MANIFEST)
        manifest.parentFile?.mkdirs()
        val payload = paths.joinToString(separator = "\n", postfix = "\n").toByteArray(Charsets.UTF_8)
        val atomic = AtomicFile(manifest)
        val stream = atomic.startWrite()
        try {
            stream.write(payload)
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
        return Plan(manifest, packages, paths.size, "dependency-closure-manifest")
    }

    private fun matchingLists(infoDir: File, packageName: String): List<File> {
        val exact = File(infoDir, "$packageName.list")
        val prefixed = infoDir.listFiles { file ->
            file.isFile && file.name.startsWith("$packageName:") && file.name.endsWith(".list")
        }.orEmpty().toList()
        return buildList {
            if (exact.isFile) add(exact)
            addAll(prefixed)
        }
    }
}
