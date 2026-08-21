# HG2Gui — The Hitchhiker's Terminal to the Galaxy

**HG2Gui** is a touch-optimised terminal designed after Douglas Adams' The Hitchhiker's Guide to the Galaxy — every command, subcommand and argument is an animated pill thinger you tap, with the goal being to use a real shell without a keyboard. 

Inspired by the *Hitchhiker's Guide to the Galaxy* (2005) aesthetic and built on Termux.

## Features

*   **Point-and-click commands.** The suggestion tree is the input method. Tap a category, tap
    a command, tap an argument. Picking the last parameter a command needs runs it immediately —
    no separate confirmation — or press **Run** yourself at any point. The keyboard is optional.
*   **A real Termux backend.** Shell commands run in a long-lived shell session backed by the
    actual Termux bootstrap — the same rootfs archive the official Termux app installs — giving
    real `bash`, `apt`/`pkg`, and real coreutils, not Android's own toybox. It installs itself
    automatically on first launch, exactly like Termux does, with progress visible in the buffer.
*   **The tree reflects what's actually installed.** Shell pills are discovered live from the
    bootstrap's own `bin/`, matched back to the package that owns each one via dpkg's own
    bookkeeping, and grouped into a category (Package management, Network, Development, …) —
    `pkg install python` and it shows up as a pill the next time a command finishes, no restart
    needed. Hyphenated command families (`apt-get`, `apt-key`, `apt-mark`, …) nest under one
    shared parent pill instead of cluttering the list as unrelated flat entries. Beyond a small
    hand-curated set, every real binary on PATH also gets its own `--help` output probed once in
    the background and parsed into tappable flag pills, so typing a flag from memory is the
    exception, not the rule.
*   **Browse what's installable, don't type it blind.** The `install` pill under `apt`/`apt-get`/
    `pkg` drills into a categorized, scrollable catalog parsed from `apt update`'s own downloaded
    package index — Libraries, Networking, Python, Development headers, and so on — instead of
    demanding a package name you already have to know.
*   **A graphical file picker.** Any command that takes a file argument gets a `file…` pill that
    opens a graphical Select File/Folder screen from the settled pill crumb — no typing a path by
    hand.
*   **Interactive prompts, answered graphically.** A command that stops mid-run to ask something
    gets a tap-only reply whenever the shape of the question allows it: a yes/no question gets a
    dedicated **Answer** stack (`YES`/`NO`), a `select`-style numbered menu gets one pill per
    option (labelled, not just numbered), and a bracketed/comma-separated choice list (dpkg's own
    conffile prompt, git's interactive add, …) gets one pill per token. A password prompt masks
    the input field instead; anything else falls back to a plain field, whose Run button becomes
    Send.
*   **Kotlin-native suggestions.** Autosuggestion (from history), "did you mean" (after a failed
    command), and alias hints (`gs` for `git status`, and friends) surface as ordinary pills,
    implemented natively rather than as a shell plugin — there's no live PTY for a shell's own
    line editor to attach to.
*   **A fixed set of built-ins for what Android exposes no shell path to.** System toggles
    (wifi, bluetooth, airplane mode, flashlight, volume, brightness), `call`/`contacts`, a
    sandboxed `vfs` filesystem, `calc`, and `edit` — a small Compose text editor for files a
    real terminal editor can't render without a pty. Everything else (`apps`, `alias`, sharing
    files, and so on) is a real shell binary now, not a reimplementation of one.
*   **Sessions.** Named terminal sessions, each with its own scrollback and working directory.
*   **Aliases.** Native shell aliases plus `ShellAliases.kt`'s own suggestion pills — no
    duplicate alias system layered on top of the real shell's.
*   **A real file manager.** Search, sort, kind/hidden/recency filters, multi-select batch actions,
    in-place rename, an automatic media grid, and a storage-by-type breakdown over the `vfs`
    sandbox — folders are capsules that expand in place, siblings squishing into thin coloured
    rods beside them.
*   **The Guide.** A chaptered glossary of real commands paired with invented, Hitchhiker's-
    Guide-style definitions, reachable from the command picker — reading material, not another
    way to run something.
*   **SSH presets.** The `ssh` pill (under Network) opens saved connections plus a **new…**
    wizard that collects host/user/port/key one step at a time and drops the assembled command
    on the line to review. Host-key and password prompts reuse the same interactive-prompt
    machinery every other command gets.
*   **Workflows.** Save a command template with `{placeholder}` slots; running it asks for each
    placeholder's value, then drops the assembled command on the line — nothing runs on its own.
*   **AI chat.** A natural-language-to-command suggestion screen (your own Anthropic API key,
    set in Settings). It proposes, optionally with a per-flag breakdown; a **USE ▸** pill is the
    only thing that ever puts a suggestion on the command line.
