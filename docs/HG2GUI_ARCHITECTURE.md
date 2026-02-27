# HG2Gui Android Architecture

## Overview
HG2Gui (Android) is a specialized terminal emulator designed to replace the standard Android home screen. Unlike traditional launchers that use grids and widgets, HG2Gui presents a text-based interface inspired by *The Hitchhiker's Guide to the Galaxy*.

## Core Components

### 1. `LauncherActivity` (The Terminal Window)
*   **Role**: The main entry point and UI container.
*   **Responsibility**:
    *   Initializes the application environment.
    *   Manages the Android Activity lifecycle.
    *   Hosting the `UIManager` (View) and `MainManager` (Controller).
    *   Handling global events like Back presses and Context Menus.

### 2. `MainManager` (The Kernel)
*   **Role**: The central logic controller.
*   **Responsibility**:
    *   **Command Parsing**: Intercepts user input from the terminal.
    *   **Execution**: routes commands to the appropriate handler (Shell, Internal Command, or Alias).
    *   **Context Management**: Integrates with `SystemContext` to handle OS-switching logic (`ubuntu`, `macos`, `windows`).

### 3. `SystemContext` (The Simulation)
*   **Role**: Maintains the state of the simulated operating system.
*   **Responsibility**:
    *   Stores the current OS type (Ubuntu, MacOS, Windows).
    *   Affects which commands are available or how they behave.
    *   *Note*: This is a key divergence from the original T-UI, allowing for the "Context-Aware Shell" feature.

### 4. `UIManager` & `TerminalManager`
*   **Role**: The Presentation Layer.
*   **Responsibility**:
    *   **TerminalManager**: Manages the `EditText` (Input) and `TextView` (Output/History).
    *   **Suggestions**: Displays the floating command menu.
    *   **Theming**: Applies the "wonky" retro-futuristic styling (colors, fonts).

## Data Flow
1.  **Input**: User types `switchos macos`.
2.  **Routing**: `LauncherActivity` -> `UIManager` -> `MainManager`.
3.  **Parsing**: `MainManager` identifies `switchos` as a registered command.
4.  **Execution**: `switchos.java` class is instantiated.
5.  **State Change**: `SystemContext` updates OS to `MACOS`.
6.  **Feedback**: Success message returned to `TerminalManager` for display.

## Differences from T-UI
*   **Stripped Features**: Traditional app drawer and complex widget support have been minimized/removed to focus on the text stream.
*   **Added Features**: `SystemContext`, `switchos`, and specific "Guide" styling logic.
