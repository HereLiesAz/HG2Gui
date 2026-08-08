# Architecture

HG2Gui is an Android **terminal application** — not a launcher. It follows the "Command"
pattern for its engine and a "Manager" pattern for system resources. The presentation layer is
Jetpack Compose; the engine beneath it is Java.

## Core Components

### 1. TerminalActivity (`TerminalActivity.kt`)
The entry point.
*   **Role:** Initialises the application, manages permissions and lifecycle, and sets the
    Compose content.
*   **Key Responsibilities:**
    *   Initialising `MainManager`.
    *   Handling `Intent`s and `BroadcastReceiver`s for inter-process communication.
    *   Edge-to-edge window handling (required from `compileSdk` 35).

### 2. Main Manager (`MainManager.java`)
The central coordinator.
*   **Role:** Bridges the UI and the underlying logic.
*   **Key Responsibilities:**
    *   Initialising all sub-managers (Apps, File, Contacts, …).
    *   Routing user input to the appropriate command or manager.
    *   Managing the `CommandRepository`.

`MainManager` does not return output. Commands broadcast what they print over
`LocalBroadcastManager` as `PrivateIOReceiver.ACTION_OUTPUT`, so output is collected by
listening, not by a return value — see `TerminalEngine` below.

### 3. Execution layer (`terminal/`)
*   `ShellSession.kt` — an `expect`/`actual` pair (`commonMain`/`androidMain`). The Android
    `actual` picks the best of three shell tiers in order: a bundled static `zsh` binary, a real
    Termux bootstrap (see `DistroManager` below) if one is installed, or bare `/system/bin/sh`
    as the last resort. Whichever wins is kept alive for the life of the session so `cd` and
    exported variables persist. Commands are framed by a sentinel the shell echoes after each
    line, carrying `$?` and `$PWD`. Output is read as a raw buffer, not line-by-line — a real
    prompt (`Overwrite file? [y/N] `) never prints its own trailing newline while it waits, so
    an idle gap with an unterminated tail is treated as a live prompt and surfaced through
    `stream`'s `onNeedInput` callback rather than blocking forever. There is still no pty: no
    job control, no cursor addressing, and full-screen programs will not behave.
*   `DistroManager.kt` (`androidMain`) — downloads and extracts the real Termux bootstrap: the
    same rootfs zip archive the official Termux app installs, giving genuine `bash`, `apt`/
    `pkg`, and coreutils. Runs automatically on first launch (see `TerminalActivity`) and can
    also be triggered by hand via the `bootstrap` command.
*   `TerminalEngine.kt` — decides where a line runs. A verb matching a built-in goes to
    `MainManager` and its broadcast output is captured (including a `StreamableCommand`'s
    incremental output, via `Command.execStream`); anything else goes to the shell. Built-ins
    win ties, so `apps` is the app list rather than whatever is on `PATH`. Also bridges
    `ShellSession`'s blocking `onNeedInput` callback to a suspend function via `runBlocking`,
    safe here since this branch already runs on a background dispatcher.
*   `ShellAliases.kt` (`commonMain`) — Kotlin-native replacements for what a live shell line
    editor would offer (autosuggestion, "did you mean", alias hints), since `ShellSession` sends
    one complete line at a time and reads one complete result back — there's no live PTY for a
    real line editor to attach to. Surfaced through `TerminalScreen` as an ordinary `MenuNode`
    host+children, rendered by the same `PillMenu` every other command uses.

Sessions are one `ShellSession` + `TerminalEngine` pair each, so scrollback, command history
and working directory never leak between tabs. Built-ins still route through the single
shared `MainManager` — app-wide settings (`theme`, `alias`, …) apply everywhere, only the
shell itself is per-session. `TerminalActivity` owns the list and switches which pair is
"active"; nothing below the UI layer knows sessions exist.

