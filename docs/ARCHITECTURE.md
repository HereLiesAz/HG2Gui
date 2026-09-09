# Architecture

HG2Gui is an Android command-line environment built around a real Termux-derived runtime and multiple native interaction surfaces. The shell remains authoritative for shell behavior; HG2Gui supplies Android-specific execution policy, package compatibility, structured input, shell/TUI interpretation, and purpose-built native screens.

The application is Kotlin/Compose. `:composeApp` contains Android entry points and platform transport. Most reusable UI and execution logic lives in `:shared`. `:terminal-emulator` contains the vendored terminal emulator/PTY layer.

## 1. Session and execution model

Each terminal tab owns a persistent `TerminalEngine`, which owns a persistent `ShellSession`. Shell state and working directory survive between ordinary commands.

`TerminalEngine` is the policy seam in front of that shell. Its broad dispatch order is:

1. bootstrap/download controls;
2. explicit execution authority;
3. package lifecycle controls;
4. package ownership and disabled-package blocking;
5. HG2Gui package transactions and translated mutating apt operations;
6. Android-facing built-ins;
7. package-owned backend selection;
8. configured persistent shell execution.

Package policy, compatibility, isolation, and authority are therefore not implemented as aliases in a user's shell configuration.

### Headless execution

`runToCompletion()` supports normal non-interactive work but never grants ambient ADB/root authority. Authority status may be queried; elevated execution requires foreground approval.

## 2. Termux-derived runtime

`DistroManager.kt` installs a pinned Termux bootstrap into HG2Gui's app-private prefix, typically:

```text
/data/user/0/com.hereliesaz.hg2gui/files/usr
```

Android does not treat arbitrary native binaries copied into writable app storage the same way it treats APK-delivered native code. HG2Gui therefore maps required native executables through Android-legal native-library delivery where necessary. `BootstrapManifest.kt` and the packaged native entry points must remain synchronized with the bootstrap.

## 3. Shell Adapter

The Shell Adapter interprets ordinary interactive-shell presentation without pretending the theme itself owns HG2Gui's UI.

Supported shell families currently include Bash, Zsh, and Fish. `ShellPreference` persists the default shell used by new sessions when that shell is installed.

`ShellPresentation` / `ShellAdapterRegistry` recognize presentation frameworks including Oh My Zsh, Powerlevel10k, Starship, Pure, and custom prompts. They project semantic state such as:

- current working directory;
- git branch and dirty state;
- last exit status;
- user/host where meaningful;
- shell family and presentation framework.

The raw prompt remains available. Presentation parsing is conservative; HG2Gui does not require a theme to match a particular glyph layout.

## 4. Completion Bridge

Completion providers normalize into one shell-agnostic candidate model. Current sources include:

- Bash/Zsh/Fish completion adapters;
- filesystem candidates;
- installed package names;
- Git branches;
- SSH hosts;
- service candidates;
- supported command-specific protocols;
- declarative subsets of native shell completion definitions that can be consumed without sourcing arbitrary scripts.

Candidates are stored per session and projected into native selection surfaces. Selecting a completion replaces only the current partial token and does not execute the command.

## 5. Adaptive TUI Wrapper

Full-screen and alternate-screen terminal applications remain the source of truth. HG2Gui reads the live emulator and derives a toolkit-neutral `TuiSnapshot`.

The semantic model can represent:

- nested/modal menu layers;
- checkbox/radio choices;
- tabs;
- scrolling lists;
- tables and selectable results;
- prompts and confirmations;
- progress/status regions;
- multiple panes;
- mouse-aware state;
- selection conveyed by inverse, emphasis, underline, cursor, or recognized markers.

`TuiInteractionController` verifies generated navigation against subsequent terminal state. It does not blindly assume that sending N arrow keys reached item N. If the application cannot be reconciled, HG2Gui falls back to **RAW** terminal interaction.

Mouse-awareness is based on actual terminal mouse-tracking state, not text heuristics.

## 6. Package management

Top-level mutating `apt`/`apt-get` operations are translated into HG2Gui package transactions before upstream apt can bypass Android compatibility handling.

The package transaction layer includes:

- resumable/reusable downloads;
- repository mirror failover;
- architecture selection;
- signed `InRelease` verification;
- authenticated index SHA-256 verification;
- verified package downloads;
- relocation from Termux's hardcoded prefix;
- control metadata and symlink repair;
- dependency resolution including Pre-Depends, alternatives, Provides, Conflicts/Breaks/Replaces, and version-aware relations;
- dpkg database/install-root control and chrootless operation;
- interpreter-aware maintainer-script handling;
- Android-safe native execution trampolines for maintainer-script phases;
- triggers, alternatives and diversions;
- explicit `postrm purge` handling;
- whole-plan rollback/recovery.

Compatibility fixes are driven by concrete runtime cases. Arbitrary command failure is not treated as evidence that a command should silently be retried through PRoot.

## 7. Installed-package lifecycle

`PackageLifecycleStore` normalizes inventory from:

- dpkg/pkg;
- pip;
- pipx;
- npm;
- RubyGems.

The common model records manager, package name, version, runnable binaries, disabled state, and isolation state.

HG2Gui-owned lifecycle semantics include:

- **Disable/Enable** — retain installation while HG2Gui blocks/allows owned binaries;
- **Reset** — retain installed payload while deleting attributable runtime state;
- **Isolate/Release isolation** — select private-root execution policy independent of the package manager.

Manager-native operations such as update/remove/purge are delegated through the manager adapter where meaningful.

## 8. Package execution backends

`PackageExecutionBackend` chooses execution based on the actual owned executable/runtime case:

