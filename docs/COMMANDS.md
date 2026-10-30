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

Installs or repairs HG2Gui's pinned Termux-derived userspace. The archive is checked against a SHA-256 pinned in the build, extracted into `usr.staging`, verified, and swapped over `usr` atomically; the old prefix is kept as `usr.previous` until the swap succeeds, so a failed bootstrap (offline, checksum mismatch, full storage) leaves an existing installation as it was.

A successful bootstrap over an existing prefix **replaces** it: packages installed on top of the previous bootstrap are not carried over (your home directory is untouched). Refused (exit 126) for the external API, MCP and installers.

### `download ...` / `hg2download ...`

HG2Gui downloader entry points with resume/reuse support where possible.

- `download [--allow-http] <url> [filename]`. Only `https://` URLs are accepted unless `--allow-http` is given.
- A partial file is resumed only when the server's `ETag` / `Last-Modified` validator (sent as `If-Range`) still matches and a `206` reply's `Content-Range` starts exactly where the file ends; otherwise it starts over.
- "Already downloaded" is reported only when the validator and length recorded when the file was saved match.

These are execution-control verbs handled by `TerminalEngine` before ordinary shell dispatch.

## Package transactions

### `pkg ...` / `hg2pkg ...`

HG2Gui package-manager entry points: `install` (`--reinstall` re-plans an installed package), `update`, `upgrade [package...]`, `remove`, `purge`, `search`, `show`, `list-installed`, `clean`.

- `pkg upgrade` upgrades every installed package that has a newer version; `pkg upgrade <name>` upgrades just that package and fails if it is not installed. This is what Update runs for a Termux/`pkg` package, since `pkg install <name>` does nothing for an installed package.
- Unknown `-`/`--` options are rejected rather than ignored. Accepted: `-y`, `--yes`, `--assume-yes`, `-q`, `-qq`, `--quiet`, `--no-install-recommends`, and for `install` only `--reinstall` (alias `--force-requested`). Dry-run flags (`-s`, `--simulate`, `--dry-run`, …) are refused because ignoring them would turn a simulation into a real change.
- One package operation runs at a time across all tabs and callers.

Top-level mutating `apt`/`apt-get` operations are translated into this transaction layer so Android compatibility handling cannot be bypassed by upstream apt invoking dpkg directly:

- translated: `install`, `reinstall` (becomes `pkg install --reinstall`), `remove`, `purge`, `update`, `upgrade`/`dist-upgrade`/`full-upgrade` (all become `pkg upgrade`), `clean`/`autoclean`;
- read-only operations (`search`, `show`, `list`, `policy`, …) go to the real apt unchanged;
- refused with an explanation and nothing run: dry-run flags, `autoremove`/`autopurge`, `build-dep`, `satisfy`, `aptitude` mutations, operands that are not plain package names (versions, releases, paths), and options other than the ones listed above.

This applies to `pkg`/`apt` typed as their own command or as one step of a `;` / `&&` / `||` list. A mutating one behind `sudo`/`env`/`time`, in a pipe, `bash -c`, `$( )` or a control structure is refused (exit 2) because it cannot be journaled. A direct `dpkg` call, and a command word only known at run time (`$tool`, a function, a sourced script), are not covered by the transaction layer.

An unterminated quote, `$(` or backtick anywhere in a line is refused before it reaches the shell (exit 2).

Supported transaction work includes repository verification, download reuse/resume, relocation, metadata repair, dependency planning, maintainer-script handling, triggers, alternatives/diversions, rollback/recovery, and native execution compatibility.

Repository trust: the index is accepted only if its signed `InRelease` verifies against a Termux keyring that is pinned in the build or was recorded after HG2Gui itself installed `termux-keyring`; an `InRelease` older than the last accepted one, or one with no `Valid-Until` older than 180 days, is rejected. An upstream key rotation therefore needs an HG2Gui update to add the new pin.

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
- `snapshot` — create a bounded restore point of HG2Gui-managed runtime state;
- `restore <manager> <package> <id>` — replace managed state from a restore point (confirms);
- `snapshot-delete <manager> <package> <id>` — delete a restore point (confirms);
- `capability <manager> <package> [capability [ask|deny|allow-once]]` — show or set the capability policy (see below);
- `capability-request <manager> <package> <capability>` — ask for a one-shot grant of a capability (see below);
- `remove`
- `purge`

Background callers (the external API, MCP, installers) may run only `info` and `snapshot`. `enable` is additionally allowed for an API call that arrives while HG2Gui is on screen. `update`, `disable`, `isolate`, `release`, `reset`, `restore`, `snapshot-delete`, `remove`, `purge`, `capability` and `capability-request` are refused by the engine itself for those callers with exit code 126 ("run it from a terminal tab"); the same engine also refuses `bootstrap`, `vfs mount` and every `hg2auth` subcommand except `status` (see Android-facing built-ins for the device-affecting built-ins). In a terminal tab the destructive actions ask for confirmation.

The owning manager currently comes from adapters for:

- `pkg` / dpkg
- `pip`
- `pipx`
- `npm`
- `gem`

