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
