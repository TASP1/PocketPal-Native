# PocketPal Native (Jetpack Compose)

**UI + remote-client port** of [PocketPal AI](https://github.com/a-ghorbani/pocketpal-ai) → Kotlin / Jetpack Compose (TASP1 + Kaggle Bridge).

## Progress (~25–30% of full rewrite)

| Area | Status |
|------|--------|
| MD3 theme tokens | Done |
| Drawer navigation (6 routes) | Done |
| Chat shell (header, chips, empty) | Done |
| **SSE streaming** + stop generation | Done |
| Reasoning / Thoughts panel | Done |
| Sources cards + tool-JSON strip | Done |
| Markdown subset (bold/italic/code/links) | Done |
| Image attach + OCR + vision quality | Done |
| DataStore settings (URL, key, model, theme) | Done |
| Bridge health + model protocol flags | Done |
| Models / Pals / Settings / Benchmark / About UI | Partial |
| Local GGUF / llama.cpp | Pending |
| HF search & download | Pending |
| Full sessions DB / multi-chat history | Pending |
| Full CommonMark tables | Pending |

## Architecture

```
ui/chat/ChatViewModel  →  network/BridgeClient (SSE)
                       →  data/AttachmentProcessor (images/OCR)
                       →  data/SettingsRepository (DataStore)
protocol/ModelFlags    →  :web :think :shell :effort
```

## Defaults

- Bridge URL / key: `BuildConfig.BRIDGE_*` (overridable in Settings)
- Open in Android Studio → Run

```bash
./gradlew :app:assembleDebug
```
