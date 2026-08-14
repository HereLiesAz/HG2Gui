package com.hereliesaz.hg2gui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationEngineTest {

    @Test
    fun testSimpleAddition() {
        assertEquals(4.0, CalculationEngine.eval("2+2"), 0.0)
    }

    @Test
    fun testMultiplication() {
        assertEquals(15.0, CalculationEngine.eval("3*5"), 0.0)
    }

    @Test
    fun testSqrt() {
        assertEquals(3.0, CalculationEngine.eval("sqrt(9)"), 0.0)
    }

    @Test
    fun testComplex() {
        assertEquals(14.0, CalculationEngine.eval("2+3*4"), 0.0)
    }
}
