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

### Line planning

Before anything runs, `LinePlanner` (in `CommandRouting.kt`) analyses the whole line with `CommandLine`:

- an unterminated quote, `$(` or backtick is refused (exit 2) instead of reaching the shell;
- a disabled package's binary blocks the line wherever it can be statically seen: after `&&`/`;`/`|`, behind `env`/`time`/`sudo`, in `bash -c`, `$( )`, backticks or `find -exec`;
- a mutating `pkg`/`apt` that cannot be routed (wrapped, piped, nested, inside a control structure) is refused (exit 2) rather than reaching the real apt unjournaled;
- an isolated package's binary runs only as its own command or as one step of a `;` / `&&` / `||` list, and is refused (exit 126) anywhere else;
- a `;` / `&&` / `||` list that contains a command the engine handles itself runs one step at a time with the shell's short-circuit rules; everything else goes to the shell as typed.

A command word that is only known at run time (`$tool`, a function, a sourced script) is invisible to this analysis, and a direct `dpkg` call is outside the transaction layer; the planner does not claim to cover them.

### Headless execution

Each non-interactive caller constructs the engine with an `ExecutionPolicy` (`API`, `MCP`, `INSTALLER`). The engine itself, not each entry point, refuses what a background caller may not do, with exit code 126:

- `bootstrap`, `vfs mount`, and every `hg2auth` subcommand except `status`;
- package lifecycle actions other than `info` and `snapshot`; `enable` is allowed only for an API call that arrives while HG2Gui is on screen;
- the device-affecting built-ins (`call`, `contacts`, `brightness`, `wifi`, `bluetooth`, `airplane`, `flash`, `edit`, `volume set|profile`) unless a person answers yes to that exact line. The current background callers (API execute, MCP, installers) answer every prompt with nothing, so in practice they are refused.

`runToCompletion()` supports normal non-interactive work but never grants ADB/root authority. Authority status may be queried; elevated execution requires foreground approval.

## 2. Termux-derived runtime

`DistroManager.kt` installs a pinned Termux bootstrap into HG2Gui's app-private prefix, typically:

```text
/data/user/0/com.hereliesaz.hg2gui/files/usr
```

Android does not treat arbitrary native binaries copied into writable app storage the same way it treats APK-delivered native code. HG2Gui therefore maps required native executables through Android-legal native-library delivery where necessary. `BootstrapManifest.kt` and the packaged native entry points must remain synchronized with the bootstrap.

