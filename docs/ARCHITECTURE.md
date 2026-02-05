# Architecture

HG2Gui is an Android application designed as a terminal emulator launcher. It follows a modular architecture centered around the "Command" pattern and a "Manager" pattern for handling system resources.

## Core Components

### 1. LauncherActivity (`LauncherActivity.java`)
The entry point of the application.
*   **Role:** Initializes the application, manages Android permissions, handles lifecycle events (onCreate, onPause, onDestroy), and sets up the UI.
*   **Key Responsibilities:**
    *   Initializing `MainManager` and `UIManager`.
    *   Handling `Intent`s and `BroadcastReceiver`s for inter-process communication.
    *   Managing window flags (fullscreen, status bar colors).

### 2. Main Manager (`MainManager.java`)
The central coordinator of the application logic.
*   **Role:** Bridges the gap between the UI (`UIManager`) and the underlying logic (`managers`).
*   **Key Responsibilities:**
    *   Initializing all sub-managers (Apps, File, Contacts, etc.).
    *   Routing user input to the appropriate command or manager.
    *   Managing the `CommandRepository`.

### 3. UI Manager (`UIManager.java`)
The view controller for the terminal interface.
*   **Role:** Manages the `TerminalView` (or equivalent layout), handles text input/output, and manages UI-related settings (colors, fonts).
*   **Key Responsibilities:**
    *   Displaying the prompt and user input.
    *   Rendering output from commands (text, colors).
    *   Handling suggestions and autocomplete visualization.

## Package Structure (`com.hereliesaz.hg2gui`)

*   **`root`**: Contains Activities (`LauncherActivity`, `GuideActivity`, `PanicActivity`).
*   **`managers/`**: Contains logic for specific domains.
    *   `AppsManager`: Loading and launching installed apps.
    *   `FileManager`: File system operations.
    *   `TerminalManager`: Core terminal state.
    *   `SystemContext`: OS/Environment emulation context.
    *   `...`: Others (Contact, Time, Location, etc.).
*   **`commands/`**: Implements the command pattern.
    *   `CommandAbstraction`: Interface for all commands.
    *   `CommandRepository`: Index of available commands.
    *   `main/`: Core system commands.
    *   `tuixt/`: The built-in text editor.
*   **`tuils/`**: Utilities (likely "T-UI Utils").
    *   `Tuils.java`: General static helper methods.
    *   `interfaces/`: Common interfaces (`Inputable`, `Outputable`).

## Data Flow

1.  **Input:** User types text in `LauncherActivity` (handled by `UIManager`).
2.  **Processing:** `MainManager` receives the input.
3.  **Parsing:** The input is parsed to identify the command name and arguments.
4.  **Execution:**
    *   If it's a known command, the corresponding `Command` class in `commands/` is executed.
    *   If it matches an app name, `AppsManager` launches the app.
    *   If it's an alias, `AliasManager` expands it.
5.  **Output:** The result is sent back to `UIManager` via the `Outputable` interface to be displayed.
