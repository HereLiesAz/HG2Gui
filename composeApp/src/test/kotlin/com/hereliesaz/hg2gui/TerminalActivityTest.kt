package com.hereliesaz.hg2gui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TerminalActivityTest {

    @Test
    fun bootstrapDownloadFraction_parsesAMatchingLine() {
        assertEquals(0.25f, bootstrapDownloadFraction("Downloaded: 10MB / 40MB"))
    }

    @Test
    fun bootstrapDownloadFraction_coercesAnOvershootDownloadToOne() {
        // A chunk can land right after the total was already reported reached - the fraction
        // still has to read as "done", not a value over 100%.
        assertEquals(1f, bootstrapDownloadFraction("Downloaded: 41MB / 40MB"))
    }

    @Test
    fun bootstrapDownloadFraction_returnsNullForAnUnknownTotal() {
        // The server didn't report Content-Length - see DistroManager.bootstrap's own "?" case.
        assertNull(bootstrapDownloadFraction("Downloaded: 10MB / ?"))
    }

    @Test
    fun bootstrapDownloadFraction_returnsNullForUnrelatedOutput() {
        assertNull(bootstrapDownloadFraction("Starting bootstrap process..."))
        assertNull(bootstrapDownloadFraction("Bootstrap successful! You can now use 'apt', 'pkg', and real coreutils."))
    }
}
