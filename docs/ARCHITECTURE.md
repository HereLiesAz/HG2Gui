# Architecture

**Current repository version:** `0.7.218` with `versionBuild=489` in `version.properties` at the time of this documentation pass. CI may advance the build counter independently.

HG2Gui is an Android terminal application, not a launcher. The product is built around one principle: **the shell remains real, while HG2Gui supplies the touch interface and the Android-specific policy/compatibility layers that the shell cannot safely provide by itself.**

The application is Kotlin/Compose. `:composeApp` is the thin Android entry point; most UI and execution logic lives in `:shared`. The shared module currently targets Android.

## 1. Execution model

Each terminal session owns a persistent `ShellSession` and a `TerminalEngine`. `ShellSession` preserves working-directory and shell state between commands. `TerminalEngine` is the policy boundary in front of that shell.

The current dispatch order is intentionally broader than `builtin-or-shell`:

1. `bootstrap` → `DistroManager`
2. `download` / `hg2download` → `Hg2Downloader`
3. `hg2auth` → explicit execution-authority layer
4. `hg2package` → installed-package lifecycle layer
5. package-owner lookup → block a disabled package before execution
6. `pkg` / `hg2pkg`, plus translated mutating `apt`/`apt-get` operations → `Hg2PackageManager`
7. verbs in `Builtins.NAMES` → Android built-ins
8. commands owned by an isolated package → private-root execution
9. everything else → the normal persistent shell

This order is an architectural invariant. Package safety, isolation, and elevated authority are not shell aliases; they are enforced before a command reaches the ordinary shell.

### Headless execution

`TerminalEngine.runToCompletion()` is used by non-interactive callers. It may run normal shell/package work and isolated packages, but **ADB-shell and root elevation are denied headlessly**. `hg2auth status` is readable; elevated actions require the interactive UI's confirmation path.

## 2. Termux-derived runtime

`DistroManager.kt` installs a pinned Termux bootstrap into HG2Gui's private prefix, typically:

```text
/data/user/0/com.hereliesaz.hg2gui/files/usr
```

The bootstrap release is pinned in source. Upgrading it is not a URL-only change: the native executable manifest and packaged native libraries must remain in lockstep with the bootstrap.

### Android native-execution constraint

Modern Android does not treat arbitrary native executables copied into writable app data the same way it treats native code delivered with the APK. HG2Gui therefore packages bootstrap executables as Android-legal `.so` names in the APK native-library directory and links the corresponding `$PREFIX/bin/...` paths to them according to `BootstrapManifest.kt`.

That mechanism is why the runtime must not be documented as "just unzip Termux somewhere else." HG2Gui has to preserve Termux userspace semantics while satisfying a different Android application ID and Android's executable-loading rules.

## 3. Package-management architecture

### 3.1 Command routing

Top-level mutating `apt`/`apt-get` operations are translated into HG2Gui package operations before upstream apt can invoke dpkg directly. Current translated operations include install, remove, purge, update, upgrade, dist-upgrade, and full-upgrade.

Read-only commands that are not translated can still reach the real shell tools.

This avoids two problems exposed by real-device testing:

- upstream apt prompting for confirmation when HG2Gui already owns the interaction model;
- upstream apt/dpkg transactions assuming Termux's own application environment and helper execution rules.

### 3.2 Downloader

`Hg2Downloader.kt` provides package/update downloads with reuse and resume behavior:

- a valid completed target is reused;
- expected SHA-256 can validate an existing file;
- remote length can be compared when no SHA is supplied;
- `.part` files resume with HTTP Range when possible;
- complete partials can be promoted;
- oversized/invalid partials restart cleanly.

The downloader streams progress and is shared by package/update paths rather than reimplementing network transfer per feature.

### 3.3 `.deb` preparation

`Hg2PackageManager.kt` transforms Termux packages into an archive layout dpkg can install correctly under HG2Gui.

The preparation pipeline currently handles:

- relocation of archive payloads rooted at Termux's hardcoded `/data/data/com.termux/files/usr`;
- text-prefix rewriting where appropriate;
- special treatment of `DEBIAN/conffiles` so conffile paths remain archive-root absolute (`/etc/...`), not HG2Gui-host absolute;
- special treatment of `DEBIAN/md5sums` so payload paths remain package-relative;
- extraction of maintainer scripts (`preinst`, `postinst`, `prerm`, `postrm`) out of dpkg's direct-exec path;
- verified/atomic staging of existing installed maintainer scripts before upgrade/remove transactions.

The original downloaded SHA is verified before archive transformation where an expected digest is available. The transformed archive is deliberately not byte-identical to the repository package.

### 3.4 dpkg launcher

The bundled dpkg launcher supplies:

- `--admindir=$PREFIX/var/lib/dpkg`
- `--instdir=$PREFIX`
- `--force-script-chrootless`

`script-chrootless` is required because a normal Android application UID cannot perform the chroot behavior dpkg otherwise expects for maintainer scripts.

### 3.5 Maintainer scripts

