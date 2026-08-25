# Architecture

**Current version:** 0.7.28 build 299 (`version.properties`)

HG2Gui is an Android **terminal application** — not a launcher. The UI and execution layer are
Kotlin Multiplatform (Compose); there is no separate reflection-based command framework. Built-in
commands are a fixed dispatch table in `terminal/Builtins.kt`. `:shared` currently builds for
Android only (no other KMP targets are declared) — "multiplatform" here names the tooling/module
split (a thin `:composeApp` entry point over shared UI/logic), not a live cross-platform target.

## Core Components

### 1. TerminalActivity (`TerminalActivity.kt`)
The entry point.
*   **Role:** Initialises the application, manages permissions and lifecycle, and sets the
    Compose content.
*   **Key Responsibilities:**
    *   Building the first `TerminalEngine`/`ShellSession` pair and running the bootstrap
        installer automatically if the Termux prefix isn't there yet.
    *   Persisting `fullscreen`/`font_scale_percent` via plain `SharedPreferences`.
    *   Edge-to-edge window handling (required from `compileSdk` 35).

### 2. Execution layer (`terminal/`)
*   `ShellSession.kt` — an `expect`/`actual` pair (`commonMain`/`androidMain`). The Android
    `actual` prefers a real Termux bootstrap (see `DistroManager` below) if one is installed,
    falling back to bare `/system/bin/sh` as the last resort. Whichever wins is kept alive for
    the life of the session so `cd` and exported variables persist. Commands are framed by a
    sentinel the shell echoes after each line, carrying `$?` and `$PWD`. Output is read as a raw
    buffer, not line-by-line — a real prompt (`Overwrite file? [y/N] `) never prints its own
    trailing newline while it waits, so an idle gap with an unterminated tail is treated as a
    live prompt and surfaced through `stream`'s `onNeedInput` callback rather than blocking
    forever. The default backend is a plain `ProcessBuilder` pipe: no job control, no cursor
    addressing, full-screen programs will not behave — which is exactly why `edit` exists as a
    separate Compose screen rather than trying to run `nano` through this. An experimental,
    off-by-default Settings toggle (`PtyPreference`) swaps this for the app's own bundled native
    pty bridge (`:terminal-emulator`'s `JNI.kt`) instead; unverified on real hardware, so the
    pipe stays the default.
*   `DistroManager.kt` (`androidMain`) — downloads and extracts the real Termux bootstrap: the
    same rootfs zip archive the official Termux app installs, giving genuine `bash`, `apt`/
    `pkg`, and coreutils. Runs automatically on first launch (see `TerminalActivity`) and can
    also be triggered by hand via the `bootstrap` verb. Also symlinks the bootstrap's main-repo
    APT keyring, writes a custom `apt.conf` pointing every `Dir::*` setting at this app's real
    prefix, rewrites hardcoded-Termux-package shebangs, and writes a bash-function wrapper
    profile so bootstrap-shipped shell scripts (which can't use Android's exec-exemption trick)
    still run via `source`.
*   `AptCatalog.kt` (`androidMain`) — everything `apt install` could install, not just what's on
    disk: parsed from `apt update`'s own downloaded `var/lib/apt/lists/*_Packages` index and
    heuristically categorized by name/description (Termux's repo carries no Section/Tag
    metadata). Backs the `install` pill's browsable catalog under `apt`/`apt-get`/`pkg`.
*   `HelpCatalog.kt` (`androidMain`) — background-probes each real binary on PATH with
    `<binary> --help` (bounded timeout, cached in `SharedPreferences`) to discover flag hints
    beyond `CommandTree`'s hand-curated `SHELL_HINTS` list. Read-only and synchronous at
    pill-composition time; the actual probing runs off-thread.
*   `Builtins.kt` — the eleven commands the real shell has no path to: `wifi`, `bluetooth`,
    `airplane`, `flash`, `volume`, `brightness` (system toggles with no shell binary behind
    them), `call`/`contacts` (via `ContactManager`), `vfs` (the sandboxed filesystem, see below),
    `calc` (via `util/CalculationEngine.kt`), and `edit` (the Compose editor). A plain
    `fun run(context, line): String` dispatch
    on the verb — no interface, no reflection, no per-command class.
*   `TerminalEngine.kt` — decides where a line runs: `bootstrap` streams from `DistroManager`,
    a verb in `Builtins.NAMES` goes to `Builtins.run` (a single synchronous result), anything
    else goes to the shell. Built-ins win ties. Also bridges `ShellSession`'s blocking
    `onNeedInput` callback to a suspend function via `runBlocking`, safe here since this branch
    already runs on a background dispatcher.
*   `ShellAliases.kt` (`commonMain`) — Kotlin-native replacements for what a live shell line
    editor would offer (autosuggestion, "did you mean", alias hints), since `ShellSession` sends
    one complete line at a time and reads one complete result back by default — there's no live
    PTY for a real line editor to attach to unless the experimental pty setting is on. Also
    detects the *shape* of a stalled interactive prompt (yes/no, a bracketed/comma-separated
    choice list, a `select`-style numbered menu, a password field) so `TerminalScreen` can offer
    a tap-only reply instead of a text field wherever the shape allows it. Surfaced through
    `TerminalScreen` as an ordinary `MenuNode` host+children, rendered by the same `PillMenu`
    every other command uses.

Sessions are one `ShellSession` + `TerminalEngine` pair each, so scrollback, command history and
working directory never leak between tabs. `TerminalActivity` owns the list and switches which
pair is "active"; nothing below the UI layer knows sessions exist.

### 3. UI layer (`ui/`)
Compose. A screen is a function of state; there is no view-hierarchy manager class behind it.
*   `TerminalScreen.kt` — session tabs, working directory, command line, modifier keys, output.
    Reads and writes through `SessionUiState`, one instance per session, so switching tabs
    never touches another session's scrollback or in-progress input. Also owns the
    auto-run-on-terminal-pick behaviour (a pill pick that leaves no further parameters runs
    immediately) and the pending-prompt hand-off (`SessionUiState.awaitPromptAnswer` /
    `answerPrompt`, a `CompletableDeferred` pair) that lets a stalled interactive command
    suspend the UI without a second channel.
*   `ui/menu/PillMenu.kt` — the suggestion tree and its motion. `MenuNode` children can be
    static (`children`) or resolved lazily on first navigation into a node (`resolveChildren`,
    e.g. the ssh/workflow presets lists) instead of eager materialization of a combinatorially
    large subtree. A node's `value` is the token text it contributes if different from its
    display `label`; `emitsToken = false` marks a purely navigational pick that should never
    itself land on the command line. `wizardId` hands a pick off to the caller's own multi-step
    flow instead of drilling into more children or emitting a token; `settleBeforeWizard` delays
    that hand-off until the pick's own trail crumb has actually settled and reported its on-screen
    position (`onCrumbPositioned`) - for a wizard whose entrance animation needs to grow out from
    that exact spot, like the Select File/Folder pill.
*   `ui/menu/CommandTree.kt` — builds the tree: the eleven `Builtins` verbs (fixed lists, not
    discovered) into Device / Apps & nav / Features, and the shell's real PATH binaries into one
    root category per package category — `DpkgCatalog` reads which package owns a binary from
    dpkg's own bookkeeping, and a hand-curated map (Termux's packages carry no Debian Section
    field to read a category from directly) turns that into "Package management", "Network",
    "System", "Development", and so on — with hyphenated command families nested under a shared
    parent. "Device" (the fixed hardware-toggle builtins: wifi/bluetooth/airplane/flash/volume/
    brightness) is named apart from the shell-discovered "System" category (procps/tmux/htop and
    the like) on purpose - both used to be labelled "System", two identically-named root pills
    with nothing but hue to tell them apart. A small `CATEGORY_OF_BINARY` override map takes
    priority over the package-level map for binaries whose package doesn't reflect their actual
    role (`pkg` ships in `termux-tools`, mapped to "System", but belongs with `apt`/`dpkg` in
    "Package management"). See [COMMANDS.md](COMMANDS.md).
