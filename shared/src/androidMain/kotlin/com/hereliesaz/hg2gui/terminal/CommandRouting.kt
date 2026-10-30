package com.hereliesaz.hg2gui.terminal

/**
 * Who is asking [TerminalEngine] to run a line, and what that origin may do.
 *
 * Background and headless callers (the signature API, the MCP server, installers) have no person
 * looking at the screen, so anything that changes what a package may do, replaces the runtime or
 * reaches into the device is denied *here*, in the engine, instead of each entry point
 * regex-filtering strings first. A denial returns [DENIED_EXIT_CODE] with an explanatory line.
 *
 * Pure (no Android) so the table is unit tested.
 */
class ExecutionPolicy private constructor(
    val origin: Origin,
    /** True when a person is demonstrably at HG2Gui right now (the API call arrived while it is foreground). */
    val userPresent: Boolean
) {
    enum class Origin { INTERACTIVE, API, MCP, INSTALLER }

    /** Anything but a typed command in a visible terminal tab. */
    val headless: Boolean get() = origin != Origin.INTERACTIVE

    /** Words for denial messages: "the API", "the MCP server", … */
    val label: String
        get() = when (origin) {
            Origin.INTERACTIVE -> "a terminal tab"
            Origin.API -> "the HG2Gui API"
            Origin.MCP -> "the MCP server"
            Origin.INSTALLER -> "a background installer"
        }

    /**
     * Null when the `hg2package` lifecycle [action] may run, otherwise the refusal text. Headless
     * callers may read (`info`) and snapshot; `enable` only while [userPresent]; everything else
     * (disable, isolate, release, update, reset, restore, remove, purge, capability…) needs a person.
     */
    fun lifecycleDenial(action: String?): String? {
        if (!headless) return null
        if (action == "info" || action == "snapshot") return null
        if (action == "enable" && userPresent) return null
        return "HG2Gui package '${action ?: ""}' is not available from $label; run it from a terminal tab" +
            (if (action == "enable") " (or while HG2Gui is on screen)." else ".")
    }

    /** `bootstrap` replaces the whole runtime prefix. */
    fun bootstrapDenial(): String? =
        if (headless) "bootstrap is not available from $label; run it from a terminal tab." else null

    /** `vfs mount` needs a root confirmation from a person. */
    fun vfsMountDenial(): String? =
        if (headless) "vfs mount is not available from $label; it needs a root confirmation in a terminal tab." else null

    /**
     * `hg2auth` hands out ADB/root. Headless callers may only read `status`; elevated commands come
     * through the dialog-approved [TerminalEngine.runApprovedAuthority] entry point instead.
     */
    fun authorityDenial(subcommand: String?): String? {
        if (!headless) return null
        if (subcommand == null || subcommand == "status") return null
        return "hg2auth $subcommand is not available from $label; ADB/root authority needs a foreground approval."
    }

    /** True when [line] is an Android-facing builtin a headless caller must not run unprompted. */
    fun builtinNeedsApproval(line: String): Boolean = headless && BuiltinApproval.requiresApproval(line)

    companion object {
        /** Exit code for every policy refusal ("found but may not be run"). */
        const val DENIED_EXIT_CODE = 126

        /** A typed command in a visible terminal tab: nothing is denied by origin. */
        val INTERACTIVE = ExecutionPolicy(Origin.INTERACTIVE, userPresent = true)

        /** The paired-agent MCP server. A person enabled it but is not watching each command. */
        val MCP = ExecutionPolicy(Origin.MCP, userPresent = false)

        /** Installers that run `pkg install` for a dependency list. */
        val INSTALLER = ExecutionPolicy(Origin.INSTALLER, userPresent = false)

        /** A same-signature app calling the API; [appInForeground] says whether a person is looking at HG2Gui. */
        fun api(appInForeground: Boolean): ExecutionPolicy = ExecutionPolicy(Origin.API, appInForeground)
    }
}