Directly executing maintainer scripts from writable app-private storage produced Android `Permission denied` failures even when their Unix executable bits were correct. HG2Gui therefore:

1. moves package maintainer scripts out of the rebuilt `.deb` control directory;
2. moves already-installed maintainer scripts out of `var/lib/dpkg/info` during a transaction;
3. verifies the source paths are actually absent rather than trusting `File.delete()`/copy side effects;
4. invokes the scripts explicitly through bundled Bash with dpkg maintainer-script environment variables;
5. lets dpkg unpack/configure while those scripts are outside its direct-exec path;
6. stores the scripts back for future HG2Gui-managed transactions;
7. restores staged scripts on failure where possible.

If staging cannot remove a script from dpkg's reach, HG2Gui fails before dpkg instead of continuing into a misleading execute-permission failure.

### 3.6 Compatibility scope

The package layer is general, not a `python-pip` special case. Real-device testing has successfully crossed download → prepare → unpack/configure → execute for packages including `python-pip`, `nsnake`, and `curl`.

This is still **compatibility engineering, not a blanket guarantee that every Termux package works unchanged**. Packages can contain hardcoded application paths, unusual maintainer-script interpreters, absolute symlinks, ELF/linker assumptions, services, triggers, alternatives/diversions, dependency relationships, or native delivery requirements that expose additional Android differences.

## 4. Installed-package lifecycle

`PackageLifecycleStore.kt` adds lifecycle semantics above whichever package manager installed the software.

### Inventory adapters

The current on-device inventory recognizes:

- `pkg` / dpkg — dpkg status and ownership metadata;
- Python / `pip` — `.dist-info` metadata and console entry points;
- Node / `npm` — global `node_modules` package metadata and declared bins;
- Ruby / `gem` — gem specifications and executables.

The adapters normalize packages into a common `InstalledPackage` model containing manager, name, version, runnable binaries, disabled state, and isolated state.

### Package UI

`PackageLifecycleTree.kt` renders:

```text
Packages
  → package manager
    → installed package
      → Run
      → Enable / Disable
      → Isolate / Release isolation
      → Update
      → Reset
      → Info
      → Remove
      → Purge (where supported)
```

Only discoverable executables appear under **Run**.

### Disable

Disable is HG2Gui-owned; the package manager does not need to support it. A disabled package remains installed, versioned, inspectable, updateable, resettable, and removable. Before ordinary execution, `TerminalEngine` resolves the command's package owner and returns exit code 126 when that owner is disabled.

### Reset

Reset is also HG2Gui-owned.

For a normal package, the reset candidate set includes:

- conventional package/binary-named XDG cache/config/data/state locations under HG2Gui home;
- matching package-specific `var/cache`, `var/log`, and `var/tmp` locations;
- paths HG2Gui positively observed appearing in the package's working directory during a run.

HG2Gui does **not** claim to know every file a non-isolated program may have modified. The non-isolated tracker is intentionally conservative so Reset does not guess its way into deleting unrelated user data.

For an isolated package, Reset discards the private isolation root. The next run reseeds it from the currently installed runtime/package version.

## 5. Package isolation

`PackageIsolation.kt` provides a filesystem boundary for package-owned commands when an executable PRoot engine is available.

### Private root

An isolated package receives a private root under app-private storage. On first run (or after a version change/reset), HG2Gui:

- clears the prior private root;
- copies the HG2Gui prefix into the matching guest prefix path;
- creates a private home and XDG cache/config/data/state/tmp tree;
- records the installed package version in the seed marker;
- removes privileged tools such as `adb`, `su`, `tsu`, `magisk`, and `proot` from the guest prefix.

The real HG2Gui prefix and home are not bind-mounted into the guest.

### Host exposure

The PRoot command exposes only the Android paths currently needed for process/runtime operation (`/system`, `/apex`, `/proc` when present). Common host `su` paths are masked by binding a denied file over them.

Isolation is therefore separate from authority: **a package does not gain ADB/root merely because HG2Gui can use those authorities itself.**

### Audit

Before and after an isolated run, HG2Gui snapshots the private root's path/size/mtime/type metadata and reports:

- created paths;
- modified paths;
- deleted paths.

The output is an observation of private-root filesystem changes. It is not presently a syscall-level network/process trace.

### Failure posture

If a package is marked isolated but no executable PRoot engine is available, execution fails explicitly. HG2Gui does not silently fall back to unsandboxed execution.

## 6. Execution authority

`ExecutionAuthority.kt` separates privilege from ordinary command execution.

### App

Normal HG2Gui/app authority is the default. Merely having ADB or root available does not change ordinary command authority.

### ADB shell

HG2Gui looks for an executable ADB client in its native-library directory or `$PREFIX/bin/adb`. If none exists, the UI points the user toward the `android-tools` package.

The `hg2auth adb` surface supports:

- `devices`
- `pair <endpoint> <code>`
- `connect <endpoint>`
- `disconnect [endpoint]`
- `shell <command...>`

