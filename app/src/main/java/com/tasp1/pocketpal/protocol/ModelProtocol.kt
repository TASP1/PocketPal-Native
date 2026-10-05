package com.tasp1.pocketpal.protocol

/**
 * Kaggle Bridge model-id protocol (gateway v3.x).
 * Flags are suffixes, any order: :web :think :low :medium :high :nothink :tags :shell :field
 * Shell is exclusive with :web on the gateway.
 */
data class ModelFlags(
    val web: Boolean = false,
    val think: Boolean = false,
    val shell: Boolean = false,
    val effort: Effort? = null, // overrides bare :think
    val tags: Boolean = false,
) {
    enum class Effort { low, medium, high, nothink }

    fun toModelId(base: String): String {
        val clean = base.substringBefore(":").ifBlank { base }
        val parts = mutableListOf(clean)
        if (web && !shell) parts += "web"
        if (shell) parts += "shell"
        when {
            effort == Effort.nothink -> parts += "nothink"
            effort != null -> parts += effort.name
            think -> parts += "think"
        }
        if (tags) parts += "tags"
        return parts.joinToString(":")
    }

    companion object {
        fun parse(modelId: String): Pair<String, ModelFlags> {
            val bits = modelId.split(":").filter { it.isNotBlank() }
            if (bits.isEmpty()) return "google/gemini-2.5-flash" to ModelFlags()
            val base = bits.first()
            val rest = bits.drop(1).map { it.lowercase() }.toSet()
            val effort = when {
                "high" in rest -> Effort.high
                "medium" in rest -> Effort.medium
                "low" in rest -> Effort.low
                "nothink" in rest -> Effort.nothink
                else -> null
            }
            return base to ModelFlags(
                web = "web" in rest,
                think = "think" in rest || effort != null,
                shell = "shell" in rest,
                effort = effort,
                tags = "tags" in rest,
            )
        }
    }
}

/** Streaming event protocol from OpenAI-compatible SSE */
sealed class StreamEvent {
    data class ReasoningDelta(val text: String) : StreamEvent()
    data class ContentDelta(val text: String) : StreamEvent()
    data class SearchProgress(val text: String) : StreamEvent()
    data class Done(val finishReason: String?) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}
