# Future Vision

## Goal
To create the ultimate "Hitchhiker's Guide" style terminal for Android—a device that feels like a powerful, futuristic hacking tool but remains accessible via intuitive touch interactions.

## Roadmap

### Phase 1: Foundation (Current)
-   [x] Decouple from legacy launcher features.
-   [x] Implement "React-like" dark UI with scanline aesthetics.
-   [x] Build the "Point-and-Click" command menu system.
-   [x] Add basic OS context switching simulation.

### Phase 2: Enhanced Simulation
-   **Virtual Filesystem:** Implement a persistent virtual filesystem so users can `mkdir`, `touch`, and edit files without cluttering their actual Android storage, unless they explicitly mount it.
-   **Scripting:** Add support for a custom scripting language or deeper Python integration for automating tasks within the terminal.

### Phase 3: Connectivity
-   **SSH Client:** Integrate a full-fledged SSH client into the terminal, allowing users to manage remote servers using the same point-and-click convenience.
-   **Plugin System:** Allow community-created command menus (JSON/YAML based) to be imported, extending the suggestion tree for tools like `kubectl`, `aws`, etc.

### Phase 4: AI Integration
-   **LLM Assistant:** Embed a local or API-based LLM to generate commands from natural language queries (e.g., "How do I untar a file?").
