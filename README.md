# HG2Gui — The Hitchhiker's Terminal to the Galaxy

**HG2Gui** is a touch-first Android terminal inspired by the visual language of *The Hitchhiker's Guide to the Galaxy*. The command tree is the primary input method: commands, flags, package names, choices, and other enumerable values become animated pills; typing is reserved for genuinely open-ended input.

Underneath the interface is a real Termux-derived runtime in HG2Gui's own app-private prefix, plus Android-specific compatibility layers for package installation, package lifecycle management, isolation, and explicit device/root authority.

## What it does

- **Touch-first command composition.** Selecting a normal leaf composes the command; it does not auto-run. **RUN** is the explicit execution step. If more free-form input is required, the input field is focused and tells the user what kind of value is expected.
- **Structured inputs stay structured.** Package names come from package/catalog pills. File and directory operands open the graphical file picker. Yes/no prompts use a real confirmation dialog. Numbered and bracketed choices remain tappable. Text entry is the fallback, not the default.
- **Live output follows itself.** The active result card auto-scrolls as stdout/stderr arrives so the newest output stays visible.
- **Real Termux-derived runtime.** HG2Gui bootstraps a pinned Termux userspace into its own private `$PREFIX`, with bundled native executables exposed through Android-safe native-library delivery where required.
- **Android-safe package transactions.** Top-level mutating `apt`/`apt-get` operations are routed through HG2Gui's package manager instead of letting upstream apt drive an Android-incompatible dpkg transaction. Package archives are relocated from Termux's hardcoded prefix, metadata is repaired, maintainer scripts are externalized and run through bundled Bash, and dpkg is forced to HG2Gui's own database/install root without chrooting.
- **Resumable/reusable downloads.** Completed package downloads are reused when valid; partial downloads resume when the server supports ranges.
- **Browsable installation.** `apt`/`apt-get`/`pkg` install choices are populated from the downloaded APT package index so users can pick package names instead of typing them blind.
- **Installed-package management.** **Packages** groups installed software by package manager. HG2Gui currently discovers Termux/dpkg packages plus Python `pip`, Node `npm`, and RubyGems installations from their on-disk metadata.
- **Package actions.** Installed packages expose the actions that make sense for them: **Run**, **Update**, **Info**, **Remove**, and (for dpkg packages) **Purge**, plus HG2Gui-owned **Enable/Disable**, **Reset**, and **Isolate/Release isolation**.
- **Disable without uninstalling.** A disabled package stays installed and visible, but HG2Gui blocks commands owned by it until it is enabled again.
- **Reset runtime state.** Reset keeps the installed package payload while clearing package-scoped cache/config/data/state/log locations and paths HG2Gui positively observed the package creating. For isolated packages, Reset discards the private runtime so it is reseeded cleanly on next run.
- **Package isolation.** An isolated package runs inside a private PRoot-backed filesystem seeded from the HG2Gui runtime. The real HG2Gui prefix/home are not bind-mounted into the guest. Each run snapshots the private root and reports created, modified, and deleted paths afterward. `adb`, `su`, `tsu`, `magisk`, and `proot` are removed from the guest prefix, and common host `su` paths are masked.
- **Explicit execution authority.** **Packages → Authority** separates normal app execution from elevated backends. ADB and root are never ambient capabilities of normal or isolated package runs.
- **ADB shell.** When an executable ADB client is available (normally via `android-tools`), HG2Gui exposes devices, pair, connect, disconnect, and shell operations. ADB-shell commands require explicit confirmation.
- **Root.** When an executable `su` provider is present, HG2Gui exposes root testing and root-shell execution. Every root operation that executes with elevation requires explicit confirmation from the visible interactive UI.
- **No headless elevation.** `runToCompletion` callers (including MCP/headless integrations) can query authority status but cannot invoke ADB-shell or root authority.
- **Live command discovery.** Real binaries are discovered from the HG2Gui prefix and associated with their owning dpkg package when possible. Help text is probed in the background to turn discoverable flags into pills.
- **Sessions, Files, Guide, SSH, Workflows, AI, Store, and MCP.** The rest of the product remains integrated around the same rule: assemble or choose explicitly, then execute deliberately.

## Package lifecycle

The package manager remains responsible for installation/update/removal. HG2Gui adds lifecycle state above it:

```text
Packages
  ├─ Authority
  │   ├─ App
  │   ├─ ADB shell
  │   └─ Root
  ├─ Termux / pkg
  │   └─ <installed package>
  │       ├─ Run → <owned binaries>
  │       ├─ Disable / Enable
  │       ├─ Isolate / Release isolation
  │       ├─ Update
  │       ├─ Reset
  │       ├─ Info
  │       ├─ Remove
  │       └─ Purge
  ├─ Python / pip
  ├─ Node / npm
  └─ Ruby / gem
```

