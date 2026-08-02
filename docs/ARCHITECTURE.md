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
*   `ShellSession.java` — one long-lived `/system/bin/sh`, kept for the life of the session so
    `cd` and exported variables persist. Commands are framed by a sentinel the shell echoes
    after each line, carrying `$?` and `$PWD`. There is no pty: no job control, no cursor
    addressing, and full-screen programs will not behave.
*   `TerminalEngine.kt` — decides where a line runs. A verb matching a built-in goes to
    `MainManager` and its broadcast output is captured; anything else goes to the shell.
    Built-ins win ties, so `apps` is the app list rather than whatever is on `PATH`.

### 4. UI layer (`ui/`)
Compose. There is no `UIManager`; a screen is a function of state.
*   `TerminalScreen.kt` — sessions, working directory, command line, modifier keys, output.
*   `ui/menu/PillMenu.kt` — the suggestion tree and its motion.
*   `ui/menu/CommandTree.kt` — builds the tree from the live `CommandGroup`, plus a Shell
    category for verbs that run in `ShellSession`.
*   `Theme.kt` — Azphalt colour and type tokens.

## Package Structure (`com.hereliesaz.hg2gui`)

*   **root**: `TerminalActivity` (Kotlin), plus `GuideActivity`, `PanicActivity` — feature
    screens reached by command, never by icon.
*   **`ui/`**: Compose UI and the pill menu.
*   **`managers/`**: Logic for specific domains.
    *   `AppsManager`: launching installed apps (as a command, not a drawer).
    *   `FileManager`: file system operations.
    *   `TerminalManager`: core terminal state — scrollback, history, sessions.
    *   `SystemContext`: OS/environment emulation.
*   **`commands/`**: The command pattern.
    *   `CommandAbstraction`: interface for all commands.
    *   `CommandRepository`: index of available commands — also the source of the menu tree.
    *   `main/`: core system commands.
    *   `tuixt/`: the built-in text editor.
*   **`tuils/`**: Utilities.

## Data Flow

1.  **Input:** The user taps pills in `PillMenu`; `TerminalScreen` accumulates tokens. Typing
    is possible but secondary.
2.  **Processing:** Run joins the tokens and calls `TerminalEngine.run`, off the main thread.
3.  **Routing:** The first token decides the path — a built-in name goes to `MainManager`,
    anything else to `ShellSession`.
4.  **Execution:**
    *   A known command runs its `Command` class in `commands/`, and its broadcast output is
        captured for the duration of the call.
    *   A matching app name goes to `AppsManager`.
    *   An alias is expanded by `AliasManager`.
    *   Otherwise the line is written to the shell and read back to the sentinel.
5.  **Output:** The result returns as a string and becomes screen state, rendered in a record
    tile. `Outputable` remains for commands that stream.

## Migration status

| Layer | State |
| --- | --- |
| Entry point, terminal screen, suggestion menu | Kotlin + Compose |
| Command engine, managers, `tuixt`, Guide | Java, unchanged |
| XML layouts for the terminal | Replaced |
| Launcher-specific code (drawer, app menus, fake launcher) | Removed |