### Disable / Enable

Disable keeps the package installed while HG2Gui blocks owned commands. Enable removes that HG2Gui policy flag.

### Isolate / Release

Isolation selects the private-root backend. If the isolation engine is unavailable, an isolated package's launch fails with an explanation instead of running unisolated.

An isolated package runs only as its own command or as one step of a `;` / `&&` / `||` list. In a pipe, `bash -c`, `$( )`, a control structure or any other wrapper it is refused (exit 126), because HG2Gui could not keep it inside its private root there. Isolation is state separation with best-effort observation, not a verified security sandbox: PRoot runs as the same Android user as HG2Gui and network access is not confined.

### Reset

Reset keeps the installed payload while clearing package runtime state HG2Gui can safely attribute. For an isolated package this removes the private root so the next run reseeds it.

### Update / Remove / Purge

These actions delegate through the owning manager where meaningful (for a `pkg` package, Update runs `pkg upgrade <name>`). Destructive lifecycle actions require confirmation in interactive use.

### Capability policy

Each installed package has a policy for five capabilities: `adb`, `root`, `network`, `host-read`, `host-write`. The mode is one of:

- **ASK** (default) — not granted until a human answers; `capability-request` prompts "Allow this request once?";
- **ALLOW ONCE** (`allow-once`) — a single grant, held only in memory and consumed by exactly one decision, so it never survives an app restart or becomes standing authority;
- **DENY** — always refused.

Only ASK and DENY are stored persistently.

**Only `network` is enforced.** It is the one capability HG2Gui can really withhold (the isolated-launch gate below). `adb`, `root`, `host-read` and `host-write` have no enforcement point: HG2Gui cannot hand them to a package process (they are masked inside isolation and never inherited), so a `capability-request` for any of them is refused with exit code 1 whatever the policy says. Run a one-off elevated command yourself with `hg2auth` instead. Examples:

~~~text
hg2package capability pkg git
hg2package capability pkg git network allow-once
hg2package capability-request pkg git network
~~~

**Isolated-run network grant.** A PRoot-isolated package cannot be given a truthful offline network namespace on Android, so launching it requires a `network` grant: with the policy at ASK or DENY the launch fails closed with an explanation. Arm it with **Authority → Package policies → (package) → network → Allow once**, then run the package again; the grant is consumed by that launch.

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

ADB shell execution requires foreground confirmation. Android pairing/connect authorization still applies. From the external API, MCP and installers every `hg2auth` subcommand except `status` is refused (exit 126); the API's separate authority request goes through its own approval dialog.

### Root

- `hg2auth root test`
- `hg2auth root shell <command...>`

Root execution requires foreground confirmation and remains subject to the device's `su` provider/root manager.

### Authority invariant

HG2Gui never hands ADB or root to a normal command, an isolated package or a headless caller. Inside an isolation `adb`, `su` and similar tools are masked or removed. What HG2Gui does not do is intercept an `su` or `adb` binary that you run yourself from the shell: it behaves as it would in any terminal, subject to the device's root manager.

## Interactive prompts

When a process is already running:

- yes/no can become a confirmation dialog;
- finite numbered/bracketed choices can become native selections;
- passwords/passphrases use masked input;
- open-ended prompts use text input.

## Shell Adapter

Bash, Zsh, and Fish sessions can expose structured presentation state without changing the underlying shell.

Recognized presentation layers include Oh My Zsh, Powerlevel10k, Starship, Pure, and custom prompts. They are detected for full-screen interactive shell sessions, where there is a prompt to read.

Structured state can include:

- cwd;
- Git branch/dirty state;
- last exit status;
- user/host where meaningful.

In the ordinary command line there is no prompt to parse, so HG2Gui shows a status line under the tab strip, refreshed after each command: the working directory, `GIT <branch>` (with a dot when there are uncommitted changes), user@host when known, and `EXIT <n>` for a non-zero status. It is display only; Git branch names are offered by the Completion Bridge after `git checkout`, `switch`, `branch`, `merge` and `rebase`.

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

Selecting a completion replaces the active partial token and does not execute the command. The inserted text is the candidate's value quoted as one inert shell word, so a name containing spaces or shell characters cannot turn into extra arguments.

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

A real command encountered while reading the Guide can be tapped and appended to the active terminal composition flow.

