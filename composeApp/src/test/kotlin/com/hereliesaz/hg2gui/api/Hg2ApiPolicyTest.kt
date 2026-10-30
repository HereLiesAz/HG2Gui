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

    // --- approval text vs executed text (M-11) -----------------------------------------------

    @Test
    fun rawCommandLosesItsQuotesInTheEngine_theBugThisGuards() {
        // The old activity built "hg2auth root shell $command". The engine's tokenizer strips the
        // quotes, so the text it runs is NOT what the dialog showed.
        val command = "sh -c \"cd /data/x && rm -rf *\""
        val naive = "hg2auth root shell $command"
        assertEquals("sh -c cd /data/x && rm -rf *", Hg2ApiAuthorityCommand.effectiveCommand(naive))
        assertNotEquals(command, Hg2ApiAuthorityCommand.effectiveCommand(naive))
    }

    @Test
    fun lossless_carriesTheCommandUnchanged() {
        listOf(
            "sh -c \"cd /data/x && rm -rf *\"",
            "pm list packages",
            "echo it's",
            "a  b",
            "echo \"unterminated",
        ).forEach { command ->
            val line = Hg2ApiAuthorityCommand.lossless("root", command)
            assertEquals(command, line?.let(Hg2ApiAuthorityCommand::effectiveCommand))
        }
    }

    @Test
    fun lossless_exactLines() {
        assertEquals(
            "hg2auth root shell 'sh -c \"cd /data/x && rm -rf *\"'",
            Hg2ApiAuthorityCommand.lossless("root", "sh -c \"cd /data/x && rm -rf *\"")
        )
        assertEquals("hg2auth adb shell \"echo it's\"", Hg2ApiAuthorityCommand.lossless("adb", "echo it's"))
        // Surrounding whitespace is trimmed, like the engine does.
        assertEquals("hg2auth root shell 'id'", Hg2ApiAuthorityCommand.lossless("root", "  id  "))
    }

    @Test
    fun lossless_refusesWhatItCannotCarry() {
        // Mixed quote styles with unquoted spaces survive no wrapping.
        assertNull(Hg2ApiAuthorityCommand.lossless("root", "echo \"x y\" it's"))
        assertNull(Hg2ApiAuthorityCommand.lossless("root", ""))
        assertNull(Hg2ApiAuthorityCommand.lossless("root", "   "))
        assertNull(Hg2ApiAuthorityCommand.lossless("root", "x".repeat(MAX_AUTHORITY_COMMAND_CHARS + 1)))
        assertNull(Hg2ApiAuthorityCommand.lossless("sudo", "id"))
        assertNull(Hg2ApiAuthorityCommand.lossless("", "id"))
    }

    @Test
    fun lossless_acceptsTheLongestAllowedCommand() {
        val command = "x".repeat(MAX_AUTHORITY_COMMAND_CHARS)
        assertEquals(command, Hg2ApiAuthorityCommand.lossless("root", command)?.let(Hg2ApiAuthorityCommand::effectiveCommand))
    }

    @Test
    fun expectedPrompt_matchesTheEnginesConfirmationText() {
        assertEquals("Run as root? id [y/N]", Hg2ApiAuthorityCommand.expectedPrompt("root", "id"))
        assertEquals("Run through ADB shell? pm list packages [y/N]", Hg2ApiAuthorityCommand.expectedPrompt("adb", "pm list packages"))
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
