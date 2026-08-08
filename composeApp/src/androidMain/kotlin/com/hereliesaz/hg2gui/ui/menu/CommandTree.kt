package com.hereliesaz.hg2gui.ui.menu

import android.content.Context
import com.hereliesaz.hg2gui.commands.CommandAbstraction
import com.hereliesaz.hg2gui.terminal.DistroManager
import com.hereliesaz.hg2gui.terminal.DpkgCatalog
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

    // A single category can carry far more names than the fan-out animation is built to show at
    // once; this caps the count per category rather than silently rendering (or hanging on) an
    // unbounded list.
    private const val MAX_SHELL_ENTRIES = 300

    /** Debian Section names, prettied up for display; a Section with no entry here is just
     *  shown as-is (still perfectly readable, e.g. "MISC"). */
    private val SECTION_LABELS = mapOf(
        "admin" to "Admin",
        "utils" to "Utilities",
        "net" to "Network",
        "shells" to "Shells",
        "devel" to "Development",
        "libdevel" to "Development",
        "text" to "Text",
        "libs" to "Libraries",
        "python" to "Python",
        "interpreters" to "Interpreters",
        "editors" to "Editors",
        "database" to "Database",
        "web" to "Web",
        "vcs" to "Version control",
        "science" to "Science"
    )
    private const val UNCATEGORIZED_SECTION = "other"

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

    private fun shellLeaf(fullName: String, label: String, filePickerRoot: File): MenuNode {
        val hints = SHELL_HINTS[fullName].orEmpty().map { MenuNode("sh/$fullName/$it", it) }
        val children = hints + FileBrowser.pickerNode("sh/$fullName/file", filePickerRoot)
        return MenuNode(
            id = "sh/$fullName",
            label = label,
            cap = children.size.toString(),
            children = children,
            value = fullName
        )
    }

    /**
     * Groups binaries that share a hyphenated prefix - apt-get, apt-key, apt-mark - under one
     * host node named for that shared prefix, instead of leaving them as N flat entries that
     * only read as related to a human who already knows the naming convention. Only hyphenated
     * siblings count toward the "at least 2" threshold: a lone hyphenated name isn't worth a
     * parent of its own, and a name with no hyphen was never part of a family to begin with.
     */
    private fun groupByFamily(names: List<String>, filePickerRoot: File): List<MenuNode> {
        val families = names.filter { it.contains('-') }
            .groupBy { it.substringBefore('-') }
            .filterValues { it.size >= 2 }

        val consumed = mutableSetOf<String>()
        val nodes = mutableListOf<MenuNode>()

        for ((prefix, members) in families) {
            consumed.addAll(members)
            val hasBare = prefix in names
            if (hasBare) consumed.add(prefix)

            val children = members.sorted().map { full -> shellLeaf(full, full.removePrefix("$prefix-"), filePickerRoot) }
            nodes.add(
                MenuNode(
                    id = "sh/$prefix",
                    label = prefix,
                    cap = children.size.toString(),
                    children = children,
                    // No bare `apt` alongside apt-get/apt-key means this host is purely
                    // navigational - there's nothing to actually run by picking it alone.
                    value = if (hasBare) prefix else null,
                    emitsToken = hasBare
                )
            )
        }

        for (name in names) {
            if (name in consumed) continue
            nodes.add(shellLeaf(name, name, filePickerRoot))
        }

        return nodes.sortedBy { it.label }
    }

    /**
     * The real binaries on the shell's own PATH - which, exactly like real Termux, means only
     * the bootstrapped prefix's bin/, never Android's own /system/bin. Termux doesn't fall back
     * to the system shell either: it bootstraps its own coreutils and treats that prefix as the
     * entire world, so a device's toybox was never meant to stand in for it. Before a bootstrap
     * is installed there's honestly nothing to list yet - so Shell offers exactly the one real
     * command available at that point, the one that fixes that.
     *
     * Once there's a real prefix, its own dpkg bookkeeping already records which package every
     * binary belongs to and that package's Debian Section (admin, utils, net, shells, devel,
     * text...) - real metadata, not a guess - so each Section becomes its own root category
     * instead of dumping every discovered binary into one "Shell" pile.
     */
    private fun scanShell(context: Context, filePickerRoot: File): List<MenuNode> {
        if (!DistroManager.isInstalled(context)) {
            return listOf(MenuNode("sh", "Shell", "1", listOf(MenuNode(id = "sh/bootstrap", label = "bootstrap", cap = "run"))))
        }

        val prefix = DistroManager.prefixDir(context)
        val binDir = File(prefix, "bin")
        val names = try {
            (binDir.listFiles() ?: emptyArray())
                .filter { it.isFile && it.canExecute() }
                .map { it.name }
        } catch (e: SecurityException) {
            emptyList()
        }.distinct().sorted()

        val sectionOf = DpkgCatalog.binariesBySection(prefix)
        val bySection = names.groupBy { sectionOf[it] ?: UNCATEGORIZED_SECTION }

        return bySection.entries.sortedBy { it.key }.map { (section, members) ->
            val capped = members.take(MAX_SHELL_ENTRIES)
            val children = groupByFamily(capped, filePickerRoot) + if (members.size > capped.size) {
                listOf(
                    MenuNode(
                        id = "sh/$section/more",
                        label = "+${members.size - capped.size} more",
                        cap = "…",
                        emitsToken = false
                    )
                )
            } else {
                emptyList()
            }
            MenuNode(
                id = "sh/$section",
                label = SECTION_LABELS[section] ?: section,
                cap = children.size.toString(),
                children = children
            )
        }
    }

    fun from(commands: List<CommandAbstraction>, context: Context): List<MenuNode> {
        val names = commands.map { it.commandName() }

        val system = names.filter { it in SYSTEM }
        val apps = names.filter { it in APPS }
        val features = names.filter { it !in SYSTEM && it !in APPS }

        val filePickerRoot = pickerRoot(context)
        val shellRoots = scanShell(context, filePickerRoot)

        return shellRoots + listOf(
            MenuNode("sys", "System", system.size.toString(), system.map { node(it, filePickerRoot) }),
            MenuNode("apps", "Apps & nav", apps.size.toString(), apps.map { node(it, filePickerRoot) }),
            MenuNode("feat", "Features", features.size.toString(), features.map { node(it, filePickerRoot) })
        )
    }
}
