package com.hereliesaz.hg2gui.ui.menu

import android.content.Context
import com.hereliesaz.hg2gui.managers.SshPresets
import com.hereliesaz.hg2gui.terminal.DistroManager
import com.hereliesaz.hg2gui.terminal.DpkgCatalog
import com.hereliesaz.hg2gui.ui.ssh.SshFlow
import java.io.File

/*
 * The suggestion tree. Shell is discovered live from the Termux bootstrap's own PATH; System,
 * Apps & nav and Features are a fixed, hand-written list of the ten Builtins verbs (see
 * `terminal/Builtins.kt`) - there is no reflection-based command framework left to discover
 * them from, and no plan to grow this list by convention rather than by hand.
 */

object CommandTree {

    private val SYSTEM = setOf("wifi", "bluetooth", "airplane", "flash", "volume", "brightness")
    private val APPS = setOf("call", "contacts")
    private val FEATURES = setOf("vfs", "edit", "calc")

    /** Argument hints for the commands whose arguments are not discoverable at runtime. */
    private val ARGS = mapOf(
        "volume" to listOf("get", "set media", "profile"),
        "brightness" to listOf("auto", "50", "100"),
        "contacts" to listOf("ls", "add", "about", "edit", "rm"),
        "vfs" to listOf("ls", "pwd", "cd", "mkdir", "touch", "cat", "rm", "mv", "cp", "mount")
    )

    /** Built-ins whose argument is a file, not a literal choice - they get a file… picker child. */
    private val FILE_PARAM_COMMANDS = setOf("edit")

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

    /**
     * Which category a package's binaries belong to. Termux's own packages carry no Debian
     * "Section" field to read this from (verified against a real bootstrap - 0 of 82 base
     * packages have one), so unlike SHELL_HINTS this can't fall back to "discover it, hint it";
     * there is no live source of truth at all, only which package owns a binary
     * (`DpkgCatalog.binariesByPackage`). Covers every package the base bootstrap actually
     * installs, plus common `pkg install` targets; anything else still shows up, just under
     * "Other".
     */
    private val CATEGORY_OF_PACKAGE = mapOf(
        "apt" to "Package management", "dpkg" to "Package management",
        "gnupg" to "Package management", "gpgv" to "Package management",
        "bash" to "Shells", "dash" to "Shells", "zsh" to "Shells", "fish" to "Shells",
        "tar" to "Archives", "gzip" to "Archives", "bzip2" to "Archives",
        "xz-utils" to "Archives", "zstd" to "Archives", "unzip" to "Archives",
        "zip" to "Archives", "xxhash" to "Archives", "p7zip" to "Archives",
        "curl" to "Network", "libcurl" to "Network", "net-tools" to "Network",
        "inetutils" to "Network", "libidn2" to "Network", "openssh" to "Network",
        "wget" to "Network", "nmap" to "Network", "rsync" to "Network",
        "socat" to "Network", "mtr" to "Network", "whois" to "Network",
        "openssl" to "Security", "libgcrypt" to "Security", "libgpg-error" to "Security",
        "libnpth" to "Security", "libtasn1" to "Security", "libassuan" to "Security",
        "p11-kit" to "Security", "libcap-ng" to "Security", "libacl" to "Security",
        "nano" to "Editors", "vim" to "Editors", "neovim" to "Editors",
        "micro" to "Editors", "emacs" to "Editors",
        "less" to "Editors", "dialog" to "Editors",
        "util-linux" to "System", "procps" to "System", "psmisc" to "System",
        "termux-tools" to "System", "termux-am" to "System", "termux-am-socket" to "System",
        "termux-core" to "System", "termux-exec" to "System", "termux-licenses" to "System",
        "lsof" to "System", "debianutils" to "System", "ncurses" to "System",
        "tmux" to "System", "screen" to "System", "htop" to "System",
        "coreutils" to "Text & files", "findutils" to "Text & files",
        "diffutils" to "Text & files", "grep" to "Text & files", "sed" to "Text & files",
        "gawk" to "Text & files", "attr" to "Text & files", "ed" to "Text & files",
        "dos2unix" to "Text & files", "patch" to "Text & files", "tree" to "Text & files",
        "ripgrep" to "Text & files", "fd" to "Text & files", "bat" to "Text & files",
        "jq" to "Text & files",
        "python" to "Development", "python3" to "Development", "nodejs" to "Development",
        "git" to "Development", "make" to "Development", "clang" to "Development",
        "golang" to "Development", "rust" to "Development", "ruby" to "Development",
        "perl" to "Development", "php" to "Development", "cmake" to "Development",
        "pcre2" to "Development",
        "sqlite" to "Database", "postgresql" to "Database", "mariadb" to "Database",
        "redis" to "Database"
    )
    private const val UNCATEGORIZED = "Other"

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

