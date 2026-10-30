# HG2Gui — The Hitchhiker's Terminal to the Galaxy

**HG2Gui** is a touch-first Android command-line environment inspired by the visual language of *The Hitchhiker's Guide to the Galaxy*. It keeps a real Termux-derived runtime underneath while projecting command-line software into the Android interaction surface that best fits the task: command composition, Guide entries, Files, structured shell state, adaptive TUI wrappers, prompts, package management, and dedicated native views.

Typing remains available whenever the value is genuinely open-ended. When HG2Gui can discover valid values, it can expose those values directly instead.

## What it does

- **Touch-first command composition.** Commands, flags, package names, branches, hosts, services, and other discoverable values can be assembled through native controls. Normal command composition does not auto-run; **RUN** is explicit.
- **The Guide is an input surface.** Commands shown while reading Guide entries are tappable. Tapping one appends it to the active session's input for review (it never runs by itself) and the Guide stays open, so reading and command composition interleave.
- **Completion Bridge.** Bash, Zsh, Fish, filesystem, installed-package, Git branch, SSH host, service, and supported command-specific completion sources normalize into one semantic completion model and feed native selection controls.
- **Shell Adapter.** In the ordinary command line, a status line under the tab strip shows the working directory, the Git branch (with a dot when there are uncommitted changes), a non-zero exit status, and user@host when known, refreshed after each command. Prompt frameworks (Oh My Zsh, Powerlevel10k, Starship, Pure, and similar) are detected for full-screen interactive shell sessions, where there is a prompt to read; they remain presentation layers on the shell that actually owns the session. Git branch names are offered as completion candidates after `git checkout`, `switch`, `branch`, `merge` and `rebase`; the status tag itself is display only.
- **Adaptive TUI Wrapper.** Alternate-screen terminal interfaces can be recognized from live terminal state and projected into native menus, modal layers, checkbox/radio choices, tabs, lists, tables, prompts, confirmation controls, progress/status regions, and multiple panes. The child program remains authoritative, and **RAW** remains available whenever HG2Gui cannot safely model the interaction.
- **Semantic TUI interaction.** Generated navigation is verified against screen evolution instead of blindly sending a fixed number of keys. When state cannot be reconciled, HG2Gui falls back to the raw terminal.
- **Real PTY support.** Full-screen and interactive terminal applications run through the vendored Termux terminal emulator/PTY path when PTY mode is enabled.
- **Live output follows itself.** Active result records keep their newest line in view as output arrives, but only while you are at the bottom: scroll up to read and it stops following, and a **LATEST** chip jumps back. Tap a result for **COPY / RE-RUN / SHARE / EXPAND**; long-press output to select part of it.
- **Real Termux-derived runtime.** HG2Gui bootstraps a pinned Termux userspace into its own private `$PREFIX`, with Android-safe native executable delivery where required.
- **Persistent shell selection.** New sessions can use installed Bash, Zsh, or Fish as their configured shell.
- **Android-safe package transactions.** `pkg` and mutating `apt`/`apt-get` commands run through HG2Gui's transaction layer, including relocation, metadata repair, dependency planning, verified repository/index/package downloads, maintainer-script handling, triggers, alternatives/diversions, rollback, and Android-safe native execution support. This covers `pkg`/`apt` typed as their own command or as one step of a `;` / `&&` / `||` list; a mutating one anywhere HG2Gui cannot route it (behind `sudo`/`env`, in a pipe, `bash -c`, `$( )`, a control structure) is refused instead of reaching the real apt. A direct `dpkg` call that would install, remove, purge, configure or unpack (`-i`, `-r`, `-P`, `--configure`, `--unpack`, `--force-*`, …) is refused with a pointer to `pkg install` / `pkg remove`; read-only `dpkg` queries (`-l`, `-L`, `-S`, `-s`, `--print-architecture`, …) still work. Other tools that edit dpkg's database (`dpkg-divert`, `dpkg-trigger`, `update-alternatives`) and a real `apt` run from inside a script are not covered.
- **Unified package execution backends.** Package-owned binaries are classified for direct linker execution, PRoot compatibility execution where required by a concrete ELF/runtime case, or private-root isolated execution. An isolated package that cannot get its isolation backend fails instead of running unisolated.
- **Installed-package management.** **Packages** inventories Termux/dpkg, Python `pip`, Python `pipx`, Node `npm`, and RubyGems installations from manager-owned metadata.
- **Package lifecycle.** Installed packages can expose **Run**, **Update**, **Info**, **Remove**, **Purge** where meaningful, plus HG2Gui-owned **Enable/Disable**, **Reset**, and **Isolate/Release isolation**. **Disable** is enforced twice: the command line is checked before it runs, and every shell HG2Gui starts has a directory of one-line refusal scripts first on `PATH` for each disabled binary, so a disabled tool is also refused when reached through `$tool`, `$(which tool)`, a shell function, an alias or a script. Calling the binary by its absolute path (`$PREFIX/bin/tool`) is not covered.
- **Package isolation.** Isolated packages run inside a private PRoot-backed root with private HOME/XDG state. HG2Gui removes/masks common privilege tools and observes private-root changes plus best-effort process, open-file, socket, and privilege-tool activity. This is state separation with best-effort observation, not a verified security sandbox: PRoot is a same-UID path translator, network access is not confined (an isolated launch needs a one-run `network` grant, see Capability policy), and the sampled observation is not tamper-proof. An isolated package runs only as its own command or one step of a `;` / `&&` / `||` list; it is refused in a pipe, `bash -c`, `$( )` or a control structure. Isolation is checked on the command line only: there are no `PATH` shims for isolated packages (they would break builds), so an isolated package's binary reached through a run-time command word (`$tool`), a function or a script runs outside its private root.
- **Native isolation audit.** Settings includes an Isolation Audit view for inspecting the latest observed activity from isolated packages.
- **Explicit execution authority.** HG2Gui's own routes to ADB shell and root (`hg2auth` in a terminal tab, and the external API's authority request, which shows the requesting app and the exact command) are separate and need a person's approval in the foreground; commands run headlessly through the API, MCP or installers cannot use them. Inside an isolated package `adb`, `su` and similar tools are masked or removed. HG2Gui does not intercept an `su` or `adb` binary you run yourself from the shell; it behaves as in any terminal (a root manager still asks).
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

