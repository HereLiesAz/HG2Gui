# HG2Gui Android Architecture

This is the Android-focused companion to [ARCHITECTURE.md](ARCHITECTURE.md).

## Application boundary

HG2Gui (`com.hereliesaz.hg2gui`) runs a Termux-derived userspace inside its own Android application sandbox and layers Android-specific execution policy, package compatibility, shell/TUI interpretation, native UI surfaces, trusted API transport, and explicit privilege brokerage around that runtime.

Current platform configuration is defined by Gradle; at this documentation pass it includes `compileSdk`/`targetSdk` 37, `minSdk` 24, JDK/JVM 21, and NDK `29.0.14206865`.

## Modules

### `:composeApp`

Android entry points and platform transport:

- `TerminalActivity`
- `EditorActivity`
- `IsolationAuditActivity`
- API receiver/picker/dialog/authority activities
- MCP foreground service
- manifest/resources
- build/signing/native-entry generation

### `:shared`

Most product/runtime implementation:

- Compose UI/state
- `TerminalEngine` and `ShellSession`
- shell presentation and completion adapters
- Adaptive TUI semantic/parser/interaction code
- bootstrap/runtime management
- package downloader/transaction engine
- package execution backend selection
- installed-package lifecycle/isolation/authority
- Files/VFS, SSH, workflows, AI, Store, MCP helpers

### `:terminal-emulator`

Vendored terminal emulator/PTY implementation. It supplies live screen buffers, cursor state, alternate-screen state, text attributes, terminal mouse-tracking state, and terminal key encoding used by Adaptive TUI wrapping.

### `:termux-shared`

Vendored Termux-compatible Android utilities.

## TerminalActivity and sessions

Each terminal tab owns its own `TerminalEngine` and UI state. `TerminalActivity` owns top-level navigation, permissions, screen composition, and full-screen PTY handoff; execution policy remains in `TerminalEngine`.

Settings is vertically scrollable and exposes default-shell choice, PTY controls, environment/history, Isolation Audit, AI, and MCP/developer controls.

## ShellSession

`ShellSession` is the persistent process transport.

When the HG2Gui runtime is available, new sessions use the configured installed shell (Bash/Zsh/Fish) under HG2Gui's Termux-derived environment. A bare Android system shell remains only the fallback when the runtime is unavailable.

Shell commands are framed so HG2Gui can recover exit status and resulting working directory while preserving persistent shell state.

## Shell presentation

`ShellPresentation` and `ShellAdapterRegistry` interpret the currently running shell/prompt separately from execution policy.

They can recognize Bash/Zsh/Fish plus presentation frameworks such as Oh My Zsh, Powerlevel10k, Starship, and Pure, and project semantic state including cwd, Git status, exit code, and user/host where meaningful.

Raw shell output remains authoritative and available.

## Completion Bridge

Completion providers normalize into a shared semantic model. Android-side adapters can source candidates from shell completion behavior, files, installed packages, Git, SSH, services, and supported command-specific metadata.

Completion requests must not execute the partially composed command. Results belong to the current session and replace only the active partial token when selected.

## Adaptive TUI Wrapper

`FullScreenPtySession` exposes the live terminal emulator to `TuiTerminalAdapter`.

The adapter reads:

- alternate-screen state;
- visible rows/cursor;
- inverse/bold/underline/style state;
- terminal mouse-tracking state.

`TuiSemanticParser` converts that into a toolkit-neutral model for layered/modal menus, checkboxes/radios, tabs, scrolling lists, tables/results, prompts, confirmation, progress/status, and multiple panes.

`TuiInteractionController` sends terminal keys only while re-reading the emulator after generated navigation. If expected state movement cannot be observed, the wrapper stops and returns to RAW behavior rather than continuing blindly.

## TerminalEngine

`TerminalEngine` is the execution-policy router.

Broad order:

```text
bootstrap/download
  ↓
explicit authority
  ↓
package lifecycle
  ↓
package ownership / disabled guard
  ↓
HG2 package transactions / translated mutating apt
  ↓
Android-facing built-ins
  ↓
package execution backend selection
  ↓
configured persistent shell
```

The backend selector chooses `DIRECT_LINKER`, `PROOT_COMPAT`, or `PROOT_ISOLATED` for package-owned commands.

## Termux bootstrap and native execution

The runtime lives under HG2Gui's own private prefix rather than Termux's application path. Critical native executables therefore use APK-native delivery/mapping where Android execution policy requires it.

`BootstrapManifest` and generated native entry points must remain synchronized with the pinned bootstrap.

## Package transactions

