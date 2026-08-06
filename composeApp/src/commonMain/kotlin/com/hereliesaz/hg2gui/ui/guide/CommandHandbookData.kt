package com.hereliesaz.hg2gui.ui.guide

import com.hereliesaz.hg2gui.ui.menu.MenuNode

object CommandHandbookData {
    val handbookNodes: List<MenuNode> = listOf(
        MenuNode(
            id = "guide_pkg",
            label = "PKG",
            cap = "Package Management",
            children = listOf(
                MenuNode(id = "pkg_install", label = "install", cap = "Install a package"),
                MenuNode(id = "pkg_uninstall", label = "uninstall", cap = "Remove a package"),
                MenuNode(id = "pkg_upgrade", label = "upgrade", cap = "Upgrade packages"),
                MenuNode(id = "pkg_search", label = "search", cap = "Search packages"),
                MenuNode(id = "pkg_list-all", label = "list-all", cap = "List all packages")
            )
        ),
        MenuNode(
            id = "guide_termux_api",
            label = "API",
            cap = "Termux API",
            children = listOf(
                MenuNode(id = "api_battery", label = "termux-battery-status", cap = "Get battery status"),
                MenuNode(id = "api_camera", label = "termux-camera-photo", cap = "Take a photo"),
                MenuNode(id = "api_clipboard", label = "termux-clipboard-get", cap = "Get clipboard"),
                MenuNode(id = "api_location", label = "termux-location", cap = "Get location")
            )
        ),
        MenuNode(
            id = "guide_sys",
            label = "SYS",
            cap = "System & File",
            children = listOf(
                MenuNode(id = "sys_ls", label = "ls", cap = "List files"),
                MenuNode(id = "sys_cd", label = "cd", cap = "Change directory"),
                MenuNode(id = "sys_rm", label = "rm", cap = "Remove file/dir"),
                MenuNode(id = "sys_cat", label = "cat", cap = "Print file content"),
                MenuNode(id = "sys_top", label = "top", cap = "System monitor")
            )
        )
    )
}
