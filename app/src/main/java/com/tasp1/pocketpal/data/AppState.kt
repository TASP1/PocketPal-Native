package com.tasp1.pocketpal.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Lightweight stand-in for MobX stores while the port progresses. */
object AppState {
    var baseModel by mutableStateOf(BridgeConfig.DEFAULT_MODEL)
    var serverUrl by mutableStateOf(BridgeConfig.BASE_URL)
    var apiKey by mutableStateOf(BridgeConfig.API_KEY)
    var darkTheme by mutableStateOf<Boolean?>(null) // null = system
    var contextSize by mutableStateOf("4096")
    var flashAttention by mutableStateOf("auto") // auto | on | off
    var haptics by mutableStateOf(true)

    val bridgeModels = listOf(
        "google/gemini-2.5-flash",
        "google/gemini-2.5-pro",
        "anthropic/claude-sonnet-5@default",
        "openai/gpt-5.4-mini-2026-03-17",
        "deepseek-ai/deepseek-r1-0528",
        "qwen/qwen3-235b-a22b-instruct-2507",
        "xai/grok-4",
        "meta-llama/llama-4-maverick",
    )

    data class LocalModelStub(
        val id: String,
        val name: String,
        val sizeLabel: String,
        val type: String, // text | vision | multimodal
        val downloaded: Boolean,
    )

    /** Placeholder local models UI — real GGUF later */
    val localModels = listOf(
        LocalModelStub("smollm2-1.7b", "SmolLM2 1.7B Instruct", "1.1 GB", "text", false),
        LocalModelStub("qwen2.5-1.5b", "Qwen2.5 1.5B Instruct", "1.0 GB", "text", false),
        LocalModelStub("gemma-2-2b", "Gemma 2 2B Instruct", "1.6 GB", "text", false),
        LocalModelStub("llava-1.5-7b", "LLaVA 1.5 7B", "4.1 GB", "vision", false),
    )

    data class PalStub(
        val id: String,
        val name: String,
        val description: String,
        val category: String, // assistant | roleplay | video | local
        val systemPrompt: String,
    )

    val pals = listOf(
        PalStub("assistant", "Helpful Assistant", "General-purpose helper", "assistant", "You are a helpful assistant."),
        PalStub("coder", "Code Companion", "Explains and writes code", "assistant", "You are an expert software engineer."),
        PalStub("writer", "Writing Partner", "Drafts and edits prose", "assistant", "You are a skilled editor."),
        PalStub("rpg", "Dungeon Master", "Interactive fantasy roleplay", "roleplay", "You are a creative dungeon master."),
        PalStub("teacher", "Patient Tutor", "Teaches step by step", "assistant", "You are a patient tutor."),
        PalStub("local-pal", "On-device Pal", "Works with local GGUF models", "local", "You are a concise local assistant."),
    )
}
