package com.hereliesaz.hg2gui.ui.menu

import com.hereliesaz.hg2gui.commands.CommandAbstraction

/*
 * The suggestion tree, built from the live CommandGroup rather than a hand-written list, so a
 * command added to the `raw` package shows up here without this file being touched.
 *
 * Commands have no name() — CommandGroup identifies them by class simple name, which is also
 * what the user types (`clear` is the class `clear`). That is the identifier used throughout.
 */

object CommandTree {

    private val SYSTEM = setOf(
        "airplane", "battery", "status", "bluetooth", "brightness", "calc", "call", "clear",
        "config", "data", "exit", "flash", "location", "music", "notifications", "refresh",
        "restart", "share", "shell", "time", "vibrate", "volume", "wifi"
    )
    private val APPS = setOf("apps", "alias", "cntcts", "open", "uninstall", "search")

    /** Argument hints for the commands whose arguments are not discoverable at runtime. */
    private val ARGS = mapOf(
        "wifi" to listOf("on", "off", "status"),
        "bluetooth" to listOf("on", "off", "scan"),
        "airplane" to listOf("on", "off"),
        "flash" to listOf("on", "off"),
        "brightness" to listOf("auto", "+10", "-10"),
        "volume" to listOf("media", "ring", "alarm"),
        "time" to listOf("now", "alarm"),
        "apps" to listOf("-ls", "-i", "-h"),
        "cntcts" to listOf("-ls", "-s"),
        "open" to listOf("url", "file"),
        "notes" to listOf("-a", "-ls"),
        "switchos" to listOf("ubuntu", "macos", "windows"),
        "tuixt" to listOf("file")
    )

    /**
     * The shell verbs offered alongside the built-ins. These run in the ShellSession rather
     * than the Java command engine, so the set is whatever the device's shell actually has —
     * a stock Android shell is toybox, so this is deliberately conservative.
     */
    private val SHELL = listOf(
        "ls" to listOf("-l", "-a", "-la"),
        "cd" to listOf("..", "/sdcard", "~"),
        "pwd" to emptyList(),
        "cat" to emptyList(),
        "ps" to listOf("-A"),
        "df" to listOf("-h"),
        "id" to emptyList(),
        "uname" to listOf("-a"),
        "getprop" to emptyList(),
        "ping" to listOf("-c 4 1.1.1.1")
    )

    private fun node(name: String): MenuNode = MenuNode(
        id = name,
        label = name,
        cap = ARGS[name]?.size?.toString() ?: "run",
        children = ARGS[name].orEmpty().map { MenuNode("$name/$it", it) }
    )

    private fun CommandAbstraction.commandName(): String = javaClass.simpleName

    fun from(commands: List<CommandAbstraction>): List<MenuNode> {
        val names = commands.map { it.commandName() }

        val system = names.filter { it in SYSTEM }
        val apps = names.filter { it in APPS }
        val features = names.filter { it !in SYSTEM && it !in APPS }

        val shell = SHELL.map { (name, args) ->
            MenuNode(
                id = "sh/$name",
                label = name,
                cap = if (args.isEmpty()) "run" else args.size.toString(),
                children = args.map { MenuNode("sh/$name/$it", it) }
            )
        }

        return listOf(
            MenuNode("sh", "Shell", shell.size.toString(), shell),
            MenuNode("sys", "System", system.size.toString(), system.map(::node)),
            MenuNode("apps", "Apps & nav", apps.size.toString(), apps.map(::node)),
            MenuNode("feat", "Features", features.size.toString(), features.map(::node))
        )
    }

    /** Fallback used by previews, and when the command engine has not finished loading. */
    fun empty(): List<MenuNode> = from(emptyList())
}
