# HG2Gui Android Architecture

## Overview
HG2Gui is a touch-optimised terminal emulator for Android. It is **not a launcher** — the
project began as a fork of T-UI, a home-screen replacement, and has been decoupled from it.
There is no `category.HOME` filter, no launcher lifecycle behaviour, and no app drawer.

The UI is Jetpack Compose (Kotlin). The command engine below it is the original Java.

## Core Components

### 1. `TerminalActivity` (Kotlin)
*   **Role**: The entry point and Compose host.
*   **Responsibility**:
    *   Constructs `MainManager` and hands its `CommandRepository` to the UI.
    *   Normal activity lifecycle: present in recents, keeps state, `singleTop`.
    *   Nothing else. All presentation lives in composables.

Replaces `LauncherActivity` as the entry point. Those launcher-only flags
(`clearTaskOnLaunch`, `excludeFromRecents`, `stateNotNeeded`, `taskAffinity=""`,
`launchMode="singleTask"`) are gone.

`LauncherActivity` has been removed. `MusicService` now targets `TerminalActivity` from its
playback notification, and the runtime permission request codes the command classes use live in
`PermissionCodes`.

### 2. `TerminalScreen` (Compose)
*   **Role**: The whole terminal surface.
*   **Responsibility**: Session tabs, working directory, the command line and its Run capsule,
    modifier keys, the output record tile, and the `PillMenu`. It is a function of the command
    tree plus one `onRun` callback — it holds no reference to a manager.

### 3. `PillMenu` (Compose)
*   **Role**: The suggestion tree — the app's primary input method.
*   **Responsibility**: The tree's state machine (browsing, leaving, open) and its motion.
    Each pill is an `Animatable`; the choreography is specified in [DESIGN.md](DESIGN.md).

### 4. `MainManager` (Java, the kernel)
*   **Role**: The central logic controller. Unchanged by the UI migration.
*   **Responsibility**: Command parsing; routing to the appropriate handler (internal command,
    app or alias); `SystemContext` and the OS-switching logic.
*   **Note**: it returns nothing. Commands broadcast their output as
    `PrivateIOReceiver.ACTION_OUTPUT`, so the caller listens rather than reads.

### 5. `TerminalEngine` (Kotlin) and `ShellSession` (Kotlin, `expect`/`actual`)
*   **Role**: Execution, and the seam between the two worlds.
*   **Responsibility**: `TerminalEngine` routes a line — built-in verbs to `MainManager`
    (captured, including a `StreamableCommand`'s incremental output), everything else to
    `ShellSession` — and captures the broadcast output of the former. `ShellSession` picks the
    best of three shell tiers: a bundled static `zsh`, a real Termux bootstrap (`DistroManager`)
    if installed, or bare `/system/bin/sh` as the last resort; whichever wins stays alive for the
    session and frames commands with a sentinel that reports exit status and working directory,
    so `cd` persists. It reads output as a raw buffer rather than line-by-line, since a real
    prompt never prints its own trailing newline while it waits for input — an idle gap with an
    unterminated tail is treated as a live prompt and surfaced through a callback instead of
    blocking forever.
*   **Limit**: no pty. Full-screen, cursor-addressing programs are out of scope; the UI renders
    discrete records, not a scrollback with a cursor in it. Autosuggestion, "did you mean", and
    alias hints are therefore implemented natively in Kotlin (`ShellAliases.kt`) as ordinary
    pills rather than as a shell-side line editor plugin — there's no live PTY for one to attach
    to.

### 6. `SystemContext` (Java, the simulation)
*   **Role**: The state of the simulated operating system (Ubuntu, macOS, Windows).
*   **Responsibility**: Which commands are available and how they behave. Wiring `switchos` to
    swap the visible suggestion tree is not done yet.

### 7. `CommandTree` (Kotlin, `androidMain`)
*   **Role**: Turns the live `CommandGroup` and the shell's own PATH into the menu.
*   **Responsibility**: Legacy built-ins group into System / Apps & nav / Features and get
    argument hints from a static map, per [COMMANDS.md](COMMANDS.md). Real shell binaries are
    discovered live from the Termux bootstrap's `bin/` (never Android's own `/system/bin` —
    Termux itself never depends on that either), matched back to the package that owns each one
    via dpkg's own bookkeeping (`DpkgCatalog.binariesByPackage`), and grouped into one root
    category per a hand-curated package → category map in `CommandTree.kt` itself — Termux's own
    packages carry no Debian Section field to read a category from directly (verified against a
    real bootstrap: none of its 82 base packages have one), so there is no live source of truth
    for this part, only for which package owns a binary. Within a category, binaries sharing a
    hyphenated prefix (`apt-get`, `apt-key`, `apt-mark`) nest under one host node instead of
    appearing as unrelated flat entries. Before a bootstrap exists, Shell offers exactly one
    pill: `bootstrap`. This is the seam where a command's own completion logic should eventually
    be read instead of a static map, for the legacy built-ins specifically.
*   `FileBrowser` (also `androidMain`) supplies the file-argument case: a `file…` node whose
    children are resolved lazily from a real directory listing (`MenuNode.resolveChildren`),
    attached to `open`/`tuixt` and to every discovered shell binary.

## Data Flow
1.  **Input**: The user taps `wifi`, then `status`. No text is typed. Since `status` leaves no
    further parameters, it runs immediately on that tap.
2.  **Assembly**: `TerminalScreen` holds the tokens `["wifi", "status"]`.
3.  **Execution**: Run calls `onRun("wifi status")` → `TerminalEngine.run`, off the main thread.
4.  **Routing**: `wifi` is a built-in, so `MainManager` runs it while the engine captures the
    output it broadcasts. An unrecognised verb would have gone to the shell instead, where a
    command that stalls waiting on stdin (a real prompt) suspends the caller until the UI
    supplies an answer rather than blocking the read loop forever.
5.  **Feedback**: The collected string becomes screen state and renders in a record tile.

## Differences from T-UI
*   **Removed**: The launcher identity — `LauncherActivity`, the launcher lifecycle flags, the
    second app icon that `TentacleActivity` carried, `FakeLauncherActivity`'s launcher-chooser
    trick, and the Mitosis / Snake / Magnet / Origami menus along with the `menu` and `surface`
    commands that launched them.
*   **Added**: a real shell session backed by an actual Termux bootstrap; the pill menu, with a
    graphical file picker and interactive-prompt handling; sessions; Compose.
