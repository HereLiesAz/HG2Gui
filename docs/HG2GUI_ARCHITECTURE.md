# HG2Gui Android Architecture

## Overview
HG2Gui is a touch-optimised terminal emulator for Android. It is **not a launcher** — there is
no `category.HOME` filter, no launcher lifecycle behaviour, and no app drawer.

The UI, execution layer, and the eleven built-in commands are all Kotlin. There is no separate
reflection-based command engine underneath it — eleven built-ins are a fixed dispatch table.

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

### 4. `Builtins` (Kotlin, the eleven commands the shell can't provide)
*   **Role**: The fixed dispatch table for the eleven built-in commands.
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
*   **Limit**: no pty - a plain `ProcessBuilder` with piped stdin/stdout, not `/dev/ptmx`. Full-
    screen, cursor-addressing programs are out of scope; the UI renders discrete records, not a
    scrollback with a cursor in it. That's why `edit` is its own Compose screen rather than an
    attempt to run `nano` through this. Unfinished, not undesigned: the `terminal-emulator` module
    already bundles a real native pty bridge (`JNI.kt`'s `createSubprocess`, backed by
    `jni/termux.c` - the same one upstream Termux's `TerminalSession` uses) and the
    `TerminalEmulator` `ShellSession` already builds is exactly what that bridge is meant to feed
    live, not just replay output through after the fact. See the doc comment on `ShellSession`
    itself for why rewiring the app's one shared process-I/O path onto it stayed out of scope this
    round (no device in this environment to verify a change to code every terminal command runs
    through). Autosuggestion, "did you mean", and
    alias hints are implemented natively in Kotlin (`ShellAliases.kt`) as ordinary pills rather
    than as a shell-side line editor plugin — there's no live PTY for one to attach to.

### 6. `CommandTree` (Kotlin, `androidMain`)
*   **Role**: Turns the fixed `Builtins` list and the shell's own PATH into the menu.
*   **Responsibility**: The eleven built-ins group into Device / Apps & nav / Features from a fixed
    list, with argument hints from a static map, per [COMMANDS.md](COMMANDS.md). "Device" (the
    hardware toggles: wifi/bluetooth/airplane/flash/volume/brightness) is deliberately not called
    "System" - a shell-discovered category is also named "System" (procps/tmux/htop/util-linux and
    the like), and the two used to collide: two root pills both reading SYSTEM, distinguishable
    only by hue. Real shell binaries are discovered live from the Termux bootstrap's `bin/` (never
    Android's own `/system/bin` — Termux itself never depends on that either), matched back to the
    package that owns each one via dpkg's own bookkeeping (`DpkgCatalog.binariesByPackage`), and
    grouped into one root category per a hand-curated package → category map in `CommandTree.kt`
    itself — Termux's own packages carry no Debian Section field to read a category from directly
    (verified against a real bootstrap: none of its 82 base packages have one), so there is no
    live source of truth for this part, only for which package owns a binary. A small
    `CATEGORY_OF_BINARY` override map, checked after the package-level one, exists for the rare
    binary whose package doesn't reflect its actual role - `pkg` ships in `termux-tools` (which
    correctly maps to "System" for its other utility binaries), but `pkg` itself is Termux's own
    wrapper around `apt`/`dpkg`, and belongs in "Package management" with them. Within a category,
    binaries sharing a hyphenated prefix (`apt-get`, `apt-key`, `apt-mark`) nest under one host
    node instead of appearing as unrelated flat entries; `SHELL_HINTS` seeds a curated set of
    argument pills for the handful of binaries worth hand-picking for (`apt`/`apt-get`/`pkg`/
    `dpkg`'s own subcommands among them - a bare family host like `apt` shows its own hints
    *and* its hyphenated siblings, not just the siblings). Before a bootstrap exists, Shell offers
    exactly one pill: `bootstrap`.