*   `ui/menu/FileBrowser.kt` — the Select File/Folder pill: a `file…` trigger (`wizardId`,
    `settleBeforeWizard = true`) that opens the graphical path picker (`ui/files/
    PathPickerScreen.kt`) rather than drilling further into the pill stack, wrapped in
    `PillPerimeterReveal` (see below) so the pill itself runs the screen's perimeter and becomes
    the browser.
*   `ui/menu/PillPerimeterReveal.kt` — the entrance/exit motion for the Select File/Folder pill:
    from wherever its trail crumb landed, a `hue`-coloured bar grows right along the bottom edge
    to the bottom-right corner, up the right edge, left across the top, then down the left edge -
    closing the loop back over its own start. The instant that last leg begins, a downward wipe
    fills the enclosed frame and reveals the browser underneath. The fuller, edge-by-edge sibling
    of `PillWrapReveal`'s single-rect "run the perimeter" simplification, used specifically here
    because the file picker's origin is a trail crumb, not a fixed root pill.
*   `ui/editor/EditorScreen.kt` — the `edit` command's text editor: a plain Compose screen (Save/
    Back pills, a text field), hosted by `EditorActivity` so it's also a valid target for another
    app's VIEW/EDIT intent on a text file.
*   `ui/files/FilesScreen.kt` — the graphical explorer over `VfsManager`'s sandbox, with search,
    sorting, kind/hidden/recency filters, batch actions, rename, media grid, and storage views.
