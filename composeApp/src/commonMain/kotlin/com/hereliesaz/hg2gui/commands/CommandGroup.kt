package com.hereliesaz.hg2gui.commands

interface BaseCommandGroup {
    fun getCommandByName(name: String): CommandAbstraction?
    fun getCommands(): Array<CommandAbstraction>
    fun getCommandNames(): Array<String>
}
