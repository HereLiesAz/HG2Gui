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
- [x] Declarative shell completion files can be parsed as inert metadata when their format is understood; HG2Gui does not source arbitrary completion scripts merely to discover candidates.

### The Guide as command entry

- [x] Guide entries are backed by real command tokens.
- [x] A command shown while reading can be tapped and handed to the terminal composition flow.
- [x] Reading and command composition can interleave without leaving the Guide.
- [x] Tapping a Guide command does not auto-run it.
- [x] Unambiguous command references inside prose can be tapped too.
- [x] Guide entries are linked to the live command tree for availability, caps, and discoverable options.
- [x] Package lifecycle, isolation, and execution authority have canonical Guide entries.

### Shell Adapter

- [x] Bash, Zsh, and Fish can be selected as persistent shells when installed.
- [x] Prompt state can project cwd, Git branch/dirty state, last exit status, and user/host where meaningful.
- [x] Oh My Zsh, Powerlevel10k, Starship, Pure, and similar systems are treated as presentation layers on the actual shell.
- [x] Structured Git branch projection can open a native branch chooser.
- [x] Cooperative shell metadata channels can supply fresh prompt semantics directly while the real PTY remains authoritative.

### Adaptive TUI Wrapper

- [x] Alternate-screen/full-screen terminal state is detectable from the live emulator.
- [x] Toolkit-neutral semantic snapshots can model layered/modal menus, checkboxes/radios, tabs, lists, tables, selectable results, prompts, confirmations, progress/status regions, multiple panes, and mouse-aware state.
- [x] Selection can be inferred from terminal attributes/cursor/markers beyond inverse-video alone.
- [x] Recognized interfaces can render as native HG2Gui controls.
- [x] **RAW** remains available for unmodeled or low-confidence interaction.
- [x] Generated navigation is verified against subsequent terminal state instead of blindly sending a fixed key count.
- [x] Mouse-aware applications can receive terminal-emulator mouse events for stable projected regions.
- [x] Cooperative metadata can describe adapter/profile semantics when generic inference is insufficient.
- [x] Cooperative TUI metadata is available through the protected external API.
- [x] Diff, editor, log, result, pane, table, progress, and related semantic regions can receive richer native projection where confidence is sufficient.

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
- [x] package execution backend selection between `DIRECT_LINKER`, `PROOT_COMPAT`, and `PROOT_ISOLATED`;
- [x] installed dependency closure and reverse-dependency explanation/impact views for dpkg packages.

Evidence-driven boundaries remain intentional:

- expand native/PRoot compatibility classification only when a concrete Android runtime case demonstrates the need;
- add another package-manager adapter only when the manager exposes stable inventory/action semantics.

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
- [x] Isolate / Release isolation;
- [x] dependency closure and removal-impact views;
- [x] pre-reset exact path/byte previews;
- [x] bounded optional restore points with restore/delete controls;
- [x] stronger created/modified provenance tracking for non-isolated package writes.

Package managers own installation mechanics. HG2Gui owns how installed software is allowed to execute and how its runtime state is represented.

## Isolation and observability

Isolation is one of HG2Gui's defining opportunities.

Implemented:

- [x] package-level isolation state;
- [x] private PRoot-backed root and private HOME/XDG state;
- [x] fail-closed execution when isolation cannot be established;
- [x] common ADB/root tools stripped or masked inside the guest;
- [x] version-aware reseeding;
- [x] dpkg target/dependency-closure seed planning from package ownership metadata, with a safe whole-prefix fallback if selective planning is unavailable or exceeds its bound;
- [x] explicit host-read/host-write capability policy, with no ambient writable host bind exposed to isolated packages;
- [x] per-package ASK / ALLOW ONCE / DENY capability policy;
- [x] explicit interactive capability-request brokerage; one-shot grants live only in memory and are consumed once, while headless requests fail closed;
- [x] a network launch gate that refuses to start an isolated package without a one-run network grant;
- [x] versioned machine-readable/exportable sandbox definitions;
- [x] before/after private-root filesystem audit;
- [x] best-effort same-UID `/proc` observation of process descendants;
- [x] observed open files and read/write modes;
- [x] observed TCP/TCP6/UDP/UDP6 endpoints;
- [x] ADB/root-like privilege-tool attempts;
- [x] native Settings → Isolation Audit view for latest observed package activity;
- [x] opt-in redacted AI interpretation of isolation-audit results without giving AI ambient execution authority.

