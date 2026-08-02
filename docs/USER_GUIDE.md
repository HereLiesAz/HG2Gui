# User Guide

## Getting Started
HG2Gui is a terminal app. Open it like any other app — it does not take over your home screen.
It gives you a real command line that you can drive without typing.

## Interface
From the top:

-   **Session tabs:** `main`, `tuixt`, `rss` and any you add with `+`. Each session keeps its
    own scrollback, history and working directory.
-   **Working directory:** the user and path the next command will run in.
-   **Command line:** the command you are assembling, as pills, and the **Run** capsule.
-   **Modifier keys:** `ctrl` `alt` `esc` `tab` `↑` `↓` — the keys a terminal needs that a
    phone keyboard does not have.
-   **Command tree:** the suggestion tree. This is the input method.

## Building a command by tapping
1.  Tap a category — `System`, `Apps & nav` or `Features`. The stack slides away and that pill
    drops to the bottom of the screen; it is now the host.
2.  Its commands cascade upward from it. Tap one.
3.  Its arguments cascade upward in turn. Tap one.
4.  Press **Run**. Output appears in a tile above the command line.

Tap the host pill at the bottom to go back a level.

## Typing instead
The keyboard is not forced open. Tap the command line to type; suggestions still filter as you
go, so you can type `git` and tap `commit` rather than spelling it out.

## Basic Commands
-   `help`: list available commands.
-   `clear`: clear the session.
-   `status` / `battery`: battery and system status.
-   `apps -ls`: list installed applications.
-   `switch-os <os>`: switch the simulated environment.
    -   `switch-os ubuntu` (default), `switch-os macos`, `switch-os windows`
    -   Changing the OS changes the tree: `apt` becomes `brew` becomes `winget`.

## Advanced Usage
-   **Shell:** standard Android shell commands (`ls`, `cd`, `cat`) work as expected.
-   **Root:** on a rooted device, `su` runs privileged commands.
-   **History:** the `↑` and `↓` modifier keys walk the current session's history.
-   **Editor:** `tuixt <file>` opens the in-terminal editor. It also registers as a text file
    handler, so other apps can open files in it.