/**
 * Which Android-facing builtins change device state, place calls or read personal data and so
 * must not run for a background caller without an explicit answer. Everything not listed
 * (calc, the sandboxed vfs verbs, `volume get`) is safe headless.
 */
object BuiltinApproval {
    fun requiresApproval(line: String): Boolean {
        val parts = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val verb = parts.firstOrNull() ?: return false
        val sub = parts.getOrNull(1)?.lowercase()
        return when (verb) {
            "call", "contacts", "brightness", "airplane", "wifi", "bluetooth", "flash", "edit" -> true
            "volume" -> sub == "set" || sub == "profile"
            "vfs" -> sub == "mount"
            else -> false
        }
    }
}

/** The two root/ADB prompts and the text an approval dialog should show, kept in one place. */
object AuthorityPrompts {
    fun rootShell(command: String): String = "Run as root? $command [y/N]"
    fun adbShell(command: String): String = "Run through ADB shell? $command [y/N]"
    const val ROOT_TEST = "Request root access to test the su provider? [y/N]"
}

/** Which elevated backend [TerminalEngine.runApprovedAuthority] should use. */
enum class AuthorityKind { ROOT, ADB }

/**
 * `apt`/`apt-get` -> HG2Gui package manager translation.
 *
 * The old translation scanned every word for an operation name and silently dropped every flag:
 * `apt search upgrade` became a full `pkg upgrade`, `apt install -s foo` really installed, and
 * `--purge`/`--reinstall` vanished. Here the operation is the first non-option word, only flags
 * that mean exactly what HG2Gui does anyway are accepted, and anything else on a mutating command
 * is refused with a message instead of being run differently from what was typed.
 */
internal object AptTranslation {
    sealed class Plan {
        /** Not an apt-family command word. */
        object NotApt : Plan()

        /** A read-only apt command: hand it to the shell as typed. */
        object PassThrough : Plan()

        class Translate(val pkgLine: String) : Plan()

        class Refuse(val message: String) : Plan()
    }

    private val SAFE_FLAGS = setOf(
        "-y", "--yes", "--assume-yes", "-q", "-qq", "--quiet",
        "--no-install-recommends", "--no-install-suggests"
    )
    private val SAFE_CLUSTER = Regex("^-[yq]+$")
    private val DRY_RUN_FLAGS = setOf("-s", "--simulate", "--just-print", "--dry-run", "--recon", "--no-act")

    /** apt options that take a value in the next word (`-o Foo=bar`, `-t stable`). */
    private val VALUE_OPTIONS = setOf("-o", "-c", "-t", "-a", "--option", "--config-file", "--target-release", "--default-release")

    private val TRANSLATED = setOf(
        "install", "reinstall", "remove", "purge", "update", "upgrade", "dist-upgrade", "full-upgrade", "clean", "autoclean"
    )
    private val UNSUPPORTED_MUTATING = setOf(
        "autoremove", "auto-remove", "autopurge", "build-dep", "satisfy"
    )

    /** HG2Gui's package manager re-plans an installed package with `pkg install --reinstall`. */
    private const val REINSTALL_FLAG = "--reinstall"
    private val READ_ONLY = setOf(
        "list", "search", "show", "policy", "depends", "rdepends", "madison", "showpkg", "showsrc",
        "changelog", "help", "source", "download", "check", "stats", "pkgnames", "dump", "dumpavail",
        "unmet", "dotty", "xvcg", "indextargets", "moo", "edit-sources", "versions", "why", "why-not"
    )

    private val PACKAGE_NAME = Regex("^[A-Za-z0-9][A-Za-z0-9+.\\-]*$")

    private class Parsed(val op: String?, val options: List<String>, val operands: List<String>, val allWords: List<String>)

    private fun parse(rest: List<String>): Parsed {
        var op: String? = null
        val options = mutableListOf<String>()
        val operands = mutableListOf<String>()
        var k = 0
        while (k < rest.size) {
            val w = rest[k]
            if (w.startsWith("-") && w.length > 1) {
                options += w
                if (w in VALUE_OPTIONS) k++
            } else if (op == null) {
                op = w
            } else {
                operands += w
            }
            k++
        }
        return Parsed(op, options, operands, rest)
    }

