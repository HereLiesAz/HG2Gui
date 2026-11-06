# AI providers + installable AI CLIs

Status: PLAN — awaiting approval. Nothing below is implemented.

## Facts (from source)
- One backend: Anthropic SDK in `ai/AiClient.kt`, fixed model `AI_MODEL_ID`, key in `AiSettings` (encrypted prefs, single `api_key`).
- UI: `ui/AiSettingsScreen.kt` (key entry). `AiReply`/`parse()` contract is provider-neutral and stays.
- Package install path: `Hg2PackageManager` (Termux `.deb`), `ScriptInstaller` (azp `pkg install -y` deps).
- Script execution on Android needs wrapper functions or the exec preload (see PR #296).

## Part 1 — Provider choice
- [ ] `AiProvider` enum + `AiBackend` interface (`ask(system, user) -> raw text`) in `ai/`.
- [ ] Move current SDK call behind `AnthropicBackend` unchanged (model-rejection handling kept).
- [ ] `OpenAiCompatBackend` over OkHttp (already a dependency): OpenAI, Ollama, OpenRouter, Groq, any `baseUrl`.
- [ ] `GeminiBackend` over OkHttp (`generateContent`).
- [ ] `AiSettings`: per-provider key, selected provider, model, baseUrl. Migrate existing `api_key` → Anthropic.
- [ ] `AiSettingsScreen`: provider picker, key, model, base URL (custom/Ollama).
- [ ] `AiClient.ask` dispatches on provider; `parse()` untouched.
- [ ] Host tests: request building and response extraction per backend (pure, no network), settings migration.

## Part 2 — Installable AI CLIs
- [ ] Catalogue (`AiCliCatalog`): id, label, install recipe, binary, runtime deps.
  - pkg: `ollama`; node: `@google/gemini-cli`, `@anthropic-ai/claude-code`, `@openai/codex`; pip: `llm`, `aider-chat`.
- [ ] Installer: ensure runtime (`nodejs`/`python` via engine `pkg install`), then run npm/pip through the shell.
- [ ] After any npm/pip global install: regenerate script wrappers (`PrefixRuntimeConfig.writeScriptWrappers`) and rehash so the new bins run despite W^X.
- [ ] Menu entry (CommandTree) "AI CLIs": install / run / remove, using the existing command-tree conventions.
- [ ] Per-CLI key: export the stored provider key as the env var that CLI expects, only for that CLI's process.
- [ ] Host tests: catalogue validity (names pass package-name grammar), recipe → command lines.

## Open risks
- Several of these CLIs ship native/arch-specific binaries that may not run on Android/Bionic; each needs an on-device check before it is listed. Unverified ones stay out of the catalogue.
- No Android SDK in the CI sandbox I work in: compile and host tests cannot be run here; PR CI is the check.
- Keys exported into CLI processes widen where secrets travel; needs your yes per CLI.

## Review
(to fill after implementation)