- `DIRECT_LINKER` — normal HG2Gui Termux execution through the Android-compatible direct/preload path;
- `PROOT_COMPAT` — compatibility execution without a private isolation root for specifically classified ELF/runtime cases;
- `PROOT_ISOLATED` — private-root package execution.

Dynamically linked ELF with an appropriate interpreter remains on the direct path. Compatibility routing is narrow and explicit.

If an isolated package cannot obtain its isolation backend, execution fails rather than falling back outside the sandbox.

## 9. Package isolation and observability

An isolated package receives a private filesystem root, private HOME, and private XDG cache/config/data/state/tmp directories. HG2Gui seeds the private root from the installed runtime and records the package version so stale roots can be discarded after update/reset.

The real HG2Gui prefix/home are not bind-mounted into the guest. Common privilege tools are removed from the guest prefix and common host `su` paths are masked.

### Runtime observations

HG2Gui combines before/after private-root metadata with best-effort same-UID `/proc` sampling. Current observations include:

- private-root created/modified/deleted paths;
- process tree and spawned descendants;
- live open file descriptors and observed read/write modes;
- TCP/TCP6/UDP/UDP6 socket endpoints when observable;
- privilege-tool attempts involving ADB/root-like tools.

This is not kernel audit or complete syscall tracing. Short-lived events can escape sampling, and documentation must not describe it as exhaustive interception.

The native **Settings → Isolation audit** screen exposes the latest persisted/observed isolated-package telemetry instead of requiring users to recover it from terminal output alone.

## 10. Execution authority

`ExecutionAuthority` keeps privilege separate from normal execution.

### App authority

Normal commands run with the app's ordinary authority.

### ADB shell

When an executable ADB client is installed, HG2Gui can expose devices/pair/connect/disconnect/shell operations. Android Wireless Debugging pairing/connect rules still apply. ADB shell execution is foreground-approved.

### Root

When an executable `su` provider is detected, HG2Gui can request root execution. The device's root manager remains authoritative. Root execution is foreground-approved.

### Security invariant

Normal commands, isolated packages, and headless callers do not inherit ADB/root merely because HG2Gui can access those backends.

## 11. External Android API

The signature-protected API uses:

```text
com.hereliesaz.hg2gui.permission.API
```

`Hg2ApiReceiver` exposes typed Android actions for:

- command execution;
- package operations;
- file/folder pickers with returned results;
- notifications;
- foreground dialogs;
- clipboard get/set;
- device information;
- share/open actions;
- package lifecycle actions;
- foreground-approved ADB/root requests.

The API is capability-oriented. Authority requests are brokered through dedicated foreground UI rather than silently becoming ordinary background command execution.

## 12. Guide as command-entry surface

The Guide is both documentation and an active command-composition surface. A command encountered while reading can be tapped and handed to the current terminal session for review. Reading does not force a mode switch and tapping a Guide command does not auto-run it.

## 13. Files and other native surfaces

HG2Gui is free to use the interaction model appropriate to the object being manipulated. Current dedicated surfaces include Files/path picking, Settings, isolation audit, history, environment, AI, Store, MCP, and Adaptive TUI projections.

The terminal composer remains important, but it is not the universal visual container for every feature.

## 14. Interactive input

When HG2Gui knows the domain, it can expose structured controls:

- files/directories → graphical picker;
- yes/no → confirmation dialog;
- finite choices → native selections;
- semantic shell completions → completion controls;
- TUI menus/prompts → adaptive native wrapper;
- genuinely open-ended data → text input.

An ordinary composed command still requires explicit **RUN**. Inputs answering an already-running process are sent to that process.

## 15. Android-facing built-ins

`Builtins.kt` contains explicit Android capability bridges for cases that do not honestly exist as normal runtime binaries. Those commands are implementation surfaces, not a rule that every Android capability must become a shell verb. Capability-oriented API/native surfaces can coexist with or supersede command exposure where appropriate.

## 16. Build and verification

Authoritative toolchain values live in Gradle/version files. At this documentation pass:

- JDK 21
- AGP 9.3.2
- Kotlin 2.4.10
- Compose Multiplatform 1.12.0
- `compileSdk` / `targetSdk` 37
- `minSdk` 24
- NDK `29.0.14206865`

CI verifies what its jobs actually compile/test/analyze. Real Android behavior that depends on Android ICU, linker policy, package execution, permissions, ADB, root, or PRoot still requires device verification. A green CI run is not equivalent to device-runtime proof.

## 17. Current invariants

1. Real shell/runtime behavior remains authoritative beneath HG2Gui's presentation and policy layers.
2. Ordinary command composition does not auto-run.
3. The Guide may compose commands directly from documentation.
4. Structured values can be selected when safely discoverable; open-ended values remain typeable.
5. Adaptive TUI wrapping must preserve a RAW escape path.
6. Generated TUI navigation must be reconciled with observed terminal state.
7. Package compatibility routing must be evidence-based, not arbitrary fallback.
8. Isolated packages never silently fall back outside isolation.
9. ADB/root are explicit brokered authorities, never ambient capabilities.
10. Headless execution cannot elevate through ADB/root authority.
11. External API authority requests require foreground approval.
12. Native screens are allowed whenever they fit the task better than command composition.

## 18. Documentation authority

`README.md`, this file, `HG2GUI_ARCHITECTURE.md`, `COMMANDS.md`, `USER_GUIDE.md`, `DESIGN.md`, `VISION.md`, `CONTRIBUTING.md`, and `TODO.md` are the live software/product documentation. Guide, visual, and motion source material under `docs/` remains authoritative for its own content domain but does not override current runtime behavior.