    private fun flagAllowed(flag: String): Boolean = flag in SAFE_FLAGS || SAFE_CLUSTER.matches(flag)

    /** Plans a routable `apt`/`apt-get`/`aptitude` command; [words] starts with the command word. */
    fun plan(words: List<String>): Plan {
        val command = words.firstOrNull() ?: return Plan.NotApt
        if (command != "apt" && command != "apt-get" && command != "aptitude") return Plan.NotApt
        val parsed = parse(words.drop(1))
        val op = parsed.op

        if (op == null) {
            // Bare `apt`/`apt --version` print help; a bare `aptitude` opens an interactive installer.
            return if (command == "aptitude") {
                Plan.Refuse("aptitude can change packages outside HG2Gui's rollback journal; use pkg install/remove.")
            } else {
                Plan.PassThrough
            }
        }
        if (op in READ_ONLY) return Plan.PassThrough
        val mutating = op in TRANSLATED || op in UNSUPPORTED_MUTATING
        if (!mutating) {
            // An unknown word in operation position. If a mutating operation name shows up later
            // we mis-parsed an option value and cannot tell what real apt would do - refuse.
            return if (parsed.allWords.any { it in TRANSLATED || it in UNSUPPORTED_MUTATING }) {
                Plan.Refuse("Could not tell which apt operation '$op' is; nothing was run. Use: pkg install|remove|update|upgrade <package>.")
            } else {
                Plan.PassThrough
            }
        }
        if (command == "aptitude") {
            return Plan.Refuse("aptitude can change packages outside HG2Gui's rollback journal; use pkg $op.")
        }
        parsed.options.firstOrNull { it in DRY_RUN_FLAGS }?.let {
            return Plan.Refuse(
                "Dry run ($it) is not supported by HG2Gui's package manager; nothing was run. " +
                    "(Running it without the flag would really change the installed packages.)"
            )
        }
        if (op in UNSUPPORTED_MUTATING) {
            return Plan.Refuse(
                "apt $op is not supported by HG2Gui's package manager; nothing was run " +
                    "(real apt would change packages outside the rollback journal). " +
                    if (op.contains("remove") || op.contains("purge")) "Remove packages explicitly: pkg remove <package>." else "Use pkg install <package>."
            )
        }
        val reinstallable = op == "install" || op == "reinstall"
        parsed.options.firstOrNull { !flagAllowed(it) && !(it == REINSTALL_FLAG && reinstallable) }?.let {
            return Plan.Refuse(
                "apt option '$it' is not supported by HG2Gui's package manager; nothing was run " +
                    "(it would have been ignored and the command run differently from what you typed)."
            )
        }
        // -o/-c/-t/-a values were consumed with their option; they are not package names.
        return when (op) {
            "update", "upgrade", "dist-upgrade", "full-upgrade", "clean", "autoclean" -> {
                if (parsed.operands.isNotEmpty()) {
                    Plan.Refuse("apt $op takes no package names here; nothing was run.")
                } else {
                    Plan.Translate(
                        when (op) {
                            "update" -> "pkg update"
                            "clean", "autoclean" -> "pkg clean"
                            else -> "pkg upgrade"
                        }
                    )
                }
            }
            else -> {
                if (parsed.operands.isEmpty()) {
                    Plan.Refuse("usage: apt $op <package> [package…]")
                } else {
                    val bad = parsed.operands.firstOrNull { !PACKAGE_NAME.matches(it) }
                    if (bad != null) {
                        Plan.Refuse("'$bad' is not a plain package name (versions, releases and paths are not supported); nothing was run.")
                    } else {
                        val names = parsed.operands.joinToString(" ")
                        val asReinstall = op == "reinstall" || REINSTALL_FLAG in parsed.options
                        Plan.Translate(if (asReinstall) "pkg install $REINSTALL_FLAG $names" else "pkg $op $names")
                    }
                }
            }
        }
    }

