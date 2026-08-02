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

`LauncherActivity` itself is still declared, unexported and iconless. `MusicService` targets it
from its playback notification, and the command classes still reference its permission request
codes. Retiring it means rehoming those first; until then it is dead weight, not the front door.

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

### 5. `TerminalEngine` (Kotlin) and `ShellSession` (Java)
*   **Role**: Execution, and the seam between the two worlds.
*   **Responsibility**: `TerminalEngine` routes a line — built-in verbs to `MainManager`,
    everything else to `ShellSession` — and captures the broadcast output of the former.
    `ShellSession` keeps one `/system/bin/sh` alive for the session and frames commands with a
    sentinel that reports exit status and working directory, so `cd` persists.
*   **Limit**: no pty. Full-screen, cursor-addressing programs are out of scope; the UI renders
    discrete records, not a scrollback with a cursor in it.

### 6. `SystemContext` (Java, the simulation)
*   **Role**: The state of the simulated operating system (Ubuntu, macOS, Windows).
*   **Responsibility**: Which commands are available and how they behave. Wiring `switchos` to
    swap the visible suggestion tree is not done yet.

### 7. `CommandTree` (Kotlin)
*   **Role**: Turns the live `CommandGroup` into the menu.
*   **Responsibility**: Groups commands into Shell / System / Apps & nav / Features per
    [COMMANDS.md](COMMANDS.md) and attaches argument hints. This is the seam where a command's
    own completion logic should eventually be read instead of a static map.

## Data Flow
1.  **Input**: The user taps `wifi`, then `status`. No text is typed.
2.  **Assembly**: `TerminalScreen` holds the tokens `["wifi", "status"]`.
3.  **Execution**: Run calls `onRun("wifi status")` → `TerminalEngine.run`, off the main thread.
4.  **Routing**: `wifi` is a built-in, so `MainManager` runs it while the engine captures the
    output it broadcasts. An unrecognised verb would have gone to the shell instead.
5.  **Feedback**: The collected string becomes screen state and renders in a record tile.

## Differences from T-UI
*   **Removed**: The launcher identity — the launcher lifecycle flags, and the second app icon
    that `TentacleActivity` carried.
*   **Still present, pending removal**: `FakeLauncherActivity` and the Mitosis / Snake / Magnet
    / Origami menus. The `menu` and `surface` commands still start them, so they cannot be
    deleted until those commands are.
*   **Added**: a real shell session; the pill menu; sessions; Compose.
