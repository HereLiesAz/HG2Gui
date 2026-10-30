# User Guide

## Getting started

HG2Gui is a touch-first Android command-line environment built on a real Termux-derived runtime.

You are not restricted to one input style. Depending on what you are doing, HG2Gui can use command composition, semantic completion, Guide links, graphical file selection, shell prompt projections, adaptive TUI controls, or raw terminal input.

On first use, HG2Gui installs its pinned runtime into app-private storage. Package/bootstrap progress appears in the active output record.

While a command is running in a tab (or a full-screen program is open), HG2Gui keeps itself alive with a foreground service and a wake lock so that a long `git clone`, build or `pkg install` is not frozen when you switch apps. The wake lock is released when the command ends, or after six hours. This is best-effort: if Android refuses to start the service because HG2Gui is already in the background, the command runs without that protection.

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
- completion candidates such as Git branch names (offered after `git checkout`, `switch`, `branch`, `merge` and `rebase`).

Ordinary command composition does not auto-run. Press **RUN** when you want the composed command executed.

### Completion Bridge

While composing a command, HG2Gui can surface candidates from Bash, Zsh, Fish, files, installed packages, Git branches, SSH hosts, services, and supported command-specific completion protocols.

Choosing a completion replaces only the current partial token. It does not execute the command or write a fake history entry.

## The Guide

The Guide is both documentation and an active command-entry surface.

When a Guide entry shows a real command, tap the command to append it to the active terminal session's input for review. The Guide stays open and shows an "added to input" confirmation, so you can keep reading and composing. Tapping a Guide command does not auto-run it.

## Interactive shell applications

### Shell presentation

For normal interactive shells, HG2Gui can project:

- cwd;
- Git branch and dirty state;
- last exit status;
- user/host where meaningful.

The raw prompt remains available. Prompt themes are recognized in full-screen interactive shell sessions, where there is a prompt to read.

In the ordinary command line the same status appears under the tab strip after each command: the working directory, `GIT <branch>` (with a dot when there are uncommitted changes), user@host when known, and `EXIT <n>` for a non-zero status. It is display only; to switch branches, compose `git checkout` (or `switch`) and pick a branch from the completions.

The key row under the command line has sticky **CTRL** and **ALT**: tap one, then type a letter. CTRL supports C (stop the running command, or clear the line), D, H, J, N, P, U and W; ALT supports `.` (last argument of the previous command) and U/L/C (case of the last word). **ESC** cancels the current pick; **TAB** completes. Long-press a session tab to rename it.

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

This covers `pkg`/`apt`/`apt-get` typed as their own command or as one step of a `;` / `&&` / `||` list. Some things are refused instead of being run differently from what you typed: dry-run flags (`-s`, `--dry-run`), `apt autoremove`, options HG2Gui's manager does not implement, and a mutating `pkg`/`apt` placed behind `sudo`/`env`, in a pipe, in `bash -c` or in a control structure. `apt reinstall <name>` runs as `pkg install --reinstall <name>`. A direct `dpkg` command that would install, remove, purge, configure or unpack (`dpkg -i`, `-r`, `-P`, `--configure`, `--force-*`, …) is refused with a message pointing at `pkg install` / `pkg remove`, because it would bypass the transaction layer; read-only queries such as `dpkg -l`, `-L`, `-S` and `-s` work as usual. Calling `dpkg` by its absolute path, `dpkg-divert`/`dpkg-trigger`/`update-alternatives`, and a real `apt` run inside a script are not guarded.

A line with an unterminated quote is refused before it reaches the shell.

`bootstrap` re-installs the base runtime into a staging area and swaps it in only after it checks out, so a failed attempt leaves your current installation alone. A bootstrap that succeeds over an existing installation replaces it: packages you installed on top are not carried over.

`download` accepts only `https://` addresses; add `--allow-http` to fetch a plain `http://` one.

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

A disabled package's commands are refused when you type them (including after `&&`, `;`, `|`, behind `env`/`sudo` and inside `bash -c`) and also when they are reached indirectly, through `$tool`, `$(which tool)`, a shell function, an alias or a script, because every shell HG2Gui starts has a small refusal script for each disabled command first on its `PATH`. The refusal reads `<command> is disabled (hg2package enable <manager> <package>)` and exits 126. It does not stop a command called by its full path (`$PREFIX/bin/tool`).

### Reset

Reset retains the installed package payload while clearing runtime state HG2Gui can safely attribute to it. For isolated packages, Reset removes the private root so the next run reseeds it.

### Package execution backends

HG2Gui chooses among three package execution paths:

