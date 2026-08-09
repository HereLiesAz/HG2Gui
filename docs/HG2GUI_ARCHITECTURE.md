# HG2Gui Android Architecture

## Overview
HG2Gui is a touch-optimised terminal emulator for Android. It is **not a launcher** — there is
no `category.HOME` filter, no launcher lifecycle behaviour, and no app drawer.

The UI, execution layer, and the ten built-in commands are all Kotlin. There is no separate
reflection-based command engine underneath it — built-ins are a fixed dispatch table.

## Core Components

### 1. `TerminalActivity` (Kotlin)
*   **Role**: The entry point and Compose host.
*   **Responsibility**:
    *   Builds the first `TerminalEngine`/`ShellSession` pair, and runs the bootstrap installer
        automatically if the Termux prefix isn't there yet.
    *   Normal activity lifecycle: present in recents, keeps state, `singleTop`.
    *   Persists `fullscreen`/`font_scale_percent` via plain `SharedPreferences`.
    *   Nothing else. All presentation lives in composables.

### 2. `TerminalScreen` (Compose)
*   **Role**: The whole terminal surface.
*   **Responsibility**: Session tabs, working directory, the command line and its Run capsule,
    modifier keys, the output record tile, and the `PillMenu`. It is a function of the command
    tree plus one `onRun` callback — it holds no reference to a manager.

### 3. `PillMenu` (Compose)
*   **Role**: The suggestion tree — the app's primary input method.
*   **Responsibility**: The tree's state machine (browsing, leaving, open) and its motion.
    Each pill is an `Animatable`; the choreography is specified in [DESIGN.md](DESIGN.md).

### 4. `Builtins` (Kotlin, the ten commands the shell can't provide)
*   **Role**: The fixed dispatch table for the ten built-in commands.
*   **Responsibility**: `wifi`, `bluetooth`, `airplane`, `flash`, `volume`, `brightness` (system
    toggles Android exposes no shell path to), `call`/`contacts` (via `ContactManager`), `vfs`
    (the sandboxed filesystem), `calc` (via `util/CalculationEngine.kt`), `edit` (opens
    `EditorActivity`). A plain `fun run(context, line): String` — no per-command class, no
    reflection, no interface to implement to add one.

### 5. `TerminalEngine` (Kotlin) and `ShellSession` (Kotlin, `expect`/`actual`)
*   **Role**: Execution, and the seam between the two worlds.
*   **Responsibility**: `TerminalEngine` routes a line — `bootstrap` streams from
    `DistroManager`, a `Builtins.NAMES` verb goes to `Builtins.run` for a single synchronous
    result, everything else to `ShellSession`. `ShellSession` picks the best of three shell
    tiers: a bundled static `zsh`, a real Termux bootstrap (`DistroManager`) if installed, or
    bare `/system/bin/sh` as the last resort; whichever wins stays alive for the session and
    frames commands with a sentinel that reports exit status and working directory, so `cd`
    persists. It reads output as a raw buffer rather than line-by-line, since a real prompt
    never prints its own trailing newline while it waits for input — an idle gap with an
    unterminated tail is treated as a live prompt and surfaced through a callback instead of
    blocking forever.
*   **Limit**: no pty. Full-screen, cursor-addressing programs are out of scope; the UI renders
    discrete records, not a scrollback with a cursor in it. That's why `edit` is its own Compose
    screen rather than an attempt to run `nano` through this. Autosuggestion, "did you mean", and
    alias hints are implemented natively in Kotlin (`ShellAliases.kt`) as ordinary pills rather
    than as a shell-side line editor plugin — there's no live PTY for one to attach to.

### 6. `CommandTree` (Kotlin, `androidMain`)
*   **Role**: Turns the fixed `Builtins` list and the shell's own PATH into the menu.
*   **Responsibility**: The ten built-ins group into System / Apps & nav / Features from a fixed
    list, with argument hints from a static map, per [COMMANDS.md](COMMANDS.md). Real shell
    binaries are discovered live from the Termux bootstrap's `bin/` (never Android's own
    `/system/bin` — Termux itself never depends on that either), matched back to the package that
    owns each one via dpkg's own bookkeeping (`DpkgCatalog.binariesByPackage`), and grouped into
    one root category per a hand-curated package → category map in `CommandTree.kt` itself —
    Termux's own packages carry no Debian Section field to read a category from directly
    (verified against a real bootstrap: none of its 82 base packages have one), so there is no
    live source of truth for this part, only for which package owns a binary. Within a category,
    binaries sharing a hyphenated prefix (`apt-get`, `apt-key`, `apt-mark`) nest under one host
    node instead of appearing as unrelated flat entries. Before a bootstrap exists, Shell offers
    exactly one pill: `bootstrap`.
