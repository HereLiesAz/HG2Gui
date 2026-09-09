package com.hereliesaz.hg2gui.ui.menu

import android.content.Context
import com.hereliesaz.hg2gui.terminal.PackageLifecycleStore

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
                val children = installed.sortedBy { it.name.lowercase() }.map(::packageNode)
                MenuNode(
                    id = "packages/${manager.first}",
                    label = manager.second,
                    cap = children.size.toString(),
                    children = children,
                    emitsToken = false
                )
            }
    }

    private fun packageNode(pkg: PackageLifecycleStore.InstalledPackage): MenuNode {
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

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
}
