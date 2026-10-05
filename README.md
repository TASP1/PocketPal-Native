# PocketPal Native (Jetpack Compose)

**1:1 UI port** of [PocketPal AI](https://github.com/a-ghorbani/pocketpal-ai) into **Kotlin + Jetpack Compose**, with TASP1 Kaggle Bridge.

## Port progress

| Area | Status |
|------|--------|
| MD3 light/dark tokens | Done |
| Drawer routes (Chat, Models, Pals, Benchmark, Settings, App Info) | Done |
| Chat header / bubbles / empty state | Done |
| Web · Think · Shell chips | Done |
| Sources cards + tool-protocol strip | Done |
| Lightweight markdown (bold/italic/code/links) | Done |
| Models: Bridge + on-device cards | Done (local download later) |
| Pals grid + filter chips | Done (sheets later) |
| Settings (server, theme, context, flash attn, haptics) | Done |
| Benchmark UI presets | Done (engine later) |
| About / links | Done |
| Streaming SSE UI | Pending |
| Local GGUF / llama.cpp | Pending |
| HF search & download | Pending |
| Full CommonMark / tables | Pending |

## Bridge defaults

- URL: `https://scrubbed-calcium-subscript.ngrok-free.dev`
- Key: `BRIDGE_KEY` (Settings)

## Build

Open in Android Studio or:

```bash
./gradlew :app:assembleDebug
```
