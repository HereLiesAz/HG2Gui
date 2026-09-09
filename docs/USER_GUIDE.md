# User Guide

## Getting started

HG2Gui is a terminal application for Android. It does not replace the launcher or home screen. Open it like any other app.

Its main difference from a traditional terminal is the input model: **if HG2Gui can discover the valid choices, you tap them instead of typing them.** The keyboard is for genuinely open-ended values.

On first use, HG2Gui installs its pinned Termux-derived runtime into app-private storage. Bootstrap/package progress appears in the output card.

## The terminal screen

The terminal contains:

- **session tabs** — each session keeps its own shell state, history, working directory, and output;
- **working directory** — where the next normal shell command will run;
- **command input** — the command assembled from pills and/or typed text;
- **RUN** — the explicit execution action;
- **terminal modifier keys** — controls such as Ctrl, Alt, Esc, Tab, and history arrows;
- **command tree** — live shell commands plus HG2Gui features;
- **output/result cards** — command records; the active result auto-scrolls as new output arrives.

## Building a command by touch

1. Tap a root/category.
2. Tap a command.
3. Tap any subcommand/options that apply.
4. Supply any remaining operand using the appropriate UI.
5. Press **RUN**.

**Normal command-tree leaves do not auto-run.** A leaf may still need a positional value that was not discoverable from its help output, so selecting it means “compose this command,” not “execute now.”

### What happens when another value is needed

HG2Gui chooses the input surface by value type:

- **package name** → package/catalog pills;
- **installed package** → installed-package pills;
- **file or directory** → graphical file/folder picker;
- **finite known choice** → pills;
- **yes/no prompt from a running command** → confirmation dialog;
- **URL, host, search pattern, arbitrary text, etc.** → text field.

When text really is required, HG2Gui focuses the input box and tells you what kind of value it expects, for example **TYPE URL**, **TYPE HOST OR ADDRESS**, or **TYPE ARGUMENT** when no more specific type can be inferred.

## Interactive commands

A command that is already running may stop and ask a question. That is different from composing a new command:

- yes/no → modal **YES / NO** confirmation;
- numbered/bracketed choices → tappable choices;
- password/passphrase → masked input;
- other open input → text field and **Send**.

Package installs handled by HG2Gui do not expose apt's normal `Do you want to continue? [Y/n]` prompt.

## Installing packages

Use the package-manager pills rather than memorizing package names.

For `pkg`/`apt`/`apt-get`, the **install** branch reads HG2Gui's downloaded APT package index and presents packages by category. If the index has not been downloaded yet, run/update the package index first.

Mutating top-level `apt`/`apt-get` commands are routed through HG2Gui's Android-safe package transaction layer. This is deliberate: upstream apt can download correctly but otherwise hands the transaction to dpkg using assumptions inherited from the Termux app's own package path and Android environment.

HG2Gui's package layer relocates package payloads into HG2Gui's prefix, repairs relevant control metadata, runs maintainer scripts through bundled Bash instead of direct-executing them from writable app data, and keeps dpkg's database/install root under HG2Gui.

Downloads are reused/resumed where possible instead of starting over every time.

## Managing installed packages

Open:

**Packages → package manager → installed package**

HG2Gui currently discovers installed packages from:

- **Termux / pkg**
- **Python / pip**
- **Node / npm**
- **Ruby / gem**

A package can expose these actions depending on its manager and metadata:

### Run

Shows executable commands HG2Gui can attribute to the package. Selecting an executable composes it on the command line; press **RUN** after supplying any needed arguments.

### Disable / Enable

**Disable** does not uninstall the package. It remains visible and installed, but HG2Gui blocks its owned commands from running. Use **Enable** to make them runnable again.

### Update

Uses the package's owning manager to update/reinstall it appropriately.

### Reset

Keeps the package installed while clearing runtime state HG2Gui can safely attribute to it.

For normal packages this includes conventional package-scoped cache/config/data/state/log locations and paths HG2Gui positively observed being created during use. HG2Gui deliberately does not guess that every arbitrary user file with a similar name belongs to a package.

For isolated packages, Reset removes the entire private runtime. The next run reseeds it cleanly from the installed version.

Reset asks for confirmation.

### Remove / Purge

Uses the owning manager's removal operation. **Purge** appears where the manager has meaningful purge semantics. Destructive actions ask for confirmation.

## Isolating a package

Open:

**Packages → manager → package → Isolate**

Isolation is HG2Gui-owned. It is not dependent on whether `apt`, `pip`, `npm`, or `gem` has a sandbox feature.

When an isolated package runs:

