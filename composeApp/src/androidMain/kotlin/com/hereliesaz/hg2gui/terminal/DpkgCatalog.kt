package com.hereliesaz.hg2gui.terminal

import java.io.File

/**
 * Real Debian/dpkg package metadata already sitting in the Termux prefix - which package owns a
 * given binary, and what Debian Section (admin, utils, net, shells, devel, text, ...) that
 * package belongs to. Used to group discovered PATH binaries into real categories instead of
 * guessing at one, since apt/dpkg already know this and record it on disk at install time.
 */
object DpkgCatalog {

    /** Maps a binary's bare name (e.g. "apt-get") to its owning package's Section, best-effort -
     *  empty if dpkg's own bookkeeping isn't there (e.g. no bootstrap installed yet). */
    fun binariesBySection(prefixDir: File): Map<String, String> {
        val dpkgDir = File(prefixDir, "var/lib/dpkg")
        val statusFile = File(dpkgDir, "status")
        val infoDir = File(dpkgDir, "info")
        if (!statusFile.isFile || !infoDir.isDirectory) return emptyMap()

        val sectionByPackage = mutableMapOf<String, String>()
        var currentPackage: String? = null
        try {
            statusFile.forEachLine { line ->
                when {
                    line.startsWith("Package: ") -> currentPackage = line.removePrefix("Package: ").trim()
                    line.startsWith("Section: ") ->
                        currentPackage?.let { sectionByPackage[it] = line.removePrefix("Section: ").trim() }
                    line.isBlank() -> currentPackage = null
                }
            }
        } catch (e: Exception) {
            return emptyMap()
        }

        val binaryToSection = mutableMapOf<String, String>()
        for ((pkg, section) in sectionByPackage) {
            val listFile = File(infoDir, "$pkg.list")
            if (!listFile.isFile) continue
            try {
                listFile.forEachLine { path ->
                    if (path.contains("/bin/")) {
                        val name = path.substringAfterLast('/')
                        if (name.isNotBlank()) binaryToSection[name] = section
                    }
                }
            } catch (e: Exception) {
                // This package's own listing is unreadable - skip it, keep the rest.
            }
        }
        return binaryToSection
    }
}
