# Command Reference

HG2Gui has three kinds of command surface:

1. **real runtime binaries** discovered from the Termux-derived prefix;
2. **HG2Gui control verbs** implemented by `TerminalEngine` for bootstrap, downloading, package lifecycle, and execution authority;
3. **Android built-ins** implemented by `Builtins.kt`.

Normal command-tree leaves compose a command and stop. Press **RUN** to execute. Interactive prompt answers are the exception because they answer a command that is already running.

## Real shell commands

The shell command set is not fixed. `CommandTree` discovers binaries from HG2Gui's runtime `bin/`, associates dpkg-owned binaries with their package via `DpkgCatalog`, and groups them into human-facing categories such as Package management, Network, Shells, Development, Text & files, and System.

Hyphenated families such as `apt-get`, `apt-key`, and `apt-mark` can nest under a common host. `HelpCatalog` probes a real command's `--help` output in the background and adds discoverable options to the pill tree.

A real binary can still require a positional operand after all known option pills have been selected. HG2Gui therefore does not auto-run terminal leaves. If the remaining value is open-ended, the input field is focused and names the expected value where HG2Gui can infer it. If it is a path, the graphical file picker is used instead.

## Bootstrap

### `bootstrap`

Installs/repairs HG2Gui's pinned Termux-derived userspace. The application also bootstraps automatically when the required runtime is absent.

The bootstrap is not simply an unpacked Termux directory: HG2Gui maps critical native executables through APK-native `.so` files and repairs paths/configuration for its own application ID.

## Downloader

### `download ...`
### `hg2download ...`

HG2Gui downloader entry points. The downloader reuses valid completed targets and resumes `.part` downloads when possible.

These are control verbs handled before the normal shell.

## Package commands

### `pkg ...`
### `hg2pkg ...`

HG2Gui package-manager entry points. Package mutations run through `Hg2PackageManager`, which adapts Termux packages for HG2Gui's prefix and Android execution constraints.

Top-level mutating `apt` / `apt-get` commands are translated into this layer by `TerminalEngine` so upstream apt cannot bypass HG2Gui's transaction rules.

Translated operations include:

- `install`
- `remove`
- `purge`
- `update`
- `upgrade`
- `dist-upgrade`
- `full-upgrade`

The package UI supplies package names from the APT catalog rather than requiring free-form typing when the index is available.

### Android package-transaction behavior

The HG2 package layer currently:

- relocates Termux's hardcoded archive payload prefix into HG2Gui's package root;
- repairs control metadata such as `conffiles` and `md5sums`;
- forces dpkg's database and installation root under HG2Gui's `$PREFIX`;
- disables maintainer-script chrooting;
- externalizes `preinst`, `postinst`, `prerm`, and `postrm` from dpkg's direct-exec path;
- runs those scripts through bundled Bash;
- verifies that staged scripts are actually absent before allowing dpkg to proceed.

This is a general compatibility layer, not a list of package-name exceptions.

## Installed-package lifecycle

### `hg2package <action> <manager> <package>`

Internal/user-visible control surface backing **Packages → manager → installed package**.

Supported actions:

- `info`
- `update`
- `disable`
- `enable`
- `isolate`
- `release`
- `reset`
- `remove`
- `purge`

The UI normally supplies manager and package names through pills.

### `disable`

Keeps the package installed and visible but blocks commands HG2Gui knows are owned by it. A blocked command exits with code 126 until the package is enabled again.

### `enable`

Removes HG2Gui's disabled flag. It does not reinstall the package because the package was never removed.

### `isolate`

Marks the package for private-root execution. On the next run, HG2Gui seeds a PRoot-backed private runtime if necessary. Isolated package commands do not silently fall back to the normal shell when the isolation engine is unavailable.

### `release`

Turns off isolation. Releasing isolation is an explicit interactive lifecycle operation.

### `reset`

Keeps the package installed but clears package runtime state HG2Gui can safely attribute to it. For normal packages this includes conventional package-scoped XDG/cache/log/tmp locations plus newly-created paths HG2Gui positively observed. For isolated packages it discards the private runtime so the next run starts from a fresh seed.

Reset requires confirmation.

### `update`

Uses the owning manager's update/install mechanism. If an isolated package version changes, its old private runtime is treated as stale and reseeded on next isolated run.

### `remove` / `purge`

Use the owning package manager. `purge` is exposed where the manager has meaningful purge semantics. Destructive removal actions require confirmation.

