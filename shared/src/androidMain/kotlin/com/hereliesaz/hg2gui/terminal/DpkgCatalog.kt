package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.util.ArrayDeque

/**
 * Real dpkg package-ownership and relationship metadata already sitting in the Termux prefix.
 * Nothing here shells out or guesses: ownership comes from dpkg package list files and installed
 * package relationships come from var/lib/dpkg/status.
 */
object DpkgCatalog {

    data class InstalledPackageMetadata(
        val name: String,
        val version: String,
        val preDepends: List<List<String>>,
        val depends: List<List<String>>,
        val provides: Set<String>
    )

    data class DependencyView(
        val directDependencies: List<String>,
        val dependencyClosure: List<String>,
        val directDependents: List<String>,
        val dependentClosure: List<String>
    )

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
    fun installedVersions(prefixDir: File): Map<String, String> =
        installedMetadata(prefixDir).mapValues { it.value.version }

    /**
     * Installed dpkg relationships, preserving dependency alternatives while normalizing away
     * version/architecture/profile syntax. The status database describes the already-selected
     * installed world, so topology does not need to re-solve version constraints here.
     */
    fun installedMetadata(prefixDir: File): Map<String, InstalledPackageMetadata> {
        val status = File(prefixDir, "var/lib/dpkg/status")
        if (!status.isFile) return emptyMap()

        val result = linkedMapOf<String, InstalledPackageMetadata>()
        try {
            status.readText().split(Regex("\\n\\s*\\n")).forEach { paragraph ->
                val fields = parseParagraph(paragraph)
                val name = fields["Package"]?.takeIf { it.isNotBlank() } ?: return@forEach
                if (fields["Status"]?.endsWith(" installed") != true) return@forEach
                result[name] = InstalledPackageMetadata(
                    name = name,
                    version = fields["Version"].orEmpty(),
                    preDepends = parseRelationGroups(fields["Pre-Depends"].orEmpty()),
                    depends = parseRelationGroups(fields["Depends"].orEmpty()),
                    provides = parseRelationList(fields["Provides"].orEmpty()).toSet()
                )
            }
        } catch (_: Exception) {
            return emptyMap()
        }
        return result
    }

    /** Installed dependency closure plus reverse-removal impact for one dpkg package. */
    fun dependencyView(prefixDir: File, packageName: String): DependencyView {
        val installed = installedMetadata(prefixDir)
        val target = installed[packageName] ?: return DependencyView(emptyList(), emptyList(), emptyList(), emptyList())

        val providers = linkedMapOf<String, MutableList<String>>()
        installed.values.forEach { pkg ->
            pkg.provides.forEach { provided -> providers.getOrPut(provided) { mutableListOf() }.add(pkg.name) }
        }

        fun resolve(groups: List<List<String>>): List<String> = groups.mapNotNull { alternatives ->
            alternatives.firstOrNull { it in installed }
                ?: alternatives.firstNotNullOfOrNull { providers[it]?.firstOrNull() }
        }.distinct()

        val directByPackage = installed.mapValues { (_, pkg) -> resolve(pkg.preDepends + pkg.depends).filter { it != pkg.name } }
        val directDependencies = directByPackage[target.name].orEmpty().sorted()
        val dependencyClosure = closure(directDependencies) { directByPackage[it].orEmpty() }

        val reverse = linkedMapOf<String, MutableList<String>>()
        directByPackage.forEach { (owner, dependencies) ->
            dependencies.forEach { dependency -> reverse.getOrPut(dependency) { mutableListOf() }.add(owner) }
        }
        val directDependents = reverse[target.name].orEmpty().distinct().sorted()
        val dependentClosure = closure(directDependents) { reverse[it].orEmpty() }

        return DependencyView(
            directDependencies = directDependencies,
            dependencyClosure = dependencyClosure,
            directDependents = directDependents,
            dependentClosure = dependentClosure
        )
    }

    private fun closure(seed: List<String>, next: (String) -> List<String>): List<String> {
        val seen = linkedSetOf<String>()
        val queue = ArrayDeque<String>()
        seed.forEach { queue.addLast(it) }
        while (queue.isNotEmpty()) {
            val name = queue.removeFirst()
            if (!seen.add(name)) continue
            next(name).forEach { if (it !in seen) queue.addLast(it) }
        }
        return seen.toList().sorted()
    }

    private fun parseParagraph(paragraph: String): Map<String, String> {
        val fields = linkedMapOf<String, String>()
        var current: String? = null
        paragraph.lineSequence().forEach { line ->
            val active = current
            if (line.startsWith(' ') && active != null) {
                fields[active] = fields.getValue(active) + " " + line.trim()
            } else {
                val separator = line.indexOf(':')
                if (separator > 0) {
                    current = line.substring(0, separator)
                    fields[current!!] = line.substring(separator + 1).trim()
                }
            }
        }
        return fields
    }

    private fun parseRelationGroups(raw: String): List<List<String>> = if (raw.isBlank()) {
        emptyList()
    } else {
        raw.split(',').mapNotNull { clause ->
            clause.split('|').mapNotNull(::relationName).takeIf { it.isNotEmpty() }
        }
    }

    private fun parseRelationList(raw: String): List<String> = if (raw.isBlank()) {
        emptyList()
    } else {
        raw.split(',').mapNotNull(::relationName)
    }

    private fun relationName(raw: String): String? {
        val cleaned = raw
            .replace(Regex("\\[[^]]*]"), "")
            .replace(Regex("<[^>]*>"), "")
            .substringBefore('(')
            .trim()
        return cleaned.substringBefore(':').trim().takeIf { it.isNotBlank() }
    }
}
