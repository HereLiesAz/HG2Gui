# Future Vision

## Goal

Build the terminal Douglas Adams accidentally described: a pocket Guide that makes a hostile body of technical knowledge feel browsable, obvious, and slightly ridiculous.

For HG2Gui that means a real Android command environment that can project command-line software into the interaction surface that best fits the task. Sometimes that is a composed command. Sometimes it is the Guide, Files, a structured shell prompt, an Adaptive TUI Wrapper, a picker, an audit screen, or raw terminal input.

The governing interaction principle is:

> Understand enough structure to offer meaningful native controls without taking authority away from the real program underneath.

Typing remains available for open-ended information. Discoverable values can become direct selections. Raw terminal behavior remains available whenever HG2Gui cannot safely infer more.

## What is already established

### Command composition and completion

- [x] Real shell commands discovered from the installed runtime.
- [x] Help-derived flags and structured operands exposed through native controls.
- [x] Normal composed commands require explicit **RUN**.
- [x] File/directory operands can use the graphical picker.
- [x] Package names use repository/package-manager inventories where available.
- [x] Yes/no and finite interactive choices use native controls.
- [x] Completion Bridge normalizes Bash, Zsh, Fish, filesystem, package, Git, SSH, service, and supported command-specific candidates.
- [x] Completion selection replaces only the active partial token rather than executing the command.

### The Guide as command entry

- [x] Guide entries are backed by real command tokens.
- [x] A command shown while reading can be tapped and handed to the terminal composition flow.
- [x] Reading and command composition can interleave without leaving the Guide.
- [x] Tapping a Guide command does not auto-run it.

Future Guide work:

- [ ] make unambiguous examples/subcommands inside prose tappable too;
- [ ] connect entries more deeply to live package/help/completion metadata;
- [ ] add stable package/isolation/authority concepts to the Guide once their behavior is mature enough to deserve canonical treatment.

### Shell Adapter

- [x] Bash, Zsh, and Fish can be selected as persistent shells when installed.
- [x] Prompt state can project cwd, Git branch/dirty state, last exit status, and user/host where meaningful.
- [x] Oh My Zsh, Powerlevel10k, Starship, Pure, and similar systems are treated as presentation layers on the actual shell.
- [x] Structured Git branch projection can open a native branch chooser.

Future shell work:

- [ ] richer shell-native completion ingestion where it can be done without sourcing untrusted scripts;
- [ ] more structured prompt segments when they can be identified reliably;
- [ ] cooperative shell metadata protocols so themes can expose semantics directly instead of requiring inference.

### Adaptive TUI Wrapper

- [x] Alternate-screen/full-screen terminal state is detectable from the live emulator.
- [x] Toolkit-neutral semantic snapshots can model layered/modal menus, checkboxes/radios, tabs, lists, tables, selectable results, prompts, confirmations, progress/status regions, multiple panes, and mouse-aware state.
- [x] Selection can be inferred from terminal attributes/cursor/markers beyond inverse-video alone.
- [x] Recognized interfaces can render as native HG2Gui controls.
- [x] **RAW** remains available for unmodeled or low-confidence interaction.
- [x] Generated navigation is verified against subsequent terminal state instead of blindly sending a fixed key count.

Future TUI work:

- [ ] richer mouse/gesture projection for applications that expose stable regions;
- [ ] adapter profiles for toolkits/protocols when generic inference is insufficient;
- [ ] cooperative metadata through the external API so applications can describe their own structure directly;
- [ ] richer diff/editor/log/result projections where terminal semantics are strong enough to justify them.

The underlying process must remain the source of truth. HG2Gui presents and controls it; it does not reimplement its business logic.

## Package compatibility and execution

The package project has moved from one-off install repair to a general Android compatibility layer.

Implemented layers include:

- [x] mutating apt/apt-get routing through HG2Gui transactions;
- [x] resumable/reusable downloads;
- [x] repository mirror failover and architecture selection;
- [x] signed `InRelease` verification plus authenticated index/package SHA-256 verification;
- [x] relocation of Termux package payloads into HG2Gui's prefix;
- [x] control metadata and relocated-symlink repair;
- [x] dependency planning including Pre-Depends, alternatives, Provides, Conflicts/Breaks/Replaces, and version-aware relations;
- [x] chrootless dpkg with HG2Gui-owned database/install root;
- [x] interpreter-aware maintainer-script execution through Android-safe native entry points;
- [x] triggers, alternatives, diversions, explicit `postrm purge`, and rollback/recovery;
- [x] package execution backend selection between `DIRECT_LINKER`, `PROOT_COMPAT`, and `PROOT_ISOLATED`.

Future compatibility work should remain evidence-driven:

- [ ] expand native/PRoot compatibility classification only when a concrete Android runtime case demonstrates the need;
- [ ] dependency closure/explanation views such as “why is this installed?” and “what requires this?”;
- [ ] additional package-manager adapters only when the manager exposes stable inventory/action semantics.

Arbitrary command failure must never become a silent “retry under PRoot” policy.

