package com.hereliesaz.hg2gui.terminal

import java.io.File

/**
 * Real dpkg package-ownership metadata already sitting in the Termux prefix - which package
 * installed a given binary. Read from var/lib/dpkg/info/<package>.list, one file per installed
 * package listing every path it put down (already on disk, no guessing which binary came from
 * which package).
 *
 * Termux's own packages carry no Debian "Section" field (verified against a real bootstrap - 0
 * of 82 base packages have one), so there is no live category metadata to read for a binary,
 * only which package owns it. `CommandTree`'s own hand-curated package→category map is what
 * actually turns this into a category.
 */
object DpkgCatalog {

    /** Maps each installed package's name to the binaries (bare names, under bin/) it owns. */
    fun binariesByPackage(prefixDir: File): Map<String, List<String>> {
        val infoDir = File(prefixDir, "var/lib/dpkg/info")
        val listFiles = infoDir.listFiles { f -> f.name.endsWith(".list") } ?: return emptyMap()

        val result = mutableMapOf<String, MutableList<String>>()
        for (listFile in listFiles) {
            val pkg = listFile.name.removeSuffix(".list")
            try {
                listFile.forEachLine { path ->
                    if (path.contains("/bin/") && !path.endsWith("/")) {
                        val name = path.substringAfterLast('/')
                        if (name.isNotBlank()) result.getOrPut(pkg) { mutableListOf() }.add(name)
                    }
                }
            } catch (_: Exception) {
                // This package's own listing is unreadable - skip it, keep the rest.
            }
        }
        return result
    }

    /** Installed package versions from dpkg's status database, limited to `Status: ... installed`. */
    fun installedVersions(prefixDir: File): Map<String, String> {
        val status = File(prefixDir, "var/lib/dpkg/status")
        if (!status.isFile) return emptyMap()

        val result = linkedMapOf<String, String>()
        var name: String? = null
        var version: String? = null
        var installed = false

        fun commit() {
            val packageName = name
            if (installed && !packageName.isNullOrBlank()) result[packageName] = version.orEmpty()
            name = null
            version = null
            installed = false
        }

        try {
            status.forEachLine { line ->
                if (line.isBlank()) {
                    commit()
                } else {
                    when {
                        line.startsWith("Package:") -> name = line.substringAfter(':').trim()
                        line.startsWith("Version:") -> version = line.substringAfter(':').trim()
                        line.startsWith("Status:") -> installed = line.substringAfter(':').trim().endsWith(" installed")
                    }
                }
            }
            commit()
        } catch (_: Exception) {
            return emptyMap()
        }
        return result
    }
}
