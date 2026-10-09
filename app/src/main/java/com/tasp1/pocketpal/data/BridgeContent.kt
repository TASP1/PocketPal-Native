package com.tasp1.pocketpal.data

/**
 * Strip tool noise + lift Sources into structured cards.
 */
data class BridgeSource(val index: Int, val title: String, val url: String)

data class BridgeContentParts(val body: String, val sources: List<BridgeSource>)

object BridgeContent {
    private val toolJsonInline =
        Regex("""\[\s*"web\.run"\s*,\s*\{[\s\S]*?}]\s*""")
    private val sourcesBlock =
        Regex("""(?:^|\n)\s*\*{0,2}Sources?\*{0,2}\s*\n((?:[\s\S]*))""", RegexOption.IGNORE_CASE)
    private val citeBracket =
        Regex("""\[(\d+)]\s*([^\n\[]*?)\s*[-–—]\s*(https?://\S+)""")
    private val citeMd =
        Regex("""\[(\d+)]\s*([^\n]+?)\s*-\s*(https?://\S+)""")
    private val bareUrl =
        Regex("""\[(\d+)]\s*(https?://\S+)""")

    fun prepare(raw: String): BridgeContentParts {
        var text = raw.replace(toolJsonInline, "").trim()
        val sources = mutableListOf<BridgeSource>()
        val seen = mutableSetOf<String>()

        fun add(index: Int, title: String, url: String) {
            val u = url.trimEnd('.', ',', ')', ';', ']')
            if (u in seen) return
            seen.add(u)
            sources.add(BridgeSource(index, title.trim().ifBlank { u }, u))
        }

        // Pull trailing Sources section out of body
        val split = Regex("""(?is)(?:^|\n)\s*\*{0,2}Sources?\*{0,2}\s*\n""")
        val m = split.find(text)
        val sourceSection = if (m != null) {
            val sec = text.substring(m.range.last + 1)
            text = text.substring(0, m.range.first).trim()
            sec
        } else ""

        val scan = sourceSection.ifBlank { text }
        citeBracket.findAll(scan).forEach { add(it.groupValues[1].toIntOrNull() ?: (sources.size + 1), it.groupValues[2], it.groupValues[3]) }
        citeMd.findAll(scan).forEach { add(it.groupValues[1].toIntOrNull() ?: (sources.size + 1), it.groupValues[2], it.groupValues[3]) }
        bareUrl.findAll(scan).forEach { add(it.groupValues[1].toIntOrNull() ?: (sources.size + 1), "", it.groupValues[2]) }

        // Also pull [n] citations that appear only in body if no section
        if (sources.isEmpty()) {
            citeBracket.findAll(text).forEach { add(it.groupValues[1].toIntOrNull() ?: (sources.size + 1), it.groupValues[2], it.groupValues[3]) }
        }

        text = text.replace(Regex("\n{3,}"), "\n\n").trim()
        return BridgeContentParts(body = text, sources = sources.sortedBy { it.index })
    }
}
