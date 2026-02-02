# Architecture & Design

This document outlines the technical structure and design patterns used in **HG2Gui**.

## 1. Directory Structure

```
/
├── components/         # React Presentation Components
│   ├── Terminal.tsx    # Main CLI input/output area
│   ├── CommandMenu.tsx # Floating suggestion engine
│   ├── GuideScreen.tsx # Rich media display for command info
│   └── PanicButton.tsx # UI Trigger for visual resets
├── services/
│   └── geminiService.ts # Google GenAI SDK integration
├── constants.ts        # Static data, command trees, and color palettes
├── types.ts            # TypeScript interfaces and Enums
├── App.tsx             # Main layout and Global State Controller
└── index.tsx           # Entry point
```

## 2. Core Concepts

### The System Context (`SystemContext`)
Unlike standard terminal emulators that are dumb pipes to a shell, HG2Gui maintains a client-side state of the "simulated machine."
*   **State**: `os` (Ubuntu/MacOS/Windows), `user`, `hostname`.
*   **Impact**: Changing the `os` context fundamentally alters the `CommandMenu` suggestions. For example, switching to Windows enables `winget` but disables `brew`.

### Kinetic UI (Framer Motion)
We utilize `framer-motion` to replicate the "wonky" physics of the movie.
*   **AnimatePresence**: Used heavily in `Terminal.tsx` and `CommandMenu.tsx` to ensure items exit gracefully before being removed from the DOM.
*   **Layout Animations**: The `layout` prop is used on lists to allow items to slide into new positions when their siblings are added or removed.

### Command Tree (`constants.ts`)
Commands are not a flat list. They are structured as a recursive tree of `MenuOption` nodes.
*   **Root**: Top-level commands (e.g., `git`, `docker`).
*   **Children**: Sub-commands (e.g., `commit`, `push`).
*   **Resolution**: The `getCommandSuggestions` function parses the current input string, traverses this tree, and returns valid next-steps based on the `SystemContext`.

## 3. AI Integration (Gemini)

The app uses the `@google/genai` SDK.
*   **Model**: `gemini-3-flash-preview` is used for low-latency responses.
*   **Structured Output**: We strictly enforce `responseMimeType: "application/json"` with a defined schema to ensure the AI returns data that can be directly rendered by the `GuideScreen` component (e.g., specific `visualType` enums like 'robot' or 'planet').

## 4. Styling Architecture

*   **Tailwind**: Used for layout, spacing, and typography.
*   **Custom Config**: `tailwind.config` in `index.html` extends the palette to include specific hex codes derived from the 2005 movie props (Guide Red, Vogon Yellow, Trillian Green).
*   **Fonts**: 
    *   `Cousine`: For code/terminal text (Monospace).
    *   `Fredoka`: For UI elements (Rounded Sans), mimicking the "VAG Rounded" look of the film.
