# HG2Gui - The Hitchhiker's Guide Terminal (Android)

**HG2Gui** is an Android Launcher that transforms your device into a retro-futuristic, text-based terminal. Inspired by the "Hitchhiker's Guide to the Galaxy" (2005) aesthetic and built upon the legacy of T-UI Launcher, it offers a powerful command-line interface for power users.

## 🚀 Features

*   **Linux-like CLI:** Use commands to launch apps, manage files, and control system settings.
*   **Customizable Interface:** Change colors, fonts, sizes, and layout via XML configuration or in-app commands.
*   **Smart Suggestions:** T9-like suggestion system for commands and apps.
*   **Aliases:** Create custom aliases for frequently used commands.
*   **RSS Reader:** Built-in RSS feed reader in the terminal.
*   **Text Editor:** Includes `tuixt`, a simple in-terminal text editor.
*   **Themes:** Support for custom themes.

## 🛠 Project Structure

The project is a standard Android application written in Java.

*   `app/src/main/java/com/hereliesaz/hg2gui/` - Main source code.
    *   `LauncherActivity.java` - The entry point and main terminal activity.
    *   `commands/` - Implementation of all terminal commands.
    *   `managers/` - Core logic for suggestions, apps, files, and system integration.
    *   `tuils/` - Utilities and helper classes.
*   `app/src/main/res/` - Android resources (layouts, values, drawables).

## 🔨 Build

To build the project, use Gradle:

```bash
./gradlew assembleDebug
```

## 📜 Documentation

Detailed documentation is available in the `docs/` directory:

*   [Architecture](docs/ARCHITECTURE.md)
*   [Commands](docs/COMMANDS.md)
*   [Contributing](docs/CONTRIBUTING.md)

## ⚖️ License

[Include License Here - originally GPLv3 or similar from T-UI]
