package com.hereliesaz.hg2gui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The exit-code gate completion probes use so a failed launch is never parsed as data. */
class ProcessOutputTest {
    private fun output(code: Int, text: String = "x\ny\n") = ProcessOutput(code, text.toByteArray(Charsets.UTF_8), false)

    @Test
    fun zeroExitReturnsTheText() {
        assertEquals("x\ny\n", output(0).textIfExit())
    }

    @Test
    fun nonZeroExitIsRejectedByDefault() {
        assertNull(output(1).textIfExit())
        assertNull(output(127, "CANNOT LINK EXECUTABLE").textIfExit())
        assertNull(output(134, "CANNOT LINK EXECUTABLE").textIfExit())
    }

    @Test
    fun callerCanAcceptSpecificNonZeroCodes() {
        val ok = setOf(0, 1)
        assertEquals("", output(1, "").textIfExit(ok))
        assertNull(output(2).textIfExit(ok))
        assertNull(output(126).textIfExit(ok))
    }

    @Test
    fun decodesUtf8() {
        assertEquals("café", output(0, "café").textIfExit())
    }

    @Test
    fun collectOutput_reportsTheRealExitCode() {
        val sh = java.io.File("/bin/sh")
        org.junit.Assume.assumeTrue(sh.canExecute())
        val failing = ProcessBuilder("/bin/sh", "-c", "echo out; echo err >&2; exit 3").start()
            .collectOutput(5_000, 1_024)!!
        assertEquals(3, failing.exitCode)
        assertEquals("out\n", failing.stdout.toString(Charsets.UTF_8))
        assertNull(failing.textIfExit())
    }
}