- **DIRECT_LINKER** — normal Android-compatible Termux execution;
- **PROOT_COMPAT** — compatibility execution for specifically classified ELF/runtime cases;
- **PROOT_ISOLATED** — private-root execution for isolated packages.

A generic command failure is not enough to trigger a silent PRoot retry.

## Capability policy

Every installed package has a per-capability policy for `adb`, `root`, `network`, `host-read` and `host-write`:

- **ASK** (default): HG2Gui asks you each time; nothing is granted until you answer.
- **ALLOW ONCE**: allows exactly one request and is then used up. It is kept only in memory, so restarting the app clears it.
- **DENY**: always refused.

Change it from **Authority → Package policies**, or with `hg2package capability`. A package asks for a one-time grant with `hg2package capability-request`.

Only **network** is actually enforced. Running an isolated package needs a **network** grant for that launch (isolation cannot provide a real offline network on Android); without it the run is refused with a message saying how to allow it once. HG2Gui has no way to hand `adb`, `root`, `host-read` or `host-write` to a package, so a request for one of those is always refused, whatever the policy says. For a one-off elevated command, use `hg2auth` yourself.

## Restore points

**Snapshot** (`hg2package snapshot`) saves a bounded restore point of the state HG2Gui manages for a package; **Restore** puts it back after a confirmation, and `snapshot-delete` removes one.

## Isolating a package

Choose **Isolate** on an installed package.

An isolated package receives:

- a private root;
- private HOME/XDG state;
- no bind mount of the real HG2Gui home/prefix;
- common privilege tools removed from the guest prefix;
- common host `su` paths masked;
- fail-closed behavior when isolation cannot be established (the package does not run unisolated instead).

Isolation keeps a package's files and state separate and lets HG2Gui see what changed. It is not a verified security sandbox: the isolated process runs as the same Android user as HG2Gui, and its network access is not confined. An isolated package runs only as its own command or one step of a `;` / `&&` / `||` list; in a pipe, `bash -c`, `$( )` or a control structure it is refused. This check reads the command line only: HG2Gui puts no `PATH` shims in front of isolated packages (they would break builds), so an isolated package's command reached through `$tool`, a shell function or a script runs normally, outside its private root.

HG2Gui also performs best-effort runtime observation of:

- process descendants;
- live opened files and observed access modes;
- TCP/TCP6/UDP/UDP6 socket endpoints;
- ADB/root-like privilege-tool attempts;
- before/after private-root filesystem changes.

This is not complete kernel syscall tracing and is not tamper-proof. Very short-lived events can escape observation.

### Isolation Audit

Open **Settings → Isolation audit** to inspect latest observed activity for isolated packages. The view groups authority attempts, network observations, processes, and file observations and shows current sandbox size/state.

If you have saved an Anthropic API key, **ASK AI · SEND SUMMARY** sends a summary of that package's latest run to Anthropic for interpretation, after a confirmation that lists what goes: the package name and version, the sandbox file count and size, how many files were created, modified and deleted, the file names only (never folders) of up to 24 in each group (any whose path mentions a password, secret, token, key or similar is left out), a count of observed events per kind, and the protocol and remote port (never the address) of up to 24 network connections. Full paths, command lines and addresses stay on your device. Nothing is sent unless you confirm.

## Execution authority

**Packages → Authority** exposes explicit authority backends.

### App

Normal default Android app authority.

### ADB shell

When an executable ADB client is available, HG2Gui can perform devices/pair/connect/disconnect/shell operations. Android's normal Wireless Debugging authorization still applies.

ADB shell execution requires foreground confirmation. `hg2auth` is not available to the external API, MCP or installers (it is refused with exit code 126), apart from `hg2auth status`.

### Root

When an executable `su` provider is present, HG2Gui can request root execution. The device's root manager remains authoritative and HG2Gui asks for foreground confirmation.

### No inherited privilege

HG2Gui does not hand ADB or root to normal commands, isolated packages or headless callers merely because it can reach those backends. Inside an isolated package, `adb`, `su` and similar tools are masked or removed. HG2Gui does not intercept an `su` or `adb` binary you run yourself from the shell; it behaves as in any terminal, and your root manager still decides.

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

Apps can also read the API's own schema (`CAPABILITIES`) and request history (`AUDIT_HISTORY`), and exchange short-lived **metadata channels** (publish/read/list/clear) so HG2Gui can understand what they are showing. Enabling a package over the API requires HG2Gui to be in the foreground; of the package lifecycle actions only `info` and `snapshot` run from the background, and the engine itself refuses the rest (`update`, `disable`, `isolate`, `reset`, `remove`, …), `bootstrap`, `vfs mount`, `hg2auth` (except `status`) and the device-affecting built-ins (`call`, `contacts`, `brightness`, `wifi`, `bluetooth`, `airplane`, `flash`, `edit`, `volume set|profile`) for API, MCP and installer callers, with exit code 126.

