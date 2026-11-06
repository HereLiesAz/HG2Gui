package com.hereliesaz.hg2gui.terminal

/**
 * The AI command-line tools `aicli` can install. Pure data and planning (no Android, no I/O) so the
 * recipes are unit tested; [TerminalEngine] runs the [Step]s.
 *
 * Each entry was checked against the real registries rather than assumed:
 *  - `ollama` and `codex` are packages in the Termux repository (`pkg install`).
 *  - `gemini` is `@google/gemini-cli`, a pure-JavaScript bundle (`bin: bundle/gemini.js`).
 *  - `agy` is Google's Antigravity CLI. Google ships no Termux or npm install for it; the only Android
 *    route found is the community package `@bash0816/agy-termux` (single maintainer, GPL-3.0). It fetches
 *    Google's `linux_arm64` release from GitHub and patches it, bundles a prebuilt `sigsys_shim.so`, and has
 *    an update path driven by a config file on its `main` branch. It is pinned to an exact version here and
 *    labelled as a community build; replace it with an official route as soon as one exists.
 *  - `claude` is `@anthropic-ai/claude-code`. From 2.1.113 on the package ships a native binary
 *    chosen by a postinstall step that has no Android (bionic) build, so it is pinned to 2.1.112,
 *    the last release whose `bin` is a JavaScript file Node can run.
 *
 * Installation only: no key is ever passed to a CLI from here; each CLI asks for its own sign-in.
 * Nothing here was run on a device; the notes say so where it matters.
 */
internal object AiCliCatalog {
    enum class Source { PKG, NPM }

    data class Cli(
        /** What the user types: `aicli install <id>`. */
        val id: String,
        val label: String,
        /** The executable name that lands in `$PREFIX/bin`. */
        val command: String,
        val source: Source,
        /** Termux package name (PKG) or npm package name (NPM). */
        val packageName: String,
        /** Exact npm version to install, or null for the latest. Ignored for PKG. */
        val version: String? = null,
        /** Termux packages that must be installed first (the Node runtime for npm tools). */
        val runtime: List<String> = emptyList(),
        val note: String
    )

    sealed class Step {
        /** `pkg <op> <names>` through HG2Gui's journaled package manager. [op] is "install" or "remove". */
        data class Pkg(val op: String, val names: List<String>) : Step()

        /** One line run in the shell. */
        data class Shell(val line: String) : Step()
    }

    val ALL: List<Cli> = listOf(
        Cli(
            id = "ollama", label = "Ollama", command = "ollama", source = Source.PKG, packageName = "ollama",
            note = "Runs models on this device. Start it with `ollama serve`, then choose the Ollama provider in AI settings."
        ),
        Cli(
            id = "codex", label = "OpenAI Codex CLI", command = "codex", source = Source.PKG, packageName = "codex",
            note = "OpenAI's coding agent, from the Termux repository. Sign in with `codex`."
        ),
        Cli(
            id = "gemini", label = "Gemini CLI", command = "gemini", source = Source.NPM,
            packageName = "@google/gemini-cli", runtime = listOf("nodejs-lts"),
            note = "Google's Gemini agent (pure JavaScript). Not yet run on a device."
        ),
        Cli(
            id = "agy", label = "Antigravity CLI (community build)", command = "agy", source = Source.NPM,
            packageName = "@bash0816/agy-termux", version = "1.4.12", runtime = listOf("nodejs-lts"),
            note = "Google's Antigravity CLI, repackaged for Termux by a community maintainer, not by Google. " +
                "Pinned to 1.4.12, but that package downloads and patches Google's release binary from GitHub, " +
                "ships a prebuilt native shim, and can update itself. Not yet run on a device."
        ),
        Cli(
            id = "claude", label = "Claude Code", command = "claude", source = Source.NPM,
            packageName = "@anthropic-ai/claude-code", version = "2.1.112", runtime = listOf("nodejs-lts"),
            note = "Pinned to 2.1.112, the last JavaScript-only release: newer builds ship a native binary with no " +
                "Android build. Older than current; not yet run on a device."
        )
    )

    private val NPM_NAME = Regex("^(@[a-z0-9][a-z0-9._-]*/)?[a-z0-9][a-z0-9._-]*$")
    private val PKG_NAME = Regex("^[a-z0-9][a-z0-9+.-]*$")
    private val VERSION = Regex("^[0-9]+\\.[0-9]+\\.[0-9]+$")
    private val ID = Regex("^[a-z][a-z0-9-]*$")

    /** True when every field that reaches a shell line or `pkg` argument matches its package-name grammar. */
    fun isValid(cli: Cli): Boolean =
        ID.matches(cli.id) && ID.matches(cli.command) &&
            when (cli.source) {
                Source.PKG -> PKG_NAME.matches(cli.packageName)
                Source.NPM -> NPM_NAME.matches(cli.packageName) && (cli.version?.let(VERSION::matches) ?: true)
            } &&
            cli.runtime.all(PKG_NAME::matches)

    init {
        // A recipe that could smuggle shell syntax is a programming error, never a runtime condition.
        ALL.forEach { require(isValid(it)) { "invalid aicli catalogue entry: ${it.id}" } }
        require(ALL.map { it.id }.toSet().size == ALL.size) { "duplicate aicli id" }
    }

    fun find(id: String): Cli? = ALL.firstOrNull { it.id == id }

    fun ids(): String = ALL.joinToString("|") { it.id }

    /** The runtime packages first, then the tool itself. `pkg install` is a no-op for what is already there. */
    fun installPlan(cli: Cli): List<Step> = buildList {
        if (cli.runtime.isNotEmpty()) add(Step.Pkg("install", cli.runtime))
        when (cli.source) {
            Source.PKG -> add(Step.Pkg("install", listOf(cli.packageName)))
            Source.NPM -> {
                val spec = cli.packageName + (cli.version?.let { "@$it" } ?: "")
                add(Step.Shell("npm install -g --no-audit --no-fund $spec"))
            }
        }
    }

    /** Removes only the tool; the runtime stays, since other things may use it. */
    fun removePlan(cli: Cli): List<Step> = when (cli.source) {
        Source.PKG -> listOf(Step.Pkg("remove", listOf(cli.packageName)))
        Source.NPM -> listOf(Step.Shell("npm uninstall -g ${cli.packageName}"))
    }

    /** The `aicli list` text: one line per tool with whether it is installed, then the note. */
    fun listing(isInstalled: (Cli) -> Boolean): String = buildString {
        appendLine("AI command-line tools (aicli install <${ids()}> / aicli remove <…>):")
        ALL.forEach { cli ->
            val state = (if (isInstalled(cli)) "installed" else "not installed").padEnd(13)
            appendLine("  ${cli.id.padEnd(8)} $state ${cli.label} - ${cli.note}")
        }
    }.trimEnd()
}
