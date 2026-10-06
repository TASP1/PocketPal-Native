package com.tasp1.pocketpal.domain

/**
 * Remote / local backends the app can route chat through.
 * Kaggle Bridge stays first-class; any OpenAI-compatible endpoint works.
 */
data class ServerProfile(
    val id: String,
    val name: String,
    val kind: Kind,
    val baseUrl: String,
    val apiKey: String = "",
    val defaultModel: String = "",
    val enabled: Boolean = true,
) {
    enum class Kind {
        /** TASP1 Kaggle PocketPal bridge (flags :web :think :shell) */
        KaggleBridge,
        /** Generic OpenAI chat/completions + optional vision */
        OpenAICompatible,
        /** Anthropic Messages API shape (mapped via adapter) */
        AnthropicCompatible,
        /** On-device GGUF / llama.cpp */
        LocalGguf,
    }

    companion object {
        fun kaggleBridge(url: String, key: String) = ServerProfile(
            id = "kaggle-bridge",
            name = "Kaggle Bridge",
            kind = Kind.KaggleBridge,
            baseUrl = url.trimEnd('/'),
            apiKey = key,
            defaultModel = "google/gemini-2.5-flash",
        )

        fun openAi(url: String, key: String, name: String = "OpenAI Compatible") = ServerProfile(
            id = "openai-${url.hashCode()}",
            name = name,
            kind = Kind.OpenAICompatible,
            baseUrl = url.trimEnd('/'),
            apiKey = key,
            defaultModel = "gpt-4o-mini",
        )

        fun local() = ServerProfile(
            id = "local-gguf",
            name = "On-device (GGUF)",
            kind = Kind.LocalGguf,
            baseUrl = "local://gguf",
            defaultModel = "",
        )
    }
}
