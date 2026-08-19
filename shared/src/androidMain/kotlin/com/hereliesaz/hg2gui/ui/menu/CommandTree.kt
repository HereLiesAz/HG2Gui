package com.hereliesaz.hg2gui.ui.menu

import android.content.Context
import com.hereliesaz.hg2gui.managers.OsContextStore
import com.hereliesaz.hg2gui.managers.SshPresets
import com.hereliesaz.hg2gui.managers.WorkflowStore
import com.hereliesaz.hg2gui.terminal.AptCatalog
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

    // Labelled "Device", not "System" - a shell category also named "System" is discovered live
    // from PATH (procps/tmux/htop/util-linux and friends), and the two used to collide: two root
    // pills both reading SYSTEM, one for hardware toggles and one for OS utilities, with nothing
    // but hue to tell them apart.
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

    /** Command names whose invocation can plausibly change what's installed / on PATH - the only
     *  ones worth re-scanning [from] for afterward. Reuses the same package-manager binaries
     *  SHELL_HINTS already special-cases below, rather than inventing a second list. */
    val PACKAGE_MANAGER_COMMANDS = setOf("pkg", "apt", "apt-get", "dpkg")

    /** Curated flag hints for the handful of real shell binaries worth hand-picking for; every
     *  other binary discovered on PATH still gets a plain file… child and can take whatever
     *  else the user types besides. */
    private val SHELL_HINTS = mapOf(
        "ls" to listOf("-l", "-a", "-la"),
        "cd" to listOf("..", "/sdcard", "~"),
        "ps" to listOf("-A"),
        "df" to listOf("-h"),
        "uname" to listOf("-a"),
        "ping" to listOf("-c 4 1.1.1.1"),
        "osint-lookup" to listOf("example.com"),
        "pkg" to listOf("update", "upgrade", "install", "search", "list-installed", "uninstall"),
        "apt" to listOf("update", "upgrade", "install", "search", "list --installed", "remove"),
        "apt-get" to listOf("update", "upgrade", "install", "remove", "autoremove"),
        "dpkg" to listOf("-l", "-L", "-S", "-i")
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

    /** Per-binary overrides, checked before [CATEGORY_OF_PACKAGE] - for a binary whose package
     *  doesn't reflect what it's actually for. `pkg` ships as part of `termux-tools` (mapped to
     *  "System" above, correctly, for that package's other utility binaries), but `pkg` itself is
     *  Termux's own package-manager wrapper around `apt` and belongs with it. */
    private val CATEGORY_OF_BINARY = mapOf("pkg" to "Package management")

    private const val UNCATEGORIZED = "Other"

    /** Fallback browsing root for the Select File/Folder pill when a session has no live working
     *  directory yet (e.g. before its first command runs) - TerminalActivity prefers the actual
     *  session cwd over this whenever one is available. */
    fun pickerRoot(context: Context): File =
        if (DistroManager.isInstalled(context)) DistroManager.homeDir(context)
        else context.getExternalFilesDir(null) ?: context.filesDir

    private fun node(name: String): MenuNode {
        val argChildren = ARGS[name].orEmpty().map { MenuNode("$name/$it", it) }
        val children = if (name in FILE_PARAM_COMMANDS) {
            argChildren + FileBrowser.pickerNode("$name/file")
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

    /** The Workflows root pill: saved templates as picks (each launches its own fill-in-the-
     *  placeholders wizard), plus a "new…" leaf that launches the save wizard. Same lazy
     *  resolveChildren reasoning as [sshLeaf] - a freshly-saved workflow shows up next time this
     *  pill opens. This is a synthesized root like sys/apps/feat, not a shell binary, so it's
     *  added in [from] rather than discovered from PATH. */
    private fun workflowsRoot(context: Context): MenuNode = MenuNode(
        id = "wf",
        label = "Workflows",
        emitsToken = false,
        resolveChildren = {
            val saved = WorkflowStore.list(context).map { workflow ->
                MenuNode(
                    id = "wf/${workflow.name}",
                    label = workflow.name,
                    cap = "run",
                    emitsToken = false,
                    wizardId = "workflow-run:${workflow.name}"
                )
            }
            saved + MenuNode(
                id = "wf/new",
                label = "new…",
                cap = "new",
                emitsToken = false,
                wizardId = "workflow-new"
            )
        }
    )

    /** The AI root pill: one leaf that opens the AI chat screen (a wizardId consumer that
     *  navigates rather than collecting prompt answers - a valid reuse of the same "the tree
     *  wants this id handled outside normal token emission" hook). */
    private fun aiRoot(): MenuNode = MenuNode(
        id = "ai",
        label = "AI",
        emitsToken = false,
        children = listOf(
            MenuNode(id = "ai/chat", label = "chat…", cap = "open", emitsToken = false, wizardId = "ai-chat")
        )
    )

    /** The Store root pill: azphalt.store package search, plus a leaf listing what's already
     *  installed. Both navigate to the AzpStoreScreen rather than collecting prompt answers -
     *  same wizardId-as-navigation reuse as [aiRoot]. */
    private fun azpRoot(): MenuNode = MenuNode(
        id = "azp",
        label = "Store",
        emitsToken = false,
        children = listOf(
            MenuNode(id = "azp/search", label = "search…", cap = "open", emitsToken = false, wizardId = "azp-store")
        )
    )

    // Reference-only command sets for a foreign OS - not live-discovered like Shell, since these
    // aren't necessarily real local binaries. For working over an active `ssh` connection into a
    // host of that kind, where the local Termux PATH tells you nothing about what's actually
    // there. git/ls/ssh genuinely overlap with what's locally real; the package/service managers
    // don't - that's the whole point of picking a context.
    private data class OsCmd(val label: String, val args: List<String>)
    private val OS_COMMANDS: Map<String, List<OsCmd>> = mapOf(
        "ubuntu" to listOf(
            OsCmd("apt", listOf("install", "update", "upgrade", "search")),
            OsCmd("systemctl", listOf("status", "start", "restart")),
            OsCmd("ls", listOf("-la", "-lh", "~")),
            OsCmd("git", listOf("status", "pull", "commit", "push")),
            OsCmd("ssh", listOf("user@host"))
        ),
        "macos" to listOf(
            OsCmd("brew", listOf("install", "update", "upgrade", "list")),
            OsCmd("launchctl", listOf("list", "load", "unload")),
            OsCmd("ls", listOf("-la", "-lh", "~")),
            OsCmd("git", listOf("status", "pull", "commit", "push")),
            OsCmd("ssh", listOf("user@host"))
        ),
        "windows" to listOf(
            OsCmd("winget", listOf("install", "upgrade", "search", "list")),
            OsCmd("sc", listOf("query", "start", "stop")),
            OsCmd("dir", listOf("/a", "/b", "%USERPROFILE%")),
            OsCmd("git", listOf("status", "pull", "commit", "push")),
            OsCmd("ssh", listOf("user@host"))
        )
    )
    private val OS_LABELS = mapOf("ubuntu" to "Ubuntu", "macos" to "macOS", "windows" to "Windows")

    /** The reference tree for the currently-picked foreign OS context - swapped in alongside the
     *  real Shell categories, never replacing them. Pure token-emitting leaves, same as any Shell
     *  pill: picking one just assembles the command onto the input line for review. */
    private fun osReferenceRoot(os: String): MenuNode {
        val children = OS_COMMANDS[os].orEmpty().map { c ->
            MenuNode(
                id = "ctx/$os/${c.label}",
                label = c.label,
                cap = c.args.size.toString(),
                children = c.args.map { a -> MenuNode(id = "ctx/$os/${c.label}/$a", label = a) }
            )
        }
        return MenuNode(
            id = "ctx/$os",
            label = OS_LABELS[os] ?: os,
            cap = children.size.toString(),
            children = children,
            emitsToken = false
        )
    }

    /** The Context root pill: pick which OS's commands the reference tree above should offer,
     *  or "local" to turn it off. Picking one is a state change, not a token - same
     *  wizardId-as-navigation reuse as [aiRoot]/[azpRoot], handled by re-fetching [from]. */
    private fun contextRoot(context: Context): MenuNode {
        val current = OsContextStore.current(context)
        val choices = listOf("local", "ubuntu", "macos", "windows").map { os ->
            MenuNode(
                id = "ctx-pick/$os",
                label = (OS_LABELS[os] ?: os.replaceFirstChar { it.uppercase() }) + if (os == current) " (current)" else "",
                emitsToken = false,
                wizardId = "switchos:$os"
            )
        }
        return MenuNode(
            id = "ctx",
            label = "Context",
            cap = (OS_LABELS[current] ?: current),
            children = choices,
            emitsToken = false
        )
    }

    /** A single SHELL_HINTS entry, turned into a real pill. Every hint is a flat, terminal leaf
     *  by default - picking it just types that literal text and stops there. Every real package
     *  manager here has one hint that actually needs a browsable target instead:
     *  apt/apt-get/pkg's "install" wants a *name* from the repo catalog (apt update's own
     *  downloaded index); dpkg's "-i" wants a *path* to an already-downloaded .deb, not a
     *  repo-resolved name, so it gets the same file picker every other shell command's own
     *  file… child already uses, not the catalog. Shared by [shellLeaf] and [groupByFamily]'s own
     *  bareHints - a hyphenated family (apt-get/apt-cache/... alongside bare "apt") builds the
     *  bare command's hints separately from its family members, so both call this rather than
     *  only one of them silently missing the special-casing. */
    private fun hintChild(context: Context, fullName: String, hint: String): MenuNode = when {
        fullName in PACKAGE_MANAGER_COMMANDS && hint == "install" ->
            installNode(context, "sh/$fullName/install")
        // "-i" still has to actually reach the command line as its own token - unlike
        // FILE_PARAM_COMMANDS' "edit" (where the file *is* the whole rest of the command), dpkg
        // needs both "-i" and the path after it. Wrapping the picker as this node's own child,
        // instead of swapping "-i" out for it directly, keeps "-i" emitting normally while still
        // drilling into the same picker every other file… child uses.
        fullName == "dpkg" && hint == "-i" ->
            MenuNode(
                id = "sh/$fullName/-i",
                label = "-i",
                cap = "1",
                children = listOf(FileBrowser.pickerNode("sh/$fullName/-i/file"))
            )
        else -> MenuNode("sh/$fullName/$hint", hint)
    }

    private fun shellLeaf(context: Context, fullName: String, label: String): MenuNode {
        val hints = SHELL_HINTS[fullName].orEmpty().map { hint -> hintChild(context, fullName, hint) }
        val children = hints + FileBrowser.pickerNode("sh/$fullName/file")
        return MenuNode(
            id = "sh/$fullName",
            label = label,
            cap = children.size.toString(),
            children = children,
            value = fullName
        )
    }

    /** The "install" pill under apt/pkg/apt-get: still contributes "install" to the command
     *  line like any other hint (it really is the next literal word), but instead of stopping
     *  there, drills into [AptCatalog]'s categorized package list - resolved lazily since
     *  parsing ~2900 index entries on every tree rebuild would be wasted work the vast majority
     *  of the time this pill never actually gets opened. */
    private fun installNode(context: Context, id: String): MenuNode = MenuNode(
        id = id,
        label = "install",
        cap = "browse",
        resolveChildren = { installCategories(context) }
    )

    /** A category is purely navigational, same reasoning as [scanShell]'s own category nodes -
     *  "Libraries" or "Networking" is never itself part of the command line, only whatever
     *  package a pick resolves to inside it. */
    private fun installCategories(context: Context): List<MenuNode> {
        val prefix = DistroManager.prefixDir(context)
        if (!AptCatalog.hasIndex(prefix)) {
            return listOf(
                MenuNode(
                    id = "aptcat/none",
                    label = "run 'apt update' first",
                    cap = "…",
                    emitsToken = false
                )
            )
        }
        val byCategory = AptCatalog.all(prefix)
            .groupBy { AptCatalog.categoryOf(it.name, it.description) }

        return byCategory.entries.sortedBy { it.key }.map { (category, members) ->
            val sorted = members.sortedBy { it.name }
            val capped = sorted.take(MAX_SHELL_ENTRIES)
            val children = capped.map { pkg ->
                MenuNode(id = "aptcat/$category/${pkg.name}", label = pkg.name)
            } + if (sorted.size > capped.size) {
                listOf(
                    MenuNode(
                        id = "aptcat/$category/more",
                        label = "+${sorted.size - capped.size} more",
                        cap = "…",
                        emitsToken = false
                    )
                )
            } else {
                emptyList()
            }
            MenuNode(
                id = "aptcat/$category",
                label = category,
                cap = children.size.toString(),
                children = children,
                emitsToken = false
            )
        }
    }

    /**
     * Groups binaries that share a hyphenated prefix - apt-get, apt-key, apt-mark - under one
     * host node named for that shared prefix, instead of leaving them as N flat entries that
     * only read as related to a human who already knows the naming convention. Only hyphenated
     * siblings count toward the "at least 2" threshold: a lone hyphenated name isn't worth a
     * parent of its own, and a name with no hyphen was never part of a family to begin with.
     */
    private fun groupByFamily(context: Context, names: List<String>): List<MenuNode> {
        val families = names.filter { it.contains('-') }
            .groupBy { it.substringBefore('-') }
            .filterValues { it.size >= 2 }

        val consumed = mutableSetOf<String>()
        val nodes = mutableListOf<MenuNode>()

        for ((prefix, members) in families) {
            consumed.addAll(members)
            val hasBare = prefix in names
            if (hasBare) consumed.add(prefix)

            // The bare command's own hints (e.g. "apt" -> update/upgrade/install) lead, followed
            // by its hyphenated family (apt-get/apt-key/apt-mark) - otherwise a bare command with
            // real SHELL_HINTS of its own would never be able to show them, since this host's
            // children were always just the family list.
            val bareHints = if (hasBare) {
                SHELL_HINTS[prefix].orEmpty().map { hint -> hintChild(context, prefix, hint) }
            } else {
                emptyList()
            }
            val children = bareHints +
                members.sorted().map { full -> shellLeaf(context, full, full.removePrefix("$prefix-")) }
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
            nodes.add(if (name == "ssh") sshLeaf(context) else shellLeaf(context, name, name))
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
    private fun scanShell(context: Context): List<MenuNode> {
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
        categoryOf.putAll(CATEGORY_OF_BINARY)
        val byCategory = names.groupBy { categoryOf[it] ?: UNCATEGORIZED }

        return byCategory.entries.sortedBy { it.key }.map { (category, members) ->
            val capped = members.take(MAX_SHELL_ENTRIES)
            val children = groupByFamily(context, capped) + if (members.size > capped.size) {
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
                children = children,
                // A category is purely navigational - "Development" or "Network" is never
                // itself a runnable command, only whatever real binary gets picked inside it.
                emitsToken = false
            )
        }
    }

    fun from(context: Context): List<MenuNode> {
        val shellRoots = scanShell(context)
        val os = OsContextStore.current(context)
        val osRoots = if (os == "local") emptyList() else listOf(osReferenceRoot(os))

        return shellRoots + osRoots + listOf(
            MenuNode("sys", "Device", SYSTEM.size.toString(), SYSTEM.sorted().map { node(it) }, emitsToken = false),
            MenuNode("apps", "Apps & nav", APPS.size.toString(), APPS.sorted().map { node(it) }, emitsToken = false),
            MenuNode("feat", "Features", FEATURES.size.toString(), FEATURES.sorted().map { node(it) }, emitsToken = false),
            workflowsRoot(context),
            aiRoot(),
            azpRoot(),
            contextRoot(context)
        )
    }
}
