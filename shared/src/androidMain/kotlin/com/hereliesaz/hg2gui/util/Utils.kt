package com.hereliesaz.hg2gui.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

/**
 * Small shared constants and logging/crash-reporting helpers still used by [StoppableThread],
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

    /** Broadcasts [s] on [PrivateIOReceiver.ACTION_OUTPUT] - nothing has listened for this since
     *  the legacy engine's output view went away, but it's a harmless no-op destination rather
     *  than a crash for the couple of call sites (flashlight errors) that still fire it. */
    @JvmStatic
    fun sendOutput(color: Int, context: Context, s: CharSequence) {
        val intent = Intent(PrivateIOReceiver.ACTION_OUTPUT)
        intent.putExtra(PrivateIOReceiver.TEXT, s)
        intent.putExtra(PrivateIOReceiver.COLOR, color)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    @JvmStatic
    fun log(o: Any?) {
        if (o is Throwable) {
            Log.e("hg2gui", "", o)
        } else {
            Log.e("hg2gui", o.toString())
        }
    }

    /** Appends a crash report to a file in app-private storage - best-effort, swallows its own
     *  failures since it only ever runs from an already-failing thread's exception handler. */
    @JvmStatic
    fun toFile(o: Any?) {
        val context = applicationContext ?: return
        if (o == null) return
        try {
            val stream = FileOutputStream(File(context.filesDir, "crash.txt"), true)
            stream.write((NEWLINE + NEWLINE).toByteArray())
            if (o is Throwable) {
                o.printStackTrace(PrintStream(stream))
            } else {
                stream.write(o.toString().toByteArray())
            }
            stream.write((NEWLINE + "----------------------------").toByteArray())
            stream.close()
        } catch (e: Exception) {
            // Nothing useful to do if the crash log itself can't be written.
        }
    }
}
