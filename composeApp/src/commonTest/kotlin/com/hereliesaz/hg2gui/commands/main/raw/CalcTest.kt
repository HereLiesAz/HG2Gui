package com.hereliesaz.hg2gui.commands.main.raw

import com.hereliesaz.hg2gui.PlatformContext
import com.hereliesaz.hg2gui.commands.ExecutePack
import org.junit.Assert.assertEquals
import org.junit.Test

class CalcTest {

    private val mockPack = object : ExecutePack() {
        override val context: PlatformContext = object : PlatformContext {
            override fun getString(resId: Int): String = ""
            override fun getString(resId: Int, vararg args: Any): String = ""
        }
        override fun getPrefsSave(): Any? = null
        override fun getLaunchInfo(): Any? = null
    }

    @Test
    fun testSimpleAddition() {
        val cmd = calc()
        mockPack.args = arrayOf("2+2")
        mockPack.currentIndex = 0

        val result = cmd.exec(mockPack)
        assertEquals("4.0", result)
    }

    @Test
    fun testMultiplication() {
        val cmd = calc()
        mockPack.args = arrayOf("3*5")
        mockPack.currentIndex = 0

        val result = cmd.exec(mockPack)
        assertEquals("15.0", result)
    }

    @Test
    fun testSqrt() {
        val cmd = calc()
        mockPack.args = arrayOf("sqrt(9)")
        mockPack.currentIndex = 0

        val result = cmd.exec(mockPack)
        assertEquals("3.0", result)
    }

    @Test
    fun testComplex() {
        val cmd = calc()
        mockPack.args = arrayOf("2+3*4")
        mockPack.currentIndex = 0

        val result = cmd.exec(mockPack)
        assertEquals("14.0", result)
    }
}
