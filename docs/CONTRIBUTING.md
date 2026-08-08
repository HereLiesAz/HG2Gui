# Contributing to HG2Gui

Thank you for your interest in contributing.

## What this project is
A **terminal app** for Android. Not a launcher. Contributions that reintroduce home-screen
behaviour — app drawers, widget grids, a `category.HOME` filter, launcher lifecycle flags —
are out of scope, however faithful they are to the T-UI lineage this forked from.

## Project Structure
*   **Kotlin shared across platforms:** `composeApp/src/commonMain/kotlin/com/hereliesaz/hg2gui/`
    — Compose UI (`ui/`), the `ShellSession` contract and `ShellAliases` (`terminal/`).
*   **Kotlin/Java, Android-specific:** `composeApp/src/androidMain/kotlin/com/hereliesaz/hg2gui/`
    — the `ShellSession`/`DistroManager`/`TerminalEngine` implementations (`terminal/`), the
    legacy Java engine (`commands/`, `managers/`, `tuils/`), and the Android-only parts of the UI
    (`ui/menu/CommandTree.kt`, `ui/menu/FileBrowser.kt`).
*   **Resources:** `composeApp/src/androidMain/res/`

## Coding Standards
*   **Language:** Kotlin for anything new. Java is maintained, not extended — new commands may
    be Java to match their neighbours, but new UI is always Compose.
*   **UI:** Compose only. No new XML layouts. A screen is a function of state; a composable
    that reaches for a manager is doing too much — pass a callback instead.
*   **Design:** Follow the Azphalt system and `docs/DESIGN.md`. Concretely: no borders, no
    shadows, no icons, no blur, no gradients on surfaces, 999px radii, Jost only, and the ten
    capsule hues assigned by hashing the identifier. If a new surface needs a glyph, it does
    not — use an end-cap.
*   **Motion:** Motion values are specified in `docs/DESIGN.md` and implemented in
    `PillMenu.kt`. Do not add easing, fades or spinners; Azphalt has none.
*   **Documentation:** All new code must be documented.
    *   **Class doc:** explain the purpose.
    *   **Function doc:** explain parameters and return values.
    *   **Inline comments:** explain the logical steps, especially anything non-obvious about
        timing or layout.

## Toolchain
JDK 21, Gradle 8.13, AGP 8.13, Kotlin 2.2.20, `compileSdk` 37, `minSdk` 24.

## Pull Requests
1.  Fork the repository.
2.  Create a feature branch.
3.  Make your changes.
4.  Document them thoroughly.
5.  Check that nothing you added assumes the app is the home screen.
6.  Submit a PR.

## Building
```bash
./gradlew assembleDebug
```
