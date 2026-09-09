# Contributing to HG2Gui

Thank you for your interest in contributing.

## What this project is

HG2Gui is a touch-first Android command-line environment built around a real Termux-derived runtime plus native Android interaction layers for command composition, completion, shell presentation, Adaptive TUI projection, Files, package lifecycle/isolation, execution authority, and trusted external integrations.

## Project structure

HG2Gui is a Kotlin Multiplatform project. `:composeApp` contains Android entry points and platform transport; reusable UI/runtime logic lives primarily in `:shared`.

- **Common/shared Kotlin:** Compose UI models/components, shell/TUI/completion semantic models, managers, and reusable logic.
- **Android-specific shared Kotlin:** `ShellSession`, `DistroManager`, `TerminalEngine`, package management, execution backends, lifecycle/isolation/authority, Android managers, shell/TUI adapters, and Android-only UI helpers.
- **`:composeApp`:** Android activities/services/API receivers, resources, build/signing/packaging.
- **`:terminal-emulator`:** vendored terminal emulator and PTY implementation.
- **`:termux-shared`:** vendored Termux-compatible Android utilities.

## Coding standards

- **Language:** Kotlin for application code. Native C is used only where Android-safe executable entry points or terminal JNI require it.
- **UI:** Compose. Prefer explicit state/callback contracts over hidden manager access from reusable composables.
- **Design:** Follow `docs/DESIGN.md` and current Azphalt tokens, but choose the visual/control type that fits the represented object. Do not force every feature into the pill stack.
- **Command composition:** ordinary composed commands require explicit **RUN**. A discoverable value may become a native selection; genuinely open-ended values remain typeable.
- **Guide:** real commands shown in Guide entries may be directly composed into the terminal. Do not reintroduce a read-only separation between Guide and command entry.
- **Completion:** completion providers must normalize into the shared semantic model and must not execute a partially composed command merely to obtain candidates.
- **Shell adapters:** shell/theme parsing must be conservative and must preserve raw shell behavior.
- **Adaptive TUI:** the child program remains authoritative. Generated navigation must be reconciled with terminal state and a RAW fallback must remain available.
- **Packages:** compatibility fixes should address a class of package/runtime assumptions rather than special-casing package names unless the package truly has unique semantics.
- **Execution backends:** do not turn arbitrary failure into silent PRoot fallback. Compatibility routing must be based on concrete executable/runtime classification.
- **Authority:** ordinary commands, isolated packages, and headless callers must not inherit ADB/root. Elevated execution remains explicit and foreground-approved.
- **Isolation:** isolated execution must fail closed when the boundary cannot be established. Observation claims must distinguish best-effort `/proc` sampling from exhaustive kernel/syscall audit.
- **External API:** expose typed capabilities and preserve the same lifecycle/isolation/authority rules as in-app requests.
- **Documentation:** behavior claims must be checked against current code and CI/device evidence before they are retained.

### Documentation requirements

Update affected live docs when behavior changes:

- `README.md`
- `docs/ARCHITECTURE.md`
- `docs/HG2GUI_ARCHITECTURE.md`
- `docs/COMMANDS.md`
- `docs/USER_GUIDE.md`
- `docs/DESIGN.md`
- `docs/VISION.md`
- `docs/TODO.md`

Class/function/inline comments should explain real constraints, especially Android runtime, security, package, shell/TUI, and compatibility boundaries.

## Verification

CI success proves only the gates represented in CI. Android-specific runtime behavior may still require device testing, especially for:

- Android ICU/regex behavior;
- native linker/executable policy;
- package execution;
- permissions;
- ADB/root;
- PRoot/isolation.

Do not mark an on-device verification item complete solely because the JVM/compiler workflow is green.

## Toolchain

Authoritative versions live in Gradle/version files. At this documentation pass:

- JDK 21
- Gradle 9.7.0
- AGP 9.3.2
- Kotlin 2.4.10
- Compose Multiplatform 1.12.0
- `compileSdk` / `targetSdk` 37
- `minSdk` 24
- NDK `29.0.14206865`

## Pull requests

1. Create the smallest coherent change.
2. Update affected documentation and `docs/TODO.md` when roadmap state changes.
3. Verify architecture/security invariants.
4. Build/test the affected modules.
5. Include device evidence when the claim depends on Android runtime behavior.

## Building

```bash
./gradlew assembleDebug
```