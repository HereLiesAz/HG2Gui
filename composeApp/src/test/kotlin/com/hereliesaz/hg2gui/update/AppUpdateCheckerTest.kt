package com.hereliesaz.hg2gui.update

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun releaseSeries_takesMajorMinorFromVersionName() {
        assertEquals("0.7", AppUpdateChecker.releaseSeries("0.7.474.745"))
        assertEquals("1.12", AppUpdateChecker.releaseSeries("1.12.3"))
    }

    @Test
    fun releaseSeries_fallsBackWhenUnparseable() {
        assertEquals("0.7", AppUpdateChecker.releaseSeries(null))
        assertEquals("0.7", AppUpdateChecker.releaseSeries("dev"))
    }
}
