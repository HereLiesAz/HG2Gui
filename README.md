# HG2Gui — The Hitchhiker's Terminal to the Galaxy

**HG2Gui** is a touch-first Android command-line environment inspired by the visual language of *The Hitchhiker's Guide to the Galaxy*. It keeps a real Termux-derived runtime underneath while projecting command-line software into the Android interaction surface that best fits the task: command composition, Guide entries, Files, structured shell state, adaptive TUI wrappers, prompts, package management, and dedicated native views.

Typing remains available whenever the value is genuinely open-ended. When HG2Gui can discover valid values, it can expose those values directly instead.

## What it does

- **Touch-first command composition.** Commands, flags, package names, branches, hosts, services, and other discoverable values can be assembled through native controls. Normal command composition does not auto-run; **RUN** is explicit.
- **The Guide is an input surface.** Commands shown while reading Guide entries are tappable and can be composed into the active terminal session for review. Reading and command composition can interleave without leaving the Guide.
- **Completion Bridge.** Bash, Zsh, Fish, filesystem, installed-package, Git branch, SSH host, service, and supported command-specific completion sources normalize into one semantic completion model and feed native selection controls.
- **Shell Adapter.** Bash, Zsh, and Fish sessions expose structured prompt state such as cwd, git branch/dirty state, exit status, and user/host where meaningful. Oh My Zsh, Powerlevel10k, Starship, Pure, and similar systems remain presentation layers on the shell that actually owns the session.
- **Adaptive TUI Wrapper.** Alternate-screen terminal interfaces can be recognized from live terminal state and projected into native menus, modal layers, checkbox/radio choices, tabs, lists, tables, prompts, confirmation controls, progress/status regions, and multiple panes. The child program remains authoritative, and **RAW** remains available whenever HG2Gui cannot safely model the interaction.
- **Semantic TUI interaction.** Generated navigation is verified against screen evolution instead of blindly sending a fixed number of keys. When state cannot be reconciled, HG2Gui falls back to the raw terminal.
- **Real PTY support.** Full-screen and interactive terminal applications run through the vendored Termux terminal emulator/PTY path when PTY mode is enabled.
- **Live output follows itself.** Active result records auto-scroll as output arrives.
- **Real Termux-derived runtime.** HG2Gui bootstraps a pinned Termux userspace into its own private `$PREFIX`, with Android-safe native executable delivery where required.
- **Persistent shell selection.** New sessions can use installed Bash, Zsh, or Fish as their configured shell.
- **Android-safe package transactions.** Mutating package operations run through HG2Gui's transaction layer, including relocation, metadata repair, dependency planning, verified repository/index/package downloads, maintainer-script handling, triggers, alternatives/diversions, rollback, and Android-safe native execution support.
- **Unified package execution backends.** Package-owned binaries are classified for direct linker execution, PRoot compatibility execution where required by a concrete ELF/runtime case, or private-root isolated execution. Isolation never silently falls back outside its boundary.
- **Installed-package management.** **Packages** inventories Termux/dpkg, Python `pip`, Python `pipx`, Node `npm`, and RubyGems installations from manager-owned metadata.
- **Package lifecycle.** Installed packages can expose **Run**, **Update**, **Info**, **Remove**, **Purge** where meaningful, plus HG2Gui-owned **Enable/Disable**, **Reset**, and **Isolate/Release isolation**.
- **Package isolation.** Isolated packages run inside a private PRoot-backed root with private HOME/XDG state. HG2Gui removes/masks common privilege tools and observes private-root changes plus best-effort process, open-file, socket, and privilege-tool activity.
- **Native isolation audit.** Settings includes an Isolation Audit view for inspecting the latest observed activity from isolated packages.
- **Explicit execution authority.** ADB shell and root are separate foreground-approved authorities. They are never ambient capabilities of normal commands, isolated packages, or headless execution.
- **Capability-oriented Android API.** Signature-protected external integrations can request typed HG2Gui capabilities including command execution, package operations, pickers with returned results, notifications/dialogs, clipboard, device information, share/open actions, lifecycle operations, and foreground-approved authority requests.
- **Files and dedicated native surfaces.** File browsing, picking, package/audit management, settings, and other object-oriented tasks are free to use purpose-built native screens rather than being forced into the terminal composer.

## Package lifecycle

The owning package manager remains responsible for its native installation/update/removal semantics. HG2Gui adds lifecycle and execution policy above it:

```text
Packages
  ├─ Authority
  │   ├─ App
  │   ├─ ADB shell
  │   └─ Root
  ├─ Termux / pkg
  │   └─ <installed package>
  │       ├─ Run → <owned binaries>
  │       ├─ Disable / Enable
  │       ├─ Isolate / Release isolation
  │       ├─ Update
  │       ├─ Reset
  │       ├─ Info
  │       ├─ Remove
  │       └─ Purge
  ├─ Python / pip
  ├─ Python apps / pipx
  ├─ Node / npm
  └─ Ruby / gem
```

