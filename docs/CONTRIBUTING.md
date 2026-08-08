# Contributing to HG2Gui

Thank you for your interest in contributing.

## What this project is
A **terminal app** for Android. Not a launcher. Contributions that reintroduce home-screen
behaviour — app drawers, widget grids, a `category.HOME` filter, launcher lifecycle flags —
are out of scope.

## Project Structure
*   **Kotlin shared across platforms:** `composeApp/src/commonMain/kotlin/com/hereliesaz/hg2gui/`
    — Compose UI (`ui/`), the `ShellSession` contract and `ShellAliases` (`terminal/`), the
    `calc` expression parser (`util/CalculationEngine.kt`).
*   **Kotlin, Android-specific:** `composeApp/src/androidMain/kotlin/com/hereliesaz/hg2gui/`
    — the `ShellSession`/`DistroManager`/`TerminalEngine`/`Builtins` implementations
    (`terminal/`), `ContactManager`/`VfsManager`/`flashlight/` (`managers/`), a handful of
    shared helpers (`util/`), and the Android-only parts of the UI (`ui/menu/CommandTree.kt`,
    `ui/menu/FileBrowser.kt`, `ui/editor/`).
*   **Resources:** `composeApp/src/androidMain/res/`

## Coding Standards
*   **Language:** Kotlin for everything, old and new. There is no separate legacy engine to
    match — a new built-in command is a branch in `terminal/Builtins.kt`, not a new class.
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
