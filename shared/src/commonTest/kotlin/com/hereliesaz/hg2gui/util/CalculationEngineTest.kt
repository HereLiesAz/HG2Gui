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

    @Test
    fun testPowerAfterFunctionAppliesToResult() {
        assertEquals(9.0, CalculationEngine.eval("sqrt(9)^2"), 1e-9)
        assertEquals(1.0, CalculationEngine.eval("sin(90)^2"), 1e-9)
    }

    @Test
    fun testPowerAndParens() {
        assertEquals(8.0, CalculationEngine.eval("2^3"), 0.0)
        assertEquals(25.0, CalculationEngine.eval("(2+3)^2"), 0.0)
        assertEquals(20.0, CalculationEngine.eval("(2+3)*4"), 0.0)
        assertEquals(-4.0, CalculationEngine.eval("-2^2"), 0.0)
    }

    @Test
    fun testTrigInDegreesAndCaseInsensitive() {
        assertEquals(1.0, CalculationEngine.eval("sin(90)"), 1e-9)
        assertEquals(1.0, CalculationEngine.eval("COS(0)"), 1e-9)
        assertEquals(1.0, CalculationEngine.eval("Tan(45)"), 1e-9)
        assertEquals(4.0, CalculationEngine.eval("SQRT(16)"), 0.0)
    }

    @Test
    fun testWhitespace() {
        assertEquals(7.0, CalculationEngine.eval(" 1 + 2 * 3 "), 0.0)
    }

    @Test(expected = RuntimeException::class)
    fun testMissingCloseParenIsError() {
        CalculationEngine.eval("(2+3")
    }

    @Test(expected = RuntimeException::class)
    fun testFunctionMissingCloseParenIsError() {
        CalculationEngine.eval("sqrt(9")
    }

    @Test(expected = RuntimeException::class)
    fun testUnknownFunctionIsError() {
        CalculationEngine.eval("foo(1)")
    }

    @Test(expected = RuntimeException::class)
    fun testTrailingGarbageIsError() {
        CalculationEngine.eval("2+2)")
    }
}
