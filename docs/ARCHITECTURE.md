# Architecture

HG2Gui is an Android **terminal application** — not a launcher. The UI and execution layer are
Kotlin Multiplatform (Compose); there is no separate reflection-based command framework. Built-in
commands are a fixed dispatch table in `terminal/Builtins.kt`.

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
    `actual` picks the best of three shell tiers in order: a bundled static `zsh` binary, a real
    Termux bootstrap (see `DistroManager` below) if one is installed, or bare `/system/bin/sh`
    as the last resort. Whichever wins is kept alive for the life of the session so `cd` and
    exported variables persist. Commands are framed by a sentinel the shell echoes after each
    line, carrying `$?` and `$PWD`. Output is read as a raw buffer, not line-by-line — a real
    prompt (`Overwrite file? [y/N] `) never prints its own trailing newline while it waits, so
    an idle gap with an unterminated tail is treated as a live prompt and surfaced through
    `stream`'s `onNeedInput` callback rather than blocking forever. There is still no pty: no
    job control, no cursor addressing, and full-screen programs will not behave — which is
    exactly why `edit` exists as a separate Compose screen rather than trying to run `nano`
    through this.
*   `DistroManager.kt` (`androidMain`) — downloads and extracts the real Termux bootstrap: the
    same rootfs zip archive the official Termux app installs, giving genuine `bash`, `apt`/
    `pkg`, and coreutils. Runs automatically on first launch (see `TerminalActivity`) and can
    also be triggered by hand via the `bootstrap` verb.
*   `Builtins.kt` — the ten commands the real shell has no path to: `wifi`, `bluetooth`,
    `airplane`, `flash`, `volume`, `brightness` (system toggles with no shell binary behind
    them), `call`/`contacts` (via `ContactManager`), `vfs` (the sandboxed filesystem, see below),
    `calc` (via `util/CalculationEngine.kt`). A plain `fun run(context, line): String` dispatch
    on the verb — no interface, no reflection, no per-command class.
*   `TerminalEngine.kt` — decides where a line runs: `bootstrap` streams from `DistroManager`,
    a verb in `Builtins.NAMES` goes to `Builtins.run` (a single synchronous result), anything
    else goes to the shell. Built-ins win ties. Also bridges `ShellSession`'s blocking
    `onNeedInput` callback to a suspend function via `runBlocking`, safe here since this branch
    already runs on a background dispatcher.
*   `ShellAliases.kt` (`commonMain`) — Kotlin-native replacements for what a live shell line
    editor would offer (autosuggestion, "did you mean", alias hints), since `ShellSession` sends
    one complete line at a time and reads one complete result back — there's no live PTY for a
    real line editor to attach to. Surfaced through `TerminalScreen` as an ordinary `MenuNode`
    host+children, rendered by the same `PillMenu` every other command uses.

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
    static (`children`) or resolved lazily on first navigation into a node
    (`resolveChildren`) — used by the file picker and by nothing else needing eager
    materialization of a combinatorially large subtree. A node's `value` is the token text it
    contributes if different from its display `label`; `emitsToken = false` marks a purely
    navigational pick (a directory on the way to a file, a "browse for a file" trigger) that
    should never itself land on the command line.
*   `ui/menu/CommandTree.kt` — builds the tree: the ten `Builtins` verbs (fixed lists, not
    discovered) into System / Apps & nav / Features, and the shell's real PATH binaries into one
    root category per package category — `DpkgCatalog` reads which package owns a binary from
    dpkg's own bookkeeping, and a hand-curated map (Termux's packages carry no Debian Section
    field to read a category from directly) turns that into "Package management", "Network",
    "Development", and so on — with hyphenated command families nested under a shared parent. See
    [COMMANDS.md](COMMANDS.md).
*   `ui/menu/FileBrowser.kt` — the in-stack graphical file picker: a `file…` trigger whose
    children are resolved from a real directory listing, navigated through the same pill stack
    rather than a separate screen.
*   `ui/editor/EditorScreen.kt` — the `edit` command's text editor: a plain Compose screen (Save/
    Back pills, a text field), hosted by `EditorActivity` so it's also a valid target for another
    app's VIEW/EDIT intent on a text file.
*   `ui/files/FilesScreen.kt` — the graphical explorer over `VfsManager`'s sandbox.
*   `Theme.kt` — Azphalt colour and type tokens.

## Package Structure (`com.hereliesaz.hg2gui`)

*   **root**: `TerminalActivity`, `EditorActivity` — the only two activities.
*   **`ui/`**: Compose UI and the pill menu, `ui/editor/`, `ui/files/`.
*   **`managers/`**: `ContactManager` (contacts, backs `call`/`contacts`), `VfsManager` (a
    sandboxed file layer rooted at `filesDir/vfs` — real files, but confined to the app's private
    storage and never touched by the real shell), `flashlight/` (the torch implementation behind
    `flash`).
*   **`terminal/`**: `ShellSession`, `TerminalEngine`, `Builtins` (the ten built-in commands),
    `DistroManager` (the Termux bootstrap installer), `DpkgCatalog` (reads dpkg's own bookkeeping
    for which package owns a binary — `CommandTree`'s hand-curated map turns that into a
    category).
*   **`util/`**: `CalculationEngine` (the `calc` expression parser, `commonMain`), plus a handful
    of Android-only helpers still in use — logging/crash reporting, the interactive-shell wrapper
    `vfs mount` needs for `su`, `GenericFileProvider`.

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
