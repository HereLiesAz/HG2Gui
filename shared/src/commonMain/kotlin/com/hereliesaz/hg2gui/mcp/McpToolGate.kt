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
