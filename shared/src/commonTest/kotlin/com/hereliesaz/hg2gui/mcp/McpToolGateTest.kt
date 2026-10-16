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

    @Test
    fun hiddenTopLevelDotfilesAreProtected() {
        assertTrue(isMcpProtectedVfsPath("/.bash_profile"))
        assertTrue(isMcpProtectedVfsPath("/.bashrc"))
        assertTrue(isMcpProtectedVfsPath("/.profile"))
        assertTrue(isMcpProtectedVfsPath("/.zshrc"))
        assertTrue(isMcpProtectedVfsPath("/.hg2gui-env"))
        assertTrue(isMcpProtectedVfsPath("/.config/autostart/x.desktop"))
    }

    @Test
    fun sshDirectoryIsProtectedAtAnyDepth() {
        assertTrue(isMcpProtectedVfsPath("/.ssh/authorized_keys"))
        assertTrue(isMcpProtectedVfsPath("/projects/.ssh/config"))
    }

    @Test
    fun dotDotTraversalIsNormalizedBeforeChecking() {
        assertTrue(isMcpProtectedVfsPath("/notes/../.bashrc"))
        assertTrue(isMcpProtectedVfsPath("/./.profile"))
    }

    @Test
    fun rootItselfIsProtected() {
        assertTrue(isMcpProtectedVfsPath("/"))
        assertTrue(isMcpProtectedVfsPath("/notes/.."))
    }

    @Test
    fun ordinaryFilesAreNotProtected() {
        assertFalse(isMcpProtectedVfsPath("/notes.txt"))
        assertFalse(isMcpProtectedVfsPath("/Downloads/photo.png"))
        // Dotfiles nested below a visible top-level folder are user content, not shell config.
        assertFalse(isMcpProtectedVfsPath("/project/.gitignore"))
    }
}
