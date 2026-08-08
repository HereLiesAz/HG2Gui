# HG2Gui Android Architecture

## Overview
HG2Gui is a touch-optimised terminal emulator for Android. It is **not a launcher** — there is
no `category.HOME` filter, no launcher lifecycle behaviour, and no app drawer.

The UI, execution layer, and the ten built-in commands are all Kotlin. There is no separate
reflection-based command engine underneath it — built-ins are a fixed dispatch table.

## Core Components

### 1. `TerminalActivity` (Kotlin)
*   **Role**: The entry point and Compose host.
*   **Responsibility**:
    *   Builds the first `TerminalEngine`/`ShellSession` pair, and runs the bootstrap installer
        automatically if the Termux prefix isn't there yet.
    *   Normal activity lifecycle: present in recents, keeps state, `singleTop`.
    *   Persists `fullscreen`/`font_scale_percent` via plain `SharedPreferences`.
    *   Nothing else. All presentation lives in composables.

### 2. `TerminalScreen` (Compose)
*   **Role**: The whole terminal surface.
*   **Responsibility**: Session tabs, working directory, the command line and its Run capsule,
    modifier keys, the output record tile, and the `PillMenu`. It is a function of the command
    tree plus one `onRun` callback — it holds no reference to a manager.

### 3. `PillMenu` (Compose)
*   **Role**: The suggestion tree — the app's primary input method.
*   **Responsibility**: The tree's state machine (browsing, leaving, open) and its motion.
    Each pill is an `Animatable`; the choreography is specified in [DESIGN.md](DESIGN.md).

### 4. `Builtins` (Kotlin, the ten commands the shell can't provide)
*   **Role**: The fixed dispatch table for the ten built-in commands.
*   **Responsibility**: `wifi`, `bluetooth`, `airplane`, `flash`, `volume`, `brightness` (system
    toggles Android exposes no shell path to), `call`/`contacts` (via `ContactManager`), `vfs`
    (the sandboxed filesystem), `calc` (via `util/CalculationEngine.kt`), `edit` (opens
    `EditorActivity`). A plain `fun run(context, line): String` — no per-command class, no
    reflection, no interface to implement to add one.

### 5. `TerminalEngine` (Kotlin) and `ShellSession` (Kotlin, `expect`/`actual`)
*   **Role**: Execution, and the seam between the two worlds.
*   **Responsibility**: `TerminalEngine` routes a line — `bootstrap` streams from
    `DistroManager`, a `Builtins.NAMES` verb goes to `Builtins.run` for a single synchronous
    result, everything else to `ShellSession`. `ShellSession` picks the best of three shell
    tiers: a bundled static `zsh`, a real Termux bootstrap (`DistroManager`) if installed, or
    bare `/system/bin/sh` as the last resort; whichever wins stays alive for the session and
    frames commands with a sentinel that reports exit status and working directory, so `cd`
    persists. It reads output as a raw buffer rather than line-by-line, since a real prompt
    never prints its own trailing newline while it waits for input — an idle gap with an
    unterminated tail is treated as a live prompt and surfaced through a callback instead of
    blocking forever.
*   **Limit**: no pty. Full-screen, cursor-addressing programs are out of scope; the UI renders
    discrete records, not a scrollback with a cursor in it. That's why `edit` is its own Compose
    screen rather than an attempt to run `nano` through this. Autosuggestion, "did you mean", and
    alias hints are implemented natively in Kotlin (`ShellAliases.kt`) as ordinary pills rather
    than as a shell-side line editor plugin — there's no live PTY for one to attach to.

### 6. `CommandTree` (Kotlin, `androidMain`)
*   **Role**: Turns the fixed `Builtins` list and the shell's own PATH into the menu.
*   **Responsibility**: The ten built-ins group into System / Apps & nav / Features from a fixed
    list, with argument hints from a static map, per [COMMANDS.md](COMMANDS.md). Real shell
    binaries are discovered live from the Termux bootstrap's `bin/` (never Android's own
    `/system/bin` — Termux itself never depends on that either), matched back to the package that
    owns each one via dpkg's own bookkeeping (`DpkgCatalog.binariesByPackage`), and grouped into
    one root category per a hand-curated package → category map in `CommandTree.kt` itself —
    Termux's own packages carry no Debian Section field to read a category from directly
    (verified against a real bootstrap: none of its 82 base packages have one), so there is no
    live source of truth for this part, only for which package owns a binary. Within a category,
    binaries sharing a hyphenated prefix (`apt-get`, `apt-key`, `apt-mark`) nest under one host
    node instead of appearing as unrelated flat entries. Before a bootstrap exists, Shell offers
    exactly one pill: `bootstrap`.
*   `FileBrowser` (also `androidMain`) supplies the file-argument case: a `file…` node whose
    children are resolved lazily from a real directory listing (`MenuNode.resolveChildren`),
    attached to `edit` and to every discovered shell binary.

## Data Flow
1.  **Input**: The user taps `wifi`. No text is typed. Since `wifi` leaves no further
    parameters, it runs immediately on that tap.
2.  **Assembly**: `TerminalScreen` holds the token `["wifi"]`.
3.  **Execution**: Run calls `onRun("wifi")` → `TerminalEngine.run`, off the main thread.
4.  **Routing**: `wifi` is a built-in, so `Builtins.run` handles it directly and returns a
    result string. An unrecognised verb would have gone to the shell instead, where a command
    that stalls waiting on stdin (a real prompt) suspends the caller until the UI supplies an
    answer rather than blocking the read loop forever.
5.  **Feedback**: The result becomes screen state and renders in a record tile.
