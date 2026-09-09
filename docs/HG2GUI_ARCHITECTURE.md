# HG2Gui Android Architecture

This document is the Android-focused companion to [ARCHITECTURE.md](ARCHITECTURE.md). It describes the concrete Android implementation rather than product history or Guide/animation material.

## Application boundary

HG2Gui is an Android application (`com.hereliesaz.hg2gui`) that runs a Termux-derived userspace inside its own application sandbox and adds explicit bridges for Android capabilities that cannot be modeled honestly as ordinary Linux commands.

Current platform configuration:

- `compileSdk` 37
- `targetSdk` 37
- `minSdk` 24
- JDK/JVM target 21
- NDK `29.0.14206865`
- ARM64 native runtime path is the primary packaged native target

## Modules

### `:composeApp`

Thin Android application shell:

- `TerminalActivity`
- `EditorActivity`
- MCP foreground service
- Android manifest/resources
- signing/product-flavor/build configuration
- generated native entry binaries

### `:shared`

The product implementation:

- Compose screens/state
- terminal dispatch and shell session
- bootstrap/runtime management
- package downloader/package manager
- installed-package lifecycle
- package isolation
- execution authority
- Files/VFS, SSH, workflows, AI, Store, MCP protocol helpers

### `:terminal-emulator`

Vendored terminal/PTY support and parser code used by the terminal stack.

### `:termux-shared`

Vendored Termux-compatible Android support code.

## `TerminalActivity`

`TerminalActivity` owns top-level Android lifecycle/screen wiring and terminal-session creation. A terminal tab owns its own engine/session/UI state rather than sharing shell state with other tabs.

The activity is also where Android-only permission/settings flows are connected to Compose surfaces. It does not decide package execution authority; that policy lives below the UI in `TerminalEngine`.

## `ShellSession`

`ShellSession` is the persistent process transport. The Android implementation uses HG2Gui's bundled Termux-style Bash/runtime when available and preserves shell state such as `$PWD` between commands.

Commands are framed with an HG2Gui sentinel carrying the command exit code and resulting working directory. The shell transport also detects commands waiting for interactive input and hands prompts back to the UI.

The shell is an execution mechanism, not the security-policy boundary. `TerminalEngine` decides whether a command is allowed to reach it normally, must be translated to the package manager, must run isolated, or must be denied pending explicit elevation.

## `TerminalEngine`

`TerminalEngine` is the central Android execution router.

Current routing, in policy order:

```text
bootstrap
  ↓
HG2Gui downloader
  ↓
hg2auth (explicit ADB/root authority)
  ↓
hg2package (installed-package lifecycle)
  ↓
disabled-package guard
  ↓
HG2 package manager / translated mutating apt
  ↓
Android built-ins
  ↓
isolated package execution
  ↓
normal persistent shell
```

The ordering matters. A command owned by a disabled package cannot bypass the lifecycle layer merely because the binary exists on disk. A command owned by an isolated package is redirected before it can touch the normal shell environment. ADB/root cannot be reached accidentally by ordinary execution.

## Termux bootstrap under HG2Gui

HG2Gui cannot treat a stock Termux bootstrap as an ordinary relocatable Linux rootfs.

Termux packages and binaries may assume:

```text
/data/data/com.termux/files/usr
```

HG2Gui instead owns its own prefix under its Android application data directory. The runtime therefore includes several relocation/repair layers.

### Native executable delivery

`BootstrapManifest.kt` maps bootstrap executable paths to Android-legal `.so` filenames packaged in the APK native-library directory. `DistroManager` links the visible `$PREFIX/bin/...` path to the packaged native executable.

This avoids relying on newly extracted executable files in writable app storage for the bootstrap's critical native command set.

### Native entry binaries

`composeApp/build.gradle.kts` generates native entry binaries for components that need an Android-safe entry path plus HG2Gui-specific arguments. Current generated entries include apt-key and dpkg.

The dpkg entry injects HG2Gui's dpkg database/install root and script-chrootless behavior before invoking the packaged dpkg implementation.

## HG2 package manager

