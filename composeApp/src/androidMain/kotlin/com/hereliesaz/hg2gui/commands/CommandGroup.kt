package com.hereliesaz.hg2gui.commands

import android.content.Context
import android.os.Build
import com.hereliesaz.hg2gui.commands.main.specific.APICommand
import com.hereliesaz.hg2gui.tuils.Tuils
import java.io.IOException

class CommandGroup(c: Context, private val packageName: String) : BaseCommandGroup {

    override val commands: Array<CommandAbstraction>
    override val commandNames: Array<String>

    init {
        var cmds: MutableList<String>
        try {
            cmds = Tuils.getClassesInPackage(packageName, c).toMutableList()
        } catch (e: IOException) {
            cmds = mutableListOf()
        }

        val cmdAbs = mutableListOf<CommandAbstraction>()
        val iterator = cmds.iterator()
        while (iterator.hasNext()) {
            val s = iterator.next()
            val ca = buildCommand(s)
            if (ca != null && (ca !is APICommand || ca.willWorkOn(Build.VERSION.SDK_INT))) {
                cmdAbs.add(ca)
            } else {
                iterator.remove()
            }
        }

        cmds.sort()
        commandNames = cmds.toTypedArray()

        cmdAbs.sortWith { o1, o2 -> o2.priority() - o1.priority() }
        commands = cmdAbs.toTypedArray()
    }

    override fun getCommandByName(name: String): CommandAbstraction? {
        for (c in commands) {
            if (c.javaClass.simpleName == name) {
                return c
            }
        }
        return null
    }

    private fun buildCommand(name: String): CommandAbstraction? {
        val fullCmdName = packageName + Tuils.DOT + name
        return try {
            val clazz = Class.forName(fullCmdName) as Class<CommandAbstraction>
            if (CommandAbstraction::class.java.isAssignableFrom(clazz)) {
                val constructor = clazz.getConstructor()
                constructor.newInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // These satisfy the BaseCommandGroup interface if it defines them as functions,
    // because Kotlin properties generate the corresponding getX() methods.
    // However, if the interface defines them as functions, we might need to name the properties differently
    // or just implement them as functions.
    // Let's check BaseCommandGroup again.
}
