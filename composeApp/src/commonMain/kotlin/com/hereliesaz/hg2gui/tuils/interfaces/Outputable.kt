package com.hereliesaz.hg2gui.tuils.interfaces

/**
 * Created by andre on 25/07/15.
 */
interface Outputable {
    fun onOutput(output: CharSequence, category: Int)
    fun onOutput(color: Int, output: CharSequence)
    fun onOutput(output: CharSequence)
    fun dispose()
}
