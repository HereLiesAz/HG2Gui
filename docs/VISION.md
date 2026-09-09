# Future Vision

## Goal

Build the terminal Douglas Adams accidentally described: a pocket Guide that makes a hostile body of technical knowledge feel browsable, obvious, and slightly ridiculous.

For HG2Gui that means a real Android command environment that can be driven primarily by touch. The menu is not decoration around a CLI; **the menu is the input method**.

The governing interaction rule is now:

> If HG2Gui can discover or enumerate the valid values, the user selects them. Typing is for genuinely open-ended information.

That applies to commands, flags, packages, installed software, files, finite prompt choices, authority levels, and eventually richer runtime objects.

## What is already established

### Touch-first command composition

- [x] Real shell commands discovered from the installed runtime.
- [x] Command families and help-derived flags exposed as pills.
- [x] Normal leaves compose rather than auto-run; **RUN** is explicit.
- [x] Free-form input focuses the field and says what kind of value is expected.
- [x] File/directory operands use the graphical picker.
- [x] Package names use repository/package-manager inventories where available.
- [x] Yes/no prompts use an actual confirmation dialog.
- [x] Other finite interactive choices remain graphical.
- [x] Live command output auto-scrolls to the newest result.

The important shift is from “make common commands tappable” to “model command input by its actual type.”

### Real Android terminal runtime

- [x] App is a normal Android terminal, not a launcher.
- [x] Pinned Termux-derived bootstrap under HG2Gui's private application prefix.
- [x] Android-safe delivery of critical native bootstrap executables through the APK native-library path.
- [x] Persistent shell sessions with per-session state/history/working directory.
- [x] Graphical Files/VFS, editor, Guide, SSH presets, workflows, AI suggestions, Store, and MCP surfaces.

### Package compatibility instead of package exceptions

The package project has moved from “can we make this one install?” to “what assumptions does a Termux package make that are false under another Android application ID?”

Completed layers include:

- [x] mutating top-level apt/apt-get operations routed through HG2Gui's package transaction layer;
- [x] resumable/reusable package downloads;
- [x] relocation of Termux package payload paths into HG2Gui's prefix;
- [x] metadata-aware repair of `conffiles` and `md5sums`;
- [x] HG2Gui-owned dpkg database/install root;
- [x] maintainer-script execution without chroot;
- [x] maintainer scripts staged outside dpkg's direct-exec path and run through bundled Bash;
- [x] verified staging so failed deletes/moves do not masquerade as dpkg errors;
- [x] real-device package installs/execution crossing the complete transaction path.

The intended direction remains broad compatibility. A package-name special case is the last resort, not the architecture.

## Package management as a user interface

A package manager knows far more than “install this string.” It knows what exists, what is installed, what version is present, and often what executable commands a package provides. HG2Gui should continuously turn that knowledge into UI.

Implemented:

- [x] installed Termux/dpkg inventory;
- [x] Python/pip inventory;
- [x] Node/npm inventory;
- [x] RubyGems inventory;
- [x] manager → installed package → action hierarchy;
- [x] package-owned executable choices under **Run**;
- [x] manager-specific Update/Info/Remove/Purge operations;
- [x] HG2Gui-owned Disable/Enable, Reset, and Isolate/Release isolation.

Next direction:

- [ ] more package-manager adapters where they add real installed-package knowledge;
- [ ] richer dependency/version/conflict metadata;
- [ ] package update-state comparison before execution;
- [ ] dependency closure views (“why is this installed?” / “what requires this?”);
- [ ] transaction recovery/rollback and interrupted-install recovery;
- [ ] mirror/signature/repository trust improvements;
- [ ] alternatives/diversions/triggers/service-aware package semantics where real packages require them.

## Lifecycle operations nobody else has to provide

Package managers own installation. HG2Gui can own **how installed software is allowed to live**.

### Disable

- [x] Keep a package installed and visible while preventing HG2Gui from running its owned commands.

Future:

- [ ] make disabled-state effects visible everywhere a package executable is surfaced;
- [ ] package-level policy presets (for example “installed but never runnable without confirmation”).

### Reset

- [x] Normal packages: clear conservative package-scoped cache/config/data/state/log locations plus paths HG2Gui positively observed being created.
- [x] Isolated packages: discard the entire private runtime and reseed on next run.

Future:

- [ ] stronger provenance tracking for non-isolated package writes;
- [ ] a pre-reset impact view listing files/folders/size before confirmation;
- [ ] snapshots/restore points when the storage cost is justified.