    private val PKG_REINSTALL_FLAGS = setOf("--reinstall", "--force-requested")
    private val PKG_MUTATING_FLAG_OPS = setOf(
        "install", "in", "add", "remove", "rm", "uninstall", "purge", "upgrade", "up", "update", "reinstall"
    )
    private val PKG_READ_ONLY_OPS = setOf("search", "show", "info", "list-all", "list-installed", "list", "files", "help")

    /**
     * Refusal text when a routable `pkg`/`hg2pkg` line carries a flag HG2Gui's manager would silently
     * drop (so `pkg install -s foo` would really install), else null.
     */
    fun pkgFlagRefusal(words: List<String>): String? {
        val command = words.firstOrNull() ?: return null
        if (command != "pkg" && command != "hg2pkg") return null
        val args = words.drop(1)
        val op = args.firstOrNull { !it.startsWith("-") } ?: return null
        if (op !in PKG_MUTATING_FLAG_OPS) return null
        val flags = args.filter { it.startsWith("-") && it.length > 1 }
        flags.firstOrNull { it in DRY_RUN_FLAGS }?.let {
            return "Dry run ($it) is not supported by HG2Gui's package manager; nothing was run."
        }
        val installOp = op == "install" || op == "in"
        flags.firstOrNull { !flagAllowed(it) && !(installOp && it in PKG_REINSTALL_FLAGS) }?.let {
            return "pkg option '$it' is not supported by HG2Gui's package manager; nothing was run " +
                "(it would have been ignored and the command run differently from what you typed)."
        }
        return null
    }

    /**
     * Whether an apt-family or pkg invocation that HG2Gui could not route (behind `sudo`/`env`,
     * in a pipe, `bash -c`, `$( )`, a control structure…) would change installed packages. Unknown
     * operations count as mutating; only the known read-only ones are let through to the shell.
     */
    fun isMutating(words: List<String>): Boolean {
        val command = CommandLine.basename(words.firstOrNull() ?: return false)
        val parsed = parse(words.drop(1))
        return when (command) {
            "pkg", "hg2pkg" -> parsed.op != null && parsed.op !in PKG_READ_ONLY_OPS
            "apt", "apt-get", "aptitude" -> when {
                parsed.options.any { it in DRY_RUN_FLAGS } -> false
                parsed.op == null -> command == "aptitude"
                parsed.op in READ_ONLY -> false
                parsed.op in TRANSLATED || parsed.op in UNSUPPORTED_MUTATING -> true
                else -> parsed.allWords.any { it in TRANSLATED || it in UNSUPPORTED_MUTATING }
            }
            else -> false
        }
    }

    val PACKAGE_MANAGER_NAMES = setOf("pkg", "hg2pkg", "apt", "apt-get", "aptitude")
}

/**
 * Decides, from the whole command line, what [TerminalEngine] does with it. Pure: owners of
 * binaries come in through [ownerOf], so tests use fabricated packages.
 *
 * The rules, in order:
 *  1. an unterminated quote is refused (it used to reach the shell and kill the tab);
 *  2. a binary of a *disabled* package anywhere a command can run - after `&&`/`;`/`|`, behind
 *     `env`/`time`/`sudo`, in `bash -c`, `$( )`, backticks or `find -exec` - blocks the line;
 *  3. a mutating package-manager command that HG2Gui cannot route (wrapped, piped, nested, inside
 *     a control structure) is refused rather than reaching the real apt/dpkg unjournaled;
 *  4. an isolated package's binary runs only as its own command or one step of a `;`/`&&`/`||` list;
 *  5. `;`/`&&`/`||` lists that contain a command the engine handles itself run step by step with
 *     shell short-circuit semantics; everything else goes to the shell exactly as typed.
 *
 * Only the line's own text is analysed. A command word computed at run time, a shell function, an
 * alias or a script is invisible here; a disabled package's binary is still refused for those by the
 * PATH shims of [DisableShims], and a mutating `dpkg` by its guard. Isolation has no such second
 * layer, and an absolute-path call escapes both.
 */