*   **Overflow**: a category or a root stack can hold more pills than one screen's height at
    `PillMenu.kt`'s `ROW_PITCH` - `rememberStackScroll` tracks drag and decay-fling offsets, then
    snaps to the nearest row; the offset is unbounded in both directions, so a drag or fling can
    carry the stack past its first or last row, leaving blank space, with nothing snapping it
    back. The offset is applied on top of every pill's animated position, since the stack's
    absolute-`translationY` positioning can't use a stock `verticalScroll`/`LazyColumn` (Compose
    would size the scroll region to each pill's own viewport-sized Box, not to the stack's real
    extent). Used for both the root stack and any `ChildBand`. Whichever pill scrolls to rest at
    row 0 goes ink ("primed") purely as a cosmetic marker - selecting a pill always requires an
    explicit tap, never a scroll-and-wait. That marker only ever applies once the user has
    actually dragged or flung the stack at least once; before any input, nothing is primed, even
    if a pill happens to start out sitting at row 0.
*   `FileBrowser` (also `androidMain`) supplies the file-argument case: a `file…` node (attached
    to `edit` and to every discovered shell binary) whose `wizardId` opens the graphical Select
    File/Folder picker instead of drilling further into the pill stack - see section 13 below.

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
*   **Typeset output** (`ui/TypesetOutput.kt` commonMain): `BufferEntry` also checks
    `looksLikeKeyValueTable()` (only when the output isn't already art) and, when every non-blank
    line matches `label: value`, renders via `KeyValueTable` instead of plain text - a two-column
    grid with 16%-ink hairline rules, dimmed uppercase labels, and right-aligned values (tabular
    figure OpenType feature where the font supports it). A PLAIN TEXT/READING toggle in the same
    tap-to-reveal row switches back to the raw string. This is a deliberately narrow slice of
    `HG2Gui_Reading.dc.html`'s "output is set, not echoed" concept - that spec's other views (a
    manual page, a file index, a diff) each need real semantic parsing of that specific command's
    output; `label: value` detection is the one generic, command-agnostic case.
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
    `https://www.azphalt.store` — `search(query, kind, page)` (`GET /packages`),
    `download(id, version)` (`GET /packages/{id}/versions/{version}/download`), and
    `discovery()` (`GET /.well-known/azphalt-repository.json`, the registry's own trust anchor —
    its `signingKeys` list). Free packages only; paid packages need a Bearer entitlement this
    client does not obtain.
*   **`azp/AzpSignatureVerifier.kt`** (androidMain): Ed25519 verification of a package's
    `manifest.json` bytes against its optional `signature.json`
    (`spec/package-format.md` § Signing — a detached signature over the exact manifest bytes as
    stored in the archive, no re-canonicalization). Uses the platform's own `java.security`
    Ed25519 support (API 33+) rather than a new crypto dependency; below API 33 it reports
    `AzpTrust.UNVERIFIABLE` instead of silently skipping the check. A signature that fails to
    verify yields `AzpTrust.INVALID`; verifying against a key the registry's `discovery()`
    publishes yields `AzpTrust.TRUSTED`, otherwise `AzpTrust.VALID` — "tamper-evidence, not
    identity" per the spec, since counter-signature chains aren't implemented. Cross-checked
    against a real signed package (`com.hereliesaz.azphalt.3d-protrusion`) downloaded live from
    `azphalt.store` with `openssl pkeyutl -verify -pubin -inkey pub.der -keyform DER -rawin -in
    manifest.json -sigfile sig.bin` → `Signature Verified Successfully`, confirming the exact
    wire assumptions this file makes: standard (not URL-safe) base64, a 44-byte SPKI DER key
    (`X509EncodedKeySpec`), and the manifest's exact stored bytes with no re-canonicalization. As
    of that same check, the live registry's `.well-known/azphalt-repository.json` doesn't publish
    a `signingKeys` field at all yet — `AzpDiscovery.signingKeys` defaults to empty and decodes
    that response fine, but it means every signed package currently tops out at `AzpTrust.VALID`
    in practice; `TRUSTED` is real code, just not yet reachable against the live registry.
*   **`azp/AzpInstaller.kt`** (androidMain): a `.azp` is a plain ZIP archive with `manifest.json`
    at its root (`spec/package-format.md`) — `install()` unzips it into
    `filesDir/azp/<id>/<version>/` (rejecting any entry that would escape via `..`), reads
    `manifest.json`'s `kind`, and for `kind:"skill"` collects the declared `skill.skills[].id`
    list so the SKILL.md payloads can be found again later. Runs `AzpSignatureVerifier` against
    the manifest bytes and the package's `signature.json` (if present); an `AzpTrust.INVALID`
    result deletes the extracted files and fails the install outright, per the spec's own mandate
    to "verify the signature... and reject on mismatch". `TerminalActivity` fetches
    `AzpClient.discovery()` once per process (cached) and passes its `signingKeys` in as
    `install()`'s `trustedKeys`.
*   **`managers/AzpLibrary.kt`** (androidMain): flat-SharedPreferences record of installed
    packages, same shape as `WorkflowStore`/`SshPresets` — now including each package's
    `AzpTrust` result alongside id/name/version/kind. `installedSkillTexts()` reads each
    installed skill package's `skills/<id>/SKILL.md` off disk, capped to a fixed character budget,
    for `AiClient` to fold into its system prompt.
*   **`ui/azp/AzpStoreScreen.kt`** (commonMain): search box, kind-filter pills (all/skill/mcp/
    code/pack/asset/app/script), and a result list with an INSTALL/INSTALLED pill per package — same
    visual idiom as `AiChatScreen`. An installed row also shows a trust badge (TRUSTED SIGNER /
    SIGNED · UNKNOWN SIGNER / SIGNED · UNVERIFIED (OS) / UNSIGNED) — `AzpListing.trust` carries
    the androidMain `AzpTrust` enum's name as a plain string, since commonMain can't reference a
    platform-specific type directly. A not-yet-installed row shows "TRUST UNKNOWN · CHECKED AT
    INSTALL" instead of nothing (AZP-7): the repository's search API (`AzpPackageSummary`) never
    returns anything signature-related, so there's no real verdict to show before downloading and
    verifying — the deliberate choice is to say so plainly rather than let the absence of a badge
    silently read as "already vetted". Reached via a synthesized `azp` root pill
    (`CommandTree.azpRoot`, `wizardId = "azp-store"` navigates to the screen, same reuse of
    `onWizard` as the AI pill).
*   `AzpInstaller` computes SHA-256 while extracting and rejects a package when a payload is
    unlisted or differs from `manifest.files`; path containment and signature checks still apply.
    For `kind:"script"`, `ScriptInstaller` resolves declared `apt` dependencies through the Termux
    prefix and writes an executable wrapper into `prefix/bin`, pointing at the verified entry file.
    HG2Gui still has no general WASM runtime or MCP client: kinds other than `skill` and `script`
    remain download-and-unpack data. That boundary avoids pretending arbitrary packages are safe or
    executable merely because they arrived in a ZIP.

### 10. Ground rotation (Kotlin, `commonMain`)
*   **`ui/menu/PillMenu.kt`**: `Azphalt.currentGround` is a process-wide `mutableStateOf(Ground)`
    (not `remember`-scoped) so every screen agrees on the same background, including
    `EditorScreen` - which runs in its own Activity (`EditorActivity`) with its own composition,
    so a per-composition roll would let it disagree with the rest of the app. `Azphalt.grounds`
    (Mustard weighted heavily as the primary, five others sharing the rest - a sixth, Olive, was
    dropped for reading too close to Mustard's own yellow) and
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

### 11. Guide reader motion (Kotlin, `commonMain`)
*   **`ui/guide/GuideReaderScreen.kt`**: an entry never just appears - each field wipes on in
    reading order via `WipeItem`, sequenced `120 + seq*110` ms apart with
    `CubicBezierEasing(0f, .9f, .1f, 1f)` (the Azphalt "unfold" easing). Text/rules reveal via a
    left-to-right `clipRect`; capsules/pills instead animate their real layout width from 0 (so a
    chip's Row slot grows along with it), clipped to that reported width so an in-flight chip
    never overlaps whatever the Row places next to it.
*   `GuideWash`: a faint, oversized echo of the entry's own command name (9% ink, ~100sp) behind
    the content, drifting in from the right over 2400ms on the same easing - "depth is speed," no
    blur or dimming. Tied to the same `wipeKey` as every `WipeItem`, so Prev/Next/Replay restart
    it along with everything else. This is the one piece of `HG2Gui_Guide_Motion.dc.html`'s "01 -
    Entry" demo the screen was missing; the wipe-in cascade itself already matched. The spec's "02
    - Parallax" scroll-driven variant (a background wash plane at 0.2x scroll speed, mid-plane
    rods at 0.5x) still isn't implemented, though the reader gained a real scroll position to
    eventually drive it from (see below) - `GuideWash` itself stays a fixed, non-scrolling layer.
*   **`ui/guide/GuideContent.kt`**: the source text (`Hitchhiker's Guide to Termux`, revised
    master) runs to multi-paragraph entries now, each with an optional "Animation candidate" scene
    and an optional trailing editorial note (`GuideEntry.animation`/`.note`, both nullable - most
    entries carry neither). A chapter with no command entries at all (`Chapter 5`'s "the chapter
    that went missing" gag) renders its intro directly in the index instead of behind a pick that
    would otherwise never exist. To fit the longer entries, `GuideEntryReader` split into three
    tiers: the header (title, hue bar) and the Prev/Next row stay fixed, and only the body between
    them - blurb, animation block, note, chapter recap - scrolls (`verticalScroll` on a
    `weight(1f)` middle `Column`), so a long entry no longer risks clipping off the bottom of a
    single fixed screen the way a short one-paragraph blurb never did.

### 12. Context (OS-reference tree) and AI part breakdown (Kotlin, `androidMain`/`commonMain`)
*   **`managers/OsContextStore.kt`** (androidMain): flat SharedPreferences, one string key,
    `current()`/`set()` - the OS a session is mentally "in" (`local`, the default, or
    `ubuntu`/`macos`/`windows`). Persisted, not session-scoped, since a device is usually SSH'd
    into the same kind of host repeatedly.
*   **`CommandTree.kt`**: `contextRoot()` is a synthesized root pill (like `workflowsRoot`/
    `aiRoot`) with one leaf per OS choice, each `wizardId = "switchos:<os>"` - picking one is a
    state change, not a token, handled by `TerminalActivity`'s `onWizard` (sets
    `OsContextStore`, then re-fetches `CommandTree.from`). When the current OS isn't `local`,
    `from()` adds one more root, `osReferenceRoot()`: a static (not live-discovered) reference
    tree for that OS's package manager, service manager, and the commands that genuinely overlap
    with the local Termux install (`git`, `ls`, `ssh`). This is `HG2Gui_Redesign.dc.html`'s "2b -
    context-aware" demo, scoped to what's actually useful without a live remote-shell-awareness
    mechanism this app doesn't have: a reference tree for working over an `ssh` connection, not a
    live suggestion set that knows what's really installed on the far end.
*   **AI part breakdown** (`ai/AiClient.kt`, `ui/ai/AiChatScreen.kt`): the system prompt now asks
    the model to optionally follow a `CMD:` reply with a `PARTS:` block, one `<token>|<what it
    does>` line per flag, for commands worth breaking down. `AiClient.parse` splits this into
    `AiReply.parts`, rendered as a "WHAT EACH PART DOES" panel under the USE pill
    (`AiChatScreen.kt`'s `AiBubble`). This is the tractable slice of `HG2Gui_Redesign.dc.html`'s
    "2c - Phase 4" concept - the breakdown panel, not the full separate screen with per-token
    tap-to-swap-from-the-tree editing, which would need a way to re-enter the pill tree from an
    arbitrary token position and is out of scope here.

### 13. Select File/Folder: the graphical path picker (Kotlin, `commonMain`/`androidMain`)
*   **`ui/menu/FileBrowser.kt`** (androidMain): the `file…` pill attached to `edit` and to every
    discovered shell binary. Unlike an ordinary pill it carries a `wizardId` (`"pick-path:<node
    id>"`) and `settleBeforeWizard = true` instead of `resolveChildren` - picking it doesn't drill
    further into the stack, it hands off to `TerminalActivity`'s `onWizard`, and only after its own
    trail crumb has actually settled and reported where it landed.
*   **`PillMenu.kt`'s `settleBeforeWizard`**: a wizard pick normally fires `onWizard` immediately,
    with its trail-crumb drop animating separately and unobserved. A `settleBeforeWizard` pick
    instead waits for `Azphalt.DROP_MS + Azphalt.SWING_MS` (the crumb's own drop-and-settle time)
    plus one short buffer, adds the crumb to the trail, *then* fires `onWizard` - so by the time the
    caller acts, `onCrumbPositioned(id, rect)` has already reported that crumb's real on-screen
    rect (`TrailCrumb`'s own `Modifier.onGloballyPositioned`, in root coordinates).
    `TerminalActivity` keeps every reported rect in `crumbRects`, keyed by node id, and recovers
    the right one by stripping `FileBrowser.WIZARD_PREFIX` back off the wizardId string.
*   **`PillPerimeterReveal.kt`** (commonMain): the entrance/exit motion. Where `PillWrapReveal`
    (section 5a of `DESIGN.md`, used only for the fixed `FILES` root pill) simplifies "run the
    perimeter" into one continuous rect interpolation, this is the fuller, edge-by-edge version:
    from the crumb's own rect, a `hue`-coloured bar grows right along the bottom edge to the
    bottom-right corner, up the right edge to the top-right corner, left across the top to the
    top-left corner, then down the left edge - closing the loop back over the crumb's own start.
    The moment that last leg begins, a downward wipe (`clipRect(bottom = ...)`) starts filling the
    now-enclosed frame with `hue` and revealing whatever's rendered inside it as it descends -
    `PerimeterRevealState.open()`/`close()` sequence the four `Animatable` leg progresses (`bottom
    Leg`/`rightLeg`/`topLeg`/`leftLeg`, 260ms each) and the `flood` progress (420ms, launched
    alongside `leftLeg`) explicitly, rather than the single `wrap`/`flood` pair `PillWrapReveal`
    uses.
*   **`ui/files/PathPickerScreen.kt`** (commonMain): the browser rendered inside the reveal - a
    real (not VFS-sandboxed) directory listing via a caller-supplied `listDir`, folders tap-to-
    descend, files tap-to-select-immediately, and a fixed **SELECT THIS FOLDER** pill so a
    folder-typed parameter can be satisfied by the open directory itself - nothing here knows
    ahead of time whether the command it's filling in wants a file or a directory, since
    `FileBrowser` offers the same one pill for both cases.
*   **Browsing root**: `TerminalActivity.realFsListDir` walks the real filesystem with plain
    `java.io.File` (unlike `FilesScreen`'s `vfsListDir`, which is sandboxed to `VfsManager`'s
    app-private root) - "the working directory's contents are shown" means the *session's own live
    cwd* (`SessionUiState.cwd`, kept in sync with the shell after every command), falling back to
    `CommandTree.pickerRoot()` (the Termux home dir, or app-external-files before a bootstrap
    exists) only when a session has no cwd yet.
*   **Handing the pick back**: selecting a file or folder appends its absolute path straight onto
    `session.ui.tokens` - the same list `CommandLine`'s chip row and `RUN` both read from - and
    clears `inputText`, then closes the reveal. `PillMenu` keeps its own separate `trail`/`tokens`
    for the pill stack's own display, unaware of this external append; every command offering this
    picker today treats the file/folder as its last argument, so no further pick ever overwrites
    it, but a future multi-arg-after-file command would need `PillMenu` to expose a trail-sync hook
    to stay safe.

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