*   `Theme.kt` — Azphalt colour and type tokens.

## Package Structure (`com.hereliesaz.hg2gui`)

*   **`composeApp` root**: `TerminalActivity`, `EditorActivity` — the only two activities — plus
    `mcp/McpServerService.kt`, the MCP server's foreground `Service`. Everything else below lives
    in the separate `:shared` module (`commonMain`/`androidMain`).
*   **`ui/`**: Compose UI and the pill menu, `ui/editor/`, `ui/files/`, `ui/guide/` (the command
    glossary), `ui/ssh/` (the ssh connection wizard), `ui/ai/` (the AI chat screen), `ui/azp/`
    (the Store browser).
*   **`managers/`**: `ContactManager` (contacts, backs `call`/`contacts`), `VfsManager` (a file
    layer rooted, by default, at `filesDir/home` - the real Termux `$HOME`, still app-private
    storage; normal operations stay confined there or, opt-in, to real device storage
    (`StorageAccessManager`), while the explicit root-only `vfs mount` command can bind-mount it
    into the real filesystem), `SshPresets`/`WorkflowStore`/`AzpLibrary` (flat-`SharedPreferences` stores),
    `PtyPreference` (the real-pty Settings toggle), `flashlight/` (the torch implementation
    behind `flash`).
*   **`terminal/`**: `ShellSession`, `TerminalEngine`, `Builtins` (the eleven built-in commands),
    `DistroManager` (the Termux bootstrap installer), `DpkgCatalog` (reads dpkg's own bookkeeping
    for which package owns an installed binary), `AptCatalog` (everything `apt install` could
    install, for the browsable install catalog), `HelpCatalog` (background `--help`-flag
    discovery) — `CommandTree`'s hand-curated map turns package ownership into a category.
*   **`ai/`**: the AI chat's Anthropic API client. **`azp/`**: the azphalt Store client (search,
    download, Ed25519 signature verification, dependency-resolving script installs). **`mcp/`**:
    the MCP server's JSON-RPC protocol and tool registry.
*   **`util/`**: `CalculationEngine` (the `calc` expression parser, `commonMain`), plus a handful
    of Android-only helpers still in use — logging/crash reporting, the interactive-shell wrapper
    `vfs mount` needs for `su`, `GenericFileProvider`.

## Gradle Module Boundaries

*   **`:composeApp`** — the thin Android entry point: the two activities, the MCP foreground
    service, resources, product flavor config. No terminal/UI logic of its own.
*   **`:shared`** — the Kotlin Multiplatform module (Android-only today; no other targets are
    declared) holding the actual Compose UI, terminal routing, managers, and platform
    integrations described above.