Not every package exposes a runnable binary, and not every manager supports the same native lifecycle operations. HG2Gui's own **Disable**, **Reset**, and **Isolate** semantics remain consistent across managers.

## Command routing

`TerminalEngine` is the execution seam. In broad order it handles:

1. `bootstrap`
2. HG2Gui downloader commands (`download` / `hg2download`)
3. explicit authority (`hg2auth`)
4. package lifecycle (`hg2package`)
5. disabled-package blocking
6. HG2Gui package operations (`pkg` / `hg2pkg`) and translated mutating `apt`/`apt-get`
7. Android built-ins
8. isolated package execution when the command belongs to an isolated package
9. the normal persistent shell

This ordering is intentional. Package safety and explicit authority are policy layers in front of the ordinary shell, not aliases implemented inside it.

## Package compatibility

HG2Gui does **not** assume a Termux `.deb` can simply be unpacked and executed unchanged under another Android application ID. The package layer currently handles several structural incompatibilities:

- Termux's hardcoded `/data/data/com.termux/files/usr` archive layout is relocated into HG2Gui's prefix.
- text references to the old prefix are rewritten where appropriate;
- `DEBIAN/conffiles` and `DEBIAN/md5sums` are rewritten according to Debian metadata semantics rather than as ordinary text files;
- dpkg uses HG2Gui's `var/lib/dpkg` and install root;
- maintainer scripts are staged out of dpkg's direct-exec path, verified absent, and invoked through bundled Bash;
- dpkg maintainer-script chrooting is disabled;
- package output is channel-safe and streamed to the UI.

This has been runtime-tested with real packages including `python-pip`, `nsnake`, and `curl`, but it is not a claim that every Termux package is automatically compatible. Native binaries can still expose Android/application-ID/linker assumptions package by package.

## Input rules

HG2Gui follows one general rule: **if valid values can be discovered or enumerated, the user picks them.**

- package → package/catalog pills
- installed package → package lifecycle pills
- package executable → **Run** pills
- file/directory → graphical picker
- yes/no → confirmation dialog
- finite choice set → pills
- URL/host/search text/message/other open value → focused text field with a specific cue

Normal command-tree selections compose the command and stop. The user presses **RUN** to execute it. Interactive prompt answers are the exception because they answer a command that is already running.

## Project structure

- `:composeApp` — Android application entry point, activities/services, resources, build/signing/packaging.
- `:shared` — Compose UI, terminal routing, package management, lifecycle/isolation/authority, managers, integrations.
- `:terminal-emulator` — vendored terminal/PTY support.
- `:termux-shared` — vendored Termux-compatible Android support library.

Key Android-side files:

- `terminal/TerminalEngine.kt` — dispatch and policy boundary.
- `terminal/ShellSession.kt` — persistent shell transport and prompt handling.
- `terminal/DistroManager.kt` / `BootstrapManifest.kt` — pinned bootstrap and Android-safe native executable mapping.
- `terminal/Hg2PackageManager.kt` — package transactions.
- `terminal/Hg2Downloader.kt` — resumable/reusable downloads.
- `terminal/DpkgCatalog.kt` / `AptCatalog.kt` — installed ownership/version data and repository catalog.
- `terminal/PackageLifecycleStore.kt` — manager inventory plus Enable/Disable/Reset/Isolate state.
- `terminal/PackageIsolation.kt` — private-root isolation and filesystem audit.
- `terminal/ExecutionAuthority.kt` — ADB/root availability and command construction.
- `ui/menu/CommandTree.kt` — live command tree.
- `ui/menu/PackageLifecycleTree.kt` — installed-package and authority UI.

## Build

Current toolchain is defined by the repository, notably `gradle/libs.versions.toml` and `composeApp/build.gradle.kts`:

- JDK 21
- AGP 9.3.2
- Kotlin 2.4.10
- Compose Multiplatform 1.12.0
- `compileSdk` / `targetSdk` 37
- `minSdk` 24
- NDK `29.0.14206865`

```bash
./gradlew assembleDebug
```

## Documentation

- [Vision](docs/VISION.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Android architecture](docs/HG2GUI_ARCHITECTURE.md)
- [Commands](docs/COMMANDS.md)
- [Design](docs/DESIGN.md)
- [User guide](docs/USER_GUIDE.md)
- [Contributing](docs/CONTRIBUTING.md)

The large Guide manuscript, animation packets, style-lock prompts, images, videos, and HTML motion/design studies under `docs/` are creative/reference source material. They are intentionally not treated as a live description of the runtime architecture; the Markdown files linked above are the authoritative software documentation.