internal object LinePlanner {
    enum class StepKind {
        /** pkg/apt/download/hg2auth/… handled by the engine itself. */
        ENGINE,

        /** An Android-facing builtin (wifi, call, calc, vfs, …). */
        BUILTIN,

        /** A binary owned by an installed package (tracking, PRoot backends, isolation). */
        OWNED,

        /** Plain shell. */
        SHELL
    }

    class Step(
        val connector: CommandLine.Connector,
        val text: String,
        val words: List<String>,
        val kind: StepKind,
        /** The package that owns the command word, when known (also set for ENGINE steps, for pass-through). */
        val owner: PackageLifecycleStore.InstalledPackage?,
        /** The command word as typed (path included) for [kind] OWNED/ENGINE/BUILTIN. */
        val verb: String
    )

    sealed class Plan {
        class Refuse(val message: String, val exitCode: Int) : Plan()

        /** Send [line] to the shell untouched. */
        class Shell(val line: String) : Plan()

        class Steps(val steps: List<Step>) : Plan()
    }

    private val ENGINE_VERBS = setOf("bootstrap", "download", "hg2download", "remote", "hg2auth", "hg2package", "pkg", "hg2pkg", "apt", "apt-get")
    private val NO_OWNER_LOOKUP = setOf("hg2package", "hg2auth", "remote")

    /**
     * Verbs that exist only inside HG2Gui and take the *rest of the line as their own arguments*
     * (`download http://x/y?a=1&b=2`, `hg2auth root shell a && b`, `calc 2*(3+4)`). They are never
     * shell commands, so the line is not tokenised as shell: it goes to them whole, as it always did.
     * pkg/apt are deliberately not here - they can be mistaken for real shell commands and are
     * analysed like any other line.
     */
    private val WHOLE_LINE_VERBS = setOf("bootstrap", "download", "hg2download", "remote", "hg2auth", "hg2package")

    fun plan(
        rawLine: String,
        builtinNames: Set<String>,
        ownerOf: (String) -> PackageLifecycleStore.InstalledPackage?
    ): Plan {
        val line = rawLine.trim()

        val firstToken = line.split(Regex("\\s+"), limit = 2).first()
        if (firstToken in WHOLE_LINE_VERBS || firstToken in builtinNames) {
            val owner = if (firstToken in NO_OWNER_LOOKUP) null else ownerOf(firstToken)
            if (owner?.disabled == true) return Plan.Refuse(disabledMessage(owner), ExecutionPolicy.DENIED_EXIT_CODE)
            val words = CommandLine.words(line) ?: line.split(Regex("\\s+"))
            val isVfsMount = firstToken == "vfs" && words.getOrNull(1) == "mount"
            val kind = if (firstToken in WHOLE_LINE_VERBS || isVfsMount) StepKind.ENGINE else StepKind.BUILTIN
            return Plan.Steps(listOf(Step(CommandLine.Connector.NONE, line, words, kind, owner, firstToken)))
        }

        val parsed = CommandLine.parse(line)
        if (parsed.unterminated && !parsed.hasHeredoc) {
            return Plan.Refuse("Unterminated quote or substitution - nothing was run.", 2)
        }

        fun lookup(name: String): PackageLifecycleStore.InstalledPackage? =
            if (name.isEmpty() || name in NO_OWNER_LOOKUP) null else ownerOf(name)

        for (invocation in parsed.invocations) {
            if (invocation.dynamic) continue
            val owner = lookup(invocation.name) ?: continue
            if (owner.disabled) return Plan.Refuse(disabledMessage(owner), ExecutionPolicy.DENIED_EXIT_CODE)
        }

        val steps: List<Step> = if (parsed.complex) emptyList() else parsed.segments.map { buildStep(it, builtinNames, ::lookup) }

        val routed = HashSet<CommandLine.Invocation>()
        val allowedIsolated = HashSet<CommandLine.Invocation>()
        parsed.segments.zip(steps).forEach { (segment, step) ->
            val final = segment.stages.singleOrNull()?.final ?: return@forEach
            when (step.kind) {
                StepKind.ENGINE, StepKind.BUILTIN -> routed += final
                StepKind.OWNED -> if (step.owner?.isolated == true) allowedIsolated += final
                StepKind.SHELL -> Unit
            }
        }

        for (invocation in parsed.invocations) {
            if (invocation.dynamic || invocation in routed) continue
            if (invocation.name in AptTranslation.PACKAGE_MANAGER_NAMES && AptTranslation.isMutating(invocation.words)) {
                return Plan.Refuse(unroutablePackageManagerMessage(invocation), 2)
            }
        }
        for (invocation in parsed.invocations) {
            if (invocation.dynamic || invocation in routed || invocation in allowedIsolated) continue
            val owner = lookup(invocation.name) ?: continue
            if (owner.isolated) return Plan.Refuse(isolatedMessage(owner), ExecutionPolicy.DENIED_EXIT_CODE)
        }

        if (parsed.complex || steps.isEmpty()) return Plan.Shell(line)
        if (steps.all { it.kind == StepKind.SHELL }) return Plan.Shell(line)
        return Plan.Steps(steps)
    }