The engine analyses the whole line first. An unterminated quote, `$(` or backtick is refused before it reaches the shell (exit 2). A disabled package's binary blocks the line wherever it can be statically seen, including behind `env`/`sudo`, in `bash -c` or `$( )`; a command word that is only known at run time (`$tool`) cannot be seen by the planner, but a disabled package's binary is still refused by its `PATH` shim (see Package lifecycle; absolute-path calls are not covered). Background callers (the external API, MCP, installers) carry their own policy: the engine itself refuses `bootstrap`, `vfs mount`, `hg2auth` (except `status`) and package `disable`/`isolate`/`update`/`reset`/`remove` and similar for them, with exit code 126, and asks a person before running `call`, `contacts`, `brightness`, `wifi`, `bluetooth`, `airplane`, `flash`, `edit` or `volume set|profile`. See [Commands](docs/COMMANDS.md).

## Package compatibility

HG2Gui does **not** assume a Termux `.deb` can simply be unpacked and executed unchanged under another Android application ID. The package/runtime layer includes support for:

- relocation of Termux's hardcoded prefix;
- control metadata and relocated symlink repair;
- dependency planning with Pre-Depends, alternatives, Provides, Conflicts/Breaks/Replaces, and version-aware relations;
- dpkg database/install-root control and chrootless operation;
- interpreter-aware maintainer-script execution through Android-safe native entry points;
- dpkg triggers, alternatives, diversions, and explicit `postrm purge` handling;
- whole-plan transaction recovery/rollback;
- mirror failover, architecture selection, signed `InRelease` verification against keyrings that are either pinned in the build or recorded after HG2Gui itself installed `termux-keyring` (a key rotation upstream therefore needs an app update to add the new pin), a freshness bound (an `InRelease` without `Valid-Until` older than 180 days is rejected), authenticated index SHA-256 verification, and verified package downloads;
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

It exposes typed actions instead of making an untrusted broadcast string synonymous with terminal execution. ADB/root requests are routed through a foreground approval activity that names the requesting app and shows the exact command; dismissing it answers the caller with a denial. Headless callers do not inherit elevation. Each request leaves `request`, `decision` and `result` rows in a bounded (200 rows) app-private audit log, readable through `AUDIT_HISTORY`.

### Capability policy

