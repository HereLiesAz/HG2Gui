package com.hereliesaz.hg2gui.mcp

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class McpToolGateTest {

    @Test
    fun shellToolIsGatedWhenDisabled() {
        assertTrue(isShellToolGated("shell.exec", shellExecEnabled = false))
    }

    @Test
    fun shellToolIsNotGatedWhenEnabled() {
        assertFalse(isShellToolGated("shell.exec", shellExecEnabled = true))
    }

    @Test
    fun vfsToolIsNeverGatedRegardlessOfShellExecFlag() {
        assertFalse(isShellToolGated("vfs.read", shellExecEnabled = false))
        assertFalse(isShellToolGated("vfs.write", shellExecEnabled = true))
    }

    @Test
    fun onlyTheShellDotPrefixCounts() {
        // A tool merely named after the shell ("shelldone.x") must not be swept into the
        // shell.* gate by a loose substring check - only the literal "shell." prefix counts.
        assertFalse(isShellToolGated("shelldone.x", shellExecEnabled = false))
    }
}
