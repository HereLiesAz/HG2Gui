package com.hereliesaz.hg2gui.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TermuxReleaseFreshnessTest {

    private fun release(vararg fields: String) = (fields.toList() + "SHA256:" + " abc 1 main/x").joinToString("\n")

    private val aug16 = parseReleaseDate("Sun, 16 Aug 2026 12:00:00 UTC")!!

    @Test
    fun parsesDebianDate() {
        assertNotNull(parseReleaseDate("Sun, 16 Aug 2026 12:00:00 UTC"))
        assertNull(parseReleaseDate("not a date"))
    }

    @Test
    fun acceptsFreshReleaseAndReturnsDate() {
        val text = release("Date: Sun, 16 Aug 2026 12:00:00 UTC", "Valid-Until: Sun, 23 Aug 2026 12:00:00 UTC")
        assertEquals(aug16, checkReleaseFreshness(text, aug16 + 1000, aug16 - 1000))
    }

    @Test
    fun rejectsExpiredValidUntil() {
        val text = release("Date: Sun, 16 Aug 2026 12:00:00 UTC", "Valid-Until: Sun, 16 Aug 2026 13:00:00 UTC")
        assertThrows(IllegalStateException::class.java) {
            checkReleaseFreshness(text, aug16 + 2 * 3_600_000L, null)
        }
    }

    @Test
    fun rejectsDateOlderThanLastAccepted() {
        val text = release("Date: Sun, 16 Aug 2026 12:00:00 UTC")
        assertThrows(IllegalStateException::class.java) {
            checkReleaseFreshness(text, aug16, aug16 + 1)
        }
    }

    @Test
    fun releaseWithoutDateFieldsPasses() {
        assertNull(checkReleaseFreshness(release("Origin: Termux"), aug16, aug16))
    }
}
