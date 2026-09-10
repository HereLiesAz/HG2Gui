# HG2Gui TODO

This is the active implementation roadmap. Keep it current with feature commits.

## Completed

- [x] Completion Bridge
  - [x] Shell-agnostic completion model.
  - [x] Bash, Zsh, Fish, filesystem, package, Git, SSH, service, and supported command-specific providers.
  - [x] Per-session semantic completion state.
  - [x] Native selection surface and current-token replacement.
  - [x] Declarative shell-completion ingestion where it can be read as inert metadata without sourcing arbitrary scripts.

- [x] Guide command entry
  - [x] Real command-backed Guide entries.
  - [x] Tappable command headings that return to command composition without auto-running.
  - [x] Unambiguous command references inside prose are tappable.
  - [x] Live command-tree availability, caps, and options are shown while reading.
  - [x] Canonical package lifecycle, isolation, and authority concepts are part of the Guide catalog.

- [x] Adaptive TUI Wrapper
  - [x] Nested/modal menus, checkbox/radio lists, tabs, scrolling lists, tables, selectable results, prompts, progress/status, and multiple panes.
  - [x] Mouse-awareness from terminal mouse-tracking state and terminal-emulator mouse event projection.
  - [x] Selection detection beyond inverse-video alone.
  - [x] Semantic navigation reconciled against live terminal state.
  - [x] Diff, editor, log, result, and other structured regions when semantics are strong enough.
  - [x] Cooperative adapter/profile metadata through the protected API.
  - [x] RAW fallback whenever state cannot be verified.

- [x] External Android API
  - [x] Signature-protected transport.
  - [x] Versioned capability discovery/schema.
  - [x] Command execution, package operations, lifecycle, picker results, notifications/dialogs, clipboard, device info, share/open, and foreground-approved authority requests.
  - [x] Typed result envelopes.
  - [x] Caller-visible bounded audit history.
  - [x] Short-lived cooperative shell/TUI metadata publish/read/list/clear channels.

- [x] Shell selection and presentation
  - [x] Persistent Bash/Zsh/Fish default-shell choice when installed.
  - [x] Same selected-shell contract for full-screen PTY sessions.
  - [x] Oh My Zsh, Powerlevel10k, Starship, Pure, and similar frameworks treated as presentation layers.
  - [x] Structured cwd, Git branch/dirty state, exit status, and user/host projection.
  - [x] Native Git branch chooser.
  - [x] Cooperative shell metadata can supply fresh semantic prompt state without replacing the PTY as source of truth.

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

- [x] Package lifecycle and impact
  - [x] Disable / Enable, Reset, Isolate / Release isolation.
  - [x] Installed dependency closure and reverse removal-impact views for dpkg packages.
  - [x] Exact pre-reset path/byte impact preview.
  - [x] Bounded restore points with restore/delete controls and symlink refusal.
  - [x] Created/modified runtime provenance for non-isolated package writes.

- [x] Unified package execution backends
  - [x] `DIRECT_LINKER` normal HG2Gui Termux execution.
  - [x] Package-owned ELF program-header inspection.
  - [x] `PROOT_COMPAT` compatibility path for specifically classified ELF/runtime cases.
  - [x] Automatic PRoot provisioning when a compatibility/isolation backend requires it.
  - [x] `PROOT_ISOLATED` private-root execution with no silent fallback.
  - [x] Same backend selection policy for interactive and headless package-owned execution.
  - [x] Compatibility classification remains explicit; arbitrary command failure is not a silent PRoot trigger.

