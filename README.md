# HG2Gui — The Hitchhiker's Terminal to the Galaxy

**HG2Gui** is a touch-optimised terminal designed after Douglas Adams' The Hitchhiker's Guide to the Galaxy — every command, subcommand and argument is an animated pill thinger you tap, with the goal being to use a real shell without a keyboard. 

Inspired by the *Hitchhiker's Guide to the Galaxy* (2005) aesthetic and built on Termux.

## Features

*   **Point-and-click commands.** The suggestion tree is the input method. Tap a category, tap
    a command, tap an argument. Picking the last parameter a command needs runs it immediately —
    no separate confirmation — or press **Run** yourself at any point. The keyboard is optional.
*   **A real Termux backend.** Shell commands run in a long-lived shell session backed by the
    actual Termux bootstrap — the same rootfs archive the official Termux app installs — giving
    real `bash`, `apt`/`pkg`, and real coreutils, not Android's own toybox. It installs itself
    automatically on first launch, exactly like Termux does, with progress visible in the buffer.
*   **The tree reflects what's actually installed.** Shell pills are discovered live from the
    bootstrap's own `bin/`, matched back to the package that owns each one via dpkg's own
    bookkeeping, and grouped into a category (Package management, Network, Development, …) —
    `pkg install python` and it shows up as a pill the next time a command finishes, no restart
    needed. Hyphenated command families (`apt-get`, `apt-key`, `apt-mark`, …) nest under one
    shared parent pill instead of cluttering the list as unrelated flat entries.
*   **A graphical file picker.** Any command that takes a file argument gets a `file…` pill that
    browses the real filesystem through the same pill stack — no separate screen, no typing a
    path by hand.
*   **Interactive prompts, answered graphically.** A command that stops mid-run to ask a
    yes/no question gets a dedicated **Answer** stack (`YES`/`NO`) instead of leaving you to
    type a blind reply; anything else falls back to the input field, whose Run button becomes
    Send.
*   **Kotlin-native suggestions.** Autosuggestion (from history), "did you mean" (after a failed
    command), and alias hints (`gs` for `git status`, and friends) surface as ordinary pills,
    implemented natively rather than as a shell plugin — there's no live PTY for a shell's own
    line editor to attach to.
*   **A fixed set of built-ins for what Android exposes no shell path to.** System toggles
    (wifi, bluetooth, airplane mode, flashlight, volume, brightness), `call`/`contacts`, a
    sandboxed `vfs` filesystem, `calc`, and `edit` — a small Compose text editor for files a
    real terminal editor can't render without a pty. Everything else (`apps`, `alias`, sharing
    files, and so on) is a real shell binary now, not a reimplementation of one.
*   **Sessions.** Named terminal sessions, each with its own scrollback and working directory.
*   **Aliases.** Native shell aliases plus `ShellAliases.kt`'s own suggestion pills — no
    duplicate alias system layered on top of the real shell's.
*   **A real file manager.** Search, sort, multi-select batch actions, in-place rename, an
    automatic media grid, and a storage-by-type breakdown over the `vfs` sandbox — folders are
    capsules that expand in place, siblings squishing into thin coloured rods beside them.
*   **The Guide.** A chaptered glossary of real commands paired with invented, Hitchhiker's-
    Guide-style definitions, reachable from the command picker — reading material, not another
    way to run something.

## Interface

The screen is, top to bottom: session tabs, the working directory, the command line with a Run
capsule, modifier keys (`ctrl` `alt` `esc` `tab` `↑` `↓`), and the command tree. Tapping a
category sends the stack off the left edge, drops that pill to the bottom of the screen, and
cascades its children upward from it. Output arrives in a record tile, not a scrollback wall.

## Project structure

Kotlin Multiplatform (Compose) for the UI and execution layer. There is no separate command
framework: the ten built-ins are a fixed dispatch table (`terminal/Builtins.kt`), not a
reflection-discovered plugin system.

*   `composeApp/src/commonMain/kotlin/com/hereliesaz/hg2gui/`
    *   `terminal/ShellSession.kt` — the `expect` shell-session contract, plus
        `ShellAliases.kt` — Kotlin-native alias expansion, history autosuggestion and "did you
        mean", implemented here rather than as a shell plugin because there's no live PTY for a
        real shell line editor to attach to.
    *   `util/CalculationEngine.kt` — the expression parser behind `calc`.
    *   `ui/` — Compose UI. `TerminalScreen.kt` (the terminal), `SessionUiState.kt` (per-session
        state, including the pending-prompt hand-off for interactive commands), `ui/editor/` (the
        `edit` command's text editor screen), `ui/files/` (the `vfs` sandbox's file manager —
        `FilesScreen.kt`'s nested-accordion browser, `FolderPicker.kt`'s tile/rod/panel Move/Copy
        destination picker, `StorageScreen.kt`), `ui/guide/` (`CommandGuideScreen.kt`'s command
        picker, nesting `GuideReaderScreen.kt` and its content in `GuideContent.kt`).
    *   `ui/menu/PillMenu.kt` — the suggestion tree and its choreography. `MenuNode` supports
        lazily-resolved children (`resolveChildren`) for live data, like a directory listing.
*   `composeApp/src/androidMain/kotlin/com/hereliesaz/hg2gui/`
    *   `TerminalActivity.kt` — the entry point. `EditorActivity.kt` — hosts the `edit` screen,
        and is also the target of another app's VIEW/EDIT intent on a text file.
    *   `terminal/` — execution. `ShellSession.kt` (the `actual` implementation: a long-lived
        shell framed by a sentinel that reports exit status and working directory, with
        idle-based detection of a stalled interactive prompt), `TerminalEngine.kt` (routes a
        line to a built-in, the bootstrap installer, or the shell), `Builtins.kt` (the ten
        built-ins: system toggles, `call`/`contacts`, `vfs`, `calc`, `edit`), `DistroManager.kt`
        (downloads and extracts the real Termux bootstrap), `DpkgCatalog.kt` (reads dpkg's own
        bookkeeping for which package owns a given binary — Termux's packages carry no Debian
        Section field to read a category from directly, so `CommandTree`'s own hand-curated map
        turns package into category).
    *   `ui/menu/CommandTree.kt` — builds the tree: the ten built-ins into System / Apps & nav /
        Features, real PATH binaries by category (Package management, Network, Development, …)
        with hyphenated command families (`apt-get`/`apt-key`/…) nested under their shared
        parent.
    *   `ui/menu/FileBrowser.kt` — the in-stack graphical file picker.
    *   `managers/` — `ContactManager.kt` (contacts, for `call`/`contacts`), `VfsManager.kt` (the
        sandboxed filesystem `vfs` and the Files screen operate on), `flashlight/` (the torch
        implementation behind `flash`).
    *   `util/` — the handful of shared helpers (logging, crash reporting, the interactive-shell
        wrapper, `GenericFileProvider`) still in use.
*   `composeApp/src/androidMain/res/` — resources. Jost lives in `res/font/`.

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
