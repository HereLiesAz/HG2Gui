# Future Vision

## Goal
The ultimate "Hitchhiker's Guide" terminal for Android — a device that feels like a powerful,
futuristic tool and stays usable with one thumb. **A terminal app, not a launcher.** It does
not replace the home screen; it sits alongside everything else and is opened when you want a
shell.

The premise: a real command line is unusable on a phone because typing one is miserable. So
the menu *is* the interface. Every command, subcommand and argument is a pill you tap.

## Roadmap

### Phase 1: Foundation (Complete)
-   [x] Decouple from legacy launcher features.
-   [x] Build the "point-and-click" command menu system.
-   [x] Basic OS context switching simulation.
-   [x] Drop the launcher identity: no HOME category, normal task lifecycle, present in
        recents, keyboard no longer forced open.
-   [x] Rebuild the UI in Kotlin and Jetpack Compose.
-   [x] Adopt the Azphalt visual system — the capsule as the only primitive.

### Phase 2: Enhanced Simulation
-   [x] **Sessions:** Named sessions with independent scrollback, working directory and
        history — switched via tabs inside the one terminal screen. Not a separate recents
        entry per session: that would reintroduce the multi-task/launcher-lifecycle
        complexity Phase 1 deliberately dropped, for a benefit real terminal apps (Termux
        included) don't bother with either.
-   [x] **Virtual Filesystem:** Rooted, by default, at the app's private storage (`VfsManager`,
        `filesDir/home` — the real Termux `$HOME` every shell command already reads and writes,
        not a separate empty pocket) — real files, invisible to every other app and cleared on
        uninstall, opt-in-switchable to real device storage (DEV-STORAGE-1). Reached two ways:
        the **Files** screen (graphical, tree-style, Azphalt capsules — no icons, folders read
        by a trailing `/` and a hue) for browsing/creating/deleting, and the `vfs` command
        (`vfs mkdir`, `vfs ls`, `vfs cat`, …) for the same
        operations from the command line.

        This does **not** virtualize the real shell. `ls`, `cat`, `rm` and friends still run
        for real against real storage — `TerminalEngine` only intercepts a command whose verb
        exactly matches an app builtin, and there is no pty/FUSE layer to intercept anything
        else. A literal transparent VFS underneath arbitrary shell input isn't buildable on a
        non-rooted phone, which is why the sandbox is namespaced under `vfs` rather than
        shadowing `ls`/`cd`/`cat` — those verbs are already offered as real-shell pills, and
        shadowing them would have silently broken that.

        "Mount" is real, not simulated, but conditional: `vfs mount <path>` bind-mounts the
        sandbox onto a real path with `su`, and only runs when root is available
        (`Shell.SU.available()`, from the vendored `libsuperuser`). Without root it says so
        plainly rather than pretending to work.
-   [x] **A real Termux backend.** The shell isn't Android's own toybox pretending to be
        Linux — `DistroManager` downloads and extracts the actual Termux bootstrap (the same
        rootfs archive the official app installs), automatically on first launch, giving real
        `bash`, `apt`/`pkg`, and coreutils. The command tree reflects it live: shell pills are
        discovered from the bootstrap's own `bin/`, matched to the package that owns each one
        via dpkg's own bookkeeping, and sorted into a category by hand (Termux's own packages
        carry no category metadata of their own to read this from), so installing a package
        adds pills without touching the app. Since the app has no live
        PTY for a shell's own line editor to attach to, the conveniences one would usually get
        from shell plugins — autosuggestion, "did you mean", alias hints, a graphical file
        picker, graphical yes/no answers to an interactive prompt — are implemented natively as
        pills instead.
-   [x] **Installable-package browsing.** The `install` pill under `apt`/`apt-get`/`pkg` no
        longer requires typing a package name blind — it drills into a categorized, browsable
        catalog parsed from `apt update`'s own downloaded index (`AptCatalog`), the same
        "menu is the interface" premise applied to the one place it was still missing.
-   [x] **Auto-discovered command flags.** Beyond the hand-curated hint list, every real binary
        on PATH gets its `--help` output probed once in the background and parsed into tappable
        flag pills (`HelpCatalog`) — narrowing, not replacing, how much of "anything Termux
        handles" still requires typing a flag from memory.
-   [ ] **Scripting:** A custom scripting language or deeper Python integration for
        automating tasks within the terminal.

### Phase 3: Connectivity
-   [x] **SSH Client** *(landed narrower than originally scoped)*: saved connection presets plus
        a **new…** wizard that collects host/user/port/key and drops the assembled command on
        the line — not the original "tree populates from the remote host's own completions"
        vision, which would need a live connection to introspect. Host-key and password prompts
        reuse the same tap-only interactive-prompt machinery every other command gets.
-   [x] **Context switching:** a lighter-weight take on remote-host awareness than a live
        completions feed — the **Context** pill offers a static reference tree
        (package/service manager, `git`/`ls`/`ssh`) for `ubuntu`/`macos`/`windows`, for working
        over an active `ssh` session into that kind of host.
-   [ ] **Plugin System:** Community-created command menus (JSON/YAML) imported to extend the
        suggestion tree for `kubectl`, `aws`, and the rest. Not built as originally scoped; the
        **Store** below covers an adjacent but different need (installing whole packages, not
        authoring new menu branches for an existing binary).
-   [x] **The Store (azphalt.store).** Not in the original roadmap at all: a **Store** pill
        browses [azphalt.store](https://azphalt.store)'s package registry for `.azp`
        extensions — assets, sandboxed code, packs, companion apps, MCP-server headers, and
        AI-skill bundles — each install path-contained, SHA-256-checked, and Ed25519-signature-
        verified.
-   [x] **MCP server.** Also outside the original roadmap: an optional, loopback-only,
        explicit-start JSON-RPC server (Settings → MCP SERVER) a paired external AI agent can
        use to read/write the app's sandboxed files; running real shell commands through it is a
        second, biometric-gated switch, off by default.

### Phase 4: AI Integration
-   [x] **LLM Assistant:** a chat screen (the **AI** pill, an Anthropic API key you supply
        yourself) turns a plain-English request into a suggested shell command, optionally with
        a per-flag "what each part does" breakdown. It proposes; it never runs — the reply's
        **USE ▸** pill drops the command onto the input line for you to review and press Run,
        same as every wizard-produced command in the app.

## Non-goals
-   Being a home screen. The launcher lineage is where this came from, not where it is going.
-   A widget grid, an app drawer, or anything else that competes with the system launcher.
-   Hiding the shell behind a chat box. The command line stays visible and literal.
