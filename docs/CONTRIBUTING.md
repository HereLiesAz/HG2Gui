# Contributing to HG2Gui

Thank you for your interest in contributing.

## What this project is

HG2Gui is a touch-first Android terminal built around a real Termux-derived runtime, structured command composition, package lifecycle management, execution authority, isolation, and native graphical surfaces where plain terminal interaction is unnecessarily hostile.

## Project structure

HG2Gui is a Kotlin Multiplatform project: `:composeApp` is the thin Android entry point (`TerminalActivity.kt`, `EditorActivity.kt`, `mcp/McpServerService.kt`); the actual UI and execution logic lives in the separate `:shared` module.

- **Kotlin shared across platforms:** `shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/` — Compose UI (`ui/`), the `ShellSession` contract and `ShellAliases` (`terminal/`), the `calc` expression parser (`util/CalculationEngine.kt`), plus managers/MCP code that does not need an Android-specific API.
- **Kotlin, Android-specific:** `shared/src/androidMain/kotlin/com/hereliesaz/hg2gui/` — `ShellSession`, `DistroManager`, `TerminalEngine`, `Builtins`, package management/lifecycle/isolation/authority, Android managers, AI/Store integration, and Android-only UI such as `ui/menu/CommandTree.kt` and `ui/menu/FileBrowser.kt`.
- **`:terminal-emulator`** — vendored VT100/PTY implementation used by the terminal stack.
- **`:termux-shared`** — vendored Termux-compatible Android filesystem/process/terminal utilities.
- **Resources:** `composeApp/src/main/res/`.

## Coding standards

- **Language:** Kotlin for application code. Native C exists only for Android-safe executable entry points and terminal JNI where required.
- **UI:** Compose. A screen is a function of state; pass callbacks rather than reaching into managers from composables.
- **Design:** Follow the Azphalt system and `docs/DESIGN.md`: capsules, Jost, hashed capsule hues, no gratuitous icons/shadows/blur, and the established motion language.
- **Command interaction:** normal command-tree choices compose; explicit **RUN** executes. If a value can be enumerated, expose a selection UI. File/directory values use the graphical picker. Free-form input must identify what the user is expected to type.
- **Packages:** compatibility fixes should address a class of package/runtime assumptions rather than special-casing package names unless the package truly has unique semantics.
- **Authority:** ordinary commands do not inherit ADB/root. Elevated operations remain explicit and confirmed. Headless paths must not bypass that rule.
- **Isolation:** an isolated package must fail closed if the isolation boundary cannot be established.
- **Documentation:** all behavior claims must be checked against current code before they are added or retained.

### Documentation requirements

- Class documentation explains purpose and boundary.
- Function documentation explains non-obvious parameters, return values, side effects, and security behavior.
- Inline comments explain constraints or reasoning, especially Android/runtime compatibility work.
- Update the live product docs when behavior changes: `README.md`, `docs/ARCHITECTURE.md`, `docs/HG2GUI_ARCHITECTURE.md`, `docs/COMMANDS.md`, `docs/USER_GUIDE.md`, `docs/DESIGN.md`, and `docs/VISION.md` as applicable.

## Toolchain

Current authoritative versions live in Gradle/version files. At this documentation pass:

- JDK 21
- Gradle 9.7.0
- AGP 9.3.2
- Kotlin 2.4.10
- Compose Multiplatform 1.12.0
- `compileSdk` / `targetSdk` 37
- `minSdk` 24
- NDK `29.0.14206865`

## Pull requests

1. Fork the repository.
2. Create a feature branch.
3. Make the smallest coherent change.
4. Update affected documentation.
5. Verify behavior against the current architecture invariants.
6. Build/test the affected modules.
7. Submit the PR.

## Building

```bash
./gradlew assembleDebug
```
