package com.hereliesaz.hg2gui

import com.hereliesaz.hg2gui.terminal.PackageIsolation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure parts of the Isolation Audit screen: what "send to AI" transmits, and what it tells the user. */
class IsolationAuditTest {

    private fun entry(audit: PackageIsolation.Audit) = IsolationAuditEntry(
        packageName = "demo",
        managerLabel = "pkg",
        version = "1.2",
        savedAudit = PackageIsolation.SavedAudit(1L, audit),
        fileCount = 3,
        bytes = 2048L
    )

    @Test
    fun promptSendsBasenamesNotPaths() {
        val prompt = auditInterpretationPrompt(
            entry(PackageIsolation.Audit(created = listOf("/data/user/0/app/files/x/bin/tool"), modified = emptyList(), deleted = emptyList()))
        )
        assertTrue(prompt.contains("created: tool"))
        assertFalse(prompt.contains("/data/user/0"))
    }

    @Test
    fun promptDropsSensitiveNamesAndCapsEachGroup() {
        val created = listOf("/s/id_token.txt") + (1..AI_MAX_ROWS + 10).map { "/s/file$it" }
        val prompt = auditInterpretationPrompt(entry(PackageIsolation.Audit(created, emptyList(), emptyList())))
        assertFalse(prompt.contains("id_token"))
        assertEquals(AI_MAX_ROWS, prompt.lines().count { it.startsWith("created: ") })
    }

    @Test
    fun promptRedactsNetworkPeersToAProtocolAndPort() {
        val telemetry = listOf("network:tcp:0100007F:1F90:0A01020B:01BB:01:123", "process:sh -c secret-cmd")
        val prompt = auditInterpretationPrompt(entry(PackageIsolation.Audit(emptyList(), emptyList(), emptyList(), telemetry)))
        assertTrue(prompt.contains("network: tcp remote port 443"))
        assertTrue(prompt.contains("observed NETWORK events: 1"))
        assertTrue(prompt.contains("observed PROCESS events: 1"))
        assertFalse(prompt.contains("0A01020B"))
        assertFalse(prompt.contains("secret-cmd"))
    }

    @Test
    fun disclosureNamesWhatTheCodeActuallySends() {
        val text = aiDisclosureText()
        assertTrue(text.contains("file names (never the folders)"))
        assertTrue(text.contains("how many runtime events of each kind"))
        assertTrue(text.contains("remote port (never the address)"))
        assertTrue(text.contains("up to $AI_MAX_ROWS"))
    }

    @Test
    fun redactedNetworkRowRejectsUnparseableRows() {
        assertNull(redactedNetworkRow("network:tcp:garbage"))
        assertEquals("udp remote port 53", redactedNetworkRow("network:udp:0100007F:0035:08080808:0035:07:1"))
    }

    @Test
    fun nextVisibleCountStepsAndStopsAtTheTotal() {
        assertEquals(80, nextVisibleCount(40, 200, 40))
        assertEquals(60, nextVisibleCount(40, 60, 40))
        assertEquals(40, nextVisibleCount(40, 40, 40))
        assertEquals(40, nextVisibleCount(40, 10, 40))
    }

    @Test
    fun uiScaleFollowsTheSettingAndIsClamped() {
        assertEquals(1f, uiScaleFromPercent(100), 0f)
        assertEquals(1.5f, uiScaleFromPercent(150), 0f)
        assertEquals(0.85f, uiScaleFromPercent(10), 0f)
        assertEquals(2f, uiScaleFromPercent(900), 0f)
    }

    @Test
    fun formatBytesUsesBinaryUnits() {
        assertEquals("512 B", formatBytes(512))
        assertEquals("2.0 KiB", formatBytes(2048))
    }
}