`Hg2PackageManager` is the compatibility transaction layer for repository packages.

### Why top-level apt installation is intercepted

A real upstream `apt install` can download packages correctly but then invoke dpkg with assumptions inherited from the Termux application. HG2Gui therefore intercepts top-level mutating `apt`/`apt-get` operations in `TerminalEngine` and routes the mutation through its own package manager.

This also removes apt's own `Do you want to continue? [Y/n]` interaction from HG2Gui-owned package operations. Genuine interactive prompts from other commands still surface through the general prompt UI.

### Archive relocation

Termux `.deb` payload paths rooted under `data/data/com.termux/files/usr/...` are moved to the package archive root before rebuilding. Without this step, `--instdir=$PREFIX` would install the old absolute hierarchy *inside* HG2Gui's prefix.

### Control metadata

Relocated payloads require metadata-aware rewriting:

- `DEBIAN/conffiles`: old Termux absolute prefix → package-root absolute path such as `/etc/...`
- `DEBIAN/md5sums`: old Termux relative prefix → payload-relative path such as `etc/...`

These files are excluded from generic prefix rewriting because their path syntax has dpkg semantics.

### Maintainer scripts

Android can reject direct execution of package scripts from writable application data even when permissions look executable. HG2Gui externalizes `preinst`, `postinst`, `prerm`, and `postrm`, verifies that they are no longer visible to dpkg, and invokes them through bundled Bash.

Existing installed scripts are staged out of `var/lib/dpkg/info` during upgrade/remove transactions for the same reason. Staging is verified; a failed source removal aborts before dpkg is allowed to continue.

### dpkg database/root

The packaged native entry uses HG2Gui's own:

```text
$PREFIX/var/lib/dpkg
$PREFIX
```

and disables maintainer-script chrooting because the Android app UID cannot chroot.

## Package inventories

`PackageLifecycleStore` normalizes installed software from multiple package-manager ecosystems:

- dpkg / Termux `pkg`
- Python `pip`
- global Node `npm`
- RubyGems `gem`

It reads existing metadata on disk; it does not maintain a second shadow installation database.

For dpkg packages, `DpkgCatalog` also resolves which package owns which command under `bin/`. That ownership data is used both for the menu and for lifecycle enforcement.

## Disable/Enable

Disabled state is stored by HG2Gui, not by the package manager. The package remains installed. Before shell execution, `TerminalEngine` resolves the command owner and blocks a disabled owner's executable with exit code 126.

This is intentionally different from uninstalling or removing the binary from dpkg's database.

## Reset

Normal-package Reset is conservative:

- conventional package/binary-named XDG cache/config/data/state directories;
- matching package-specific cache/log/tmp directories under the prefix;
- new paths HG2Gui positively observed appearing in the command's working directory.

It does not claim complete provenance for arbitrary writes by a non-isolated process.

For an isolated package, Reset removes the isolation root, which provides a much stronger and simpler reset guarantee.

## Isolation

`PackageIsolation` provides package-owned command isolation when an executable PRoot engine exists.

### Seeding

A private root is stored under HG2Gui's application files. It receives:

- a private copy of the current HG2Gui prefix;
- a private home;
- private XDG config/cache/data/state and temp locations;
- a version marker tied to the installed package version.

A package version change makes the seed stale, so the next isolated run rebuilds the private root.

### Privilege stripping

Before the private runtime is marked ready, HG2Gui removes common privilege/boundary tools from the copied guest prefix:

- `adb`
- `su`
- `tsu`
- `magisk`
- `proot`

When common host `su` paths exist under `/system`, PRoot binds a denied file over them inside the guest.

### Host visibility

The guest receives only the Android runtime mounts currently required for execution (`/system`, `/apex`, `/proc` when present). HG2Gui's real prefix/home are not host binds into the guest.

### Filesystem audit

HG2Gui snapshots private-root path metadata before and after the isolated run and reports created/modified/deleted paths. This gives complete visibility into changes *inside the private root* at the file-tree level.

It is not currently a full syscall/network/process audit and should not be documented as one.

### No unsafe fallback

