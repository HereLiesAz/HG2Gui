package com.hereliesaz.hg2gui

interface PlatformContext {
    fun getString(resId: Int): String
    fun getString(resId: Int, vararg args: Any): String
}
