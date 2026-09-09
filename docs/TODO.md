# HG2Gui TODO

This is the active implementation roadmap. Keep it current with feature commits.

## Completed

- [x] Completion Bridge
  - [x] Define shell-agnostic completion candidate/request/provider model.
  - [x] Normalize Bash, Zsh, Fish, filesystem, package-manager, and command-protocol candidates into one semantic model.
  - [x] Store completion candidates per HG2Gui session.
  - [x] Add safe live Bash/Zsh/Fish completion adapters without executing the partially composed command.
  - [x] Add installed-package, Git branch, and known SSH host semantic completion providers.
  - [x] Feed semantic completion candidates into HG2Gui-native selection surfaces.
  - [x] Replace free-form typing with selectable values whenever a provider can enumerate valid choices.
  - [x] Add service and richer command-specific completion providers.
  - [x] Consume the declarative subset of native Bash/Zsh completion definitions without sourcing or executing completion scripts.

- [x] Deepen the Adaptive TUI Wrapper.
  - [x] Nested/modal menus.
  - [x] Checkbox and radio lists.
  - [x] Tabs.
  - [x] Scrolling lists.
  - [x] Tables.
  - [x] Selectable text/results.
  - [x] Confirmation dialogs.
  - [x] Progress/status regions.
  - [x] Multiple panes.
  - [x] Mouse-aware TUIs through terminal mouse-reporting state.
  - [x] Detect selection represented by color/style beyond inverse text.

- [x] Make Adaptive TUI interaction semantic rather than blind key-count emulation.
  - [x] Observe screen evolution after generated navigation actions.
  - [x] Reconcile dynamic/reordered lists before confirming selection.
  - [x] Fall back to RAW when state cannot be verified.

- [x] Expose an HG2Gui external API in the spirit of Termux:API, but capability-oriented rather than command-string-oriented.
  - [x] Execute commands.
  - [x] Package operations.
  - [x] File/folder picker with returned result.
  - [x] Notifications and foreground dialogs.
  - [x] Clipboard.
  - [x] Device information.
  - [x] Share/open actions.
  - [x] Package lifecycle actions.
  - [x] Explicit foreground-approved ADB/root authority requests.
  - [x] Signature-protected Android transport.

- [x] Shell selection and configuration.
  - [x] Choose Bash, Zsh, or Fish as the persistent session shell when installed.
  - [x] Persist the default shell choice.
  - [x] Keep full-screen PTY sessions on the same selected-shell contract.
  - [x] Treat Oh My Zsh, Powerlevel10k, Starship, and similar frameworks as presentation layers on their actual shell.

- [x] Structured prompt projection.
  - [x] CWD.
  - [x] Git branch and dirty state.
  - [x] Last exit status.
  - [x] User/host where meaningful.
  - [x] Make Git branch projection interactive through a native branch chooser.

- [x] Package isolation instrumentation foundation.
  - [x] Process tree and spawned subprocesses.
  - [x] Best-effort observed files opened/read/written/deleted through same-UID `/proc` sampling.
  - [x] Observed TCP/TCP6/UDP/UDP6 socket endpoints.
  - [x] ADB/root-like privilege-tool attempts.
  - [x] Before/after private-filesystem audit.

## In progress

- [ ] Unified package execution backends.
  - [x] `DIRECT_LINKER`: normal HG2Gui Termux execution through the HG2Gui exec preload.
  - [x] Inspect package-owned ELF program headers and keep dynamically linked ELF on the direct path.
  - [x] `PROOT_COMPAT`: reuse the same PRoot engine without a private root for ELF that lacks `PT_INTERP`.
  - [x] Automatically provision the `proot` package when compatibility execution requires it.
  - [x] `PROOT_ISOLATED`: isolated packages always use the private-root backend and never fall back outside it.
  - [x] Apply the same backend selection to interactive and headless package-owned command execution.
  - [ ] Confirm the combined direct/preload + compatibility + isolation head in CI and on-device package execution.
  - [ ] Expand compatibility classification only when a concrete unsupported ELF/runtime case is observed; do not turn arbitrary command failure into silent PRoot fallback.

- [ ] Surface isolation telemetry as a dedicated native HG2Gui audit view instead of only command output.

## Package/runtime follow-ups

- [ ] Broaden package-manager adapters only where installed managers expose reliable inventory/action semantics.
  - [x] dpkg/pkg.
  - [x] pip.
  - [x] pipx.
  - [x] npm.
  - [x] RubyGems.
  - [ ] Add additional managers when their own stable metadata/commands justify an adapter.
- [x] Improve dependency-resolution compatibility: Pre-Depends, alternatives, Provides, Conflicts/Breaks/Replaces, and version-aware relations.
- [x] Handle dpkg triggers, alternatives, and diversions through authoritative dpkg tooling and transaction rollback.
- [x] Make maintainer-script execution interpreter-aware and refuse unsupported interpreters rather than mis-running them as Bash.
- [x] Audit/repair relocated package symlinks and special control metadata for hard-coded Termux prefixes.
- [x] Add whole-plan transaction recovery/rollback for interrupted package operations.
- [x] Add repository mirror failover, architecture selection, signed `InRelease` verification, authenticated index SHA-256 verification, and verified package downloads.
- [x] Deliver dpkg-triggered maintainer scripts through an APK-native exec-exempt trampoline.
- [x] Deliver the explicit `postrm purge` phase while the package transaction is still rollback-capable.
- [ ] Continue native ELF compatibility only for verified Android runtime gaps; `PROOT_COMPAT` is the current fallback backend.

## Documentation

- [ ] Keep README, Architecture, Commands, User Guide, Vision, Design, agent handoff docs, and this TODO synchronized with implemented behavior.
- [ ] Continue removing inherited product directives that do not describe HG2Gui's current design.
- [x] Document the Guide as an active command-composition surface wherever command-entry surfaces are described.
- [ ] Finish the current documentation pass for Shell Adapter, Adaptive TUI Wrapper, Completion Bridge, external API, package lifecycle/isolation, execution backends, and execution authority.