## Package-manager inventories

The **Packages** tree currently discovers installed packages from:

- **Termux / pkg** — dpkg status and `.list` ownership metadata;
- **Python / pip** — `.dist-info` metadata and console scripts;
- **Node / npm** — global `node_modules` package metadata and declared bins;
- **Ruby / gem** — gem specifications and executables.

Each package row may expose:

```text
Run
Disable / Enable
Isolate / Release isolation
Update
Reset
Info
Remove
Purge (where supported)
```

`Run` itself contains the package's discovered executable names.

## Execution authority

### `hg2auth status`

Reports which explicit authority backends HG2Gui can currently use:

- App — always the default;
- Isolated package — available when a PRoot engine is executable;
- ADB shell — available when an executable ADB client is present;
- Root — available when an executable `su` provider is detected.

This status query is allowed headlessly. Elevated execution is not.

### `hg2auth adb devices`

Runs `adb devices -l` through the detected client.

### `hg2auth adb pair <host:port> <pairing-code>`

Pairs the ADB client with an Android Wireless Debugging endpoint. The code and endpoint come from Android's Wireless Debugging UI.

### `hg2auth adb connect <host:port>`

Connects the client to a paired/debugging endpoint.

### `hg2auth adb disconnect [host:port]`

Disconnects one endpoint or the client's active ADB connections according to ADB semantics.

### `hg2auth adb shell <command...>`

Runs a command through ADB shell. HG2Gui asks for explicit interactive confirmation before executing it.

Possessing the ADB client is not authorization by itself; Android's pairing/connect rules still apply.

### `hg2auth root test`

After confirmation, requests the detected `su` provider to run `id`. The device's root manager still decides whether the request is granted.

### `hg2auth root shell <command...>`

After confirmation, runs the command through `su -c`.

### Authority invariant

Ordinary shell commands and isolated package commands do **not** inherit ADB or root merely because HG2Gui can access those backends. Headless `runToCompletion` callers cannot execute elevated `hg2auth` actions.

## Interactive prompts

When a command is already running and asks for input:

- yes/no → modal confirmation dialog;
- numbered or bracketed finite choices → graphical choices;
- password/passphrase → masked input;
- other open-ended prompt → focused text field and **Send**.

Package-manager mutations owned by HG2Gui do not expose upstream apt's `Do you want to continue? [Y/n]` prompt.

## SSH

The discovered `ssh` command gets a dedicated UI branch with saved presets and a **new…** flow. Host/user/port/key data is assembled into the command line for review. Key paths use the graphical picker. Host-key confirmation and password/passphrase requests use the same generic prompt mechanisms as other commands.

## Synthesized root surfaces

In addition to live shell categories, the command tree contains non-shell roots including:

- **Packages** — installed package managers, package lifecycle, and execution authority;
- **Workflows** — stored command templates with placeholders;
- **AI** — natural-language command suggestions that are inserted for review, never auto-executed;
- **Store** — azphalt.store `.azp` browser/install surface;
- **Context** — static remote-OS reference command trees for local/Ubuntu/macOS/Windows contexts;
- Android device/app/features roots backed by built-ins.

## Android built-ins

`Builtins.kt` remains the explicit fixed dispatch table for Android-facing capabilities that do not have an honest ordinary runtime-binary equivalent.

### Device

- `wifi` — show/open Wi-Fi controls as allowed by the Android version.
- `bluetooth` — request/open Bluetooth controls according to platform restrictions.
- `airplane` — open airplane-mode settings.
- `flash` — toggle flashlight; camera permission may be required.
- `volume` — inspect/set stream levels and ringer profile.
- `brightness` — set a percentage or automatic mode; modify-system-settings permission may be required.

### Apps & navigation

- `call <name or number>` — place a call, resolving contacts where possible.
- `contacts <ls|add|about|edit|rm> ...` — contact operations.

### Features

- `vfs <ls|cd|pwd|mkdir|touch|cat|rm|mv|cp|mount> ...` — HG2Gui-managed filesystem operations.
- `calc <expression>` — calculator.
- `edit <file>` — open HG2Gui's text editor. File operands should be selected through the graphical picker when using the touch UI.

## Creative Guide commands

`docs/GUIDE.md`, the Hitchhiker's Guide manuscripts, and animation packets describe Guide entries and visual/story material. A phrase such as “a user runs `pwd`” in those documents is narrative language, not a statement that selecting a UI leaf auto-executes today.
