# HG2Gui — The Hitchhiker's Terminal to the Galaxy

**HG2Gui** is a touch-optimised terminal designed after Douglas Adams' The Hitchhiker's Guide to the Galaxy — every command, subcommand and argument is an animated pill thinger you tap, with the goal being to use a real shell without a keyboard. 

Inspired by the *Hitchhiker's Guide to the Galaxy* (2005) aesthetic and built on Termux.

## Features

*   **Point-and-click commands.** The suggestion tree is the input method. Tap a category, tap
    a command, tap an argument, press Run. The keyboard is optional.
*   **A real shell.** Shell commands run in a long-lived `/system/bin/sh` process, so `cd`
    sticks, exported variables survive, and the working directory in the header is the one the
    shell is actually in. Android's shell is toybox — expect `ls`, `ps`, `getprop`, not `apt`.
*   **Linux-like CLI.** Built-in commands for apps, files and system settings, dispatched ahead
    of the shell so `apps` means the app list rather than whatever is on `PATH`.
*   **Sessions.** Named terminal sessions, each with its own scrollback and working directory.
*   **Aliases.** Custom aliases for frequent commands.
*   **RSS reader**, **`tuixt`** (an in-terminal text editor), and **themes**.

## Interface

The screen is, top to bottom: session tabs, the working directory, the command line with a Run
capsule, modifier keys (`ctrl` `alt` `esc` `tab` `↑` `↓`), and the command tree. Tapping a
category sends the stack off the left edge, drops that pill to the bottom of the screen, and
cascades its children upward from it. Output arrives in a record tile, not a scrollback wall.

The visual language matches [Azphalt](https://azphalt.org) — a yellow page, no borders, no shadows,
no icons; a capsule is the only primitive.

## Project structure

Kotlin and Jetpack Compose Multiplatform for the UI; the command engine is still Java.

*   `app/src/main/java/com/hereliesaz/hg2gui/`
    *   `TerminalActivity.kt` — the entry point.
    *   `terminal/` — execution. `ShellSession.java` (the long-lived shell, framed into
        commands by a sentinel that reports exit status and working directory),
        `TerminalEngine.kt` (routes a line to a built-in or to the shell, and collects the
        output each one produces).
    *   `ui/` — Compose UI. `TerminalScreen.kt` (the terminal), `Theme.kt` (Azphalt tokens).
    *   `ui/menu/` — `PillMenu.kt` (the suggestion tree and its choreography),
        `CommandTree.kt` (the tree, built from the live `CommandGroup`).
    *   `commands/` — implementation of all built-in commands.
    *   `managers/` — suggestions, apps, files, system integration.
    *   `tuils/` — utilities.
*   `app/src/main/res/` — resources. Jost lives in `res/font/`.

## Build

JDK 21, Gradle 8.13, AGP 8.13, Kotlin 2.2.20. `compileSdk`/`targetSdk` 37, `minSdk` 24.

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

## License

GPLv3, inherited from T-UI.
