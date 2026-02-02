# User Guide

## Getting Started
Welcome to the Hitchhiker Terminal. This app provides a powerful command-line interface on your Android device with a touch-friendly, guided experience.

## Interface
-   **Input Area:** Located at the bottom. Tap to type commands.
-   **Suggestions:** Floating buttons appear above the input area. These are context-aware shortcuts. Tap them to auto-complete commands.
-   **Terminal Output:** The main area displays command results.

## Basic Commands
-   `help`: Displays a list of available commands.
-   `clear`: Clears the screen.
-   `switch-os <os>`: Switches the simulated environment.
    -   `switch-os ubuntu` (Default)
    -   `switch-os macos`
    -   `switch-os windows`
    -   *Note: Changing the OS affects the available suggestions in the menu (e.g., `apt` vs `brew`).*

## Touch Interaction
-   **Smart Suggestions:** As you type (e.g., `git`), the suggestion bar will show valid subcommands (`status`, `commit`, etc.). Click these to build complex commands without typing.
-   **History:** Use the Up/Down arrows (if enabled in settings) or swipe gestures to navigate command history.

## Advanced Usage
-   **Shell:** Standard Android shell commands (`ls`, `cd`, `cat`) work as expected.
-   **Root:** If your device is rooted, you can use `su` to execute privileged commands.
