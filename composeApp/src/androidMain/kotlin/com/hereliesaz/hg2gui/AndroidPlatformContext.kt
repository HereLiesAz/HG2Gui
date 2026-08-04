package com.hereliesaz.hg2gui

import android.content.Context

class AndroidPlatformContext(val androidContext: Context) : PlatformContext {
    override fun getString(resId: Int): String = androidContext.getString(resId)
    override fun getString(resId: Int, vararg args: Any): String = androidContext.getString(resId, *args)
}
