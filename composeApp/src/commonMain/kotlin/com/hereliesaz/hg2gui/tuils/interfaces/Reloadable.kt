package com.hereliesaz.hg2gui.tuils.interfaces

/**
 * Created by francescoandreuzzi on 05/03/2018.
 */
interface Reloadable {
    companion object {
        const val MESSAGE = "msg"
    }

    fun reload()
    fun addMessage(header: String, message: String)

    class ReloadMessageCategory(@JvmField val header: String) {
        @JvmField
        val lines: MutableList<String> = mutableListOf()

        fun text(): CharSequence {
            val sequence = "$header\n"
            val builder = StringBuilder()
            val dash = "-"
            for (line in lines) {
                builder.append(" ").append(dash).append(" ").append(line).append("\n")
            }
            return "$sequence$builder"
        }

        override fun toString(): String {
            return text().toString()
        }
    }
}