### Isolate

This is one of HG2Gui's defining opportunities.

Current implementation:

- [x] package-level **Isolate / Release isolation** state;
- [x] PRoot-backed private filesystem root;
- [x] private HOME/XDG state;
- [x] fail closed when the isolation engine is unavailable;
- [x] strip/mask common ADB/root tools inside the guest;
- [x] filesystem before/after audit for created/modified/deleted paths;
- [x] version-aware reseeding after package updates.

The intended destination is stronger than “a different HOME.” An isolated package should be able to run with everything it legitimately needs while HG2Gui can explain what it tried to do and prevent it from modifying anything outside the allowed environment.

Future isolation work:

- [ ] reduce private-root seeding from a whole-prefix copy to a dependency closure/overlay strategy;
- [ ] explicit read/write bind policy per package;
- [ ] network policy and visibility;
- [ ] process/child-process audit;
- [ ] syscall-level observation where Android/runtime constraints make it practical;
- [ ] per-run capability requests: package asks, HG2Gui explains, user grants or denies;
- [ ] human-readable audit summaries rather than raw path diffs alone;
- [ ] exportable/reproducible sandbox definitions.

## Execution authority

HG2Gui now treats privilege as an explicit backend rather than an environmental accident.

Implemented authority levels:

- [x] **App** — ordinary HG2Gui Android application authority, default.
- [x] **Isolated package** — private package runtime without ambient elevated tools.
- [x] **ADB shell** — explicit ADB client operations, Wireless Debugging pair/connect, and confirmed shell commands.
- [x] **Root** — explicit `su` detection, confirmed test/root-shell operations.
- [x] headless callers cannot elevate through the authority surface.

This separation matters more than the individual commands. A package should never receive ADB/root merely because the host application happens to possess it.

Future:

- [ ] richer Android-device operation pills built on ADB shell (`pm`, `am`, `cmd`, `settings`, `dumpsys`, `logcat`, etc.) generated from capability-aware adapters rather than giant hand-written command lists;
- [ ] persistent but revocable ADB pairing state/status UI;
- [ ] elevation previews that show the exact command and authority boundary;
- [ ] per-package capability brokerage from isolation to host authority;
- [ ] optional “ask every time / allow once / deny” capability policies without granting ambient shell access;
- [ ] root-aware features only where root actually adds a truthful capability, never as a silent fallback.

## Connectivity

- [x] SSH saved presets and graphical host/user/port/key collection.
- [x] remote OS context reference trees.
- [x] ADB client authority model for Android device interaction.
- [x] loopback MCP server.

Future:

- [ ] live remote command/help discovery over an active SSH connection;
- [ ] remote package-manager adapters using the selected connection/context;
- [ ] remote filesystem picks that remain visually distinct from local paths.

## AI

- [x] natural-language command suggestion surface;
- [x] suggestions are composed for review, not auto-run;
- [x] optional explanation of command parts.

Future AI should understand HG2Gui's structured model rather than bypass it:

- [ ] return typed command intentions (package/file/host/choice/free text), not just strings;
- [ ] populate pill paths directly from suggestions;
- [ ] explain package/isolation/authority consequences before execution;
- [ ] consume isolation audit results to explain what a package actually changed;
- [ ] never receive an implicit permission to elevate merely because it proposed the command.

## The Guide

The Guide remains more than documentation. It is the product's cultural interface: real commands explained with Douglas-Adams-style literalism, animation, and visual metaphor.

The large Guide manuscript and animation production files under `docs/` are creative source material. They are intentionally allowed to say things like “a user runs `pwd`” without becoming normative descriptions of the current terminal UI. Runtime behavior belongs in the software documentation; Guide canon belongs in the Guide.

Future:

- [ ] continue Guide animation production using the established canonical packets/style rules;
- [ ] connect Guide entries more deeply to discovered commands/package metadata without turning the Guide into a second command launcher;
- [ ] surface runtime/package/isolation concepts as Guide entries once the behavior is stable enough to deserve a joke.

## Long-term product principle

A normal terminal gives every program whatever authority the shell already has and expects the human to understand strings.

HG2Gui should invert both assumptions:

1. **understand the values well enough that the human can pick them;**
2. **understand the authority well enough that software receives only what the human intended.**

The destination is not merely “Termux with buttons.” It is a terminal that knows what its choices mean.
