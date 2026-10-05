package com.tasp1.pocketpal.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Lightweight markdown subset for chat bubbles (bold, italic, code, links, bullets).
 * Full CommonMark → later; enough to match PocketPal's readable assistant chrome.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.secondary
    val codeBg = MaterialTheme.colorScheme.surfaceVariant

    val annotated = remember(markdown, color, linkColor) {
        buildAnnotatedString {
            // Normalize simple bullets
            val lines = markdown.lines()
            lines.forEachIndexed { idx, rawLine ->
                var line = rawLine
                val bullet = Regex("""^\s*[-*•]\s+""").find(line)
                if (bullet != null) {
                    append("• ")
                    line = line.substring(bullet.range.last + 1)
                }
                // Inline parse: **bold**, *italic*, `code`, [text](url), bare URLs
                var i = 0
                while (i < line.length) {
                    when {
                        line.startsWith("**", i) -> {
                            val end = line.indexOf("**", i + 2)
                            if (end > i) {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                                    append(line.substring(i + 2, end))
                                }
                                i = end + 2
                            } else {
                                append(line[i]); i++
                            }
                        }
                        line.startsWith("`", i) -> {
                            val end = line.indexOf('`', i + 1)
                            if (end > i) {
                                withStyle(
                                    SpanStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        background = codeBg,
                                        color = color,
                                    ),
                                ) { append(line.substring(i + 1, end)) }
                                i = end + 1
                            } else {
                                append(line[i]); i++
                            }
                        }
                        line.startsWith("[", i) -> {
                            val close = line.indexOf(']', i)
                            val openParen = if (close >= 0) line.indexOf('(', close) else -1
                            val closeParen = if (openParen >= 0) line.indexOf(')', openParen) else -1
                            if (close > i && openParen == close + 1 && closeParen > openParen) {
                                val label = line.substring(i + 1, close)
                                val url = line.substring(openParen + 1, closeParen)
                                pushStringAnnotation("URL", url)
                                withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                                    append(label)
                                }
                                pop()
                                i = closeParen + 1
                            } else {
                                append(line[i]); i++
                            }
                        }
                        line.startsWith("http://", i) || line.startsWith("https://", i) -> {
                            val end = line.indexOfFirstFrom(i) { c -> c.isWhitespace() || c == ')' }
                            val url = line.substring(i, end)
                            pushStringAnnotation("URL", url)
                            withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                                append(url)
                            }
                            pop()
                            i = end
                        }
                        line.startsWith("*", i) && !line.startsWith("**", i) -> {
                            val end = line.indexOf('*', i + 1)
                            if (end > i) {
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = color)) {
                                    append(line.substring(i + 1, end))
                                }
                                i = end + 1
                            } else {
                                append(line[i]); i++
                            }
                        }
                        else -> {
                            withStyle(SpanStyle(color = color)) { append(line[i]) }
                            i++
                        }
                    }
                }
                if (idx < lines.lastIndex) append("\n")
            }
        }
    }

    var layout: TextLayoutResult? = null
    BasicText(
        text = annotated,
        modifier = modifier.pointerInput(annotated) {
            detectTapGestures { pos ->
                layout?.let { lay ->
                    val offset = lay.getOffsetForPosition(pos)
                    annotated.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()
                        ?.let { uriHandler.openUri(it.item) }
                }
            }
        },
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        onTextLayout = { layout = it },
    )
}

private fun String.indexOfFirstFrom(start: Int, pred: (Char) -> Boolean): Int {
    for (i in start until length) if (pred(this[i])) return i
    return length
}
