package com.hereliesaz.hg2gui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.singleWindowApplication
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import com.hereliesaz.hg2gui.ui.menu.PillMenu
import com.hereliesaz.hg2gui.ui.menu.pageBrush

/**
 * Dev tooling, not the real app: the real HG2Gui is Android-only (its actual shell execution,
 * biometrics, and Termux integration are all androidMain-specific), and Compose Hot Reload's MCP
 * server - the thing this whole desktop target exists for - only runs against a JVM target, which
 * Android isn't. Rather than block UI iteration entirely, this renders the pure-UI pieces of the
 * menu system - PillMenu, StackPill's entrance animations, the pill-stack layout this session's
 * own PRs #186/#187 touched - against mock data and no-op callbacks, so both a human at Android
 * Studio and an AI agent driving the hot-reload MCP server's click/screenshot/get_semantic_tree
 * tools can see and interact with the real composables, live, without a device or emulator.
 *
 * Run via `./gradlew :shared:hotRunDesktop` (or Android Studio's own "Run with Compose Hot
 * Reload" gutter action once IDE support for a non-default main function is configured); the MCP
 * server itself starts via `:shared:hotMcpServerDesktop` (see the repo-root .mcp.json, which an
 * MCP-capable agent picks up automatically).
 *
 * In a headless environment (no real GPU/X server - a CI runner, a cloud coding session) Skiko's
 * default GL backend fails outright ("Cannot create Linux GL context"); setting
 * `SKIKO_RENDER_API=SOFTWARE` forces its CPU rasterizer instead, at some cost to frame rate that
 * doesn't matter for a preview nobody's scrolling quickly. .mcp.json already sets this for the
 * MCP server's own launch; set it by hand for a manual `hotRunDesktop` in the same kind of
 * environment.
 */
fun main() {
    Azphalt.rerollGround()
    singleWindowApplication(title = "HG2Gui - PillMenu preview") {
        PreviewRoot()
    }
}

@Composable
private fun PreviewRoot() {
    Box(Modifier.fillMaxSize().background(Azphalt.currentGround.pageBrush())) {
        PillMenu(roots = mockCommandTree())
    }
}

// A small, representative slice of the real command tree (see CommandTree.kt, androidMain-only
// since it scans the real PATH) - enough rows and depth to exercise the stack's own row-count-
// dependent behavior (width cycling, entrance stagger) without needing a real filesystem.
private fun mockCommandTree(): List<MenuNode> = listOf(
    MenuNode(
        id = "files",
        label = "Files",
        emitsToken = false,
        children = listOf(
            MenuNode(id = "files-ls", label = "ls", value = "ls"),
            MenuNode(id = "files-find", label = "find", value = "find"),
            MenuNode(id = "files-cp", label = "cp", value = "cp")
        )
    ),
    MenuNode(
        id = "git",
        label = "Git",
        emitsToken = false,
        children = listOf(
            MenuNode(id = "git-status", label = "status", value = "git status"),
            MenuNode(id = "git-log", label = "log", value = "git log"),
            MenuNode(id = "git-diff", label = "diff", value = "git diff"),
            MenuNode(id = "git-push", label = "push", value = "git push")
        )
    ),
    MenuNode(
        id = "net",
        label = "Network",
        emitsToken = false,
        children = listOf(
            MenuNode(id = "net-curl", label = "curl", value = "curl"),
            MenuNode(id = "net-ssh", label = "ssh", value = "ssh")
        )
    ),
    MenuNode(
        id = "pkg",
        label = "Package",
        emitsToken = false,
        children = listOf(
            MenuNode(id = "pkg-install", label = "install", value = "pkg install"),
            MenuNode(id = "pkg-upgrade", label = "upgrade", value = "pkg upgrade")
        )
    ),
    MenuNode(
        id = "proc",
        label = "Process",
        emitsToken = false,
        children = listOf(
            MenuNode(id = "proc-ps", label = "ps", value = "ps"),
            MenuNode(id = "proc-kill", label = "kill", value = "kill")
        )
    )
)
