package com.hereliesaz.hg2gui.commands.main.raw

import com.hereliesaz.hg2gui.PlatformContext
import com.hereliesaz.hg2gui.commands.ExecutePack
import com.hereliesaz.hg2gui.managers.SystemContext
import org.junit.Assert.*
import org.junit.Test

class SwitchOsTest {

    private val mockPack = object : ExecutePack() {
        override val context: PlatformContext = object : PlatformContext {
            override fun getString(resId: Int): String = ""
            override fun getString(resId: Int, vararg args: Any): String = ""
        }
        override fun getPrefsSave(): Any? = null
        override fun getLaunchInfo(): Any? = null
    }

    @Test
    fun testSwitchToMac() {
        val cmd = switchos()
        mockPack.args = arrayOf("macos")
        mockPack.currentIndex = 0

        val result = cmd.exec(mockPack)

        assertNotNull(result)
        assertTrue(result.contains("KERNEL SWITCHED TO: MACOS"))
        assertEquals(SystemContext.OSType.MACOS, SystemContext.getInstance().os)
    }

    @Test
    fun testSwitchToWindows() {
        val cmd = switchos()
        mockPack.args = arrayOf("windows")
        mockPack.currentIndex = 0

        val result = cmd.exec(mockPack)

        assertTrue(result.contains("KERNEL SWITCHED TO: WINDOWS"))
        assertEquals(SystemContext.OSType.WINDOWS, SystemContext.getInstance().os)
    }

    @Test
    fun testInvalidOs() {
        val cmd = switchos()
        mockPack.args = arrayOf("templeos")
        mockPack.currentIndex = 0

        val result = cmd.exec(mockPack)

        assertTrue(result.contains("NOT FOUND"))
    }
}
