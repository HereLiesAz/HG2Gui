package com.hereliesaz.hg2gui.mcp

import kotlin.test.Test
import kotlin.test.assertEquals
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

    // --- reads and listings (M-8) --------------------------------------------------------------

    @Test
    fun secretsAreUnreadable_notJustUnwritable() {
        assertTrue(isMcpReadProtectedVfsPath("/.ssh/id_ed25519"))
        assertTrue(isMcpReadProtectedVfsPath("/.bash_history"))
        assertTrue(isMcpReadProtectedVfsPath("/.hg2gui-env"))
        assertTrue(isMcpReadProtectedVfsPath("/.ssh"))
        assertTrue(isMcpReadProtectedVfsPath("/projects/.ssh/config"))
    }

    @Test
    fun readProtectionNormalizesTraversal() {
        assertTrue(isMcpReadProtectedVfsPath("/notes/../.ssh/id_rsa"))
        assertTrue(isMcpReadProtectedVfsPath("/./.bash_history"))
        assertTrue(isMcpReadProtectedVfsPath("\\.ssh\\id_rsa"))
        assertFalse(isMcpReadProtectedVfsPath("/.ssh/../notes.txt"))
    }

    @Test
    fun theRootStaysListable_butNotMutable() {
        // Listing "/" is the way in to everything else; only mutation of the root is refused.
        assertFalse(isMcpReadProtectedVfsPath("/"))
        assertFalse(isMcpReadProtectedVfsPath("/notes/.."))
        assertTrue(isMcpProtectedVfsPath("/"))
    }

    @Test
    fun ordinaryContentIsReadable() {
        assertFalse(isMcpReadProtectedVfsPath("/notes.txt"))
        assertFalse(isMcpReadProtectedVfsPath("/Downloads/photo.png"))
        assertFalse(isMcpReadProtectedVfsPath("/project/.gitignore"))
    }

    @Test
    fun readAndWriteProtectionAgreeOutsideTheRoot() {
        listOf("/.bashrc", "/.ssh/authorized_keys", "/notes.txt", "/a/.ssh/x", "/project/.gitignore").forEach { path ->
            assertEquals(isMcpProtectedVfsPath(path), isMcpReadProtectedVfsPath(path), path)
        }
    }
}
