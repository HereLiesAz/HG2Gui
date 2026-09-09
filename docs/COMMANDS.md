# Command and Capability Reference

HG2Gui exposes several different interaction layers over the same runtime. Not everything is represented as a command, and not every command must be entered through the pill stack.

Current entry/control surfaces include:

- real runtime commands;
- terminal command composition;
- semantic completion;
- tappable Guide commands;
- shell prompt/status projection;
- Adaptive TUI Wrapper controls;
- package/lifecycle UI;
- Files/pickers and other native screens;
- signature-protected external Android API capabilities.

Ordinary composed commands require explicit **RUN**. Interactive controls that answer an already-running process naturally send their response to that process.

## Real shell commands

The shell command set is discovered from the installed HG2Gui runtime rather than hardcoded.

`CommandTree` can expose discovered commands/options when that representation is useful. `CompletionBridge` can separately expose semantic candidates from shell/file/package/Git/SSH/service providers.

## Bootstrap and downloads

### `bootstrap`

Installs or repairs HG2Gui's pinned Termux-derived userspace.

### `download ...` / `hg2download ...`

HG2Gui downloader entry points with resume/reuse support where possible.

These are execution-control verbs handled by `TerminalEngine` before ordinary shell dispatch.

## Package transactions

### `pkg ...` / `hg2pkg ...`

HG2Gui package-manager entry points.

Top-level mutating `apt`/`apt-get` operations are translated into this transaction layer so Android compatibility handling cannot be bypassed by upstream apt invoking dpkg directly.

Supported transaction work includes repository verification, download reuse/resume, relocation, metadata repair, dependency planning, maintainer-script handling, triggers, alternatives/diversions, rollback/recovery, and native execution compatibility.

## Installed-package lifecycle

### `hg2package <action> <manager> <package>`

Internal control surface backing installed-package lifecycle actions.

Current actions include:

- `info`
- `update`
- `disable`
- `enable`
- `isolate`
- `release`
- `reset`
- `remove`
- `purge`

The owning manager currently comes from adapters for:

- `pkg` / dpkg
- `pip`
- `pipx`
- `npm`
- `gem`

### Disable / Enable

Disable keeps the package installed while HG2Gui blocks owned commands. Enable removes that HG2Gui policy flag.

### Isolate / Release

Isolation selects the private-root backend. An isolated package never silently falls back to an unsandboxed backend when the isolation engine is unavailable.

### Reset

Reset keeps the installed payload while clearing package runtime state HG2Gui can safely attribute. For an isolated package this removes the private root so the next run reseeds it.

### Update / Remove / Purge

These actions delegate through the owning manager where meaningful. Destructive lifecycle actions require confirmation in interactive use.

## Package execution backends

Package-owned commands are classified before execution:

- `DIRECT_LINKER` — normal Android-compatible HG2Gui Termux execution;
- `PROOT_COMPAT` — compatibility execution for specifically classified ELF/runtime cases;
- `PROOT_ISOLATED` — private-root execution for isolated packages.

A command does not move to PRoot merely because it failed.

## Execution authority

### `hg2auth status`

Reports currently available execution-authority backends.

### ADB

- `hg2auth adb devices`
- `hg2auth adb pair <host:port> <pairing-code>`
- `hg2auth adb connect <host:port>`
- `hg2auth adb disconnect [host:port]`
- `hg2auth adb shell <command...>`

ADB shell execution requires foreground confirmation. Android pairing/connect authorization still applies.

### Root

- `hg2auth root test`
- `hg2auth root shell <command...>`

Root execution requires foreground confirmation and remains subject to the device's `su` provider/root manager.

### Authority invariant

Normal commands, isolated packages, and headless callers do not inherit ADB/root authority.

## Interactive prompts

When a process is already running:

- yes/no can become a confirmation dialog;
- finite numbered/bracketed choices can become native selections;
- passwords/passphrases use masked input;
- open-ended prompts use text input.

## Shell Adapter

Bash, Zsh, and Fish sessions can expose structured presentation state without changing the underlying shell.

Recognized presentation layers include Oh My Zsh, Powerlevel10k, Starship, Pure, and custom prompts.

Structured state can include:

- cwd;
- Git branch/dirty state;
- last exit status;
- user/host where meaningful.

The raw prompt remains available.

## Completion Bridge

Semantic completion sources currently include:

- Bash/Zsh/Fish adapters;
- filesystem entries;
- installed package names;
- Git branches;
- SSH hosts;
- services;
- supported command-specific protocols;
- declarative subsets of shell completion definitions that can be consumed without sourcing arbitrary scripts.

Selecting a completion replaces the active partial token and does not execute the command.

## Adaptive TUI Wrapper

Alternate-screen/full-screen terminal applications can be projected into native controls when HG2Gui has enough semantic confidence.

The wrapper currently models:

- layered/modal menus;
- checkbox/radio controls;
- tabs;
- scrolling lists;
- tables/selectable results;
- text/password prompts;
- confirmations;
- progress/status regions;
- multiple panes;
- terminal mouse-awareness.

Generated navigation is reconciled with subsequent screen state. **RAW** returns to the literal terminal UI at any time.

## Guide commands

A real command encountered while reading the Guide can be tapped and inserted into the active terminal composition flow.

The Guide remains open so reading and command composition can interleave. Guide command taps do not auto-execute.

## Files and path picking

File/directory operands can invoke the graphical picker. Files is also a dedicated native browsing surface rather than merely a command category.

## Isolation Audit

**Settings → Isolation audit** opens the native audit screen for isolated packages. It reports latest observed process/file/network/authority telemetry and current sandbox state.

The observation layer is best-effort same-UID `/proc` sampling plus private-root before/after filesystem metadata. It is not complete kernel syscall tracing.

## External Android API

Trusted same-signature applications can use the signature-protected permission:

`com.hereliesaz.hg2gui.permission.API`

Typed API actions include:

- command execution;
- package operations;
- file/folder picker with returned result;
- notifications;
- foreground dialogs;
- clipboard get/set;
- device information;
- share/open actions;
- lifecycle operations;
- foreground-approved ADB/root authority requests.

The API is capability-oriented and does not make background integration synonymous with elevation.

## Android-facing built-ins

`Builtins.kt` currently contains explicit bridges for Android-facing capabilities including Wi-Fi/Bluetooth/settings controls, flashlight, volume, brightness, calls/contacts, VFS, calculator, and editor entry.

These are current implementation entry points, not a requirement that future Android capabilities must always become shell verbs. Dedicated native/API capability surfaces are equally valid where they better fit the task.

## SSH, Workflows, AI, Store, MCP

- **SSH** — presets and completion-assisted host/key composition.
- **Workflows** — command templates assembled for review.
- **AI** — suggested commands returned to the user for review.
- **Store** — `.azp` packages, separate from shell package-manager lifecycle.
- **MCP** — explicit-start loopback service; headless shell execution still cannot elevate through ADB/root.