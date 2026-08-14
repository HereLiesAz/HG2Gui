# Command Reference

## Shell

Real shell binaries aren't listed here — they aren't a fixed set. `CommandTree` discovers them
live from the Termux bootstrap's own `bin/`, matches each one back to the package that owns it
via dpkg's own bookkeeping (`DpkgCatalog`), and groups them into a root pill category
(`Package management`, `Network`, `Shells`, `Development`, `Text & files`, …) from a
hand-curated package → category map in `CommandTree.kt` — Termux's own packages carry no
Debian Section field to read a category from directly, so there's no live source of truth for
that part; `pkg install <package>` still makes its binaries appear as pills the next time a
command finishes, they just fall under "Other" until the map is taught what category they
belong in — `pkg` itself is one deliberate exception to the package-level rule: it ships as part
of `termux-tools` (mapped to "System"), but is filed under "Package management" instead, next to
`apt`/`dpkg`, since that's what it actually is. Binaries sharing a hyphenated prefix (`apt-get`,
`apt-key`, `apt-mark`, …) nest under one shared parent pill instead of appearing as unrelated flat
entries; `apt`, `apt-get`, `pkg`, and `dpkg` also seed a curated set of their own common
subcommands (`update`/`upgrade`/`install`/…) as pills, the same treatment `ls`/`cd`/`ps` get.

Before a bootstrap is installed, Shell offers exactly one pill: **`bootstrap`** — which also
runs automatically on first launch, the same way the official Termux app installs itself before
ever showing a prompt.

One binary gets a dedicated pill instead of the generic hint/file-picker treatment: **`ssh`**
(under Network) opens saved connection presets plus a **new…** pill that walks a short prompt
sequence — host, user, port, an optional private key path, and an optional preset name to save
it under — then drops the assembled `ssh` command onto the command line for you to review and
run. Host-key confirmation and password/passphrase prompts surface through the same interactive-
prompt mechanism every other command uses (a Yes/No Answer stack, or a masked free-text field for
passwords) — nothing ssh-specific happens at the shell layer.

A few more binaries are installed by the app itself rather than `pkg` — written into the
bootstrap's `bin/` right after it installs, and backfilled on launch for an older install that
predates them (`DistroManager.ensureBundledScripts`):

*   **`osint-lookup <domain>`**: self-scoped, read-only reconnaissance — whois, DNS records, and
    a certificate-transparency search (crt.sh) — for a domain you name, not a generic scanner.
*   **`net-inventory`**: this device's own interfaces, routes and DNS config. No arguments.
*   **`harden-check`**: audits this install's own SSH key permissions, listening sockets, and
    world-writable files under `$HOME`. No arguments.
*   **`sysinfo`**: installed packages, `PATH`, disk usage. No arguments.

See `docs/USER_GUIDE.md` for the full description of each.

Four more root pills sit alongside the shell categories, none backed by a shell binary:

*   **Workflows**: saved command templates. Each saved one is a pill that asks for any
    `{placeholder}` values the template needs, then drops the assembled command onto the command
    line to review and run — a `new…` pill saves one from a name plus a template string.
*   **AI**: one `chat…` pill opening a natural-language-to-command chat screen (requires an
    Anthropic API key set in Settings → AI). Never runs anything itself — a suggested command
    shows a USE pill that hands it to the command line the same way. A command reply may also
    include a "what each part does" breakdown, one line per flag.
*   **Context**: pick which OS's commands the tree should also offer — `local` (the default,
    turns this off) or a reference set for `ubuntu`/`macos`/`windows` (a synthesized root pill
    alongside the shell categories, e.g. `apt`/`systemctl`/`git`/`ssh` for Ubuntu). For working
    over an active `ssh` connection into a host of that kind, where the local Termux PATH tells
    you nothing about what's actually there — `git`/`ls`/`ssh` genuinely overlap with what's
    locally real, the package/service managers don't. Picking one just assembles the command onto
    the command line like any other pill; nothing runs automatically.
*   **Store**: one `search…` pill opening the azphalt.store package browser — the same idea as
    `pkg`/`apt`, but for `.azp` extensions (assets, code, packs, companion apps, scripts,
    MCP-server headers, and AI-skill bundles). Filter by kind, then INSTALL downloads and unpacks
    a package. Skill content augments AI chat; script packages resolve declared Termux dependencies
    and install a verified entry-point wrapper. Other kinds remain download-only. Every install
    enforces path containment, requires each extracted payload to match its declared SHA-256 digest,
    verifies any Ed25519 signature,
    and shows its trust state; a digest or signature failure rejects the package. See
    `docs/USER_GUIDE.md`.

## Built-in commands

These eleven are the only commands HG2Gui itself implements — see `terminal/Builtins.kt`. Every
other verb (app launching, file sharing, `alias`, editing with `nano`/`vim`, and so on) is a
real shell binary now, not a reimplementation of one.

### Device
*   `wifi`: Show Wi-Fi state and open the quick panel to change it (a third-party app can't
    toggle Wi-Fi directly on modern Android).
*   `bluetooth`: Request to enable Bluetooth, or open settings to disable it (no public disable
    API for third-party apps since Android 13).
*   `airplane`: Open airplane mode settings — no app can toggle this directly on any supported
    Android version.
*   `flash`: Toggle the flashlight (requests camera permission on first use).
*   `volume`: `volume` / `volume get [stream]` to read levels, `volume set <stream> <0-100>`,
    `volume profile <normal|vibrate|silent>`. Streams: `call`, `system`, `ring`, `media`,
    `alarm`, `notification`.
*   `brightness`: `brightness <0-100>` or `brightness auto` (requests the "modify system
    settings" permission on first use).

### Apps & navigation
*   `call <name or number>`: Place a call, resolving a contact name via `ContactManager`.
*   `contacts <ls|add|about|edit|rm> [number]`: List, add (via the system Contacts app), look up,
    edit or remove a contact.

### Features
*   `vfs <ls|cd|pwd|mkdir|touch|cat|rm|mv|cp|mount> [args]`: The sandboxed filesystem rooted at
    the app's private storage — also browsable graphically via the Files screen. `vfs mount`
    bind-mounts it onto a real path and requires root.
*   `calc <expression>`: Evaluate a math expression (`+ - * / ^ sqrt sin cos tan`).
*   `edit <file>`: Open the in-app text editor. `ShellSession` has no pty, so a real terminal
    editor would render garbled if run through it; `edit` is a plain Compose screen instead. Also
    registers as a `text/*` VIEW/EDIT handler, so other apps can open files in it.
