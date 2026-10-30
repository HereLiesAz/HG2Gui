package com.hereliesaz.hg2gui.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionPolicyTest {
    private val lifecycleActions = listOf(
        "info", "snapshot", "enable", "disable", "isolate", "release", "update", "reset", "restore",
        "snapshot-delete", "remove", "purge", "capability", "capability-request", "nonsense"
    )

    @Test
    fun interactive_deniesNothing() {
        val p = ExecutionPolicy.INTERACTIVE
        assertFalse(p.headless)
        lifecycleActions.forEach { assertNull(it, p.lifecycleDenial(it)) }
        assertNull(p.bootstrapDenial())
        assertNull(p.vfsMountDenial())
        assertNull(p.authorityDenial("root"))
        assertFalse(p.builtinNeedsApproval("call 555"))
    }

    @Test
    fun headless_allowsOnlyInfoAndSnapshot() {
        val allowed = setOf("info", "snapshot")
        for (policy in listOf(ExecutionPolicy.api(appInForeground = false), ExecutionPolicy.MCP, ExecutionPolicy.INSTALLER)) {
            assertTrue(policy.headless)
            for (action in lifecycleActions) {
                val denial = policy.lifecycleDenial(action)
                if (action in allowed) assertNull("${policy.origin} $action", denial) else assertNotNull("${policy.origin} $action", denial)
            }
        }
    }

    @Test
    fun enable_needsAPersonLookingAtHg2Gui() {
        assertNotNull(ExecutionPolicy.api(appInForeground = false).lifecycleDenial("enable"))
        assertNull(ExecutionPolicy.api(appInForeground = true).lifecycleDenial("enable"))
        assertNotNull(ExecutionPolicy.MCP.lifecycleDenial("enable"))
        // Foreground presence never unlocks the destructive actions (matches Hg2ApiCommandFilterTest).
        for (action in listOf("disable", "isolate", "update", "reset", "remove", "purge")) {
            assertNotNull(action, ExecutionPolicy.api(appInForeground = true).lifecycleDenial(action))
        }
    }

    @Test
    fun headless_deniesBootstrapVfsMountAndAuthority() {
        for (policy in listOf(ExecutionPolicy.api(true), ExecutionPolicy.api(false), ExecutionPolicy.MCP, ExecutionPolicy.INSTALLER)) {
            assertNotNull(policy.bootstrapDenial())
            assertNotNull(policy.vfsMountDenial())
            assertNotNull(policy.authorityDenial("root"))
            assertNotNull(policy.authorityDenial("adb"))
            assertNull(policy.authorityDenial("status"))
            assertNull(policy.authorityDenial(null))
        }
    }

    @Test
    fun denialMessagesNameTheCaller() {
        assertTrue(ExecutionPolicy.MCP.bootstrapDenial()!!.contains("MCP"))
        assertTrue(ExecutionPolicy.api(false).lifecycleDenial("disable")!!.contains("API"))
        assertEquals(126, ExecutionPolicy.DENIED_EXIT_CODE)
    }

    @Test
    fun builtinApprovalTable() {
        val needs = listOf(
            "call 555", "call alice", "contacts rm 5551234", "contacts ls", "contacts add", "brightness 50", "brightness auto",
            "wifi", "bluetooth", "airplane", "flash", "volume set media 10", "volume profile silent", "edit /sdcard/x", "vfs mount /mnt/x"
        )
        val free = listOf("calc 1+2", "volume", "volume get", "volume get media", "vfs ls", "vfs cat a", "vfs rm a", "vfs pwd", "ls", "")
        for (line in needs) assertTrue(line, BuiltinApproval.requiresApproval(line))
        for (line in free) assertFalse(line, BuiltinApproval.requiresApproval(line))
    }

    @Test
    fun builtinsAreOnlyGatedForHeadlessCallers() {
        assertTrue(ExecutionPolicy.MCP.builtinNeedsApproval("call 555"))
        assertTrue(ExecutionPolicy.api(true).builtinNeedsApproval("brightness 10"))
        assertFalse(ExecutionPolicy.MCP.builtinNeedsApproval("calc 1+2"))
        assertFalse(ExecutionPolicy.INTERACTIVE.builtinNeedsApproval("call 555"))
    }

    @Test
    fun authorityPrompts_showExactlyTheCommand() {
        val command = "sh -c \"cd /data/x && rm -rf *\""
        assertEquals("Run as root? $command [y/N]", AuthorityPrompts.rootShell(command))
        assertEquals("Run through ADB shell? $command [y/N]", AuthorityPrompts.adbShell(command))
        // The API dialog matches these prefixes to auto-answer only hg2auth's own prompts.
        assertTrue(AuthorityPrompts.ROOT_TEST.startsWith("Request root access to test"))
    }

    @Test
    fun updateOutcome_comparesVersions() {
        assertTrue(PackageUpdateOutcome.describe("git", "2.40", "2.45").changed)
        assertFalse(PackageUpdateOutcome.describe("git", "2.45", "2.45").changed)
        assertFalse(PackageUpdateOutcome.describe("git", "2.45", " 2.45 ").changed)
        assertFalse(PackageUpdateOutcome.describe("git", "2.45", null).changed)
        assertTrue(PackageUpdateOutcome.describe("git", "2.40", "2.45").message.contains("2.40 → 2.45"))
        assertTrue(PackageUpdateOutcome.describe("git", "2.45", "2.45").message.contains("already"))
    }
}
