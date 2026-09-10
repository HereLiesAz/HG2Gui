package com.hereliesaz.hg2gui.ui.menu

import android.content.Context
import com.hereliesaz.hg2gui.terminal.DistroManager
import com.hereliesaz.hg2gui.terminal.DpkgCatalog
import com.hereliesaz.hg2gui.terminal.PackageLifecycleStore
import com.hereliesaz.hg2gui.terminal.PackageRestorePoints
import java.text.DateFormat
import java.util.Date

/** Installed-package management generated from the package managers' real on-disk inventories. */
object PackageLifecycleTree {
    fun root(context: Context): MenuNode = MenuNode(
        id = "packages",
        label = "Packages",
        cap = "manage",
        emitsToken = false,
        resolveChildren = { listOf(AuthorityTree.root(context)) + managerNodes(context) }
    )

    private fun managerNodes(context: Context): List<MenuNode> {
        val packages = PackageLifecycleStore.installed(context)
        if (packages.isEmpty()) {
            return listOf(MenuNode("packages/none", "No installed packages", "…", emitsToken = false))
        }
        return packages.groupBy { it.manager to it.managerLabel }
            .entries
            .sortedBy { it.key.second.lowercase() }
            .map { (manager, installed) ->
                val children = installed.sortedBy { it.name.lowercase() }.map { packageNode(context, it) }
                MenuNode(
                    id = "packages/${manager.first}",
                    label = manager.second,
                    cap = children.size.toString(),
                    children = children,
                    emitsToken = false
                )
            }
    }

    private fun packageNode(context: Context, pkg: PackageLifecycleStore.InstalledPackage): MenuNode {
        val actions = buildList {
            if (pkg.binaries.isNotEmpty() && !pkg.disabled) {
                add(
                    MenuNode(
                        id = "packages/${pkg.key}/run",
                        label = "Run",
                        cap = pkg.binaries.size.toString(),
                        children = pkg.binaries.map { binary ->
                            MenuNode("packages/${pkg.key}/run/$binary", binary, value = binary)
                        },
                        emitsToken = false
                    )
                )
            }
            addDependencyViews(context, pkg)
            add(
                MenuNode(
                    id = "packages/${pkg.key}/provenance",
                    label = "Observed writes",
                    cap = "history",
                    emitsToken = false,
                    resolveChildren = { provenanceRows(context, pkg) }
                )
            )
            add(
                MenuNode(
                    id = "packages/${pkg.key}/${if (pkg.disabled) "enable" else "disable"}",
                    label = if (pkg.disabled) "Enable" else "Disable",
                    cap = if (pkg.disabled) "on" else "off",
                    value = "hg2package ${if (pkg.disabled) "enable" else "disable"} ${pkg.manager} ${shellQuote(pkg.name)}"
                )
            )
            add(
                MenuNode(
                    id = "packages/${pkg.key}/${if (pkg.isolated) "release" else "isolate"}",
                    label = if (pkg.isolated) "Release isolation" else "Isolate",
                    cap = if (pkg.isolated) "sandboxed" else "private",
                    value = "hg2package ${if (pkg.isolated) "release" else "isolate"} ${pkg.manager} ${shellQuote(pkg.name)}"
                )
            )
            add(MenuNode("packages/${pkg.key}/update", "Update", value = "hg2package update ${pkg.manager} ${shellQuote(pkg.name)}"))
            add(
                MenuNode(
                    id = "packages/${pkg.key}/reset-preview",
                    label = "Reset impact",
                    cap = "preview",
                    emitsToken = false,
                    resolveChildren = { resetPreviewRows(context, pkg) }
                )
            )
            add(
                MenuNode(
                    id = "packages/${pkg.key}/restore-points",
                    label = "Restore points",
                    cap = "max 3",
                    emitsToken = false,
                    resolveChildren = { restorePointRows(context, pkg) }
                )
            )
            add(
                MenuNode(
                    id = "packages/${pkg.key}/reset",
                    label = "Reset",
                    cap = if (pkg.isolated) "reseed" else "wipe",
                    value = "hg2package reset ${pkg.manager} ${shellQuote(pkg.name)}"
                )
            )
            add(MenuNode("packages/${pkg.key}/info", "Info", value = "hg2package info ${pkg.manager} ${shellQuote(pkg.name)}"))
            add(
                MenuNode(
                    id = "packages/${pkg.key}/remove",
                    label = "Remove",
                    cap = "delete",
                    value = "hg2package remove ${pkg.manager} ${shellQuote(pkg.name)}"
                )
            )
            if (pkg.manager == "pkg") {
                add(
                    MenuNode(
                        id = "packages/${pkg.key}/purge",
                        label = "Purge",
                        cap = "all",
                        value = "hg2package purge ${pkg.manager} ${shellQuote(pkg.name)}"
                    )
                )
            }
        }

        val state = when {
            pkg.disabled && pkg.isolated -> "disabled · isolated"
            pkg.disabled -> "disabled"
            pkg.isolated -> "isolated"
            else -> pkg.version.ifBlank { "installed" }
        }
        return MenuNode(
            id = "packages/${pkg.key}",
            label = pkg.name,
            cap = state,
            children = actions,
            emitsToken = false
        )
    }

    private fun MutableList<MenuNode>.addDependencyViews(
        context: Context,
        pkg: PackageLifecycleStore.InstalledPackage
    ) {
        if (pkg.manager != "pkg") return
        add(
            MenuNode(
                id = "packages/${pkg.key}/dependencies",
                label = "Dependencies",
                cap = "closure",
                emitsToken = false,
                resolveChildren = {
                    val view = DpkgCatalog.dependencyView(DistroManager.prefixDir(context), pkg.name)
                    dependencyRows(pkg, view.directDependencies, view.dependencyClosure)
                }
            )
        )
        add(
            MenuNode(
                id = "packages/${pkg.key}/impact",
                label = "Removal impact",
                cap = "closure",
                emitsToken = false,
                resolveChildren = {
                    val view = DpkgCatalog.dependencyView(DistroManager.prefixDir(context), pkg.name)
                    impactRows(pkg, view.directDependents, view.dependentClosure)
                }
            )
        )
    }