## Package lifecycle

Implemented manager inventories:

- [x] dpkg/pkg;
- [x] pip;
- [x] pipx;
- [x] npm;
- [x] RubyGems.

Implemented HG2Gui-owned lifecycle controls:

- [x] Disable / Enable;
- [x] Reset;
- [x] Isolate / Release isolation.

Package managers own installation mechanics. HG2Gui owns how installed software is allowed to execute and how its runtime state is represented.

Future lifecycle work:

- [ ] dependency closure and impact views;
- [ ] pre-reset impact previews;
- [ ] optional snapshots/restore points where storage cost is justified;
- [ ] stronger provenance tracking for non-isolated package writes.

## Isolation and observability

Isolation is one of HG2Gui's defining opportunities.

Implemented:

- [x] package-level isolation state;
- [x] private PRoot-backed root and private HOME/XDG state;
- [x] fail-closed execution when isolation cannot be established;
- [x] common ADB/root tools stripped or masked inside the guest;
- [x] version-aware reseeding;
- [x] before/after private-root filesystem audit;
- [x] best-effort same-UID `/proc` observation of process descendants;
- [x] observed open files and read/write modes;
- [x] observed TCP/TCP6/UDP/UDP6 endpoints;
- [x] ADB/root-like privilege-tool attempts;
- [x] native Settings → Isolation Audit view for latest observed package activity.

The current sampler is not kernel audit and must not be described as exhaustive syscall interception.

Future isolation work:

- [ ] reduce whole-prefix seeding toward dependency-closure/overlay strategies;
- [ ] explicit read/write bind policy per package;
- [ ] enforceable network policy in addition to network visibility;
- [ ] per-run capability brokerage: package asks, HG2Gui explains, human grants or denies;
- [ ] exportable/reproducible sandbox definitions;
- [ ] deeper observation only where Android/runtime constraints permit truthful guarantees.

## Execution authority

Implemented authority levels:

- [x] App authority;
- [x] isolated package authority boundary;
- [x] ADB shell authority with pair/connect/disconnect/shell support;
- [x] Root authority through an explicit `su` provider;
- [x] foreground approval for elevated requests;
- [x] no headless elevation.

Future authority work:

- [ ] capability-oriented Android-device operations built on ADB (`pm`, `am`, `cmd`, `settings`, `dumpsys`, `logcat`, etc.) without turning them into one giant hand-written pseudo-shell;
- [ ] persistent but revocable ADB pairing/status UI;
- [ ] per-package “allow once / ask every time / deny” capability policies;
- [ ] root-aware features only where root adds a truthful capability.

A package must never receive ADB/root merely because HG2Gui itself can obtain it.

## External Android API

Implemented:

- [x] signature-protected Android transport;
- [x] typed command execution request;
- [x] package operations;
- [x] file/folder picker with returned result;
- [x] notifications and foreground dialogs;
- [x] clipboard get/set;
- [x] device information;
- [x] share/open actions;
- [x] lifecycle operations;
- [x] foreground-approved ADB/root requests.

The API is capability-oriented rather than defining the entire integration model as “send an arbitrary command string.”

Future API work:

- [ ] versioned capability discovery/schema;
- [ ] richer typed result objects where HG2Gui already understands the domain;
- [ ] caller-visible audit history;
- [ ] cooperative TUI/shell metadata channels;
- [ ] revocable per-caller grants if HG2Gui ever supports trusted callers beyond same-signature applications.

## Native surfaces

HG2Gui should keep choosing the right surface for the object:

- Files for files;
- Guide for learning and command discovery;
- terminal composition for ordinary command assembly;
- Adaptive TUI Wrapper for structured screen-oriented programs;
- Shell Adapter for prompts/themes/completion state;
- package views for installed software;
- isolation audit for observed sandbox activity;
- foreground approval UI for elevated authority.

The terminal remains central, but it is not the required visual container for every feature.

## Connectivity and AI

Implemented:

- [x] SSH presets and native host/user/port/key collection;
- [x] known SSH hosts feeding completion;
- [x] remote OS reference contexts;
- [x] ADB authority model;
- [x] loopback MCP server;
- [x] natural-language command suggestions returned for review.

Future:

- [ ] live remote command/help/package discovery over SSH;
- [ ] remote package-manager adapters tied to the active connection;
- [ ] remote filesystem surfaces visually distinct from local paths;
- [ ] AI output expressed as typed command intentions rather than only strings;
- [ ] AI explanations of package/isolation/authority consequences before execution;
- [ ] AI interpretation of isolation audit results without granting AI ambient elevation.

## Long-term product principle

A conventional terminal expects the human to interpret strings and gives child programs whatever authority the shell already has.

HG2Gui should improve both sides:

1. understand enough structure to make command-line software directly manipulable on a touch device;
2. understand enough authority to keep privilege explicit, visible, and intentionally granted.

The destination is a runtime that can dynamically generate useful Android interfaces for command-line software while preserving the real program underneath.