*   `FileBrowser` (also `androidMain`) supplies the file-argument case: a `file…` node whose
    children are resolved lazily from a real directory listing (`MenuNode.resolveChildren`),
    attached to `edit` and to every discovered shell binary.

### 7. `McpServerService` and the `mcp` package (Kotlin, `androidMain`)
*   **Role**: An optional, loopback-only JSON-RPC 2.0 server (`mcp/McpServerService.kt`) an
    external AI agent can pair with — the app-embedded equivalent of a standalone Termux MCP
    server. Started/stopped only by explicit user action from Settings → MCP server
    (`ui/McpServerScreen.kt`), never on its own.
*   **Protocol**: `mcp/McpJsonRpc.kt` frames messages newline-delimited, matching MCP's own stdio
    transport, over a raw `ServerSocket` bound to `127.0.0.1` — never LAN-exposed. A pairing
    token (generated fresh per server start, memory-only) is required as one bespoke line before
    any JSON-RPC traffic is accepted.
*   **Tools**: `mcp/McpTools.kt` exposes two groups. `vfs.*` (`list`/`read`/`write`/`mkdir`/
    `delete`/`move`/`copy`) wraps `VfsManager` 1:1, inheriting its sandbox-escape-proof `resolve`
    for free, and is available whenever the server is running. `shell.*` (`exec`) wraps a
    Service-owned `TerminalEngine`, kept separate from the user's own visible terminal tabs, and
    is gated behind a second, explicit toggle — off by default, and only enableable through a
    `BiometricPrompt` confirmation (`TerminalActivity.requestEnableShellExec`). The gate is
    enforced server-side, in `McpTools.callTool`, on every `shell.*` invocation — not just by
    what `tools/list` chooses to advertise (it always advertises both groups).

