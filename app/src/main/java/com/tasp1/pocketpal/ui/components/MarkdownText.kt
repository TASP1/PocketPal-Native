package com.tasp1.pocketpal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasp1.pocketpal.ui.components.claude.ArtifactCard

/**
 * Lightweight markdown: **bold**, *italic*, `code`, [links](url), bullets, ```fenced code``` → ArtifactCard.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    val blocks = remember(text) { splitMarkdownBlocks(text) }
    Column(modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Code -> {
                    ArtifactCard(
                        title = "Code",
                        language = block.lang,
                        body = block.body,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
                is MdBlock.Paragraph -> {
                    InlineMarkdown(block.text, color)
                }
            }
        }
    }
}

private sealed class MdBlock {
    data class Paragraph(val text: String) : MdBlock()
    data class Code(val lang: String?, val body: String) : MdBlock()
}

private fun splitMarkdownBlocks(src: String): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    val fence = Regex("```([\\w+-]*)\\n([\\s\\S]*?)```")
    var last = 0
    fence.findAll(src).forEach { m ->
        if (m.range.first > last) {
            out += MdBlock.Paragraph(src.substring(last, m.range.first).trimEnd())
        }
        out += MdBlock.Code(m.groupValues[1].ifBlank { null }, m.groupValues[2].trimEnd())
        last = m.range.last + 1
    }
    if (last < src.length) out += MdBlock.Paragraph(src.substring(last).trimEnd())
    if (out.isEmpty()) out += MdBlock.Paragraph(src)
    return out.filter {
        when (it) {
            is MdBlock.Paragraph -> it.text.isNotBlank()
            is MdBlock.Code -> it.body.isNotBlank()
        }
    }
}

@Composable
private fun InlineMarkdown(text: String, color: Color) {
    val uriHandler = LocalUriHandler.current
    val annotated = remember(text) {
        buildAnnotatedString {
            var i = 0
            val s = text
            while (i < s.length) {
                when {
                    s.startsWith("**", i) -> {
                        val end = s.indexOf("**", i + 2)
                        if (end > i) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(s.substring(i + 2, end))
                            }
                            i = end + 2
                        } else {
                            append(s[i]); i++
                        }
                    }
                    s.startsWith("*", i) && !s.startsWith("**", i) -> {
                        val end = s.indexOf("*", i + 1)
                        if (end > i) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(s.substring(i + 1, end))
                            }
                            i = end + 1
                        } else {
                            append(s[i]); i++
                        }
                    }
                    s.startsWith("`", i) -> {
                        val end = s.indexOf("`", i + 1)
                        if (end > i) {
                            withStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = Color(0x14333333),
                                    fontSize = 13.sp,
                                ),
                            ) { append(s.substring(i + 1, end)) }
                            i = end + 1
                        } else {
                            append(s[i]); i++
                        }
                    }
                    s.startsWith("[", i) -> {
                        val mid = s.indexOf("](", i)
                        val end = if (mid > i) s.indexOf(")", mid + 2) else -1
                        if (mid > i && end > mid) {
                            val label = s.substring(i + 1, mid)
                            val url = s.substring(mid + 2, end)
                            pushStringAnnotation("URL", url)
                            withStyle(
                                SpanStyle(
                                    color = Color(0xFF2563EB),
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ) { append(label) }
                            pop()
                            i = end + 1
                        } else {
                            append(s[i]); i++
                        }
                    }
                    else -> {
                        append(s[i]); i++
                    }
                }
            }
        }
    }
    ClickableText(
        text = annotated,
        style = TextStyle(
            color = color,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                runCatching { uriHandler.openUri(it.item) }
            }
        },
    )
}