The current sampler is not kernel audit and must not be described as exhaustive syscall interception.

The current network guarantee is also precise: unprivileged PRoot does not provide a separate Android network namespace. HG2Gui therefore enforces policy by refusing an isolated launch unless the human grants networking for that run. Once granted, networking is ordinary Android networking; this is not fake “offline confinement.” True per-process offline networking remains evidence-gated on an enforceable Android/runtime primitive.

## Execution authority

Implemented authority levels and controls:

- [x] App authority;
- [x] isolated package authority boundary;
- [x] ADB shell authority with pair/connect/disconnect/shell support;
- [x] Root authority through an explicit `su` provider;
- [x] foreground approval for elevated requests;
- [x] no headless elevation;
- [x] capability-oriented Android-device operations built on ADB (`pm`, `am`, `cmd`, `settings`, `dumpsys`, `logcat`, etc.);
- [x] remembered successful ADB endpoints with reconnect and disconnect/revoke controls; pairing codes are deliberately never persisted;
- [x] per-package “allow once / ask every time / deny” capability policies.

Root-specific feature expansion remains evidence-driven: root should be used only when it creates a concrete, truthful capability rather than merely a larger shell.

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
- [x] foreground-approved ADB/root requests;
- [x] versioned capability discovery/schema;
- [x] richer typed result envelopes where HG2Gui understands the result domain;
- [x] caller-visible bounded audit history;
- [x] cooperative TUI/shell metadata publish/read/list/clear channels.

The API is capability-oriented rather than defining the entire integration model as “send an arbitrary command string.” Because the current trust boundary is same-signature applications, a separate per-caller grants system would add ceremony without increasing isolation. Revocable per-caller grants become relevant only if a broader caller trust model is introduced later.

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
- [x] target-scoped live remote command/help/package discovery over SSH with bounded cached metadata;
- [x] remote package-manager identification/inventory tied to the selected SSH preset;
- [x] remote filesystem surfaces explicitly labeled as remote rather than being mixed with local paths;
- [x] ADB authority model;
- [x] loopback MCP server;
- [x] natural-language command suggestions returned for review;
- [x] AI command suggestions parsed into typed command intentions rather than remaining string-only;
- [x] local explanations of package/isolation/authority consequences before a suggested command is handed to USE;
- [x] opt-in redacted AI interpretation of isolation audits without granting the AI ambient elevation.

Remote package mutation remains ordinary visible SSH command execution rather than a hidden second package engine. That keeps target identity and user review explicit while live manager identity/package inventory supply the native browsing context.

## What remains deliberately evidence-gated

The roadmap is no longer a list of speculative implementation promises. Remaining work should be opened only by evidence from a supported runtime:

- new native/PRoot compatibility classifications for demonstrated Android failures;
- new package-manager adapters with stable inventory/action contracts;
- true process-level offline network confinement if Android/runtime primitives can enforce it;
- deeper isolation observation only where stronger guarantees than sampled `/proc` observation are possible;
- root-specific functionality only where root provides a concrete feature;
- broader-trust per-caller API grants only if callers beyond same-signature applications are ever supported.

Physical-device verification remains tracked separately in `TODO.md`; a green JVM/Android build is not evidence that Android linker, ICU, PRoot, ADB, SSH, or device UI behavior has been exercised on hardware.

## Long-term product principle

A conventional terminal expects the human to interpret strings and gives child programs whatever authority the shell already has.

HG2Gui should improve both sides:

1. understand enough structure to make command-line software directly manipulable on a touch device;
2. understand enough authority to keep privilege explicit, visible, and intentionally granted.

The destination is a runtime that can dynamically generate useful Android interfaces for command-line software while preserving the real program underneath.