*   **`:terminal-emulator`** — a vendored VT100 output parser plus a native pty JNI bridge
    (`JNI.kt`/`jni/termux.c`). The parser is used unconditionally to flatten shell output for
    display; the pty bridge itself is only live when `PtyPreference`'s experimental setting is
    on — the default shell backend is still a `ProcessBuilder` process over ordinary streams.
*   **`:termux-shared`** — a vendored Termux-compatible Android utility library, declared as a
    dependency of both `:composeApp` and `:shared` but not currently called by any
    HG2Gui-authored code.

## Product Subsystems

*   **MCP:** an explicit-start, loopback-only JSON-RPC server. VFS tools are sandboxed; shell
    execution is separately biometric-gated and uses a service-owned terminal engine.
*   **Workflows and AI:** workflows expand reviewed command templates; AI produces command
    suggestions and optional token explanations. Neither path executes a suggestion automatically.
*   **Azphalt store:** package extraction is path-contained and signature-checked; every extracted
    payload must be declared and match its declared SHA-256 digest. Skill text can augment AI
    prompts. `script` packages can
    resolve Termux dependencies and install a PATH wrapper; other package kinds remain stored data.
*   **Context and Guide:** the OS-context tree offers static remote-OS reference commands, while
    the Guide is reading material. Neither pretends to discover a remote machine.

## Invariants

1.  The app is a terminal, not a launcher: there is no `HOME` intent filter.
2.  Built-ins are an explicit eleven-verb dispatch table; no reflection discovers commands.
3.  `bootstrap` routes first, built-ins win command-name ties, and all other input reaches the shell.
4.  Every terminal tab owns its engine, shell, UI state, history, scrollback, and working directory.
5.  VFS operations resolve paths canonically beneath whichever root is active (`filesDir/home` by
    default, opt-in real device storage otherwise); shell access requires the explicit,
    root-only `vfs mount` escape hatch.
6.  Shell processes are persistent but have no PTY, job control, or full-screen cursor semantics.
7.  Wizard- and AI-produced commands are assembled for review, never executed automatically.
8.  MCP binds only to loopback; shell tools remain disabled until explicit biometric approval.
9.  Azphalt extraction never writes outside its package directory; every extracted payload must be
    declared and match its declared SHA-256 digest, and the package must pass signature policy.

## Decisions and Reasons

*   **Compose-only presentation:** separate editor and file surfaces replace cursor-addressed terminal
    programs because the shell transport deliberately has no PTY.
*   **Fixed built-ins:** only Android capabilities with no useful shell binary live in Kotlin, keeping
    the real shell authoritative for everything else.
*   **Per-session engines:** persistent shell state is useful, but leaking it between tabs is not.
*   **Curated command categories:** Termux package metadata has no reliable Debian Section field, so
    dpkg supplies ownership while a checked-in map supplies human-facing categories.
*   **Review before execution:** graphical wizards and AI may assemble commands, but user intent is
    established only when the user runs them.

## Data Flow

1.  **Input:** The user taps pills in `PillMenu`; `TerminalScreen` accumulates tokens. Typing
    is possible but secondary. A pick that leaves no further parameters runs immediately.
2.  **Processing:** Run joins the tokens and calls `TerminalEngine.run`, off the main thread.
3.  **Routing:** The first token decides the path — `bootstrap` streams from `DistroManager`, a
    `Builtins.NAMES` verb goes to `Builtins.run`, anything else to `ShellSession`.
4.  **Execution:**
    *   A built-in runs its branch in `Builtins.run` and returns a single result string
        synchronously (`bootstrap` is the one exception, streaming progress as it downloads).
    *   Otherwise the line is written to the shell and read back to the sentinel. If the shell
        stalls waiting on stdin (a real prompt, not a hang), `SessionUiState` surfaces it and
        suspends until the UI answers — a yes/no-shaped prompt gets a dedicated Answer stack.
5.  **Output:** The result returns as a string and becomes screen state, rendered in a record
    tile.

## Migration status

| Layer | State |
| --- | --- |
| Entry point, terminal screen, suggestion menu, built-in commands, editor | Kotlin + Compose |
| XML layouts | Removed — Compose only |
| Launcher-specific code (drawer, app menus, fake launcher) | Removed |
