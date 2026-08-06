package com.hereliesaz.hg2gui.commands

import com.hereliesaz.hg2gui.PlatformContext
import kotlin.jvm.JvmField

abstract class ExecutePack {

    abstract val context: PlatformContext
    abstract val commandGroup: Any?

    @JvmField
    var args: Array<Any?>? = null
    @JvmField
    var currentIndex: Int = 0

    fun get(): Any? {
        val currentArgs = args ?: return null
        return if (currentIndex < currentArgs.size) currentArgs[currentIndex++] else null
    }

    fun get(index: Int): Any? {
        val currentArgs = args ?: return null
        return if (index < currentArgs.size) currentArgs[index] else null
    }

    fun getString(): String? = get() as? String

    fun getInt(): Int = get() as? Int ?: 0

    fun getBoolean(): Boolean = get() as? Boolean ?: false

    fun getList(): List<*>? = get() as? List<*>

    open fun getPrefsSave(): Any? = get()

    open fun getLaunchInfo(): Any? = get()

    fun set(args: Array<Any?>?) {
        this.args = args
        this.currentIndex = 0
    }

    open fun clear() {
        args = null
        currentIndex = 0
    }
}
