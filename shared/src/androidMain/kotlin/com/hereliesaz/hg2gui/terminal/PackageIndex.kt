package com.hereliesaz.hg2gui.terminal

/**
 * Pure Debian control-file parsing and dependency resolution for [Hg2PackageManager], split out so
 * both run under host unit tests with no Android runtime, dpkg binary or network.
 */
internal object DebControl {
    private val VERSION_RELATION = Regex("\\((<<|<=|=|>=|>>)\\s*([^)]+)\\)")
    private val ARCH_RESTRICTION = Regex("\\[[^]]*]")
    private val PROFILE_RESTRICTION = Regex("<[^>]*>")

    fun parseParagraph(paragraph: String): Map<String, String> {
        val fields = LinkedHashMap<String, String>()
        var current: String? = null
        for (line in paragraph.lineSequence()) {
            if (line.startsWith(' ') && current != null) {
                fields[current] = fields.getValue(current) + "\n" + line.trim()
                continue
            }
            val i = line.indexOf(':')
            if (i <= 0) continue
            current = line.substring(0, i)
            fields[current] = line.substring(i + 1).trim()
        }
        return fields
    }

    fun parseRelationGroups(raw: String): List<List<Hg2PackageManager.Relation>> {
        if (raw.isBlank()) return emptyList()
        return raw.split(',').mapNotNull { clause ->
            clause.split('|').mapNotNull(::parseRelation).takeIf { it.isNotEmpty() }
        }
    }

    fun parseRelationList(raw: String): List<Hg2PackageManager.Relation> =
        if (raw.isBlank()) emptyList() else raw.split(',').mapNotNull(::parseRelation)

    fun parseRelation(raw: String): Hg2PackageManager.Relation? {
        val withoutRestrictions = raw.replace(ARCH_RESTRICTION, "").replace(PROFILE_RESTRICTION, "").trim()
        val name = withoutRestrictions.substringBefore(' ').substringBefore(':').trim().takeIf(String::isNotBlank) ?: return null
        val match = VERSION_RELATION.find(withoutRestrictions)
        return Hg2PackageManager.Relation(
            name = name,
            operator = match?.groupValues?.getOrNull(1)?.takeIf(String::isNotBlank),
            version = match?.groupValues?.getOrNull(2)?.trim()?.takeIf(String::isNotBlank)
        )
    }

    fun parsePackages(text: String): List<Hg2PackageManager.PackageRecord> = text.split("\n\n").mapNotNull { paragraph ->
        val fields = parseParagraph(paragraph)
        val name = fields["Package"] ?: return@mapNotNull null
        val filename = fields["Filename"] ?: return@mapNotNull null
        Hg2PackageManager.PackageRecord(
            name = name,
            version = fields["Version"].orEmpty(),
            architecture = fields["Architecture"].orEmpty(),
            filename = filename,
            sha256 = fields["SHA256"].orEmpty(),
            size = fields["Size"]?.toLongOrNull() ?: 0L,
            preDepends = parseRelationGroups(fields["Pre-Depends"].orEmpty()),
            depends = parseRelationGroups(fields["Depends"].orEmpty()),
            provides = parseRelationList(fields["Provides"].orEmpty()),
            conflicts = parseRelationList(fields["Conflicts"].orEmpty()),
            breaks = parseRelationList(fields["Breaks"].orEmpty()),
            replaces = parseRelationList(fields["Replaces"].orEmpty()),
            description = fields["Description"].orEmpty()
        )
    }

    /** Fully installed packages from a dpkg `status` file. */
    fun parseInstalled(statusText: String): Map<String, Hg2PackageManager.Installed> =
        statusText.split("\n\n").mapNotNull { paragraph ->
            val fields = parseParagraph(paragraph)
            val name = fields["Package"] ?: return@mapNotNull null
            if (fields["Status"] != "install ok installed") return@mapNotNull null
            Hg2PackageManager.Installed(
                name = name,
                version = fields["Version"].orEmpty(),
                provides = parseRelationList(fields["Provides"].orEmpty()),
                conflicts = parseRelationList(fields["Conflicts"].orEmpty()),
                breaks = parseRelationList(fields["Breaks"].orEmpty())
            )
        }.associateBy { it.name }
}