The Guide reader stays open and shows an "added to input" confirmation, so reading and command composition can interleave. Guide command taps do not auto-execute. (Picking a finished command from the pill tree on the Guide's index screen returns to the terminal.)

## Files and path picking

File/directory operands can invoke the graphical picker. Files is also a dedicated native browsing surface rather than merely a command category.

## `remote`

`remote <discover|status|help|packages> <preset> [command]` inspects an SSH preset's host over SSH (OS, shell, package manager, installed commands and packages) and caches the result per preset.

- `remote discover <preset>` — query the host and cache the inventory;
- `remote status <preset>` — show the cached inventory;
- `remote help <preset> <command>` — fetch help for a command on the remote host;
- `remote packages <preset>` — list the remote packages.

It needs a saved SSH preset and an SSH client; it is available headless.

## Isolation Audit

**Settings → Isolation audit** opens the native audit screen for isolated packages. It reports latest observed process/file/network/authority telemetry and current sandbox state.

The observation layer is best-effort same-UID `/proc` sampling (from a watcher outside the guest, which writes its log outside the guest root) plus private-root before/after filesystem metadata. It is not complete kernel syscall tracing and is not tamper-proof.

**ASK AI · SEND SUMMARY** (needs a saved Anthropic API key and a confirmation that lists what is sent) sends: the package name and version; the sandbox file count and size; how many files were created, modified and deleted; the base file names (never folders) of up to 24 in each group, leaving out any whose path mentions a password, secret, token, key or similar; a count of observed events per kind; and the protocol and remote port (never the address) of up to 24 network connections. Full paths, command lines and addresses stay on the device.

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
- foreground-approved ADB/root authority requests;
- `CAPABILITIES` (`com.hereliesaz.hg2gui.api.CAPABILITIES`) — returns the machine-readable schema of every action with its required/optional extras and whether it needs foreground approval;
- `AUDIT_HISTORY` (`com.hereliesaz.hg2gui.api.AUDIT_HISTORY`) — returns the app-private audit log of API requests: up to 200 rows, each a `request`, `decision` (what the person chose in an approval, dialog or picker) or `result` row sharing a request id. Commands are recorded as a digest and length, not verbatim; an approved root/ADB command is kept as a bounded preview. A row that cannot be written is lost (logged, never reported to the caller);
- metadata channels (`METADATA_PUBLISH`, `METADATA_READ`, `METADATA_LIST`, `METADATA_CLEAR`).

**Metadata channels** let a cooperating app publish short-lived descriptions that HG2Gui can use to understand its own screen or output. A channel name is 1-96 characters of `A-Za-z0-9._:/-`; a payload is at most 128 KiB; the lifetime is 1 s to 5 min (default 30 s); at most 64 channels are held, in memory only, and the oldest is evicted first. Terminal state stays authoritative once a producer stops refreshing.

API command execution runs on a headless engine under the API execution policy and replies with the output and exit code. Only `info` and `snapshot` lifecycle operations run from the background; `enable` needs HG2Gui in the foreground. A command line that mentions `hg2auth`, `bootstrap`, `vfs mount` or any other `hg2package` action is rejected by the receiver before it reaches the engine (deliberately over-broad); the engine denies the same set itself with exit code 126, and also refuses the device-affecting built-ins (below). Long commands run in a foreground service; if Android refuses to start it, package operations are rejected and other commands get only the short broadcast time budget.

The ADB/root authority request opens an approval dialog that names the requesting app and shows the exact command (up to 8,192 characters); RUN runs that one string, and CANCEL, back, tapping away or leaving the screen answers the caller with a denial.

The API is capability-oriented and does not make background integration synonymous with elevation.

## Android-facing built-ins

`Builtins.kt` currently contains explicit bridges for Android-facing capabilities including Wi-Fi/Bluetooth/settings controls, flashlight, volume, brightness, calls/contacts, VFS, calculator, and editor entry.

These are current implementation entry points, not a requirement that future Android capabilities must always become shell verbs. Dedicated native/API capability surfaces are equally valid where they better fit the task.

For external API, MCP and installer callers, `call`, `contacts`, `brightness`, `wifi`, `bluetooth`, `airplane`, `flash`, `edit` and `volume set|profile` are refused (exit 126) unless a person answers yes to that exact line. None of those callers can currently be asked, so in practice they are refused; `volume get`, `calc` and the sandboxed `vfs` verbs run. `vfs mount` is refused outright for them.

## SSH, Workflows, AI, Store, MCP

- **SSH** — presets and completion-assisted host/key composition.
- **Workflows** — command templates assembled for review.
- **AI** — suggested commands returned to the user for review.
- **Store** — `.azp` packages, separate from shell package-manager lifecycle. Installing a skill asks for confirmation first, because a skill's text becomes part of the AI chat's instructions. Only packages whose signer key is in the registry's published set (trust `TRUSTED`) are read into the AI prompt: an unsigned, self-signed-only, unverifiable or invalid skill still installs but is not used. A script's `apt` dependencies are installed only after you confirm the list.
- **MCP** — explicit-start loopback service on `127.0.0.1` (default port 4827).
  - Each connection must present the pairing token within 10 seconds; the token is regenerated on every start and kept in memory only. Up to four clients are served at once.
  - `vfs.*` tools work on the app-private sandbox, which is the Termux `$HOME`; they never follow the Files screen into device storage. Reads and listings refuse hidden top-level entries and any `.ssh` directory; writes, moves, copies, deletes and mkdir refuse the same plus the root itself.
  - `shell.exec` is off unless shell execution is enabled in the MCP screen. It runs in a session separate from your tabs, one command at a time, and returns the full output (truncated at 256 K characters, and said so) followed by an `exit code: N` line. A non-zero exit code marks the result `isError`. It runs under the MCP execution policy: it cannot elevate through ADB/root and the engine refuses the commands listed under `hg2package` and Android-facing built-ins above.