Each installed package has an `adb` / `root` / `network` / `host-read` / `host-write` policy: **ASK** (default), **ALLOW ONCE** (one request, in memory only) or **DENY**. Only `network` is enforced: launching an isolated package needs a one-shot `network` grant, and without one the launch is refused. `adb`, `root`, `host-read` and `host-write` are recorded but HG2Gui has no way to hand them to a package, so they are never granted (`capability-request` for them exits 1). See `hg2package capability` and `capability-request` in [Commands](docs/COMMANDS.md). The API also offers `CAPABILITIES` (schema), `AUDIT_HISTORY` and short-lived metadata channels; `remote` inspects SSH presets; `hg2package snapshot`/`restore` manage restore points.

## Android permissions

HG2Gui is a terminal and file manager, so it declares a broad set; each is used only on demand and none feeds a remote service (see [Privacy policy](docs/PRIVACY_POLICY.md) for the full list):

- contacts, phone (placing calls), camera, Bluetooth: for the matching built-ins, requested at first use;
- write system settings: brightness and similar built-ins;
- all files access: reading and writing shared storage you point it at;
- install packages: handing an APK to the Android installer for self-update/APK install (Android confirms);
- network (`INTERNET`), notifications, foreground service, wake lock: downloads and results, and keeping a running command alive. While a command is running in a terminal tab (or a full-screen program is open) HG2Gui runs a foreground service holding a partial wake lock, released when the command ends or after six hours. This is best-effort: Android may refuse to start the service when HG2Gui is already in the background, and the command then runs without that protection.

## Project structure

- `:composeApp` — Android application entry points, activities/services, API transport, resources, build/signing/packaging.
- `:shared` — Compose UI, terminal routing, shell/TUI/completion adapters, package management, lifecycle/isolation/authority, managers and integrations.
- `:terminal-emulator` — vendored terminal emulator and PTY support.

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
- AGP 9.4.1
- Kotlin 2.4.21
- Compose Multiplatform 1.12.1
- `compileSdk` / `targetSdk` 37
- `minSdk` 24
- NDK `29.0.14206865` (pinned once, in `gradle/libs.versions.toml`; the app build and CI both read it from there)

```bash
./gradlew assembleDebug
```

The native launchers are compiled with that NDK during the build; the Android SDK is found from `sdk.dir` in `local.properties`, then `ANDROID_HOME` / `ANDROID_SDK_ROOT`.

The bundled Termux-derived binaries and native launchers are `arm64-v8a` only, and the app is restricted to that ABI (`abiFilters`), so it will not install on 32-bit or x86 devices. Provenance, licences and the source offer for `composeApp/src/main/jniLibs` are in its `NOTICE.md`, with checksums in `SHA256SUMS`.

`version.properties` is rewritten only when `-PautoIncrement` is passed; the release pipeline computes its version up front and commits the bump itself. `.github/workflows/ci.yml` runs on pull requests and on pushes to any branch except `gh-pages`:

- `test` — `:shared` and `:terminal-emulator` unit tests (blocking) and detekt (non-blocking: it reports but cannot fail the build);
- `composeapp` — `:composeApp` unit tests and a release Kotlin compile (blocking) and Android lint (non-blocking).

### Updates

The in-app updater checks the GitHub release tagged `latest-v<major>.<minor>` (taken from the installed `versionName`) and offers only `hg2gui-playstore-release-<version>.apk`. It verifies the SHA-256 GitHub reports when there is one, the package name, a `versionCode` that matches the release and is newer than the installed one, and that the signing certificates match the installed app, and it refuses a debuggable APK. Debug builds are signed with the local debug key, so they cannot update through this channel. The release pipeline must publish `assemblePlaystoreRelease` output to that tag; builds from before this change poll `latest-debug-v<major>.<minor>` and install debug APKs. The check runs at most once every six hours when the app starts, can be switched off in Settings, and only notifies: nothing is downloaded until you accept.

CI success proves compilation/tests/static gates represented in the workflow. Android runtime compatibility still requires real-device confirmation for cases that depend on Android linker, ICU, package execution, permissions, ADB, root, or PRoot behavior.

## Documentation

- [Animation clip production](docs/guide/CLIP_PRODUCTION_INDEX.md)
- [Vision](docs/VISION.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Android architecture](docs/HG2GUI_ARCHITECTURE.md)
- [Commands](docs/COMMANDS.md)
- [Design](docs/DESIGN.md)
- [User guide](docs/USER_GUIDE.md)
- [Contributing](docs/CONTRIBUTING.md)
- [Active TODO](docs/TODO.md)

The Guide manuscript and visual/motion source material under `docs/` are product source material in their own domains; runtime/API behavior is defined by the live software documentation and code.