    /** The ssh pill: saved connection presets as picks, plus a "new…" leaf that launches the
     *  host/user/port/key wizard (PillMenu's onWizard) instead of drilling into more pills - ssh
     *  needs to accumulate several fields, which a plain pill chain can't do (see PillMenu.kt).
     *  resolveChildren (not eager children) means a freshly-saved preset shows up the next time
     *  this pill opens, without waiting on the next unrelated command's tree rebuild. */
    private fun sshLeaf(context: Context): MenuNode = MenuNode(
        id = "sh/ssh",
        label = "ssh",
        value = "ssh",
        resolveChildren = {
            val presets = SshPresets.list(context).map { p ->
                MenuNode(
                    id = "sh/ssh/preset/${p.name}",
                    label = p.name,
                    cap = "pick",
                    value = SshFlow.argsFor(p.user, p.host, p.port, p.keyPath)
                )
            }
            presets + MenuNode(
                id = "sh/ssh/new",
                label = "new…",
                cap = "new",
                emitsToken = false,
                wizardId = "ssh-new"
            )
        }
    )

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
    private fun groupByFamily(context: Context, names: List<String>, filePickerRoot: File): List<MenuNode> {
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
            nodes.add(if (name == "ssh") sshLeaf(context) else shellLeaf(name, name, filePickerRoot))
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
     * Once there's a real prefix, dpkg's own bookkeeping already records which package every
     * binary belongs to (`DpkgCatalog.binariesByPackage`); CATEGORY_OF_PACKAGE turns that into a
     * root category. A binary from a package not in that map - or not owned by dpkg at all -
     * still shows up, just under "Other".
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

        val categoryOf = mutableMapOf<String, String>()
        for ((pkg, binaries) in DpkgCatalog.binariesByPackage(prefix)) {
            val category = CATEGORY_OF_PACKAGE[pkg] ?: continue
            for (b in binaries) categoryOf[b] = category
        }
        val byCategory = names.groupBy { categoryOf[it] ?: UNCATEGORIZED }

        return byCategory.entries.sortedBy { it.key }.map { (category, members) ->
            val capped = members.take(MAX_SHELL_ENTRIES)
            val children = groupByFamily(context, capped, filePickerRoot) + if (members.size > capped.size) {
                listOf(
                    MenuNode(
                        id = "sh/$category/more",
                        label = "+${members.size - capped.size} more",
                        cap = "…",
                        emitsToken = false
                    )
                )
            } else {
                emptyList()
            }
            MenuNode(
                id = "sh/$category",
                label = category,
                cap = children.size.toString(),
                children = children
            )
        }
    }

    fun from(context: Context): List<MenuNode> {
        val filePickerRoot = pickerRoot(context)
        val shellRoots = scanShell(context, filePickerRoot)

        return shellRoots + listOf(
            MenuNode("sys", "System", SYSTEM.size.toString(), SYSTEM.sorted().map { node(it, filePickerRoot) }),
            MenuNode("apps", "Apps & nav", APPS.size.toString(), APPS.sorted().map { node(it, filePickerRoot) }),
            MenuNode("feat", "Features", FEATURES.size.toString(), FEATURES.sorted().map { node(it, filePickerRoot) })
        )
    }
}
