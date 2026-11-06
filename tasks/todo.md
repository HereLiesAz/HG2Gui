# AI providers + installable AI CLIs

Status: implemented in PR #297 (one PR, as decided). Decisions: Claude Code and OpenAI Codex are in the
catalogue; no stored key is passed to any CLI.

## Facts (from source)
- One backend before: Anthropic SDK in `ai/AiClient.kt`, fixed model `AI_MODEL_ID`, single `api_key`.
- `AiReply`/`parse()` is provider-neutral and unchanged.
- Package install path: `Hg2PackageManager` (Termux `.deb`); script execution needs wrappers or the exec preload.

## Part 1 — Provider choice
- [x] `AiProvider` / `AiConfig` (`ai/AiProvider.kt`): Anthropic, OpenAI, Gemini, Ollama (local), Custom (OpenAI-compatible).
- [x] `OpenAiChatProtocol` / `GeminiProtocol` (`ai/AiProtocols.kt`): pure request build + reply parse.
- [x] `AiClient.ask(..., config)` dispatches; Anthropic path unchanged. Keys travel in headers only.
- [x] `AiSettings`: per-provider key (Anthropic keeps `api_key`), provider/model/endpoint in plain prefs.
- [x] `AiSettingsScreen`: provider chips, model, endpoint (editable providers), key (keyed providers), one Save.
- [x] Never send a key over cleartext http to a non-local host; `10.example.com` is not a private address.
- [x] Isolation audit disclosure names the chosen provider; privacy policy and docs updated.

## Part 2 — Installable AI CLIs (`aicli`)
- [x] `AiCliCatalog`: ollama and codex (Termux packages), gemini (npm), claude (npm, pinned 2.1.112).
- [x] Engine verb `aicli [list|install|remove]`; headless callers may only list.
- [x] Rewrites script wrappers and rehashes after an install so the tool runs in the same tab.
- [x] AI pill entry (`aicli install claude` etc.) and package-mutation classification.
- [ ] On-device check of gemini and claude installs (not possible from CI).

## Verification
- Standalone (Kotlin 2.4 compiler + JUnit): `AiProtocolsTest`, `AiConfigTest`, `AiCliCatalogTest` — 33 tests pass.
- CI task `:shared:testAndroidHostTest` and compile of `:composeApp`: see PR.

## Open risks
- Claude Code 2.1.112 is the last JavaScript-only release (7 months old; 2.1.113+ is a native binary with no Android build).
- Gemini and Claude Code installs have not been run on a device.
- Model defaults for OpenAI (`gpt-5`) and Gemini (`gemini-2.5-pro`) are editable defaults, not guarantees.
