package com.hereliesaz.hg2gui.mcp

/**
 * True iff [toolName] names a `shell.*` tool and shell execution is currently disabled - the one
 * gate every "tools/call" request for a shell.* name must pass through. Lives in commonMain and
 * takes no platform type (not even the `StateFlow<Boolean>` `McpTools` itself holds) specifically
 * so it's unit-testable without a Robolectric/instrumented Android runtime - see
 * `McpToolGateTest` in commonTest. `McpTools.callTool` (androidMain) is the only caller.
 */
fun isShellToolGated(toolName: String, shellExecEnabled: Boolean): Boolean =
    toolName.startsWith("shell.") && !shellExecEnabled

/**
 * True iff an MCP `vfs.*` mutation (write/mkdir/delete/move/copy) must be refused for the
 * root-absolute vfs [path]. The MCP vfs root is the real Termux `$HOME`, so with no guard a
 * paired agent could `vfs.write` `~/.bash_profile` (or `~/.bashrc`, `~/.ssh/authorized_keys`,
 * `~/.config/...`, the app's own `.hg2gui-*` files) and get code execution on the next shell
 * start even while shell execution is switched off - the shell-exec gate above would be
 * meaningless. So every hidden top-level entry (anything whose first path segment starts with
 * "."), any `.ssh` directory at any depth, and the root itself are off limits for mutation.
 * "." / ".." segments are normalized first so "/a/../.bashrc" can't sneak past; the caller also
 * re-checks the canonical (symlink-resolved) path for the same reason.
 */
fun isMcpProtectedVfsPath(path: String): Boolean {
    val segments = ArrayList<String>()
    for (segment in path.split('/', '\\')) {
        when (segment) {
            "", "." -> Unit
            ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.size - 1)
            else -> segments.add(segment)
        }
    }
    if (segments.isEmpty()) return true
    if (segments.first().startsWith(".")) return true
    return segments.any { it == ".ssh" }
}
