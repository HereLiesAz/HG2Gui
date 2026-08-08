# Command Reference

This document lists the built-in commands in HG2Gui — the legacy `commands/main/raw/` classes,
grouped exactly as the headings below (see `ui/menu/CommandTree.kt`).

## Shell

Real shell binaries aren't listed here — they aren't a fixed set. `CommandTree` discovers them
live from the Termux bootstrap's own `bin/`, matches each one back to the package that owns it
via dpkg's own bookkeeping (`DpkgCatalog`), and groups them into a root pill category
(`Package management`, `Network`, `Shells`, `Development`, `Text & files`, …) from a
hand-curated package → category map in `CommandTree.kt` — Termux's own packages carry no
Debian Section field to read a category from directly, so there's no live source of truth for
that part; `pkg install <package>` still makes its binaries appear as pills the next time a
command finishes, they just fall under "Other" until the map is taught what category they
belong in. Binaries sharing a hyphenated prefix (`apt-get`, `apt-key`, `apt-mark`, …) nest
under one shared parent pill instead of appearing as unrelated flat entries.

Before a bootstrap is installed, Shell offers exactly one pill: **`bootstrap`** — which also
runs automatically on first launch, the same way the official Termux app installs itself before
ever showing a prompt.

## System & Utilities
*   `airplane`: Toggle airplane mode.
*   `battery` / `status`: Show battery and system status.
*   `bluetooth`: Toggle or manage Bluetooth.
*   `brightness`: Adjust screen brightness.
*   `calc`: Simple calculator.
*   `call`: Make a phone call.
*   `clear`: Clear the terminal screen.
*   `config`: Manage settings.
*   `data`: Toggle mobile data (root may be required).
*   `exit`: Close the terminal.
*   `flash`: Toggle flashlight.
*   `location`: Manage location services.
*   `music`: Control music playback.
*   `notifications`: Read or clear notifications.
*   `refresh`: Refresh cached data (apps, contacts).
*   `restart`: Restart the app.
*   `share`: Share text or files.
*   `shell`: Execute native shell commands.
*   `time`: Show time or set alarms.
*   `vibrate`: Vibrate the device.
*   `volume`: Adjust volume levels.
*   `wifi`: Toggle WiFi.

## Apps & Navigation
*   `apps`: List installed applications.
*   `alias`: Manage command aliases.
*   `cntcts`: Search or list contacts.
*   `open`: Open a file or URL.
*   `uninstall`: Uninstall an app.
*   `search`: Web search.

## Features
*   `bootstrap`: Install the real Termux rootfs (`bash`, `apt`/`pkg`, coreutils). Runs
    automatically on first launch; only needed by hand to re-run or recover it.
*   `changelog`: View app changelog.
*   `devutils`: Developer utilities.
*   `donate`: Support the developer.
*   `guide`: Open the Hitchhiker's Guide (Help).
*   `help`: Show help for commands.
*   `htmlextract`: Extract content from HTML.
*   `notes`: Manage simple notes.
*   `panic`: Trigger the Panic Mode.
*   `rate`: Rate the app.
*   `regex`: Regex testing utility.
*   `rss`: RSS feed reader.
*   `session`: Create, switch or close terminal sessions.
*   `switchos`: Switch the simulated OS theme/context.
*   `theme`: Apply or manage themes.
*   `tui`: About T-UI.
*   `tuixt`: In-terminal text editor.
*   `tutorial`: Run the initial tutorial.

## Removed

`menu` and `surface` opened the graphical app-picker menus (Mitosis, Snake, Magnet, Origami).
Those are launcher surfaces for choosing apps from a home screen; the command tree replaces them.
