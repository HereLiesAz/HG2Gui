# HG2Gui TODO

This is the active implementation roadmap. Keep it current with feature commits.

## Completed

- [x] Completion Bridge
  - [x] Shell-agnostic completion model.
  - [x] Bash, Zsh, Fish, filesystem, package, Git, SSH, service, and supported command-specific providers.
  - [x] Per-session semantic completion state.
  - [x] Native selection surface and current-token replacement.
  - [x] Declarative shell-completion ingestion where it can be read without sourcing arbitrary scripts.

- [x] Adaptive TUI Wrapper
  - [x] Nested/modal menus.
  - [x] Checkbox/radio lists.
  - [x] Tabs.
  - [x] Scrolling lists.
  - [x] Tables/selectable results.
  - [x] Confirmation and text/password prompts.
  - [x] Progress/status regions.
  - [x] Multiple panes.
  - [x] Mouse-awareness from terminal mouse-tracking state.
  - [x] Selection detection beyond inverse-video alone.
  - [x] Semantic navigation reconciled against live terminal state.
  - [x] RAW fallback when state cannot be verified.

- [x] External Android API
  - [x] Signature-protected transport.
  - [x] Command execution.
  - [x] Package operations.
  - [x] File/folder picker with returned result.
  - [x] Notifications and foreground dialogs.
  - [x] Clipboard.
  - [x] Device information.
  - [x] Share/open actions.
  - [x] Package lifecycle actions.
  - [x] Foreground-approved ADB/root requests.

- [x] Shell selection and presentation
  - [x] Persistent Bash/Zsh/Fish default-shell choice when installed.
  - [x] Same selected-shell contract for full-screen PTY sessions.
  - [x] Oh My Zsh, Powerlevel10k, Starship, Pure, and similar frameworks treated as presentation layers.
  - [x] Structured cwd, Git branch/dirty state, exit status, and user/host projection.
  - [x] Native Git branch chooser.

- [x] Package isolation instrumentation foundation
  - [x] Process tree and spawned subprocesses.
  - [x] Best-effort observed files/open modes through same-UID `/proc` sampling.
  - [x] Observed TCP/TCP6/UDP/UDP6 endpoints.
  - [x] ADB/root-like privilege-tool attempts.
  - [x] Before/after private-root filesystem audit.
  - [x] Native **Settings → Isolation audit** view for latest isolated-package telemetry and sandbox state.

- [x] Unified package execution backends
  - [x] `DIRECT_LINKER` normal HG2Gui Termux execution.
  - [x] Package-owned ELF program-header inspection.
  - [x] `PROOT_COMPAT` compatibility path for specifically classified ELF/runtime cases.
  - [x] Automatic PRoot provisioning when a compatibility/isolation backend requires it.
  - [x] `PROOT_ISOLATED` private-root execution with no silent fallback.
  - [x] Same backend selection policy for interactive and headless package-owned execution.
  - [x] Compatibility classification remains explicit; arbitrary command failure is not a silent PRoot trigger.

- [x] Package/runtime transaction hardening
  - [x] dpkg/pkg, pip, pipx, npm, RubyGems inventory adapters.
  - [x] Pre-Depends, alternatives, Provides, Conflicts/Breaks/Replaces, and version-aware dependency relations.
  - [x] dpkg triggers, alternatives, and diversions with transaction rollback.
  - [x] Interpreter-aware maintainer-script execution.
  - [x] Relocated symlink/control metadata repair.
  - [x] Whole-plan interrupted-operation recovery/rollback.
  - [x] Mirror failover and architecture selection.
  - [x] Signed `InRelease`, authenticated index SHA-256, and verified package downloads.
  - [x] APK-native exec-exempt maintainer-script trampoline.
  - [x] Explicit rollback-capable `postrm purge` phase.

- [x] Settings usability
  - [x] Settings vertically scrolls when content exceeds the viewport.
  - [x] Isolation Audit is reachable as a dedicated native screen.

- [x] Documentation synchronization
  - [x] README synchronized with Shell Adapter, Completion Bridge, Adaptive TUI Wrapper, external API, package execution backends, lifecycle/isolation, authority, Guide command entry, and native surfaces.
  - [x] `ARCHITECTURE.md` synchronized.
  - [x] `HG2GUI_ARCHITECTURE.md` synchronized.
  - [x] `COMMANDS.md` synchronized.
  - [x] `USER_GUIDE.md` synchronized.
  - [x] `VISION.md` synchronized.
  - [x] `DESIGN.md` removes the global icon prohibition and universal pill/single-surface doctrine.
  - [x] `CONTRIBUTING.md` synchronized with current security/runtime/UI rules.
  - [x] Guide documented as an active command-entry surface.

- [x] Android ICU regex initialization crash
  - [x] Escape closing object/array delimiters in package metadata regexes so `PackageLifecycleStore` does not fail during static initialization on Android ICU.

## Device verification still required

- [ ] Confirm the latest combined package execution head on a physical Android device after the ICU regex fix:
  - [ ] ordinary command execution reaches `TerminalEngine` without `PackageLifecycleStore` initialization failure;
  - [ ] `DIRECT_LINKER` package-owned executable;
  - [ ] a concrete `PROOT_COMPAT` case when available;
  - [ ] `PROOT_ISOLATED` package execution;
  - [ ] Settings scrolling at constrained height / large text scale;
  - [ ] Settings → Isolation Audit after an isolated run.

CI success is recorded separately from this device-only checklist; a green JVM/Android build does not prove Android ICU/linker/PRoot runtime behavior.

## Evidence-driven future work

These are not incomplete promises for the current implementation; add them to the active checklist only when a concrete supported case justifies the work.

- Additional package-manager adapters when a manager exposes stable inventory/action semantics.
- Additional native ELF/PRoot compatibility classifications only for observed Android runtime gaps.
- Stronger isolation enforcement/observation where Android permits truthful guarantees, including network policy and dependency-closure seeding.
- Cooperative shell/TUI metadata protocols and richer typed API results.