# User Guide

## Getting started

HG2Gui is a touch-first Android command-line environment built on a real Termux-derived runtime.

You are not restricted to one input style. Depending on what you are doing, HG2Gui can use command composition, semantic completion, Guide links, graphical file selection, shell prompt projections, adaptive TUI controls, or raw terminal input.

On first use, HG2Gui installs its pinned runtime into app-private storage. Package/bootstrap progress appears in the active output record.

## Terminal sessions

Each terminal tab keeps its own shell state, working directory, output, and history context.

New sessions can use an installed **Bash**, **Zsh**, or **Fish** shell selected in Settings. Changing the default shell affects new sessions, not an already-running one.

Oh My Zsh, Powerlevel10k, Starship, Pure, and similar prompt/theme systems remain part of the shell presentation layer. HG2Gui can extract useful prompt state from them without requiring the raw theme layout to become the native UI.

## Composing commands

Commands can be assembled from any combination of:

- native command/option controls;
- semantic completion candidates;
- typed text;
- Guide command links;
- file/folder picker results;
- workflow/AI suggestions;
- shell prompt projections such as Git branch selection.

Ordinary command composition does not auto-run. Press **RUN** when you want the composed command executed.

### Completion Bridge

While composing a command, HG2Gui can surface candidates from Bash, Zsh, Fish, files, installed packages, Git branches, SSH hosts, services, and supported command-specific completion protocols.

Choosing a completion replaces only the current partial token. It does not execute the command or write a fake history entry.

## The Guide

The Guide is both documentation and an active command-entry surface.

When a Guide entry shows a real command, tap the command to hand it to the active terminal session for review. You can continue reading and composing without leaving the Guide. Tapping a Guide command does not auto-run it.

## Interactive shell applications

### Shell presentation

For normal interactive shells, HG2Gui can project:

- cwd;
- Git branch and dirty state;
- last exit status;
- user/host where meaningful.

The raw prompt remains available.

### Adaptive TUI Wrapper

When an application takes over the terminal as a screen-oriented interface, HG2Gui can recognize the live terminal state and replace the character-grid presentation with native controls when it has enough confidence.

Current wrapper semantics include:

- layered and modal menus;
- checkboxes and radio choices;
- tabs;
- scrolling lists;
- tables and selectable results;
- confirmation dialogs;
- text/password prompts;
- progress/status regions;
- multiple panes;
- mouse-aware terminal state.

Examples include many Python `curses`, Rich/Textual applications and interactive CLI tools such as AI coding assistants.

HG2Gui verifies generated navigation against what the child program actually displays. If the state cannot be reconciled, use **RAW** to return to the literal terminal interface.

## Files and paths

When a command needs a file or directory and HG2Gui recognizes that operand, it can open the graphical picker instead of requiring a typed path.

The broader **Files** surface provides purpose-built browsing and file operations. Files is its own native interface; it is not just another command submenu.

## Installing packages

Mutating `pkg`/`apt`/`apt-get` work runs through HG2Gui's Android-compatible package transaction layer.

The current transaction layer handles repository verification, download reuse/resume, Termux-prefix relocation, package metadata repair, dependency planning, maintainer scripts, triggers, alternatives/diversions, rollback/recovery, and Android native-execution compatibility.

Package-name choices are populated from package/catalog metadata whenever available.

## Managing installed packages

Open **Packages** to inspect installed manager inventories.

Current adapters include:

- Termux / pkg
- Python / pip
- Python apps / pipx
- Node / npm
- Ruby / gem

Packages may expose:

- **Run**
- **Disable / Enable**
- **Isolate / Release isolation**
- **Update**
- **Reset**
- **Info**
- **Remove**
- **Purge** where meaningful

### Disable / Enable

Disable keeps the package installed and visible while HG2Gui blocks commands it can attribute to that package. Enable restores normal execution.

### Reset

Reset retains the installed package payload while clearing runtime state HG2Gui can safely attribute to it. For isolated packages, Reset removes the private root so the next run reseeds it.

### Package execution backends

HG2Gui chooses among three package execution paths:

- **DIRECT_LINKER** — normal Android-compatible Termux execution;
- **PROOT_COMPAT** — compatibility execution for specifically classified ELF/runtime cases;
- **PROOT_ISOLATED** — private-root execution for isolated packages.

A generic command failure is not enough to trigger a silent PRoot retry.

## Isolating a package

Choose **Isolate** on an installed package.

An isolated package receives:

- a private root;
- private HOME/XDG state;
- no bind mount of the real HG2Gui home/prefix;
- common privilege tools removed from the guest prefix;
- common host `su` paths masked;
- fail-closed behavior when isolation cannot be established.

HG2Gui also performs best-effort runtime observation of:

- process descendants;
- live opened files and observed access modes;
- TCP/TCP6/UDP/UDP6 socket endpoints;
- ADB/root-like privilege-tool attempts;
- before/after private-root filesystem changes.

This is not complete kernel syscall tracing. Very short-lived events can escape observation.

### Isolation Audit

Open **Settings → Isolation audit** to inspect latest observed activity for isolated packages. The view groups authority attempts, network observations, processes, and file observations and shows current sandbox size/state.

## Execution authority

**Packages → Authority** exposes explicit authority backends.

### App

Normal default Android app authority.

### ADB shell

When an executable ADB client is available, HG2Gui can perform devices/pair/connect/disconnect/shell operations. Android's normal Wireless Debugging authorization still applies.

ADB shell execution requires foreground confirmation.

### Root

When an executable `su` provider is present, HG2Gui can request root execution. The device's root manager remains authoritative and HG2Gui asks for foreground confirmation.

### No inherited privilege

Normal commands, isolated packages, and headless callers do not inherit ADB/root merely because HG2Gui can access those backends.

## External Android API

HG2Gui exposes a signature-protected capability API for trusted companion applications signed with the same certificate.

Typed capabilities include:

- command execution;
- package operations;
- file/folder picker with returned result;
- notifications;
- foreground dialogs;
- clipboard get/set;
- device information;
- share/open actions;
- package lifecycle operations;
- foreground-approved ADB/root authority requests.

The API does not make background callers automatically privileged.

## SSH

SSH presets and host/key choices can be assembled natively. Known SSH hosts can also feed the Completion Bridge. Password/passphrase and host-key questions use the normal interactive prompt surfaces.

## Workflows

Workflows collect template values and compose the resulting command for review. They do not bypass the explicit RUN step for ordinary commands.

## AI

The AI surface can propose commands and return them to the terminal for review. AI suggestions remain subject to the same package, isolation, and authority policy as manually composed commands.

## Store

The Store manages `.azp` packages separately from Termux/dpkg/pip/pipx/npm/gem lifecycle inventory.

## MCP

The MCP server is loopback-only and explicitly started. Its shell execution gate does not grant ADB/root authority because headless elevation remains blocked.

## Settings

The Settings screen scrolls vertically whenever its contents exceed the viewport, including at larger text scales.

Current settings include display/text controls, PTY behavior, default shell, environment/history navigation, isolation audit, AI settings, and MCP/developer controls.

## Compatibility expectations

HG2Gui runs under its own Android application ID and security context, so Android linker, ICU, permission, package, ADB/root, and PRoot behavior must be validated on real devices in addition to CI.

When a package/runtime case fails, compatibility work should fix the class of incompatibility rather than adding a package-name exception unless the package truly has unique semantics.