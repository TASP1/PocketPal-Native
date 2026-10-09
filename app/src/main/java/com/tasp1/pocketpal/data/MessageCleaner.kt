package com.tasp1.pocketpal.data

/**
 * Split model output into visible answer vs reasoning, strip protocol noise.
 * Fixes raw <think>…</think> leaking into the chat body (Claude-style UX).
 */
object MessageCleaner {
    private val thinkBlock = Regex("(?is)<think>(.*?)</think>")
    private val thinkOpen = Regex("(?is)<think>")
    private val thinkClose = Regex("(?is)</think>")
    private val toolJson = Regex("""\[\s*"web\.run"\s*,\s*\{[\s\S]*?}]\s*""")
    private val sourcesHeader = Regex("""(?im)^\s*\*{0,2}Sources?\*{0,2}\s*$""")

    data class Cleaned(
        val body: String,
        val reasoning: String,
        val sources: List<BridgeSource>,
    )

    fun clean(raw: String, extraReasoning: String = ""): Cleaned {
        if (raw.isBlank()) {
            return Cleaned("", extraReasoning.trim(), emptyList())
        }
        var text = raw.replace(toolJson, "").trim()
        val fromTags = thinkBlock.findAll(text)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
        text = thinkBlock.replace(text, "").trim()
        // Unclosed <think> during stream — hide partial thinking from body
        val openIdx = text.indexOf("<think>", ignoreCase = true)
        val streamThink = if (openIdx >= 0) {
            val partial = text.substring(openIdx + 7)
            text = text.substring(0, openIdx).trim()
            partial.trim()
        } else ""
        text = text.replace(thinkOpen, "").replace(thinkClose, "").trim()

        val parts = BridgeContent.prepare(text)
        val reasoning = sequenceOf(extraReasoning, fromTags, streamThink)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .trim()

        return Cleaned(
            body = parts.body.trim(),
            reasoning = reasoning,
            sources = parts.sources,
        )
    }

    /** Live stream: never show open/closed think tags in the bubble body. */
    fun visibleBody(raw: String): String {
        var t = thinkBlock.replace(raw, "")
        val open = t.indexOf("<think>", ignoreCase = true)
        if (open >= 0) t = t.substring(0, open)
        return t.replace(thinkOpen, "").replace(thinkClose, "").trim()
    }

    fun visibleReasoning(raw: String, streamReasoning: String): String {
        val fromTags = thinkBlock.findAll(raw).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }
        val openIdx = raw.indexOf("<think>", ignoreCase = true)
        val partial = if (openIdx >= 0 && !raw.contains("</think>", ignoreCase = true)) {
            raw.substring(openIdx + 7).trim()
        } else ""
        return sequenceOf(streamReasoning, fromTags.joinToString("\n\n"), partial)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }
}