`Hg2PackageManager` handles mutating package work that cannot safely be delegated unchanged to upstream apt/dpkg under a different Android application ID.

Current transaction behavior includes:

- mirror failover and architecture selection;
- signed `InRelease` verification;
- authenticated index/package SHA-256 verification;
- resumable/reusable downloads;
- archive relocation and text-prefix repair;
- `conffiles`/`md5sums` and symlink/control metadata repair;
- dependency planning including Pre-Depends, alternatives, Provides, Conflicts/Breaks/Replaces, and version-aware relations;
- HG2Gui dpkg database/install root;
- chrootless dpkg operation;
- interpreter-aware maintainer scripts through Android-safe native entry paths;
- triggers, alternatives, diversions, explicit purge phase;
- whole-plan rollback/recovery.

## Package inventories and lifecycle

`PackageLifecycleStore` reads manager-owned metadata for:

- dpkg/pkg
- pip
- pipx
- npm
- RubyGems

HG2Gui adds Disable/Enable, Reset, and Isolate/Release isolation above manager-native install/update/remove semantics.

Disabled binaries are blocked before normal shell execution.

## Package execution backends

`PackageExecutionBackend` inspects the actual executable/runtime case.

- `DIRECT_LINKER` keeps compatible package-owned executables on the normal HG2Gui path.
- `PROOT_COMPAT` provides narrowly-scoped compatibility without a private sandbox.
- `PROOT_ISOLATED` provides private-root execution for isolated packages.

Arbitrary runtime failure is not a signal to retry silently under PRoot.

## Isolation

`PackageIsolation` creates a private root under HG2Gui app storage with private HOME/XDG state and version-aware seeding.

The real HG2Gui home/prefix are not bind-mounted into the guest. Common privilege tools are removed from the guest prefix and common host `su` paths are masked.

Isolation fails closed if its backend cannot be established.

### Observability

HG2Gui combines private-root before/after metadata with best-effort same-UID `/proc` sampling to observe:

- process descendants;
- live file descriptors/read-write modes;
- TCP/TCP6/UDP/UDP6 endpoints;
- ADB/root-like privilege attempts;
- private-root created/modified/deleted paths.

This is not exhaustive kernel/syscall audit. Short-lived activity can escape sampling.

`IsolationAuditActivity` provides a dedicated native **Settings → Isolation audit** view for isolated-package telemetry and sandbox state.

## Execution authority

`ExecutionAuthority` keeps ADB/root separate from ordinary app execution.

ADB support uses an installed executable client and Android's normal pairing/connect authorization. Root support uses an executable `su` provider and remains subject to the device's root manager.

Elevated requests require foreground approval. Headless execution cannot elevate.

## External Android API

`Hg2ApiReceiver` is protected by the signature permission:

```text
com.hereliesaz.hg2gui.permission.API
```

Typed actions cover command execution, package operations, pickers with returned results, notifications/dialogs, clipboard, device information, share/open, lifecycle operations, and authority requests.

`Hg2ApiAuthorityActivity` brokers ADB/root requests in the foreground so an external integration cannot turn background access into ambient elevation.

## Native surfaces

Android UI is not constrained to one terminal representation.

Current dedicated surfaces include:

- Files/path picker;
- Guide reader with tappable command composition;
- Settings;
- Isolation Audit;
- Environment/history;
- AI/Store/MCP;
- Adaptive TUI Wrapper.

## Security boundaries

The important independent boundaries are:

1. Android application sandbox;
2. HG2Gui package lifecycle policy;
3. package execution backend selection;
4. private package isolation;
5. ADB shell authority;
6. root authority;
7. signature-protected external API transport.

Availability of a stronger capability at one layer does not automatically flow into another.

## Verification

CI verifies the compile/test/static-analysis gates represented in the workflow. Android-specific behavior can still fail on device because of ICU regex rules, linker/native execution policy, package runtime behavior, permissions, ADB/root, or PRoot.

A green CI run therefore does not close an explicitly on-device verification item by itself.

## Android invariants

1. Critical bootstrap native executables use Android-safe delivery.
2. Mutating apt operations do not bypass HG2Gui package transactions.
3. Compatibility fallback remains evidence-based.
4. Disabled package binaries are blocked before shell execution.
5. Isolated packages fail closed.
6. Isolated packages do not inherit ADB/root tools.
7. Elevated ADB/root execution is foreground-approved.
8. Headless callers cannot elevate.
9. Adaptive TUI wrapping preserves RAW fallback and verifies generated navigation.
10. Guide command taps compose rather than auto-run.
11. Native screens may be used whenever they fit the represented object better than terminal composition.