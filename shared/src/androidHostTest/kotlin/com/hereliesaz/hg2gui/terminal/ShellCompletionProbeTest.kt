package com.hereliesaz.hg2gui.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The bash completion probe's command line and exit-code policy; no process is started. */
class ShellCompletionProbeTest {

    @Test
    fun bashProbeIsNeverALoginShell() {
        val args = ShellCompletionBridge.bashProbeArgs("My File", "file")
        assertFalse(args.any { it == "-l" || it == "-lc" || it == "--login" })
        assertEquals(listOf("--noprofile", "--norc", "-c"), args.take(3))
    }

    @Test
    fun typedWordTravelsAsDataNeverInTheScript() {
        val hostile = "\$(touch /tmp/pwned);`id`"
        val args = ShellCompletionBridge.bashProbeArgs(hostile, "file")
        // script is args[3]; $0, $1, $2 follow.
        assertFalse(args[3].contains("touch"))
        assertEquals(listOf("hg2gui-complete", hostile, "file"), args.drop(4))
        assertTrue(args[3].contains("\"\$prefix\""))
    }

    @Test
    fun onlyZeroAndNoMatchAreNormalProbeExits() {
        assertEquals(setOf(0, 1), ShellCompletionBridge.PROBE_OK_EXIT_CODES)
    }
}
