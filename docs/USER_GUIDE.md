# User Guide

## Getting Started
HG2Gui is a terminal app. Open it like any other app — it does not take over your home screen.
It gives you a real command line that you can drive without typing.

## Interface
From the top:

-   **Session tabs:** `main` and any you add with `+`. Each session keeps its own scrollback,
    history and working directory.
-   **Working directory:** the user and path the next command will run in.
-   **Command line:** the command you are assembling, as pills, and the **Run** capsule.
-   **Modifier keys:** `ctrl` `alt` `esc` `tab` `↑` `↓` — the keys a terminal needs that a
    phone keyboard does not have.
-   **Command tree:** the suggestion tree. This is the input method.

## Building a command by tapping
1.  Tap a category — a shell category (`Admin`, `Utilities`, `Network`, `Shells`, …, discovered
    from what's actually installed), `System`, `Apps & nav` or `Features`. The stack slides away
    and that pill drops to the bottom of the screen; it is now the host.
2.  Its commands cascade upward from it. Tap one.
3.  Its arguments cascade upward in turn. Tap one.
4.  If that was the last parameter the command needs, it **runs immediately** — no separate
    confirmation. Otherwise press **Run** whenever you're ready; you don't have to pick every
    pill a command offers.

Tap the host pill at the bottom to go back a level. A `file…` pill anywhere in the tree opens a
graphical file browser through the same stack — no separate screen, no typing a path.

## When a command needs more from you
Some commands stop mid-run to ask something. A yes/no question gets a dedicated **Answer**
stack — tap `YES` or `NO`, same as picking any other pill. Anything else falls back to the
input field: its hint shows the question, and **Run** becomes **Send**.

## Suggestions
Below the command tree, a **Suggest** pill appears when there's something to offer: the rest of
a command you've typed before (tap to complete it), a shorter alias for the command you just
ran (`gs` for `git status`, and friends), or a correction after a command isn't found. These are
implemented natively, not by a shell plugin — tap one the same way you'd tap any other pill.

## Typing instead
The keyboard is not forced open. Tap the command line to type; suggestions still filter as you
go, so you can type `git` and tap `commit` rather than spelling it out.

## Basic Commands
The ten built-in commands are `wifi`, `bluetooth`, `airplane`, `flash`, `volume`, `brightness`,
`call`, `contacts`, `vfs`, `calc` and `edit` — see [Commands](COMMANDS.md) for what each does.
Everything else — `apps`, `alias`, `clear`, listing packages, and so on — is a real shell
binary now, run in the Termux environment below, not a reimplementation of one.

## Advanced Usage
-   **Shell:** a real Termux environment, not Android's own limited toybox shell — genuine
    `bash`, `apt`/`pkg`, and coreutils. It installs itself automatically the first time you open
    the app (watch the buffer for progress); `pkg install <package>` afterward adds pills for
    whatever it installs.
-   **Root:** on a rooted device, `su` runs privileged commands.
-   **History:** the `↑` and `↓` modifier keys walk the current session's history.
-   **Editor:** `edit <file>` opens the in-terminal editor — `ShellSession` has no pty, so a
    real terminal editor would render garbled if run through it. It also registers as a text
    file handler, so other apps can open files in it.
-   **Files:** tap the **FILES** pill and it grows into the whole screen — the pill's own colour
    runs out around the edge, closes into a frame, and a vertical wipe reveals the file explorer
    already inside it, browsing a sandboxed filesystem rooted at the app's private storage (the
    `vfs` command's backing store) — separate from the real Termux filesystem the shell operates
    on. A folder is a capsule: tap one to expand it while its siblings squish into thin coloured
    rods beside it, two levels deep before you're looking at a plain list of what's inside; a
    yellow **…** chip drops in next to **Close** whenever there's a level open, to step back up
    one. From there: search across the whole sandbox, sort by name or newest, tap **Select** to
    multi-select for a batch Move, Copy, Share or Delete, rename anything in place, and a folder
    that's mostly photos renders itself as a thumbnail grid automatically — no manual list/grid
    toggle. **Storage** breaks down what's using space by type, and names the largest files.
-   **The Guide:** the command picker (reached the same way as the Files screen) has its own
    **THE GUIDE** pill in the top corner, opening a chaptered glossary of real commands paired
    with invented, Hitchhiker's-Guide-style definitions — reading material, not another way to
    run something.
-   **SSH:** the `ssh` pill (under the Network shell category) opens your saved connections plus
    a **new…** pill that asks for host, user, port and an optional key file one step at a time,
    then hands you the assembled command to run. Host-key confirmation and password/passphrase
    prompts work exactly like any other command's interactive prompts — a tap for yes/no, a
    masked field for a password.
-   **MCP server:** Settings → **MCP SERVER** starts a loopback-only server (reachable via
    `adb forward`, never over the network) that an external AI agent can pair with using a
    pairing token shown on that screen, to read/write this app's sandboxed files. Running real
    shell commands through it is a separate switch, off by default, that asks for a biometric
    confirmation the first time you turn it on — the one setting here that lets a paired agent do
    more than touch its own sandbox.
-   **osint-lookup:** installed automatically alongside the bootstrap, under Shell → Other (it
    isn't part of any package, so it doesn't get a curated category). `osint-lookup <domain>`
    runs whois, DNS records (A/AAAA/MX/TXT/NS) and a certificate-transparency search (crt.sh) for
    a domain you name — your own, or one you have a legitimate reason to look up. All three are
    passive lookups against publicly published data; it never scans, brute-forces or contacts the
    target's own infrastructure, and it isn't a tool for looking up arbitrary third parties.
    Missing tools (`whois`/`dig`/`curl`/`jq`) print a `pkg install` hint instead of failing
    silently.
-   **Blocks:** tap any entry in the buffer to reveal **COPY**, **RE-RUN** and **SHARE** — copy
    grabs the output (or the command itself, if there's no output yet), re-run drops the command
    back onto the input line for you to review and press Run (it never runs again on its own),
    and share hands the same text to the system share sheet.
-   **ASCII art rendering:** output that looks like ASCII or box-drawing art (three or more lines,
    heavy on symbols rather than letters — `cowsay`, `figlet`, `jp2a`, `chafa`, and the like)
    is traced into one smooth flat vector shape (the same contour-tracing idea a Potrace-style
    vectorizer uses) instead of literal monospace glyphs, so it reads as a constructed picture
    rather than a wall of text. The heuristic is conservative — real prose or a table always falls
    back to plain text. Tap the entry, then **PLAIN TEXT** to see the raw output (and copy/share
    it) any time.
-   **Typeset output:** output that's a block of `label: value` lines (`ifconfig`, `stat`,
    `dpkg -s`, and similar — three or more lines, every one of them that shape) is set as a
    two-column grid instead of raw monospace text — labels dimmed and uppercase, values
    right-aligned. Tap the entry, then **PLAIN TEXT** to see exactly what the command printed any
    time — a reading can be wrong.
-   **Workflows:** the **Workflows** pill (alongside the shell categories) holds saved command
    templates. Picking one asks for any `{placeholder}` values the template uses, then drops the
    assembled command onto the input line, same as everything else here — nothing runs until you
    press Run. A **new…** pill saves one: give it a name, then a template like
    `git commit -m "{message}"`.
-   **AI:** the **AI** pill opens a chat screen — type a request in plain English and it suggests
    a shell command (or a short plain-text answer if the request isn't command-shaped), via the
    Anthropic API. A suggested command shows a **USE ▸** pill that drops it onto the input line
    for review, exactly like everything else — the chat never runs a command by itself. Needs an
    API key first: Settings → **AI SETTINGS ›**. The key is stored unencrypted on this device,
    same as the MCP pairing token and other local settings.
-   **Store:** the **Store** pill opens a browser for [azphalt.store](https://azphalt.store), the
    package registry for `.azp` extensions — the same idea as `pkg`/`apt`, but general-purpose:
    assets, sandboxed code, packs, companion apps, MCP-server headers, and AI-skill bundles all
    ship as `.azp` packages. Search, filter by kind (skill / mcp / code / pack / asset / app), and
    tap **INSTALL** to download and unpack one. HG2Gui has no `.azp` execution sandbox, so most
    kinds are download-only, kept on-device for use elsewhere — the one exception is a **skill**
    package: its bundled `SKILL.md` is folded into the AI chat's system prompt automatically, so
    an installed skill actually changes how the AI pill answers. Packages are trusted as
    downloaded — this v1 does not verify the Ed25519 signature azphalt packages carry, so treat it
    the same as any other unverified download.
-   **net-inventory / harden-check / sysinfo:** three more scripts installed alongside
    `osint-lookup`, all local, no arguments needed. `net-inventory` lists this device's own
    network interfaces, routes and DNS config. `harden-check` audits this install's own SSH key
    permissions, lists what's actually listening locally, and flags any world-writable file under
    `$HOME`. `sysinfo` reports installed packages, `PATH`, and disk usage. Nothing here touches
    another host — every check is against files, sockets or config that already belong to this
    device.

## Related Resources
The shell is genuine Termux underneath, so third-party Termux material works here too:

-   [termux-scripts](https://github.com/schnatterer/termux-scripts) — shell scripts for backing
    up and restoring Android apps over local, SSH, or cloud storage, with incremental encrypted
    transfers.
-   [termux.holehan.org](https://termux.holehan.org/) — an APT repository adding extra packages
    (Hugo, sift) installable with `pkg install` once added as a source.
-   [termux-commands-free](https://github.com/Mortarelplait/termux-commands-free) — a free
    reference collection of 200+ Termux commands and tutorials, organized by category.
