# HG2Gui - The Hitchhiker's Guide Terminal

> *"DON'T PANIC."*

**HG2Gui** is a retro-futuristic terminal emulator web application inspired by the UI design of the *Hitchhiker's Guide to the Galaxy* (2005 film). It combines a functional command-line interface with the whimsical, "wonky," and tactile motion graphics seen in the movie.

## 🎨 Aesthetic Philosophy

The design strictly adheres to the **2005 Film Aesthetic**:
1.  **Low-Fi/High-Tech**: It looks like advanced technology from the perspective of the early 2000s.
2.  **Kinetic Typography**: Text doesn't just appear; it pops, slides, and bounces.
3.  **Flat Vector Graphics**: No complex 3D shading; mostly flat colors with high contrast (Deep Red, Neon Green, Cyan).
4.  **"Wonky" Physics**: UI elements use "squash and stretch" animation principles rather than linear transitions.

## 🚀 Features

*   **Context-Aware Shell**: The terminal mimics different Operating Systems (`Ubuntu`, `MacOS`, `Windows`).
    *   *Try:* `switch-os macos` or `switch-os windows` to see the available commands change dynamically.
*   **Dynamic Command Menu**: A floating, animated suggestion engine that updates based on your current input and OS context.
*   **Generative "Guide" Manual**: (Integration Ready) Uses Google Gemini 2.5/3 Flash to generate whimsical, "Guide-style" explanations for commands using Structured Output.
*   **Immersive Visuals**: Scanlines, CRT flicker effects, and custom rounded-monospace typography (`Cousine` & `Fredoka`).
*   **Easter Eggs**: A dedicated "Panic" state and humorous system messages.

## 🛠 Tech Stack

*   **Frontend**: React 18+ (React 19 compatible)
*   **Language**: TypeScript
*   **Styling**: Tailwind CSS
*   **Animation**: Framer Motion (Heavy usage for layout transitions and enter/exit effects)
*   **AI/Backend**: Google GenAI SDK (Gemini) for dynamic content generation.

## ⌨️ Usage

1.  **Type commands**: Try standard unix commands like `ls`, `git status`, or `docker ps`.
2.  **Navigation**: Use `Up/Down` arrows to cycle through command history.
3.  **Autocomplete**: Press `Tab` or click on the floating menu chips to autocomplete commands.
4.  **OS Switching**: Type `switch-os [ubuntu|macos|windows]` to change the underlying system context.

## 📜 License

MIT
