package com.hereliesaz.hg2gui.commands

interface BaseCommandGroup {
    fun getCommandByName(name: String): CommandAbstraction?
    val commands: Array<CommandAbstraction>
    val commandNames: Array<String>
}