- HG2Gui creates/seeds a private filesystem root for it;
- the package sees a private copy of the HG2Gui runtime and a private HOME/XDG state tree;
- the real HG2Gui prefix/home are not bind-mounted into the guest;
- common privilege tools (`adb`, `su`, `tsu`, `magisk`, `proot`) are removed from the private prefix;
- common host `su` paths are masked where present;
- after the run, HG2Gui reports which paths inside the private root were created, modified, or deleted.

If the package is marked isolated but no executable PRoot isolation engine is available, **the command fails closed**. HG2Gui will not quietly run it outside the sandbox.

Use **Release isolation** to return the package to normal execution.

### What isolation currently observes

The audit is a private-filesystem before/after comparison. It gives strong visibility into filesystem changes in the sandbox, but it is **not yet a full syscall/network/process trace**.

## Execution authority

Open:

**Packages → Authority**

Authority is separate from packages and separate from isolation.

### App

Normal default execution. Commands run with HG2Gui's Android application UID and permissions.

### ADB shell

When an executable ADB client is available, HG2Gui exposes:

- **Devices**
- **Pair**
- **Connect**
- **Disconnect**
- **Shell**

If no ADB client is available, install `android-tools` through the package UI.

For same-device access, Android Wireless Debugging still controls pairing and connection. HG2Gui does not bypass Android's ADB authorization model.

Running an ADB-shell command asks for explicit confirmation first.

### Root

If HG2Gui detects an executable `su` provider, Root exposes:

- **Test root** — requests root and runs `id`;
- **Root command** — executes a chosen command through `su -c`.

HG2Gui asks for confirmation before the elevated request. The device's root manager still decides whether root is actually granted.

### No inherited privilege

A package does not gain ADB/root just because HG2Gui can use those backends. In particular, isolated packages have privilege tools stripped/masked from their private runtime.

Headless/MCP command execution cannot invoke ADB-shell or root authority through `hg2auth`.

## Files and path selection

When HG2Gui recognizes that a command needs a file or directory, it opens the graphical picker instead of telling you to type a path.

The broader **Files** surface provides graphical browsing and file operations over HG2Gui's managed storage/device-storage modes. This is separate from package isolation: the Files/VFS sandbox is a user-facing filesystem feature; package isolation is a per-package private execution root.

## SSH

The `ssh` command has a dedicated branch with saved presets and a **new…** flow. Host, user, port, and key choices are assembled into the command for review. Key paths use the graphical picker. Host-key yes/no questions and password/passphrase prompts use the same generic interactive prompt UI as any other command.

## Workflows

**Workflows** stores command templates such as:

```text
git commit -m "{message}"
```

Running a workflow collects placeholder values and puts the completed command on the input line. It does not execute until you press **RUN**.

## AI

The **AI** surface can turn a natural-language request into a suggested shell command. A suggestion is inserted into the command line for review; it does not execute automatically.

AI authority does not bypass package isolation, package disabling, or the explicit ADB/root authority rules enforced by `TerminalEngine`.

## Context

**Context** adds a static reference command tree for selected remote OS families (for example Ubuntu, macOS, Windows) while you work over SSH. This is reference composition, not live discovery of the remote machine.

## Store

The **Store** browses azphalt.store `.azp` packages. `.azp` packages are separate from the Termux/dpkg/pip/npm/gem package lifecycle described above. Store packages have their own extraction, digest, and signature/trust rules.

## MCP server

HG2Gui can expose an explicit-start, loopback-only MCP server. Shell execution has its own gate, but MCP/headless callers still cannot use `hg2auth` to obtain ADB-shell or root authority.

## Output cards

The newest output in the active command record stays in view as stdout/stderr arrives. Tap completed records for the actions offered by that surface, such as copying or reusing output/commands.

## Built-in Android commands

HG2Gui retains a small explicit set of Android-facing built-ins for capabilities that are not represented honestly by ordinary Termux binaries, including device controls/settings bridges, calls/contacts, VFS, calculator, and editor entry.

See [COMMANDS.md](COMMANDS.md) for the current command/control reference.

## Compatibility expectations

HG2Gui uses a real Termux-derived userspace, but it runs under a different Android application ID and security context. Many Termux packages work after HG2Gui's relocation/maintainer-script compatibility transformations; successful real-device examples include `python-pip`, `nsnake`, and `curl`.

Do not interpret that as “every Termux package must work unchanged.” Native executables, services, hardcoded paths, unusual interpreters, symlinks, dependency metadata, triggers, alternatives, and linker assumptions can expose additional compatibility work.

When a package fails, the goal is to fix the **class of incompatibility**, not add a one-off package-name exception unless the package truly has unique semantics.