Not every package exposes a runnable binary, and not every manager supports the same native lifecycle operations. HG2Gui-owned Disable, Reset, and Isolate semantics remain independent of manager feature sets.

## Command routing

`TerminalEngine` is the execution-policy seam. In broad order it handles:

1. bootstrap/download controls;
2. explicit execution authority;
3. package lifecycle controls;
4. disabled-package blocking;
5. HG2Gui package transactions and translated mutating apt operations;
6. Android-facing built-ins;
7. package-owned backend selection (`DIRECT_LINKER`, `PROOT_COMPAT`, `PROOT_ISOLATED`);
8. the configured persistent shell for everything else.

Policy and compatibility decisions happen before ordinary shell execution; they are not aliases inside the user's shell.

## Package compatibility

HG2Gui does **not** assume a Termux `.deb` can simply be unpacked and executed unchanged under another Android application ID. The package/runtime layer includes support for:

- relocation of Termux's hardcoded prefix;
- control metadata and relocated symlink repair;
- dependency planning with Pre-Depends, alternatives, Provides, Conflicts/Breaks/Replaces, and version-aware relations;
- dpkg database/install-root control and chrootless operation;
- interpreter-aware maintainer-script execution through Android-safe native entry points;
- dpkg triggers, alternatives, diversions, and explicit `postrm purge` handling;
- whole-plan transaction recovery/rollback;
- mirror failover, architecture selection, signed `InRelease` verification, authenticated index SHA-256 verification, and verified package downloads;
- ELF inspection and narrowly-scoped PRoot compatibility for binaries that need it.

Compatibility work is driven by observed runtime cases. Arbitrary command failure is not treated as a reason to silently retry under PRoot.

## Input surfaces

HG2Gui does not require every interaction to look like the same control. Current command-entry and interaction surfaces include:

- terminal command composition;
- semantic completion selections;
- tappable commands inside the Guide;
- graphical file/folder picking;
- structured prompt dialogs/inputs;
- Adaptive TUI Wrapper projections;
- raw PTY terminal interaction;
- shell prompt/status projections;
- package, settings, audit, history, environment, AI, Store, Files, SSH, workflow, and MCP screens.

When a composed ordinary command is ready, **RUN** remains the explicit execution step. Interactive controls that answer an already-running process naturally send their response to that process.

## External API

The Android API receiver is protected by the signature permission:

`com.hereliesaz.hg2gui.permission.API`

It exposes typed actions instead of making an untrusted broadcast string synonymous with terminal execution. ADB/root requests are routed through a foreground approval activity; headless callers do not inherit elevation.

## Project structure

- `:composeApp` — Android application entry points, activities/services, API transport, resources, build/signing/packaging.
- `:shared` — Compose UI, terminal routing, shell/TUI/completion adapters, package management, lifecycle/isolation/authority, managers and integrations.
- `:terminal-emulator` — vendored terminal emulator and PTY support.
- `:termux-shared` — vendored Termux-compatible Android support library.

Key runtime files include:

- `terminal/TerminalEngine.kt` — dispatch and execution-policy boundary.
- `terminal/ShellSession.kt` — persistent shell transport.
- `terminal/ShellPresentation.kt` / `ShellAdapterRegistry.kt` — structured shell presentation.
- `terminal/CompletionBridge.kt` and completion providers — normalized semantic completion.
- `terminal/TuiSemanticModel.kt`, `TuiTerminalAdapter.kt`, `TuiInteractionController.kt` — Adaptive TUI semantics and verified interaction.
- `terminal/Hg2PackageManager.kt` — package transaction engine.
- `terminal/PackageExecutionBackend.kt` — direct/compatibility/isolation backend selection.
- `terminal/PackageLifecycleStore.kt` — installed-manager inventory and lifecycle state.
- `terminal/PackageIsolation.kt` — private-root isolation and runtime observation.
- `terminal/ExecutionAuthority.kt` — ADB/root authority backends.
- `api/Hg2ApiReceiver.kt` and API activities — signature-protected external capabilities.

## Build

Authoritative toolchain values live in Gradle/version files. At this documentation pass the repository uses:

- JDK 21
- AGP 9.3.2
- Kotlin 2.4.10
- Compose Multiplatform 1.12.0
- `compileSdk` / `targetSdk` 37
- `minSdk` 24
- NDK `29.0.14206865`

```bash
./gradlew assembleDebug
```

CI success proves compilation/tests/static gates represented in the workflow. Android runtime compatibility still requires real-device confirmation for cases that depend on Android linker, ICU, package execution, permissions, ADB, root, or PRoot behavior.

## Documentation

- [Vision](docs/VISION.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Android architecture](docs/HG2GUI_ARCHITECTURE.md)
- [Commands](docs/COMMANDS.md)
- [Design](docs/DESIGN.md)
- [User guide](docs/USER_GUIDE.md)
- [Contributing](docs/CONTRIBUTING.md)
- [Active TODO](docs/TODO.md)

The Guide manuscript and visual/motion source material under `docs/` are product source material in their own domains; runtime/API behavior is defined by the live software documentation and code.