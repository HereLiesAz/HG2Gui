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
        resolveChildren = { managerNodes(context) }
    )

    private fun managerNodes(context: Context): List<MenuNode> {
        val packages = PackageLifecycleStore.installed(context)
        if (packages.isEmpty()) {
            return listOf(
                MenuNode(
                    id = "packages/none",
                    label = "No installed packages",
                    cap = "…",
                    emitsToken = false
                )
            )
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
                            MenuNode(
                                id = "packages/${pkg.key}/run/$binary",
                                label = binary,
                                value = binary
                            )
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
                    id = "packages/${pkg.key}/update",
                    label = "Update",
                    value = "hg2package update ${pkg.manager} ${shellQuote(pkg.name)}"
                )
            )
            add(
                MenuNode(
                    id = "packages/${pkg.key}/reset",
                    label = "Reset",
                    cap = "wipe",
                    value = "hg2package reset ${pkg.manager} ${shellQuote(pkg.name)}"
                )
            )
            add(
                MenuNode(
                    id = "packages/${pkg.key}/info",
                    label = "Info",
                    value = "hg2package info ${pkg.manager} ${shellQuote(pkg.name)}"
                )
            )
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

        val state = if (pkg.disabled) "disabled" else pkg.version.ifBlank { "installed" }
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
