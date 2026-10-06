# PocketPal Native (Jetpack Compose)

Compose port of PocketPal AI + **TASP1 Kaggle Bridge**, upgraded toward Claude-style chat chrome and multi-backend engines.

## ~30–35% of full rewrite

### Architecture (v1.3)

```
ChatViewModel
    → EngineRouter
         → RemoteOpenAIEngine (Kaggle Bridge / any OpenAI-compatible)
         → LocalGgufEngine (shell ready for llama.cpp)
    → AttachmentProcessor (vision resize, ML Kit OCR, text/code previews, 50MB)
    → SettingsRepository (DataStore: URL, key, theme, useLocal)
```

### Native features

| Feature | Status |
|---------|--------|
| Pre-wired Bridge URL + API key | BuildConfig + Settings |
| Multi remote servers | Primary + alt OpenAI-compatible |
| Image vision + on-device OCR | ML Kit + JPEG long-edge quality |
| Attachments (image/pdf/text/code/sheet/audio/video) | Kind classify + previews |
| SSE stream + stop | Done |
| Claude-style Thoughts panel | Collapsible ThinkingBlock |
| Artifact-style code card component | ArtifactCard |
| Local GGUF path | Engine interface (runtime link pending) |
| Model flags :web :think :shell | Protocol |

### Claude UI notes (2026)

Claude unified chat + Cowork + Artifacts; thinking blocks; docs/slides/design from the prompt bar. We mirror **Thoughts collapse**, **artifact code panels**, and clean attachment chips — not a pixel clone of Claude.

### Build

```bash
./gradlew :app:assembleDebug
```