When an app asks for root or ADB, HG2Gui shows which app is asking and the exact command; RUN runs that command, and cancelling or dismissing the dialog answers the app with a denial. Each request, decision and result is written to a bounded audit log (the latest 200 rows) that `AUDIT_HISTORY` returns.

The API does not make background callers automatically privileged.

## Remote hosts

`remote discover <preset>` inspects a saved SSH preset's host (OS, shell, package manager, commands, packages) and caches it; `remote status|help|packages <preset>` read from it so completion and help can describe the remote machine.

## SSH

SSH presets and host/key choices can be assembled natively. Known SSH hosts can also feed the Completion Bridge. Password/passphrase and host-key questions use the normal interactive prompt surfaces.

## Workflows

Workflows collect template values and compose the resulting command for review. They do not bypass the explicit RUN step for ordinary commands.

## AI

The AI surface can propose commands and return them to the terminal for review. AI suggestions remain subject to the same package, isolation, and authority policy as manually composed commands.

## Store

The Store manages `.azp` packages separately from Termux/dpkg/pip/pipx/npm/gem lifecycle inventory.

Search by name or pick a kind (each kind chip is a filter); before a search the list says how to start, and an empty answer says "No packages found". Packages already installed are listed above the results so they can be removed without searching. Installing a **skill** first asks you to confirm, because a skill's text becomes part of the AI chat's instructions and its signature can only be checked after download. A failed install shows its reason above the list. A skill is read into the AI chat's instructions only when its signer is one the registry itself publishes (trusted); a skill with no signature, only a self-made one, or one that cannot be verified on this Android version still installs but is not used by the AI. A script that needs `apt` packages shows you the list and installs them only if you confirm.

## MCP

The MCP server is loopback-only and explicitly started. Its shell execution gate does not grant ADB/root authority because headless elevation remains blocked. Up to four paired clients can be connected at once, and each must present the pairing token within ten seconds of connecting; the token is new on every start and is kept in memory only.

File tools (`vfs.*`) are confined to the app-private home directory and never follow the Files screen into device storage; hidden entries and `.ssh` directories cannot be read or listed through them, and cannot be changed either. `shell.exec` is off until you enable it, runs one command at a time in a session separate from your tabs, and returns the output (cut at 256 K characters, with a note) followed by an `exit code: N` line; a non-zero code is reported to the agent as an error. The engine refuses it the same things it refuses the API (see External Android API).

The MCP screen shows the pairing token (tap **SHOW TOKEN** to reveal it) and the matching `adb forward` line, each with a **COPY** button. If Android's notification permission was refused the server stays off and the screen says why, with a shortcut to the app's system settings.

## Settings

The Settings screen scrolls vertically whenever its contents exceed the viewport, including at larger text scales.

Current settings include display/text controls, PTY behavior, default shell, environment/history navigation, isolation audit, AI settings, MCP/developer controls, the automatic update check, motion information, and an About entry (version, licence, privacy policy, source).

- **Text size** runs from 85% to 200%; **Reset to default** returns to 100%. A smaller value saved by an older version is raised to 85% when Settings opens.
- **Real pseudoterminal** is on by default (PTY). Switch to PIPE only if a tool misbehaves; it applies to new tabs or the next launch.
- **Default shell** lists only installed shells. If none is installed the row says so instead of offering one.
- **Update check** can be switched off. While on, HG2Gui checks GitHub for a newer release when the app starts, at most once every six hours, and only notifies you; nothing is downloaded until you accept. When off, HG2Gui never contacts GitHub on its own to look for a newer build.
- **Motion** follows Android's animation setting (Remove animations in accessibility settings, or the Animator duration scale in developer options); the row reports the current state and opens the system page.

The **AI settings** screen stores the Anthropic API key encrypted (Android Keystore). Tapping the field edits and focuses it; **SHOW KEY** reveals what you pasted. Leaving with an unsaved edit asks first, and a save that could not be persisted is reported rather than shown as saved.

## Compatibility expectations

HG2Gui runs under its own Android application ID and security context, so Android linker, ICU, permission, package, ADB/root, and PRoot behavior must be validated on real devices in addition to CI.

When a package/runtime case fails, compatibility work should fix the class of incompatibility rather than adding a package-name exception unless the package truly has unique semantics.