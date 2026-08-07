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
-   [ ] **Virtual Filesystem:** A persistent virtual filesystem so users can `mkdir`, `touch`
        and edit files without cluttering real Android storage, unless they explicitly mount
        it.
-   [ ] **Scripting:** A custom scripting language or deeper Python integration for
        automating tasks within the terminal.

### Phase 3: Connectivity
-   **SSH Client:** A full SSH client in the terminal, remote servers managed with the same
    point-and-click convenience — the tree populates from the remote host's own completions.
-   **Plugin System:** Community-created command menus (JSON/YAML) imported to extend the
    suggestion tree for `kubectl`, `aws`, and the rest.

### Phase 4: AI Integration
-   **LLM Assistant:** A local or API-based model that writes commands from natural language
    ("How do I untar a file?"). It proposes; it never runs. The proposed command arrives as
    editable pills, and you press Run.

## Non-goals
-   Being a home screen. The launcher lineage is where this came from, not where it is going.
-   A widget grid, an app drawer, or anything else that competes with the system launcher.
-   Hiding the shell behind a chat box. The command line stays visible and literal.
