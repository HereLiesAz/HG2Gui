package com.hereliesaz.hg2gui.ui.guide

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuideSupportTest {

    @Test
    fun wipeStaggerGrowsThenStopsAtTheCap() {
        assertEquals(0L, guideWipeDelayMs(0))
        assertEquals(GUIDE_STAGGER_STEP_MS, guideWipeDelayMs(1))
        assertEquals(GUIDE_STAGGER_MAX_MS, guideWipeDelayMs(1000))
        // Never decreasing, and a negative index is treated as the first block.
        var previous = 0L
        for (seq in 0..40) {
            val delay = guideWipeDelayMs(seq)
            assertTrue(delay >= previous, "delay must not shrink at seq $seq")
            previous = delay
        }
        assertEquals(0L, guideWipeDelayMs(-3))
    }

    @Test
    fun wholeWipeStaysWithinTheBudget() {
        for (seq in 0..200) {
            for (wide in listOf(true, false)) {
                val finish = guideWipeDelayMs(seq) + guideWipeDurationMs(wide)
                assertTrue(finish <= GUIDE_WIPE_TOTAL_CAP_MS, "seq $seq wide=$wide ends at $finish ms")
            }
        }
    }

    @Test
    fun commandWordingNamesTheCommandAndTrimsIt() {
        assertEquals("Add chmod to command", guideAddActionLabel("chmod"))
        assertEquals("Add chmod -R to command", guideAddActionLabel(" chmod -R "))
        assertEquals("Added “chown” to input", guideAddedMessage("chown "))
    }
}
