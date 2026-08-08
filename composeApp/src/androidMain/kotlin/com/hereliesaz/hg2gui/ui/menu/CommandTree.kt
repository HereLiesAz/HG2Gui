package com.hereliesaz.hg2gui.ui.menu

import android.content.Context
import com.hereliesaz.hg2gui.commands.CommandAbstraction
import com.hereliesaz.hg2gui.terminal.DistroManager
import java.io.File

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
        "notes" to listOf("-a", "-ls"),
        "switchos" to listOf("ubuntu", "macos", "windows")
    )

    /** Built-ins whose argument is a file, not a literal choice - they get a file… picker child. */
    private val FILE_PARAM_COMMANDS = setOf("open", "tuixt")

    /** Curated flag hints for the handful of real shell binaries worth hand-picking for; every
     *  other binary discovered on PATH still gets a plain file… child and can take whatever
     *  else the user types besides. */
    private val SHELL_HINTS = mapOf(
        "ls" to listOf("-l", "-a", "-la"),
        "cd" to listOf("..", "/sdcard", "~"),
        "ps" to listOf("-A"),
        "df" to listOf("-h"),
        "uname" to listOf("-a"),
        "ping" to listOf("-c 4 1.1.1.1")
    )

    // A busybox-style multicall bin/ can carry far more names than the fan-out animation is
    // built to show at once; this caps the count rather than silently rendering (or hanging on)
    // an unbounded list.
    private const val MAX_SHELL_ENTRIES = 300

    private fun pickerRoot(context: Context): File =
        if (DistroManager.isInstalled(context)) DistroManager.homeDir(context)
        else context.getExternalFilesDir(null) ?: context.filesDir

    private fun node(name: String, filePickerRoot: File): MenuNode {
        val argChildren = ARGS[name].orEmpty().map { MenuNode("$name/$it", it) }
        val children = if (name in FILE_PARAM_COMMANDS) {
            argChildren + FileBrowser.pickerNode("$name/file", filePickerRoot)
        } else {
            argChildren
        }
        return MenuNode(
            id = name,
            label = name,
            cap = if (children.isEmpty()) "run" else children.size.toString(),
            children = children
        )
    }

    private fun CommandAbstraction.commandName(): String = javaClass.simpleName

    /**
     * The real binaries on the shell's own PATH — the bootstrapped Termux prefix's bin/ if one
     * is installed, otherwise Android's own /system/bin — so apt/pkg and anything pkg installs
     * (python, node, pip, whatever) show up automatically instead of needing to be hand-listed
     * here. Every discovered binary also gets a file… child for picking a real path argument.
     */
    private fun scanShell(context: Context, filePickerRoot: File): List<MenuNode> {
        val binDir = if (DistroManager.isInstalled(context)) {
            File(DistroManager.prefixDir(context), "bin")
        } else {
            File("/system/bin")
        }
        val names = (binDir.listFiles() ?: emptyArray())
            .filter { it.isFile && it.canExecute() }
            .map { it.name }
            .distinct()
            .sorted()

        val capped = names.take(MAX_SHELL_ENTRIES)
        val nodes = capped.map { name ->
            val hints = SHELL_HINTS[name].orEmpty().map { MenuNode("sh/$name/$it", it) }
            val children = hints + FileBrowser.pickerNode("sh/$name/file", filePickerRoot)
            MenuNode(id = "sh/$name", label = name, cap = children.size.toString(), children = children)
        }

        return if (names.size > capped.size) {
            nodes + MenuNode(
                id = "sh/more",
                label = "+${names.size - capped.size} more",
                cap = "…",
                emitsToken = false
            )
        } else {
            nodes
        }
    }

    fun from(commands: List<CommandAbstraction>, context: Context): List<MenuNode> {
        val names = commands.map { it.commandName() }

        val system = names.filter { it in SYSTEM }
        val apps = names.filter { it in APPS }
        val features = names.filter { it !in SYSTEM && it !in APPS }

        val filePickerRoot = pickerRoot(context)
        val shell = scanShell(context, filePickerRoot)

        return listOf(
            MenuNode("sh", "Shell", shell.size.toString(), shell),
            MenuNode("sys", "System", system.size.toString(), system.map { node(it, filePickerRoot) }),
            MenuNode("apps", "Apps & nav", apps.size.toString(), apps.map { node(it, filePickerRoot) }),
            MenuNode("feat", "Features", features.size.toString(), features.map { node(it, filePickerRoot) })
        )
    }
}
