package com.hereliesaz.hg2gui.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure pieces of the signature API's policy: see Hg2ApiPolicy.kt. */
class Hg2ApiPolicyTest {

    // --- NOTIFY id clamp (m-4) ---------------------------------------------------------------

    @Test
    fun notificationIds_neverCollideWithOwnNotifications() {
        // 2001/2002 update, 2101 long-task service, 4201 MCP server, 0x4A000.. activity prompts.
        val owned = setOf(2001, 2002, 2101, 4201)
        listOf(2001, 2002, 2101, 4201, 0, 1, -1, Int.MIN_VALUE, Int.MAX_VALUE, 0x4A000, 0x59FFF).forEach { requested ->
            val clamped = clampNotificationId(requested)
            assertFalse("$requested -> $clamped", clamped in owned)
            assertTrue("$requested -> $clamped", clamped in 0x60000..0x6FFFF)
        }
    }

    @Test
    fun notificationIds_sameRequestSameSlot() {
        assertEquals(clampNotificationId(7), clampNotificationId(7))
        assertNotEquals(clampNotificationId(7), clampNotificationId(8))
        // A caller that picked 2101 lands in the private range at slot 2101, not on the service's id.
        assertEquals(0x60000 + 2101, clampNotificationId(2101))
    }

    // --- clipboard (m-4) ---------------------------------------------------------------------

    @Test
    fun clipboardRead_honestAboutBackgroundOnAndroid10Plus() {
        assertTrue(clipboardReadPossible(sdkInt = 28, appInForeground = false))
        assertFalse(clipboardReadPossible(sdkInt = 29, appInForeground = false))
        assertFalse(clipboardReadPossible(sdkInt = 35, appInForeground = false))
        assertTrue(clipboardReadPossible(sdkInt = 35, appInForeground = true))
    }

    // --- authority request validation (M-11) --------------------------------------------------

    @Test
    fun authorityCommand_isReturnedUnchangedApartFromOuterWhitespace() {
        // Quotes, spacing and metacharacters inside the command must survive byte for byte: this is the
        // string the dialog shows and the one argument the engine runs.
        val command = "sh -c \"cd /data/x && rm -rf *\"  ; echo 'it''s'"
        assertEquals(command, authorityCommandOrNull("root", command))
        assertEquals(command, authorityCommandOrNull("adb", "  $command \n"))
        assertEquals("pm list packages", authorityCommandOrNull("adb", "pm list packages"))
    }

    @Test
    fun authorityCommand_refusesUnknownAuthorityBlankAndOverlong() {
        assertNull(authorityCommandOrNull("sudo", "id"))
        assertNull(authorityCommandOrNull("", "id"))
        assertNull(authorityCommandOrNull("ROOT", "id")) // callers lower-case first
        assertNull(authorityCommandOrNull("root", ""))
        assertNull(authorityCommandOrNull("root", "  \n\t "))
        assertNull(authorityCommandOrNull("root", "x".repeat(MAX_AUTHORITY_COMMAND_CHARS + 1)))
    }

    @Test
    fun authorityCommand_acceptsTheLongestAllowed() {
        val command = "x".repeat(MAX_AUTHORITY_COMMAND_CHARS)
        assertEquals(command, authorityCommandOrNull("root", command))
    }

    // --- background-denied commands (M-2, defence in depth) ----------------------------------

    @Test
    fun backgroundDenied_lifecycleMutations() {
        listOf(
            "hg2package disable pip foo",
            "hg2package isolate npm foo",
            "hg2package update pkg git",
            "hg2package",
            "echo hi; hg2package disable pip foo",
            "/data/x/hg2package disable pip foo",
            "HG2PACKAGE disable pip foo",
            "env h\"g2\"package disable pip foo",
            "vfs mount /sdcard",
            "true && vfs mount /sdcard",
        ).forEach { assertTrue(it, containsBackgroundDeniedCommand(it, appInForeground = false)) }
    }

    @Test
    fun backgroundDenied_enableNeedsAForegroundApp() {
        assertTrue(containsBackgroundDeniedCommand("hg2package enable pip foo", appInForeground = false))
        assertFalse(containsBackgroundDeniedCommand("hg2package enable pip foo", appInForeground = true))
        // Still denied in the foreground: not an allowed operation for an external caller at all.
        assertTrue(containsBackgroundDeniedCommand("hg2package disable pip foo", appInForeground = true))
    }

    @Test
    fun backgroundDenied_allowsReadOnlyAndOrdinaryCommands() {
        assertFalse(containsBackgroundDeniedCommand("hg2package info pip foo", appInForeground = false))
        assertFalse(containsBackgroundDeniedCommand("hg2package snapshot pip foo", appInForeground = false))
        assertFalse(containsBackgroundDeniedCommand("ls -la", appInForeground = false))
        assertFalse(containsBackgroundDeniedCommand("vfs ls", appInForeground = false))
        assertFalse(containsBackgroundDeniedCommand("cat hg2package.log", appInForeground = false))
        assertFalse(containsBackgroundDeniedCommand("pkg install git", appInForeground = false))
    }

    // --- audit helpers (M-10) ----------------------------------------------------------------

    @Test
    fun commandDigest_isTheKnownSha256Prefix() {
        // Expected values computed with sha256sum, not with the code under test.
        assertEquals("e3b0c44298fc1c14", commandDigest(""))
        assertEquals("a56145270ce6b3bebd1dd012b73948677dd618d496488bc608a3cb43ce3547dd", commandDigest("id", chars = 64))
        assertEquals("e3b0c442", commandDigest("", chars = 8))
        assertNotEquals(commandDigest("id"), commandDigest("id "))
    }

    @Test
    fun auditPreview_boundsLongText() {
        assertEquals("short", auditPreview("short"))
        assertEquals("abc...", auditPreview("abcdef", max = 3))
        assertEquals("abc", auditPreview("abc", max = 3))
    }

    @Test
    fun retainAuditLines_keepsTheNewestNonBlankLines() {
        val lines = listOf("1", "", "2", "  ", "3", "4")
        assertEquals(listOf("3", "4"), retainAuditLines(lines, 2))
        assertEquals(listOf("1", "2", "3", "4"), retainAuditLines(lines, 10))
        assertEquals(emptyList<String>(), retainAuditLines(emptyList(), 5))
    }
}