The bootstrap archive is checked against a SHA-256 pinned in the build. `bootstrap` extracts into `usr.staging` next to `usr`, verifies the result (a `bin/` directory, an executable `bin/bash`, and the archive's own dpkg database), and then renames it over `usr`; an existing prefix is renamed to `usr.previous` first and deleted only after the swap succeeds, and an interrupted swap is rolled back. A failure at any step (offline, checksum mismatch, full storage) therefore leaves the existing prefix as it was. A successful bootstrap over an existing prefix replaces it: packages installed on top of the previous bootstrap are not carried over (the home directory is untouched).

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

Two paths feed it. For full-screen interactive shell sessions `ShellAdapterRegistry` reads the live prompt and detects the framework. The ordinary command composer has no prompt to parse, so `ShellStatusProjector` re-derives the state after each command: the working directory, the branch and dirty flag from `git --no-optional-locks status --porcelain=v1 --branch` (probed with a two-second timeout), and the last exit status. User and host are shown there only when a caller that knows them (an SSH session, a cooperative shell) supplied them. The composer's status line is display only; Git branch names reach the user as Completion Bridge candidates after `git checkout`, `switch`, `branch`, `merge` and `rebase`.

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

Top-level mutating `apt`/`apt-get` operations (as their own command or one step of a `;` / `&&` / `||` list) are translated into HG2Gui package transactions before upstream apt can bypass Android compatibility handling. The translation is deliberately strict:

- the operation is the first non-option word; `install`, `reinstall`, `remove`, `purge`, `update`, `upgrade`, `dist-upgrade`/`full-upgrade`, `clean` and `autoclean` are translated, and `apt reinstall` / `--reinstall` becomes `pkg install --reinstall`;
- read-only operations (`search`, `show`, `list`, `policy`, …) pass through to the real apt;
- dry-run flags (`-s`, `--simulate`, `--dry-run`, …), `autoremove`/`autopurge`, `build-dep`, `satisfy`, `aptitude`, versioned or path operands, and any option other than `-y`/`-q`/`--no-install-recommends` and their variants are refused with a message, because running them differently from what was typed would change packages unexpectedly;
- `pkg` itself rejects unknown `-`/`--` options instead of dropping them; `--reinstall` (alias `--force-requested`) is accepted only for `install`;
- `pkg upgrade <name>` upgrades just the named installed package (an error if it is not installed) and is what a lifecycle Update of a `pkg` package runs, because `pkg install <name>` is a no-op for an installed package.

A mutating `pkg`/`apt` that HG2Gui cannot route (wrapped in `sudo`/`env`, piped, nested, in a control structure) is refused; a direct `dpkg` call and run-time-computed command words are not covered by this layer.

The package transaction layer includes:

- resumable/reusable downloads: `download` is https-only unless `--allow-http` is given, resume uses `ETag` / `If-Range` / `Content-Range` validation, and "already downloaded" requires the recorded validator and length to match;
- repository mirror failover (four mirrors: `packages.termux.dev`, `packages-cf.termux.dev`, `mirror.accum.se`, `ftp.agdsn.de`);
- architecture selection;
- signed `InRelease` verification. The Termux keyrings live inside the app-writable prefix, so a keyring file is trusted only if its SHA-256 is pinned in the build or was recorded after HG2Gui itself installed `termux-keyring` from an already-authenticated index. An `InRelease` older than the newest accepted one is refused, as is one without `Valid-Until` that is more than 180 days old; there is also a fixed replay floor of 2026-06-01. The cost is that an upstream key rotation needs an app update to add the new pin;
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
- whole-plan rollback/recovery. Rollback journals live under `noBackupFilesDir/hg2-package-transactions` (journals written by older builds under `cacheDir` are migrated on the next recovery), starting a transaction checks free space, and a whole plan is committed by one atomic directory rename.

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

Manager-native operations such as update/remove/purge are delegated through the manager adapter where meaningful. For a `pkg`-managed package, Update runs `pkg upgrade <name>`.

## 8. Package execution backends

`PackageExecutionBackend` chooses execution based on the actual owned executable/runtime case:

- `DIRECT_LINKER` — normal HG2Gui Termux execution through the Android-compatible direct/preload path;
- `PROOT_COMPAT` — compatibility execution without a private isolation root for specifically classified ELF/runtime cases;
- `PROOT_ISOLATED` — private-root package execution.

Dynamically linked ELF with an appropriate interpreter remains on the direct path. Compatibility routing is narrow and explicit.

If an isolated package cannot obtain its isolation backend, execution fails rather than running unisolated. An isolated package's binary is only run as its own command or one step of a `;` / `&&` / `||` list (see Line planning).

## 9. Package isolation and observability

Isolation is state separation with best-effort observation. It is not a verified security boundary: the engine is PRoot, a ptrace-based path translator running as the same Android UID as HG2Gui, not a chroot, namespace or sandbox, and network access is not confined (see the launch gate below).

An isolated package receives a private filesystem root, private HOME, and private XDG cache/config/data/state/tmp directories. HG2Gui seeds the private root from the installed runtime and records the package version so stale roots can be discarded after update/reset.

The real HG2Gui prefix/home are not bind-mounted into the guest. Common privilege tools are removed from the guest prefix and common host `su` paths are masked. The guest is given host `/system` and `/apex` and a short allow-list of static `/proc` files (`cpuinfo`, `meminfo`, `stat`, `uptime`, `loadavg`, `version`, `filesystems`); raw `/proc` is not bound because its kernel-resolved links bypass path translation.

Launching an isolated package needs a one-run `network` grant (`PackageCapabilityPolicy`); this is a fail-closed launch gate, not offline confinement. Of the five policy capabilities only `network` has an enforcement point; `adb`, `root`, `host-read` and `host-write` are never granted to a package.

### Runtime observations

HG2Gui combines before/after private-root metadata with best-effort same-UID `/proc` sampling. Current observations include:

- private-root created/modified/deleted paths;
- process tree and spawned descendants;
- live open file descriptors and observed read/write modes;
- TCP/TCP6/UDP/UDP6 socket endpoints when observable;
- privilege-tool attempts involving ADB/root-like tools.

The sampler runs on the host side, outside the guest, and writes its log outside the guest root, so the observed process cannot rewrite it through normal path access. It samples at roughly 20 Hz and is not tamper-proof against a same-UID process. This is not kernel audit or complete syscall tracing. Short-lived events can escape sampling, and documentation must not describe it as exhaustive interception.

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

HG2Gui does not hand ADB or root to anything implicitly. Its own routes to them (`hg2auth` in a terminal tab, and the API's authority request) need a person's approval in the foreground; headless callers (API execute, MCP, installers) cannot use `hg2auth`; isolated packages have `adb`/`su`-like tools removed or masked; and the package capability policy never grants `adb` or `root` to a package. What HG2Gui cannot enforce is what you type yourself: it does not intercept an `su` or `adb` binary you run from the shell, which behaves as it would in any terminal (the device's root manager still decides).

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

The API is capability-oriented. Authority requests are brokered through dedicated foreground UI rather than silently becoming ordinary background command execution. The approval dialog names the requesting app (the creator of the reply `PendingIntent`, recorded by the system) and shows the exact command, which is passed to `su -c` / `adb shell` as one argument without being re-tokenized; dismissing it answers the caller with a denial.

API commands run in a foreground service (`LongTaskService`) so a long command survives the broadcast; when Android refuses to start it, package operations are rejected and other commands fall back to a short broadcast budget.

Every request leaves up to three rows in a bounded (200 rows), app-private audit log, tied by a request id: `request` (before any validation), `decision` (what the person chose in an approval, dialog or picker activity) and `result` (what was replied). A command is stored as a digest and length, not verbatim; only an approved root/ADB command is kept, as a bounded preview. A failed write is logged and the row is lost.

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
- AGP 9.4.1
- Kotlin 2.4.21
- Compose Multiplatform 1.12.1
- `compileSdk` / `targetSdk` 37
- `minSdk` 24
- NDK `29.0.14206865` (pinned once in `gradle/libs.versions.toml`, read by `composeApp/build.gradle.kts` and `.github/workflows/ci.yml`)

CI (`.github/workflows/ci.yml`) has two jobs. `test` runs the `:shared` and `:terminal-emulator` unit tests (blocking) and detekt (non-blocking). `composeapp` runs the `:composeApp` unit tests and a release Kotlin compile (blocking) and Android lint (non-blocking). The release/publish workflows run from outside this repository (`_reusable-build.yml` documents the contract).

The in-app updater polls the GitHub release tagged `latest-v<major>.<minor>` and accepts only `hg2gui-playstore-release-*.apk`: it requires the same package name, a matching and newer `versionCode`, matching signing certificates, and a non-debuggable APK. The release pipeline must publish `assemblePlaystoreRelease` output to that tag. Debug builds are signed with the local debug key and cannot pass the signer comparison.

CI verifies what its jobs actually compile/test/analyze. Real Android behavior that depends on Android ICU, linker policy, package execution, permissions, ADB, root, or PRoot still requires device verification. A green CI run is not equivalent to device-runtime proof.

## 17. Current invariants

1. Real shell/runtime behavior remains authoritative beneath HG2Gui's presentation and policy layers.
2. Ordinary command composition does not auto-run.
3. The Guide may compose commands directly from documentation.
4. Structured values can be selected when safely discoverable; open-ended values remain typeable.
5. Adaptive TUI wrapping must preserve a RAW escape path.
6. Generated TUI navigation must be reconciled with observed terminal state.
7. Package compatibility routing must be evidence-based, not arbitrary fallback.
8. An isolated package that cannot get its isolation backend fails instead of running unisolated, and it runs only as its own command or one step of a `;` / `&&` / `||` list; it is refused in pipes, `bash -c`, `$( )` and control structures. Isolation is state separation with best-effort observation, not a verified security boundary.
9. HG2Gui's own routes to ADB/root are explicit and brokered by foreground approval; HG2Gui does not grant them ambiently. (It does not intercept an `su`/`adb` you run yourself from the shell.)
10. Headless execution (API, MCP, installers) cannot elevate through ADB/root authority.
11. External API authority requests require foreground approval.
12. Native screens are allowed whenever they fit the task better than command composition.

## 18. Documentation authority

`README.md`, this file, `HG2GUI_ARCHITECTURE.md`, `COMMANDS.md`, `USER_GUIDE.md`, `DESIGN.md`, `VISION.md`, `CONTRIBUTING.md`, and `TODO.md` are the live software/product documentation. Guide, visual, and motion source material under `docs/` remains authoritative for its own content domain but does not override current runtime behavior.