*   **The Store.** A browser for [azphalt.store](https://azphalt.store)'s `.azp` package
    registry — skills, MCP-server headers, code, packs, companion apps, scripts. Every install is
    path-contained, SHA-256-checked against its manifest, and Ed25519-signature-verified.
*   **MCP server.** An optional, loopback-only, explicit-start server (Settings → MCP SERVER) a
    paired external AI agent can use to read/write this app's sandboxed files. Running real shell
    commands through it is a second, biometric-gated switch, off by default.
*   **An experimental real pseudoterminal.** Off by default (Settings → Real pseudoterminal):
    swaps the plain-pipe shell backend for the app's own bundled native pty bridge, so full-screen
    tools (`vim`, `top`, `less`, an interactive REPL) can draw a real screen instead of breaking.
    Unverified on real hardware — the always-worked plain pipe stays the default.

## Interface

The screen is, top to bottom: session tabs, the working directory, the command line with a Run
capsule, modifier keys (`ctrl` `alt` `esc` `tab` `↑` `↓`), and the command tree. Tapping a
category sends the stack off the left edge, drops that pill to the bottom of the screen, and
cascades its children upward from it. Output arrives in a record tile, not a scrollback wall.

## Project structure

Kotlin Multiplatform (Compose): `:composeApp` is a thin Android entry point, and the actual UI
and execution layer live in a separate `:shared` module. There is no separate command framework:
the eleven built-ins are a fixed dispatch table (`terminal/Builtins.kt`), not a
reflection-discovered plugin system.

*   `shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/`
    *   `terminal/ShellSession.kt` — the `expect` shell-session contract, plus
        `ShellAliases.kt` — Kotlin-native alias expansion, history autosuggestion, "did you
        mean", and interactive-prompt shape detection (yes/no, bracketed choice, numbered menu,
        password), implemented here rather than as a shell plugin because there's no live PTY by
        default for a real shell line editor to attach to.
    *   `util/CalculationEngine.kt` — the expression parser behind `calc`.
    *   `ui/` — Compose UI. `TerminalScreen.kt` (the terminal), `SessionUiState.kt` (per-session
        state, including the pending-prompt hand-off for interactive commands), `ui/editor/` (the
        `edit` command's text editor screen), `ui/files/` (the `vfs` sandbox's file manager),
        `ui/guide/` (the command glossary reader), `ui/ssh/` (the ssh connection wizard),
        `ui/ai/` (the AI chat screen), `ui/azp/` (the Store browser).
    *   `ui/menu/PillMenu.kt` — the suggestion tree and its choreography. `MenuNode` supports
        lazily-resolved children (`resolveChildren`) for live data, like a directory listing.
*   `shared/src/androidMain/kotlin/com/hereliesaz/hg2gui/`
    *   `terminal/` — execution. `ShellSession.kt` (the `actual` implementation: a long-lived
        shell framed by a sentinel that reports exit status and working directory, with
        idle-based detection of a stalled interactive prompt — plain pipe by default, an
        experimental real pty behind a Settings toggle), `TerminalEngine.kt` (routes a line to a
        built-in, the bootstrap installer, or the shell), `Builtins.kt` (the eleven built-ins:
        system toggles, `call`/`contacts`, `vfs`, `calc`, `edit`), `DistroManager.kt` (downloads
        and extracts the real Termux bootstrap), `DpkgCatalog.kt` (which package owns an
        installed binary, from dpkg's own bookkeeping), `AptCatalog.kt` (everything `apt install`
        could install, parsed from `apt update`'s own downloaded index, for the browsable install
        catalog), `HelpCatalog.kt` (background-probes each binary's own `--help` output for flag
        hints beyond the hand-curated list).
    *   `ui/menu/CommandTree.kt` — builds the tree: the eleven built-ins into Device / Apps & nav /
        Features, real PATH binaries by category (Package management, Network, Development, …)
        with hyphenated command families (`apt-get`/`apt-key`/…) nested under their shared
        parent.
    *   `ui/menu/FileBrowser.kt` — the in-stack graphical file picker.
    *   `ai/AiClient.kt` — the AI chat's Anthropic API client. `azp/` — the azphalt Store client
        (search, download, signature verification, dependency-resolving script installs).
    *   `mcp/` — the MCP server's JSON-RPC protocol and tool registry (`vfs.*`/`shell.*`).
    *   `managers/` — `ContactManager.kt` (contacts, for `call`/`contacts`), `VfsManager.kt` (the
        sandboxed filesystem `vfs` and the Files screen operate on), `SshPresets.kt`/
        `WorkflowStore.kt`/`AzpLibrary.kt` (flat-`SharedPreferences` stores), `PtyPreference.kt`
        (the real-pty toggle), `flashlight/` (the torch implementation behind `flash`).
    *   `util/` — the handful of shared helpers (logging, crash reporting, the interactive-shell
        wrapper, `GenericFileProvider`) still in use.
*   `composeApp/src/main/kotlin/com/hereliesaz/hg2gui/` — `TerminalActivity.kt` (the entry point
    and all top-level screen/state wiring), `EditorActivity.kt` (hosts the `edit` screen, and is
    also the target of another app's VIEW/EDIT intent on a text file), `mcp/McpServerService.kt`
    (the MCP server's foreground `Service`).
*   `composeApp/src/main/res/` — resources. Jost lives in `res/font/`.
*   `terminal-emulator/` — a vendored VT100 parser plus a native pty JNI bridge
    (`JNI.kt`/`jni/termux.c`, the same one upstream Termux's own `TerminalSession` uses). The
    VT100 parser flattens shell output on every command regardless of backend; the pty bridge
    itself is only live when the experimental real-pty setting is on.
*   `termux-shared/` — a vendored Termux-compatible Android utility library; present as a build
    dependency but not currently called by any HG2Gui-authored code.

## Build

JDK 21, Gradle 9.7.0, AGP 9.3.1, Kotlin 2.4.10, Compose Multiplatform 1.11.1.
`compileSdk`/`targetSdk` 37, `minSdk` 24.

```bash
./gradlew assembleDebug
```

## Documentation

*   [Vision](docs/VISION.md)
*   [Architecture](docs/ARCHITECTURE.md)
*   [Android architecture](docs/HG2GUI_ARCHITECTURE.md)
*   [Commands](docs/COMMANDS.md)
*   [Design](docs/DESIGN.md)
*   [User guide](docs/USER_GUIDE.md)
*   [Contributing](docs/CONTRIBUTING.md)