    private fun resetPreviewRows(
        context: Context,
        pkg: PackageLifecycleStore.InstalledPackage
    ): List<MenuNode> {
        val preview = PackageLifecycleStore.previewReset(context, pkg)
        if (preview.paths.isEmpty()) {
            return listOf(MenuNode("packages/${pkg.key}/reset-preview/none", "Nothing to reset", "0 B", emitsToken = false))
        }
        return buildList {
            add(
                MenuNode(
                    id = "packages/${pkg.key}/reset-preview/summary",
                    label = if (preview.isolated) "Sandbox root" else "Candidate paths",
                    cap = "${preview.paths.size} · ${formatBytes(preview.bytes)}",
                    emitsToken = false
                )
            )
            preview.paths.forEachIndexed { index, path ->
                add(MenuNode("packages/${pkg.key}/reset-preview/$index", path, emitsToken = false))
            }
        }
    }

    private fun restorePointRows(
        context: Context,
        pkg: PackageLifecycleStore.InstalledPackage
    ): List<MenuNode> = buildList {
        add(
            MenuNode(
                id = "packages/${pkg.key}/restore-points/create",
                label = "Create restore point",
                cap = "snapshot",
                value = "hg2package snapshot ${pkg.manager} ${shellQuote(pkg.name)}"
            )
        )
        PackageRestorePoints.list(context, pkg).forEach { point ->
            add(
                MenuNode(
                    id = "packages/${pkg.key}/restore-points/${point.id}",
                    label = formatTimestamp(point.timestampMillis),
                    cap = "${point.pathCount} · ${formatBytes(point.bytes)}",
                    children = listOf(
                        MenuNode(
                            id = "packages/${pkg.key}/restore-points/${point.id}/restore",
                            label = "Restore",
                            cap = point.packageVersion.ifBlank { "snapshot" },
                            value = "hg2package restore ${pkg.manager} ${shellQuote(pkg.name)} ${point.id}"
                        ),
                        MenuNode(
                            id = "packages/${pkg.key}/restore-points/${point.id}/delete",
                            label = "Delete restore point",
                            cap = "delete",
                            value = "hg2package snapshot-delete ${pkg.manager} ${shellQuote(pkg.name)} ${point.id}"
                        )
                    ),
                    emitsToken = false
                )
            )
        }
    }

    private fun provenanceRows(
        context: Context,
        pkg: PackageLifecycleStore.InstalledPackage
    ): List<MenuNode> {
        if (pkg.isolated) {
            return listOf(
                MenuNode(
                    "packages/${pkg.key}/provenance/isolation",
                    "See Isolation Audit",
                    "sandbox",
                    emitsToken = false
                )
            )
        }
        val entries = PackageLifecycleStore.provenance(context, pkg)
        if (entries.isEmpty()) {
            return listOf(MenuNode("packages/${pkg.key}/provenance/none", "No observed writes yet", "0", emitsToken = false))
        }
        return entries.mapIndexed { index, entry ->
            MenuNode(
                id = "packages/${pkg.key}/provenance/$index",
                label = entry.path,
                cap = entry.change,
                emitsToken = false
            )
        }
    }

    private fun dependencyRows(
        pkg: PackageLifecycleStore.InstalledPackage,
        direct: List<String>,
        closure: List<String>
    ): List<MenuNode> = relationshipRows(
        idPrefix = "packages/${pkg.key}/dependencies",
        direct = direct,
        closure = closure,
        directLabel = "Direct",
        transitiveLabel = "Transitive"
    )

    private fun impactRows(
        pkg: PackageLifecycleStore.InstalledPackage,
        direct: List<String>,
        closure: List<String>
    ): List<MenuNode> = relationshipRows(
        idPrefix = "packages/${pkg.key}/impact",
        direct = direct,
        closure = closure,
        directLabel = "Direct dependents",
        transitiveLabel = "Transitive impact"
    )

    private fun relationshipRows(
        idPrefix: String,
        direct: List<String>,
        closure: List<String>,
        directLabel: String,
        transitiveLabel: String
    ): List<MenuNode> {
        if (closure.isEmpty()) return listOf(MenuNode("$idPrefix/none", "None", "0", emitsToken = false))
        val directSet = direct.toSet()
        val directRows = direct.map { name -> MenuNode("$idPrefix/direct/$name", name, emitsToken = false) }
        val transitiveRows = closure.filterNot(directSet::contains).map { name ->
            MenuNode("$idPrefix/transitive/$name", name, emitsToken = false)
        }
        return buildList {
            add(
                MenuNode(
                    id = "$idPrefix/direct",
                    label = directLabel,
                    cap = directRows.size.toString(),
                    children = directRows.ifEmpty { listOf(MenuNode("$idPrefix/direct/none", "None", "0", emitsToken = false)) },
                    emitsToken = false
                )
            )
            add(
                MenuNode(
                    id = "$idPrefix/transitive",
                    label = transitiveLabel,
                    cap = transitiveRows.size.toString(),
                    children = transitiveRows.ifEmpty { listOf(MenuNode("$idPrefix/transitive/none", "None", "0", emitsToken = false)) },
                    emitsToken = false
                )
            )
        }
    }

    private fun formatTimestamp(timestampMillis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestampMillis))

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "%.1f KiB".format(bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L -> "%.1f MiB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GiB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
}
