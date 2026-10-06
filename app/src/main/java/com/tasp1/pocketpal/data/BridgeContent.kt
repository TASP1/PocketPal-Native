package com.tasp1.pocketpal.data

/**
 * Mirrors RN bridgeContent — strip tool protocol noise + lift Sources into cards.
 */
data class BridgeSource(val index: Int, val title: String, val url: String)

data class BridgeContentParts(val body: String, val sources: List<BridgeSource>)

object BridgeContent {
    private val toolJsonInline =
        Regex("""\[\s*"web\.run"\s*,\s*\{[\s\S]*?}]\s*""")
    private val sourcesBlock =
        Regex("""(?:^|\n)\s*Sources\s*((?:\[\d+][^\n]*)+)""", RegexOption.IGNORE_CASE)
    private val cite =
        Regex("""\[(\d+)]\s*([^\[\n]*?)\s*[-–—]\s*(https?://\S+)""")

    fun prepare(raw: String): BridgeContentParts {
        var text = raw.replace(toolJsonInline, "").trim()
        val sources = mutableListOf<BridgeSource>()
        val seen = mutableSetOf<String>()
        val blocks = mutableListOf<String>()
        text = sourcesBlock.replace(text) { m ->
            blocks.add(m.groupValues[1])
            "\n"
        }
        for (block in blocks) {
            cite.findAll(block).forEach { m ->
                val url = m.groupValues[3].trimEnd('.', ',', ')', ';')
                if (url in seen) return@forEach
                seen.add(url)
                sources.add(
                    BridgeSource(
                        index = m.groupValues[1].toIntOrNull() ?: (sources.size + 1),
                        title = m.groupValues[2].trim().ifBlank { url },
                        url = url,
                    ),
                )
            }
        }
        text = text.replace(Regex("\n{3,}"), "\n\n").trim()
        return BridgeContentParts(body = text, sources = sources)
    }
}