### 8. Blocks, Workflows, AI chat (Kotlin, `commonMain`/`androidMain`)
*   **Blocks**: no new state layer — `TerminalScreen`'s existing `BufferEntry` gained tap-to-
    reveal COPY/RE-RUN/SHARE actions. Copy/share are androidMain hooks (`ClipboardManager`,
    `Intent.ACTION_SEND`) passed down from `TerminalActivity`; re-run is pure `SessionUiState`
    mutation (writes the entry's command into `inputText`, never runs it). `BufferEntry` also
    calls `ui/AsciiArt.kt`'s `looksLikeAsciiArt()` on the entry's output and, when it matches,
    renders via `AsciiArtCanvas` instead of the plain monospace `Text` — a PLAIN TEXT toggle in
    the same tap-to-reveal row always falls back to the raw string. `AsciiArtCanvas` maps each
    character to a density (a light-to-dense ramp; box-drawing/block Unicode is treated as fully
    dense), then runs marching squares over that density grid (`buildContourPath`) to trace one
    smooth filled vector `Path` - the same contour-tracing approach a Potrace-style vectorizer
    uses, applied to the density field instead of raster pixels. No model, no network call: it's a
    deterministic, offline algorithm that constructs an actual vector shape from the art's
    character grid rather than reproducing glyphs or a blocky per-cell mosaic.
*   **Workflows** (`ui/WorkflowFlow.kt` commonMain, `managers/WorkflowStore.kt` androidMain):
    named command templates with `{placeholder}` substrings, stored the same flat-SharedPreferences
    way as `SshPresets`. Saving and running both reuse the `wizardId`/`onWizard` pill primitive and
    `SessionUiState.awaitPromptAnswer` exactly like the ssh wizard — a run asks one question per
    placeholder, then writes the rendered command into the input line for the user to review and
    press Run. `CommandTree.workflowsRoot` is a synthesized root pill (like `sys`/`apps`/`feat`),
    not a shell binary.
*   **AI chat** (`ai/AiClient.kt`, `managers/AiSettings.kt` androidMain; `ui/ai/AiChatScreen.kt`
    commonMain; `ui/AiSettingsScreen.kt` androidMain): single-turn natural-language → shell-command
    suggestion via the official Anthropic Java SDK (`com.anthropic:anthropic-java`), reached
    through a synthesized `ai` root pill (`wizardId = "ai-chat"`, used to navigate screens rather
    than collect prompt answers — a valid second use of the `onWizard` hook). The API key is
    user-supplied and stored in plain `SharedPreferences`, same posture as the MCP pairing token
    and SSH key paths. A suggested command is never executed automatically — the chat screen's
    USE pill hands it to the terminal's input line, same "assemble, don't auto-run" rule every
    wizard-produced command already follows; this deliberately does not duplicate the MCP server's
    biometric-gated `shell.exec` tool.

### 9. Store (azphalt registry client) (Kotlin, `commonMain`/`androidMain`)
*   **`azp/AzpClient.kt`** (androidMain): OkHttp client for the azphalt Repository API
    (`spec/repository-api.md` in `hereliesaz/azphalt`) against the live registry at
    `https://www.azphalt.store` — `search(query, kind, page)` (`GET /packages`) and
    `download(id, version)` (`GET /packages/{id}/versions/{version}/download`). Free packages
    only; paid packages need a Bearer entitlement this client does not obtain.
*   **`azp/AzpInstaller.kt`** (androidMain): a `.azp` is a plain ZIP archive with `manifest.json`
    at its root (`spec/package-format.md`) — `install()` unzips it into
    `filesDir/azp/<id>/<version>/` (rejecting any entry that would escape via `..`), reads
    `manifest.json`'s `kind`, and for `kind:"skill"` collects the declared `skill.skills[].id`
    list so the SKILL.md payloads can be found again later.
*   **`managers/AzpLibrary.kt`** (androidMain): flat-SharedPreferences record of installed
    packages, same shape as `WorkflowStore`/`SshPresets`. `installedSkillTexts()` reads each
    installed skill package's `skills/<id>/SKILL.md` off disk, capped to a fixed character budget,
    for `AiClient` to fold into its system prompt.
*   **`ui/azp/AzpStoreScreen.kt`** (commonMain): search box, kind-filter pills (all/skill/mcp/
    code/pack/asset/app), and a result list with an INSTALL/INSTALLED pill per package — same
    visual idiom as `AiChatScreen`. Reached via a synthesized `azp` root pill
    (`CommandTree.azpRoot`, `wizardId = "azp-store"` navigates to the screen, same reuse of
    `onWizard` as the AI pill).
*   HG2Gui has no `.azp` execution runtime (no WASM sandbox, no MCP client), so "install" for
    every kind except `skill` is download-and-unpack only — the package sits on-device for the
    user's own use elsewhere, the same as `apt download` versus `apt install`. Signature
    verification (the Ed25519 model in `package-format.md`) is **not implemented** in this v1;
    packages are trusted at download time, same posture as this app's other unverified local
    settings (the MCP pairing token, the AI API key).

### 10. Ground rotation (Kotlin, `commonMain`)
*   **`ui/menu/PillMenu.kt`**: `Azphalt.currentGround` is a process-wide `mutableStateOf(Ground)`
    (not `remember`-scoped) so every screen agrees on the same background, including
    `EditorScreen` - which runs in its own Activity (`EditorActivity`) with its own composition,
    so a per-composition roll would let it disagree with the rest of the app. `Azphalt.grounds`
    (Mustard weighted heavily as the primary, six others sharing the rest) and
    `Azphalt.randomGround(exclude)` (weighted pick) predate this; `currentGround` just makes one
    of those picks the shared, observable one.
*   `Azphalt.rerollGround()` swaps to a new weighted pick (never repeating the current one),
    capped at `Azphalt.MAX_GROUND_REROLLS` (2) per app process - "may reroll once, twice at most"
    per the style guide. The one call site is `TerminalActivity`'s `onNewSession`: opening a new
    session tab is a deliberate tap, never mid-gesture, so it's a safe moment to swap the
    background without fighting an in-flight pill animation.
*   Every screen that used to define its own local `PageYellow` gradient constant now calls the
    top-level `Azphalt.Ground.pageBrush()` extension (`Brush.linearGradient(page, foldDark,
    page)`, the same shape the old per-screen constants used) against `Azphalt.currentGround`
    instead - `TerminalScreen`, `SettingsScreen`, `McpServerScreen`, `AiSettingsScreen`,
    `AiChatScreen`, `AzpStoreScreen`, `CommandGuideScreen`, `GuideReaderScreen`, `FilesScreen`,
    `StorageScreen`, `FolderPicker`, `EditorScreen`.

## Data Flow
1.  **Input**: The user taps `wifi`. No text is typed. Since `wifi` leaves no further
    parameters, it runs immediately on that tap.
2.  **Assembly**: `TerminalScreen` holds the token `["wifi"]`.
3.  **Execution**: Run calls `onRun("wifi")` → `TerminalEngine.run`, off the main thread.
4.  **Routing**: `wifi` is a built-in, so `Builtins.run` handles it directly and returns a
    result string. An unrecognised verb would have gone to the shell instead, where a command
    that stalls waiting on stdin (a real prompt) suspends the caller until the UI supplies an
    answer rather than blocking the read loop forever.
5.  **Feedback**: The result becomes screen state and renders in a record tile.