    private fun buildStep(
        segment: CommandLine.Segment,
        builtinNames: Set<String>,
        lookup: (String) -> PackageLifecycleStore.InstalledPackage?
    ): Step {
        val stage = segment.stages.singleOrNull()
        val final = stage?.final
        if (stage == null || final == null || final.dynamic) {
            return Step(segment.connector, segment.text, emptyList(), StepKind.SHELL, null, "")
        }
        val owner = lookup(final.name)
        val direct = final.kind == CommandLine.Kind.DIRECT && stage.assignments == 0
        if (direct) {
            val verb = final.words[0]
            val isVfsMount = verb == "vfs" && final.words.getOrNull(1) == "mount"
            if (verb in ENGINE_VERBS || isVfsMount) {
                return Step(segment.connector, segment.text, final.words, StepKind.ENGINE, owner, verb)
            }
            if (verb in builtinNames) {
                return Step(segment.connector, segment.text, final.words, StepKind.BUILTIN, owner, verb)
            }
        }
        if (owner != null && (final.kind == CommandLine.Kind.DIRECT || final.kind == CommandLine.Kind.WRAPPED)) {
            return Step(segment.connector, segment.text, final.words, StepKind.OWNED, owner, final.words[0])
        }
        return Step(segment.connector, segment.text, final.words, StepKind.SHELL, null, final.words[0])
    }

    fun disabledMessage(owner: PackageLifecycleStore.InstalledPackage): String =
        "${owner.name} is installed but disabled in HG2Gui. Enable it under Packages → ${owner.managerLabel} → ${owner.name}."

    private fun isolatedMessage(owner: PackageLifecycleStore.InstalledPackage): String =
        "${owner.name} is isolated, so HG2Gui only runs it as its own command (or one step of a ; && || list). " +
            "It was not run through a pipe, bash -c, \$( ), a control structure or other wrapper that could execute it outside its sandbox."

    private fun unroutablePackageManagerMessage(invocation: CommandLine.Invocation): String =
        "'${invocation.words.take(2).joinToString(" ")}' changes installed packages, so HG2Gui only runs it as its own command " +
            "(or one step of a ; && || list) where it can be journaled and rolled back - not through sudo/env/time, " +
            "a pipe, bash -c, \$( ), or inside if/for/{ }. Nothing was run. Try: pkg install <package>."
}

/** Pure description of what a package Update did, comparing versions before and after. */
object PackageUpdateOutcome {
    class Result(val changed: Boolean, val message: String)

    fun describe(name: String, before: String, after: String?): Result = when {
        after == null -> Result(false, "$name: could not read the installed version after the update.")
        after.trim() != before.trim() -> Result(true, "$name updated: $before → $after.")
        else -> Result(false, "$name is already at the newest available version ($after).")
    }
}