/**
 * Resolves a requested package set into an install plan (dependencies first) against the
 * available index and the installed set. Version relations are decided by [compareVersions]
 * `(candidateVersion, operator, wantedVersion) -> Boolean`; production passes `dpkg
 * --compare-versions`, tests pass a fake.
 *
 * [forceRequested] re-plans a requested package even when an installed version already satisfies
 * it - the primitive behind `pkg upgrade <name>` and `pkg install --reinstall`.
 */
internal class PackageResolver(private val compareVersions: (String, String, String) -> Boolean) {

    private fun versionSatisfies(candidateVersion: String, relation: Hg2PackageManager.Relation): Boolean {
        val op = relation.operator ?: return true
        val wanted = relation.version ?: return true
        return compareVersions(candidateVersion, op, wanted)
    }

    private fun providedVersionSatisfies(provided: Hg2PackageManager.Relation, requested: Hg2PackageManager.Relation): Boolean {
        if (requested.operator == null) return true
        val providedVersion = provided.version ?: return false
        return versionSatisfies(providedVersion, requested)
    }

    fun resolve(
        requested: List<String>,
        available: Map<String, Hg2PackageManager.PackageRecord>,
        installed: Map<String, Hg2PackageManager.Installed>,
        forceRequested: Boolean
    ): List<Hg2PackageManager.PackageRecord> {
        val requestedSet = requested.toSet()
        val visiting = HashMap<String, Hg2PackageManager.PackageRecord>()
        val planned = LinkedHashMap<String, Hg2PackageManager.PackageRecord>()
        val providers = buildMap<String, MutableList<Hg2PackageManager.PackageRecord>> {
            available.values.forEach { pkg ->
                pkg.provides.forEach { provided -> getOrPut(provided.name) { mutableListOf() }.add(pkg) }
            }
        }

        fun installedSatisfies(relation: Hg2PackageManager.Relation): Boolean = installed.values.any { item ->
            if (item.name == relation.name && versionSatisfies(item.version, relation)) return@any true
            item.provides.any { provided -> provided.name == relation.name && providedVersionSatisfies(provided, relation) }
        }

        fun plannedSatisfies(relation: Hg2PackageManager.Relation): Boolean = planned.values.any { item ->
            if (item.name == relation.name && versionSatisfies(item.version, relation)) return@any true
            item.provides.any { provided -> provided.name == relation.name && providedVersionSatisfies(provided, relation) }
        }

        // A package still on the DFS stack is part of a dependency cycle (A -> B -> A). It will be
        // planned once its own walk unwinds, and dpkg configures cycles together, so it counts as
        // satisfied-in-plan rather than as an unsatisfiable relation.
        fun visitingSatisfies(relation: Hg2PackageManager.Relation): Boolean = visiting.values.any { item ->
            if (item.name == relation.name && versionSatisfies(item.version, relation)) return@any true
            item.provides.any { provided -> provided.name == relation.name && providedVersionSatisfies(provided, relation) }
        }

        fun candidateFor(relation: Hg2PackageManager.Relation): Hg2PackageManager.PackageRecord? {
            val exact = available[relation.name]
            if (exact != null && versionSatisfies(exact.version, relation)) return exact
            return providers[relation.name].orEmpty().firstOrNull { provider ->
                provider.provides.any { it.name == relation.name && providedVersionSatisfies(it, relation) }
            }
        }

        fun installedConflict(pkg: Hg2PackageManager.PackageRecord): String? {
            val relations = pkg.conflicts + pkg.breaks
            for (relation in relations) {
                val hit = installed.values.firstOrNull { item ->
                    item.name != pkg.name && (
                        (item.name == relation.name && versionSatisfies(item.version, relation)) ||
                            item.provides.any { it.name == relation.name && providedVersionSatisfies(it, relation) }
                        )
                }
                if (hit != null) return "${pkg.name} ${if (relation in pkg.breaks) "breaks" else "conflicts with"} installed ${hit.name} (${relation})"
            }
            for (item in installed.values) {
                if (item.name == pkg.name) continue
                val reverse = (item.conflicts + item.breaks).firstOrNull { relation ->
                    (pkg.name == relation.name && versionSatisfies(pkg.version, relation)) ||
                        pkg.provides.any { it.name == relation.name && providedVersionSatisfies(it, relation) }
                }
                if (reverse != null) return "installed ${item.name} conflicts with/breaks ${pkg.name} (${reverse})"
            }
            return null
        }

        fun plannedConflict(pkg: Hg2PackageManager.PackageRecord): String? {
            for (other in planned.values) {
                if (other.name == pkg.name) continue
                val forward = (pkg.conflicts + pkg.breaks).firstOrNull { relation ->
                    (other.name == relation.name && versionSatisfies(other.version, relation)) ||
                        other.provides.any { it.name == relation.name && providedVersionSatisfies(it, relation) }
                }
                if (forward != null) return "${pkg.name} conflicts with/breaks planned ${other.name} ($forward)"
                val reverse = (other.conflicts + other.breaks).firstOrNull { relation ->
                    (pkg.name == relation.name && versionSatisfies(pkg.version, relation)) ||
                        pkg.provides.any { it.name == relation.name && providedVersionSatisfies(it, relation) }
                }
                if (reverse != null) return "planned ${other.name} conflicts with/breaks ${pkg.name} ($reverse)"
            }
            return null
        }

        lateinit var visitPackage: (Hg2PackageManager.PackageRecord) -> Unit

        fun visitRelation(alternatives: List<Hg2PackageManager.Relation>, owner: String, kind: String) {
            if (alternatives.any(::installedSatisfies) || alternatives.any(::plannedSatisfies) || alternatives.any(::visitingSatisfies)) return
            val candidate = alternatives.firstNotNullOfOrNull(::candidateFor)
                ?: error("$owner $kind unavailable alternative: ${alternatives.joinToString(" | ")}")
            visitPackage(candidate)
            if (!alternatives.any(::installedSatisfies) && !alternatives.any(::plannedSatisfies) && !alternatives.any(::visitingSatisfies)) {
                error("$owner $kind was not satisfied after planning: ${alternatives.joinToString(" | ")}")
            }
        }

        visitPackage = fun(pkg: Hg2PackageManager.PackageRecord) {
            if (pkg.name in planned) return
            val forcedRequested = forceRequested && (
                pkg.name in requestedSet || requested.any { requestedName -> pkg.provides.any { it.name == requestedName } }
            )
            if (pkg.name in installed && !forcedRequested) return
            if (visiting.putIfAbsent(pkg.name, pkg) != null) return
            installedConflict(pkg)?.let { error("Cannot install ${pkg.name}: $it. HG2Gui will not auto-remove conflicting packages.") }
            plannedConflict(pkg)?.let { error("Cannot install ${pkg.name}: $it") }
            pkg.preDepends.forEach { visitRelation(it, pkg.name, "pre-depends on") }
            pkg.depends.forEach { visitRelation(it, pkg.name, "depends on") }
            visiting.remove(pkg.name)
            planned[pkg.name] = pkg
        }

        fun visitRequested(name: String) {
            val relation = Hg2PackageManager.Relation(name.substringBefore(':'))
            if (installedSatisfies(relation) && !forceRequested) return
            val pkg = candidateFor(relation) ?: error("Package or provider '$name' was not found in the Termux repository")
            visitPackage(pkg)
        }

        requested.forEach(::visitRequested)
        return planned.values.toList()
    }
}

