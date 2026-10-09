package com.tasp1.pocketpal.data

/**
 * Split model output into visible answer vs reasoning.
 * Prevents raw think-tags leaking into the chat body.
 */
object MessageCleaner {
    private const val TAG = "think"

    private val thinkBlock: Regex =
        Regex("(?is)<" + TAG + ">(.*?)</" + TAG + ">")
    private val thinkOpen: Regex =
        Regex("(?is)<" + TAG + ">")
    private val thinkClose: Regex =
        Regex("(?is)</" + TAG + ">")
    private val toolJson =
        Regex("""\[\s*"web\.run"\s*,\s*\{[\s\S]*?}]\s*""")

    data class Cleaned(
        val body: String,
        val reasoning: String,
        val sources: List<BridgeSource>,
    )

    fun clean(raw: String, extraReasoning: String = ""): Cleaned {
        if (raw.isBlank()) return Cleaned("", extraReasoning.trim(), emptyList())
        var text = raw.replace(toolJson, "").trim()
        val fromTags = thinkBlock.findAll(text)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
        text = thinkBlock.replace(text, "").trim()
        val openMarker = "<$TAG>"
        val openIdx = text.indexOf(openMarker, ignoreCase = true)
        val streamThink = if (openIdx >= 0) {
            val partial = text.substring(openIdx + openMarker.length)
            text = text.substring(0, openIdx).trim()
            partial.trim()
        } else {
            ""
        }
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

    fun visibleBody(raw: String): String {
        var t = thinkBlock.replace(raw, "")
        val openMarker = "<$TAG>"
        val open = t.indexOf(openMarker, ignoreCase = true)
        if (open >= 0) t = t.substring(0, open)
        return t.replace(thinkOpen, "").replace(thinkClose, "").trim()
    }

    fun visibleReasoning(raw: String, streamReasoning: String): String {
        val fromTags = thinkBlock.findAll(raw)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
        val openMarker = "<$TAG>"
        val closeMarker = "</$TAG>"
        val openIdx = raw.indexOf(openMarker, ignoreCase = true)
        val partial = if (openIdx >= 0 && !raw.contains(closeMarker, ignoreCase = true)) {
            raw.substring(openIdx + openMarker.length).trim()
        } else {
            ""
        }
        return sequenceOf(streamReasoning, fromTags.joinToString("\n\n"), partial)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }
}
