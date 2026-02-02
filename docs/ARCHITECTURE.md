# Architecture Documentation

## Overview
This application is an Android terminal emulator designed with a "retro-futuristic" UI and a "point-and-click" command interaction model. It decouples from traditional launcher paradigms to focus on a streamlined, context-aware command line experience.

## Key Components

### 1. `LauncherActivity`
The main entry point of the application. It initializes the UI, manages permissions, and handles the lifecycle of the terminal environment. It sets up the `UIManager` and `MainManager`.

### 2. `MainManager`
The core logic coordinator. It receives command inputs and routes them to appropriate "triggers" (e.g., `ShellCommandTrigger` for executing system commands, `TuiCommandTrigger` for internal app commands). It also manages background services and integrations (Music, Contacts, etc.).

### 3. `TerminalManager`
Manages the `EditText` (input) and `TextView` (output) views. It handles user input events, command history navigation, and text formatting (colors, spans). It delegates execution to the `MainManager` via an interface.

### 4. `SuggestionsManager`
Responsible for the "point-and-click" interface. It dynamically generates suggestions based on user input.
- **Key Class:** `CommandMenu` defines a tree structure of commands (e.g., `git` -> `status`/`add`).
- **Integration:** `SuggestionsManager` traverses this tree matching the user's input and displays available next steps as clickable buttons.

### 5. `SystemContext`
A singleton that maintains the current "simulated" OS context (Ubuntu, MacOS, Windows). This affects which commands are suggested in the `CommandMenu`.

## Data Flow
1.  **User Input:** User types or clicks a suggestion in `TerminalManager`.
2.  **Suggestion Generation:** `SuggestionsManager` queries `CommandMenu` (using `SystemContext`) to update the suggestion bar.
3.  **Command Execution:** On Enter, `TerminalManager` sends the command to `MainManager`.
4.  **Trigger Processing:** `MainManager` checks triggers. `switch-os` updates `SystemContext`. Other commands are executed via Shell or internal logic.
5.  **Output:** Results are sent back to `TerminalManager` to append to the console view.

## Design Principles
-   **No Launcher:** The app is purely a terminal utility, not a home screen replacement.
-   **Context Aware:** The UI adapts to the simulated environment.
-   **Visuals:** Dark mode (`#111111`) with high-contrast, neon-like accent colors (`#A4F644`, `#44F6F6`) and a scanline overlay effect.