/**
 * Parsed `pkg` sub-command arguments. Unknown options are rejected rather than silently dropped:
 * a dropped `--dry-run` would turn a simulation into a real mutation.
 */
internal object PkgArgs {
    /** Options accepted and deliberately ignored (non-interactive / noise-level flags). */
    private val IGNORED = setOf("-y", "--yes", "--assume-yes", "-q", "--quiet", "-qq", "--no-install-recommends")

    /** Options that force a package to be re-planned although a satisfying version is installed. */
    private val REINSTALL = setOf("--reinstall", "--force-requested")

    data class Parsed(val packages: List<String>, val reinstall: Boolean)

    /**
     * Splits [rest] (the arguments after the sub-command) into package names and options. Options
     * are only accepted when the [operation] supports them (`--reinstall` is install-only).
     */
    fun parse(operation: String, rest: List<String>): Parsed {
        val packages = mutableListOf<String>()
        var reinstall = false
        for (arg in rest) {
            when {
                !arg.startsWith("-") -> packages += arg
                arg in IGNORED -> Unit
                arg in REINSTALL && operation == "install" -> reinstall = true
                else -> error("pkg $operation: unsupported option '$arg' (refusing to ignore it silently)")
            }
        }
        return Parsed(packages, reinstall)
    }
}