Pair/connect use Android's normal ADB/Wireless Debugging model; possessing an ADB client is not itself authorization.

ADB-shell execution asks for interactive confirmation before running.

### Root

HG2Gui searches common executable `su` locations. A provider being present only means root may be requestable; the device's root manager still controls authorization.

The `hg2auth root` surface supports:

- `test` — requests root and runs `id` after confirmation;
- `shell <command...>` — requests confirmation, then executes through `su -c`.

### No ambient elevation

ADB/root authority is never inherited by:

- ordinary shell commands;
- package lifecycle operations merely because a package is installed;
- isolated package commands;
- headless `runToCompletion` calls.

This is a core security invariant.

## 7. Command tree and input semantics

`CommandTree.kt` combines fixed Android roots with live shell discovery.

- Android built-ins are fixed and explicit.
- shell binaries are discovered from the HG2Gui prefix;
- dpkg ownership informs package/category grouping;
- command families such as `apt-get`/`apt-key`/`apt-mark` can nest under a shared family host;
- `HelpCatalog` adds flags discovered from real `--help` output;
- installation uses `AptCatalog` to enumerate package names;
- package lifecycle uses actual installed-manager inventories;
- path operands use `FileBrowser`/the graphical picker.

### No normal leaf auto-run

Normal command-tree leaves compose the command and leave it on the input line. **RUN is explicit.** Prompt-answer choices are different because they answer a command that is already running.

### Missing operand guidance

When a composed command still needs free-form input, the text field is focused and the UI names the expected kind of value (for example URL, host/address, search pattern, or generic argument). Values that can be enumerated should be pills instead; paths should use the picker.

### Interactive prompts

- yes/no → confirmation dialog;
- numbered/bracketed finite choices → graphical choices;
- password → masked text input;
- other free-form prompts → focused text input / Send.

The active output record auto-scrolls while stdout/stderr changes.

## 8. Other product subsystems

### Built-ins

`Builtins.kt` remains a fixed explicit dispatch table for Android-facing capabilities that are not represented honestly as ordinary runtime binaries: device toggles/settings bridges, contacts/calls, VFS, calculator, and editor entry.

### Files / VFS

The Files UI and `vfs` command provide a managed filesystem surface. Device-storage access remains a permission-controlled mode; it is distinct from package isolation.

### Workflows and AI

Workflows and AI suggestions assemble commands for review. They do not bypass the explicit Run step.

### MCP

The MCP server remains loopback-only and explicit-start. Its own shell execution gate does not grant `hg2auth` elevation because `runToCompletion` rejects elevated authority operations.

### Azphalt Store

`.azp` packages are a separate package system from Termux/dpkg/pip/npm/gem lifecycle inventory. Store extraction/trust rules remain under the Store subsystem; do not conflate `.azp` lifecycle with the installed shell-package lifecycle UI.

## 9. Module boundaries

- `:composeApp` — Android application, activities/services, resources, product flavor/signing/build packaging, generated native launchers.
- `:shared` — UI, terminal engine, package manager, lifecycle/isolation/authority, managers and integrations.
- `:terminal-emulator` — vendored terminal/PTY support.
- `:termux-shared` — vendored Termux-compatible Android utilities.

## 10. Current invariants

1. HG2Gui is a terminal application, not a launcher.
2. Normal command-tree leaves compose; explicit **RUN** executes.
3. Enumerable inputs should be selected, not typed.
4. File/directory operands use the graphical picker when recognized.
5. Package names should come from package/catalog inventories where available.
6. `TerminalEngine` enforces lifecycle/isolation/authority before the normal shell.
7. Disabled packages remain installed but their owned commands are blocked.
8. Isolated packages never silently fall back to unsandboxed execution.
9. ADB/root are explicit authorities, not ambient capabilities.
10. Headless callers cannot elevate through `hg2auth`.
11. Mutating top-level apt operations route through the HG2Gui package transaction layer.
12. Package compatibility fixes must be class/general fixes, not package-name special cases unless a package truly requires package-specific semantics.

## 11. Build/runtime configuration

Current authoritative values live in Gradle/version files rather than this prose. At this documentation pass:

- JDK 21
- AGP 9.3.2
- Kotlin 2.4.10
- Compose Multiplatform 1.12.0
- `compileSdk` / `targetSdk` 37
- `minSdk` 24
- NDK `29.0.14206865`

The bootstrap version is pinned in `DistroManager.kt`; native executable mappings are pinned in `BootstrapManifest.kt`.

## 12. Documentation authority

This file, `README.md`, `HG2GUI_ARCHITECTURE.md`, `COMMANDS.md`, `USER_GUIDE.md`, `DESIGN.md`, `VISION.md`, and `CONTRIBUTING.md` are the live software/product documentation.

The large Hitchhiker's Guide manuscript, animation production packets, style-lock prompts, HTML motion studies, screenshots, and videos under `docs/` are creative/reference assets. They may intentionally describe a story, visual metaphor, or historical production state rather than current runtime behavior.