### 4. UI layer (`ui/`)
Compose. There is no `UIManager`; a screen is a function of state.
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
*   `ui/menu/CommandTree.kt` — builds the tree: the live `CommandGroup`'s built-ins into
    System / Apps & nav / Features, and the shell's real PATH binaries into one root category
    per package category — `DpkgCatalog` reads which package owns a binary from dpkg's own
    bookkeeping, and a hand-curated map (Termux's packages carry no Debian Section field to read
    a category from directly) turns that into "Package management", "Network", "Development",
    and so on — with hyphenated command families nested under a shared parent. See
    [COMMANDS.md](COMMANDS.md).
*   `ui/menu/FileBrowser.kt` — the in-stack graphical file picker: a `file…` trigger whose
    children are resolved from a real directory listing, navigated through the same pill stack
    rather than a separate screen.
*   `Theme.kt` — Azphalt colour and type tokens.

## Package Structure (`com.hereliesaz.hg2gui`)

*   **root**: `TerminalActivity` (Kotlin), plus `GuideActivity`, `PanicActivity` — feature
    screens reached by command, never by icon.
*   **`ui/`**: Compose UI and the pill menu.
    *   `ui/files/FilesScreen.kt`: the graphical explorer over `VfsManager`'s sandbox.
*   **`managers/`**: Logic for specific domains.
    *   `AppsManager`: launching installed apps (as a command, not a drawer).
    *   `FileManager`: file system operations against real Android storage.
    *   `VfsManager`: a second, sandboxed file layer rooted at `filesDir/vfs` — real files, but
        confined to the app's private storage and never touched by the real shell.
    *   `TerminalManager`: core terminal state — scrollback, history, sessions.
    *   `SystemContext`: OS/environment emulation.
*   **`terminal/`**: `ShellSession`, `TerminalEngine`, `DistroManager` (the Termux bootstrap
    installer), `DpkgCatalog` (reads dpkg's own bookkeeping for which package owns a binary —
    `CommandTree`'s hand-curated map turns that into a category).
*   **`commands/`**: The command pattern.
    *   `CommandAbstraction`: interface for all commands. `StreamableCommand` extends it for
        commands whose output arrives over time (`execStream`) rather than as one return value
        — e.g. `bootstrap`. `Command` (the per-invocation wrapper holding parsed args) exposes
        both `exec` and `execStream`; a dispatcher calls `execStream` first and falls back to
        `exec` when the wrapped command isn't streamable.
    *   `CommandRepository`: index of available commands — also the source of the menu tree.
    *   `main/`: core system commands, including `vfs` (the command-line face of `VfsManager`)
        and `bootstrap` (the command-line face of `DistroManager`, also run automatically on
        first launch — see `TerminalActivity`).
    *   `tuixt/`: the built-in text editor — also how `FilesScreen` opens a file.
*   **`tuils/`**: Utilities.

## Data Flow

1.  **Input:** The user taps pills in `PillMenu`; `TerminalScreen` accumulates tokens. Typing
    is possible but secondary. A pick that leaves no further parameters runs immediately.
2.  **Processing:** Run joins the tokens and calls `TerminalEngine.run`, off the main thread.
3.  **Routing:** The first token decides the path — a built-in name goes to `MainManager`,
    anything else to `ShellSession`.
4.  **Execution:**
    *   A known command runs its `Command` class in `commands/`, and its broadcast output is
        captured for the duration of the call (streamed incrementally for a `StreamableCommand`
        like `bootstrap`).
    *   A matching app name goes to `AppsManager`.
    *   An alias is expanded by `AliasManager`.
    *   Otherwise the line is written to the shell and read back to the sentinel. If the shell
        stalls waiting on stdin (a real prompt, not a hang), `SessionUiState` surfaces it and
        suspends until the UI answers — a yes/no-shaped prompt gets a dedicated Answer stack.
5.  **Output:** The result returns as a string and becomes screen state, rendered in a record
    tile. `Outputable` remains for commands that stream.

## Migration status

| Layer | State |
| --- | --- |
| Entry point, terminal screen, suggestion menu | Kotlin + Compose |
| Command engine, managers, `tuixt`, Guide | Java, unchanged |
| XML layouts for the terminal | Replaced |
| Launcher-specific code (drawer, app menus, fake launcher) | Removed |