If the package is marked isolated and no executable PRoot engine can be found, the package does not run. The user must provide PRoot or explicitly release isolation.

## Execution authority

`ExecutionAuthority` models privileged execution independently from package isolation.

### App authority

Default. Ordinary commands run with HG2Gui's Android application UID and permissions.

### ADB authority

HG2Gui looks for an executable ADB client in:

- expected APK native-library names, if one is packaged;
- `$PREFIX/bin/adb` (for example from `android-tools`).

Available operations are exposed through `hg2auth adb` and the **Packages → Authority** UI:

- devices
- pair
- connect
- disconnect
- shell

Wireless Debugging pairing/connect authorization is still Android/ADB's responsibility. HG2Gui having a client does not grant shell authority by itself.

ADB-shell execution requires explicit interactive confirmation.

### Root authority

HG2Gui looks for executable `su` providers in the prefix and common rooted-device paths. If one exists, it can offer:

- root test (`id`)
- root shell command (`su -c ...`)

The root manager on the device remains the actual authorization authority. HG2Gui asks for confirmation before requesting the elevated operation.

### Interactive-only elevation

Headless `runToCompletion` calls cannot execute ADB-shell/root operations. This applies to internal/headless callers and prevents the MCP shell path from becoming an accidental root/ADB broker.

## Command-tree input contract

The UI no longer treats a normal leaf as permission to execute.

- tapping a normal leaf assembles a command;
- **RUN** executes it;
- a missing open-ended operand focuses the input field and names the expected input kind;
- recognized path operands use the file picker;
- package names use package/catalog pills where available;
- yes/no prompts from an already-running command use a confirmation dialog;
- other finite prompt choices use pills;
- free-form prompts use the input field.

This contract is important because shell help output cannot always tell HG2Gui whether a leaf that looks complete still requires a positional operand. Explicit Run avoids converting incomplete menu metadata into accidental execution.

## Package lifecycle UI

`PackageLifecycleTree` is generated from live package inventories. It contains the **Authority** branch plus manager/package branches.

An installed package may show:

```text
Run
Disable / Enable
Isolate / Release isolation
Update
Reset
Info
Remove
Purge (dpkg/pkg)
```

`Run` lists discovered executables owned or declared by the package. Package names themselves are pills, not free-form text fields.

## Security boundaries

The important boundaries are intentionally independent:

1. **Android app sandbox** — default OS process authority.
2. **HG2Gui package lifecycle** — can block a package without uninstalling it.
3. **Package isolation** — redirects an isolated package into a private PRoot filesystem and strips privilege tools.
4. **ADB shell authority** — explicit connection/pairing plus HG2Gui confirmation.
5. **Root authority** — explicit `su` provider plus HG2Gui confirmation plus root-manager authorization.

A higher capability being available to HG2Gui does not automatically flow downward to packages.

## Files and device storage

The Files/VFS subsystem is separate from package isolation. Its sandbox/device-storage mode is controlled through Android storage permission/state. `MANAGE_EXTERNAL_STORAGE`, where used, is a deliberate file-manager capability and should not be removed merely to simplify Play declaration handling without a product decision.

## CI and release

The repository has separate workflows for merged build/release, Play release, and code scanning. Release workflows may update `version.properties` with bot commits, so version counters can advance independently of feature commits.

When diagnosing a build, use the triggering SHA and workflow job logs rather than assuming current `master` and the build's checkout are identical.

## Android invariants

1. Critical bootstrap native executables must use Android-safe delivery; do not casually replace them with writable-data copies.
2. Mutating apt operations must not bypass the HG2 package transaction layer.
3. dpkg must use HG2Gui's database/install root.
4. Maintainer scripts must not be left for dpkg to direct-exec from writable app data.
5. Disabled package commands are blocked before shell execution.
6. Isolated packages fail closed if the isolation engine is unavailable.
7. Isolated packages do not inherit ADB/root tools.
8. ADB/root execution is explicit and interactively confirmed.
9. Headless command execution cannot elevate through `hg2auth`.
10. UI metadata must not auto-run normal command leaves.