- [x] Package isolation and observability
  - [x] Private PRoot root and private HOME/XDG/cache/state/tmp.
  - [x] Fail-closed startup when isolation cannot be established.
  - [x] ADB/root-like tools stripped or masked inside the guest.
  - [x] dpkg isolation seeding from target + dependency closure + required runtime packages when file ownership is trustworthy.
  - [x] Safe full-prefix seed fallback when selective planning is unavailable or exceeds its bound.
  - [x] Explicit host-read/host-write capability policy with no ambient writable host bind exposed to isolated packages.
  - [x] Per-package ASK / ALLOW ONCE / DENY capability policies; one-shot grants are memory-only and consumed once.
  - [x] Interactive capability-request broker that fails closed for DENY/headless requests.
  - [x] Network launch gate: isolated execution refuses to start without a one-run network grant because unprivileged PRoot cannot truthfully provide an offline Android network namespace.
  - [x] Versioned machine-readable sandbox-definition JSON.
  - [x] Process tree and spawned subprocess sampling.
  - [x] Best-effort observed files/open modes through same-UID `/proc` sampling.
  - [x] Observed TCP/TCP6/UDP/UDP6 endpoints.
  - [x] ADB/root-like privilege-tool attempts.
  - [x] Before/after private-root filesystem audit.
  - [x] Native **Settings → Isolation audit** view for persisted latest completed-run telemetry and sandbox diff.
  - [x] Optional redacted AI interpretation of an isolation audit without granting AI execution authority.

The sampler remains best-effort same-UID observation, not kernel audit or syscall-complete interception.

- [x] Execution authority
  - [x] App authority and isolated-package boundary.
  - [x] Explicit ADB and root authority with foreground confirmation and no headless elevation.
  - [x] Capability-oriented ADB operations for `pm`, `am`, `cmd`, `settings`, `dumpsys`, and `logcat`.
  - [x] Remembered successful ADB endpoints with reconnect and disconnect/revoke controls; pairing secrets are never stored.
  - [x] Per-package capability-policy surface under Authority.

- [x] Connectivity and AI
  - [x] SSH presets and native host/user/port/key collection.
  - [x] Known SSH hosts feeding completion.
  - [x] Remote OS reference contexts.
  - [x] Target-scoped SSH discovery for remote commands, live `--help`, package manager identity, and package inventory.
  - [x] Remote commands/packages/filesystem entries are visually and textually labeled as remote and remain scoped to the selected SSH preset.
  - [x] Natural-language suggestions remain review-only.
  - [x] AI command suggestions are parsed into typed intentions with local package/isolation/authority consequence summaries before USE.
  - [x] AI isolation-audit interpretation is explicit opt-in, redacted, and does not add execution authority.

- [x] Settings usability
  - [x] Settings vertically scrolls when content exceeds the viewport.
  - [x] Isolation Audit is reachable as a dedicated native screen.

- [x] Android ICU regex initialization crash
  - [x] Escape closing object/array delimiters in package metadata regexes so `PackageLifecycleStore` does not fail during static initialization on Android ICU.

## Device verification still required

- [ ] Confirm the latest combined package execution head on a physical Android device:
  - [ ] ordinary command execution reaches `TerminalEngine` without `PackageLifecycleStore` initialization failure;
  - [ ] `DIRECT_LINKER` package-owned executable;
  - [ ] a concrete `PROOT_COMPAT` case when available;
  - [ ] `PROOT_ISOLATED` package execution;
  - [ ] selective dpkg dependency-closure seed succeeds for a real installed package;
  - [ ] network launch gate blocks by default and a one-run grant is consumed exactly once;
  - [ ] ASK / DENY / ALLOW ONCE capability controls behave correctly on-device;
  - [ ] remembered ADB endpoint reconnect/disconnect flow;
  - [ ] target-scoped SSH discovery against a real reachable preset;
  - [ ] Settings scrolling at constrained height / large text scale;
  - [ ] Settings → Isolation Audit after an isolated run;
  - [ ] AI audit interpretation redaction/display with a configured API key.

CI success is recorded separately from this device-only checklist; a green JVM/Android build does not prove Android ICU/linker/PRoot/ADB/SSH runtime behavior.

## Evidence-driven future work

These are deliberately not incomplete promises. Add a concrete item only when the target runtime supplies evidence and an enforceable primitive.

- Additional package-manager adapters when a manager exposes stable inventory/action semantics.
- Additional native ELF/PRoot compatibility classifications only for observed Android runtime gaps.
- True per-process offline network confinement if Android/runtime support provides a truthful enforceable mechanism; the current policy is a fail-closed launch gate, not a fake network namespace.
- Deeper isolation observation only where Android permits stronger guarantees than the current sampled `/proc` view.
- Root-specific features only where root adds a concrete capability rather than merely a broader shell.
- Broader-trust per-caller API grants only if HG2Gui later accepts callers beyond same-signature applications.
