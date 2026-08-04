package com.hereliesaz.hg2gui.commands

import android.content.Context
import android.os.Build
import com.hereliesaz.hg2gui.commands.main.specific.APICommand
import com.hereliesaz.hg2gui.tuils.Tuils
import java.io.IOException

class CommandGroup(c: Context, private val packageName: String) : BaseCommandGroup {

    private var _commands: Array<CommandAbstraction> = emptyArray()
    private var _commandNames: Array<String> = emptyArray()

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
        _commandNames = cmds.toTypedArray()

        cmdAbs.sortWith { o1, o2 -> o2.priority() - o1.priority() }
        _commands = cmdAbs.toTypedArray()
    }

    override fun getCommandByName(name: String): CommandAbstraction? {
        for (c in _commands) {
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

    override fun getCommands(): Array<CommandAbstraction> {
        return _commands
    }

    override fun getCommandNames(): Array<String> {
        return _commandNames
    }
}
