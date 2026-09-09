# HG2Gui TODO

This is the active implementation roadmap. Keep it current with feature commits.

## In progress

- [ ] Completion Bridge
  - [x] Define shell-agnostic completion candidate/request/provider model.
  - [x] Normalize Bash, Zsh, Fish, filesystem, package-manager, and command-protocol candidates into one semantic model.
  - [x] Store completion candidates per HG2Gui session.
  - [x] Add safe live Bash/Zsh/Fish completion adapters without executing the partially composed command.
  - [x] Add installed-package, Git branch, and known SSH host semantic completion providers.
  - [x] Feed semantic completion candidates into HG2Gui-native selection surfaces.
  - [x] Replace free-form typing with selectable values whenever a provider can enumerate valid choices.
  - [x] Add service and richer command-specific completion providers.
  - [ ] Consume native Bash/Zsh completion definitions beyond the conservative command/file fallback where it can be done safely.

## Next

- [ ] Deepen the Adaptive TUI Wrapper.
  - [ ] Nested/modal menus.
  - [ ] Checkbox and radio lists.
  - [ ] Tabs.
  - [ ] Scrolling lists.
  - [ ] Tables.
  - [ ] Selectable text/results.
  - [ ] Confirmation dialogs.
  - [ ] Progress/status regions.
  - [ ] Multiple panes.
  - [ ] Mouse-aware TUIs.
  - [ ] Detect selection represented by color/style beyond inverse text.

- [ ] Make Adaptive TUI interaction semantic rather than blind key-count emulation.
  - [ ] Observe screen evolution after each generated navigation action.
  - [ ] Reconcile dynamic/reordered lists before confirming selection.
  - [ ] Fall back to RAW when state cannot be verified.

- [ ] Expose an HG2Gui external API in the spirit of Termux:API, but capability-oriented rather than command-string-oriented.
  - [ ] Execute commands.
  - [ ] Package operations.
  - [ ] File/folder picker.
  - [ ] Notifications and dialogs.
  - [ ] Clipboard.
  - [ ] Device information.
  - [ ] Share/open actions.
  - [ ] Package lifecycle actions.
  - [ ] Explicit authority requests where permitted.
  - [ ] Define authenticated local/broadcast/intent transport.

- [ ] Shell selection and configuration.
  - [ ] Choose Bash, Zsh, Fish, or other installed shells as the persistent session shell.
  - [ ] Persist per-session/default shell choice.
  - [ ] Treat Oh My Zsh, Powerlevel10k, Starship, and similar frameworks as presentation layers on their actual shell.

- [ ] Structured prompt projection.
  - [ ] CWD.
  - [ ] Git branch and dirty state.
  - [ ] Last exit status.
  - [ ] User/host where meaningful.
  - [ ] Make structured prompt segments interactive where useful, such as tapping a branch to select another branch.

- [ ] Package isolation instrumentation.
  - [ ] Process tree.
  - [ ] Files opened/read/written/deleted.
  - [ ] Network destinations.
  - [ ] Spawned subprocesses.
  - [ ] Privilege/authority requests.
  - [ ] Surface the audit trail in HG2Gui.

## Package/runtime follow-ups

- [ ] Broaden package-manager adapters beyond dpkg/pkg, pip, npm, and RubyGems where installed managers expose reliable inventory/action semantics.
- [ ] Improve dependency-resolution compatibility: Pre-Depends, alternatives, Provides, Conflicts/Breaks/Replaces.
- [ ] Handle triggers, alternatives, and diversions.
- [ ] Make maintainer-script execution interpreter-aware instead of always Bash when safely possible.
- [ ] Audit relocated package symlinks and special control metadata for hard-coded Termux prefixes.
- [ ] Add transaction recovery/rollback for interrupted package operations.
- [ ] Add repository mirror failover, architecture selection, and repository signature/authentication verification.
- [ ] Continue native ELF compatibility work where Android executable/runtime constraints require repair.

## Documentation

- [ ] Keep README, Architecture, Commands, User Guide, Vision, Design, agent handoff docs, and this TODO synchronized with implemented behavior.
- [ ] Continue removing inherited product directives that do not describe HG2Gui's current design.
- [ ] Document the Guide as an active command-composition surface wherever command-entry surfaces are described.
- [ ] Document Shell Adapter, Adaptive TUI Wrapper, Completion Bridge, package lifecycle/isolation, and execution authority as they become production-ready.
