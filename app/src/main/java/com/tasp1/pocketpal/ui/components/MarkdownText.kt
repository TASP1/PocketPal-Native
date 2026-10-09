package com.tasp1.pocketpal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasp1.pocketpal.ui.components.claude.ArtifactCard

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    val blocks = remember(text) { splitBlocks(text) }
    Column(modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Code -> ArtifactCard(
                    title = block.lang?.replaceFirstChar { it.uppercase() } ?: "Code",
                    language = block.lang,
                    body = block.body,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                is MdBlock.Paragraph -> {
                    if (block.text.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = remember(block.text) { styleInline(block.text) },
                                color = color,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed class MdBlock {
    data class Paragraph(val text: String) : MdBlock()
    data class Code(val lang: String?, val body: String) : MdBlock()
}

private fun splitBlocks(src: String): List<MdBlock> {
    if (src.isBlank()) return emptyList()
    return try {
        val out = mutableListOf<MdBlock>()
        val fence = Regex("```([\\w+-]*)\\n([\\s\\S]*?)```")
        var last = 0
        fence.findAll(src).forEach { m ->
            if (m.range.first > last) {
                val p = src.substring(last, m.range.first).trimEnd()
                if (p.isNotBlank()) out += MdBlock.Paragraph(p)
            }
            out += MdBlock.Code(m.groupValues[1].ifBlank { null }, m.groupValues[2].trimEnd())
            last = m.range.last + 1
        }
        if (last < src.length) {
            val p = src.substring(last).trimEnd()
            if (p.isNotBlank()) out += MdBlock.Paragraph(p)
        }
        if (out.isEmpty()) listOf(MdBlock.Paragraph(src)) else out
    } catch (_: Exception) {
        listOf(MdBlock.Paragraph(src))
    }
}

private fun styleInline(src: String) = buildAnnotatedString {
    var i = 0
    val s = src
    while (i < s.length) {
        when {
            s.startsWith("**", i) -> {
                val end = s.indexOf("**", i + 2)
                if (end > i) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(s.substring(i + 2, end))
                    }
                    i = end + 2
                } else { append(s[i]); i++ }
            }
            s.startsWith("`", i) -> {
                val end = s.indexOf("`", i + 1)
                if (end > i) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)) {
                        append(s.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append(s[i]); i++ }
            }
            s.startsWith("- ", i) && (i == 0 || s[i - 1] == '\n') -> {
                append("• ")
                i += 2
            }
            s.startsWith("* ", i) && (i == 0 || s[i - 1] == '\n') -> {
                append("• ")
                i += 2
            }
            else -> { append(s[i]); i++ }
        }
    }
}
