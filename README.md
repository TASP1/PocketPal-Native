# PocketPal Native (Jetpack Compose)

**1:1 UI port** of [PocketPal AI](https://github.com/a-ghorbani/pocketpal-ai) screens into **Kotlin + Jetpack Compose**, with TASP1 Kaggle Bridge wired in.

## Matched from upstream

| RN / PocketPal | Compose |
|----------------|---------|
| MD3 light/dark tokens (`theme/tokens/colors.ts`) | `ui/theme/Color.kt` + `Theme.kt` |
| Drawer routes (`navigationConstants.ts`) | `ui/navigation/Routes.kt` + drawer |
| Chat header (title + model subtitle) | `ChatTopBar` |
| User bubble + assistant text | `ChatBubble` |
| Input bar + **Think** chip | `ChatInputBar` |
| Models / Settings / Pals / Benchmark / App Info | Matching screens |

## Bridge (pre-filled in Settings)

- URL: `https://scrubbed-calcium-subscript.ngrok-free.dev`
- Key: same `BRIDGE_KEY` as PocketPal remote

## Build

```bash
cd PocketPalNative
# Android Studio: Open this folder, Run app
# or: ./gradlew :app:assembleDebug
```

## Status

**Foundation complete** for visual parity on Chat + shell navigation.  
Still to port for full 1:1: full ChatView streaming UI, Markdown/premium, Pals hub, local GGUF/llama.rn, Benchmark matrix, HF download, etc.

Upstream UI remains the reference; this repo tracks screen-by-screen parity.
