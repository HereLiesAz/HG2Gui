package com.hereliesaz.hg2gui.util

import android.content.Context
import android.util.Log

/**
 * Small shared constants and logging helpers still used by [StoppableThread],
 * [com.hereliesaz.hg2gui.util.libsuperuser.Shell] and the flashlight implementation - the rest
 * of what used to live here backed the legacy command engine and went with it.
 */
object Utils {

    const val SPACE = " "
    const val NEWLINE = "\n"
    const val DOT = "."
    const val EMPTYSTRING = ""

    private var applicationContext: Context? = null

    @JvmStatic
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    /** Returns the process-wide app context after [init], or null during uninitialized previews/tests. */
    fun applicationContextOrNull(): Context? = applicationContext

    @JvmStatic
    fun log(o: Any?) {
        if (o is Throwable) {
            Log.e("hg2gui", "", o)
        } else {
            Log.e("hg2gui", o.toString())
        }
    }
}
