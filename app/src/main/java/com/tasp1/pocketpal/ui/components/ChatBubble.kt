import com.tasp1.pocketpal.ui.components.claude.ThinkingBlock
package com.tasp1.pocketpal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tasp1.pocketpal.data.BridgeContent
import com.tasp1.pocketpal.domain.ChatTurn
import com.tasp1.pocketpal.ui.theme.LocalPpExtra

@Composable
fun ChatBubble(turn: ChatTurn, modifier: Modifier = Modifier) {
    val extra = LocalPpExtra.current
    val isUser = turn.role == ChatTurn.Role.User
    val parts = remember(turn.content, isUser) {
        if (isUser) null else BridgeContent.prepare(turn.content)
    }
    val body = when {
        isUser -> turn.content
        else -> parts?.body ?: turn.content
    }
    val sources = parts?.sources.orEmpty()
    val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = align) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isUser) 320.dp else 600.dp)
                .then(
                    if (isUser) Modifier
                        .clip(shape)
                        .background(extra.userBubble)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                    else Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                ),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            if (turn.imageDataUrls.isNotEmpty()) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    turn.imageDataUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(160.dp)
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }
            }
            if (turn.attachmentNames.isNotEmpty() && turn.imageDataUrls.isEmpty()) {
                Text(
                    turn.attachmentNames.joinToString(", "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            if (!turn.ocrPreview.isNullOrBlank() && isUser) {
                Text(
                    "OCR: ${turn.ocrPreview}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                    maxLines = 4,
                )
            }
            if (turn.reasoning.isNotBlank() && !isUser) {
                ThinkingBlock(
                    text = turn.reasoning,
                    streaming = turn.isStreaming && turn.content.isBlank(),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (body.isNotBlank()) {
                if (isUser) {
                    Text(
                        body,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                } else {
                    SelectionContainer {
                        MarkdownText(markdown = body)
                    }
                }
            }
            if (sources.isNotEmpty()) {
                SourcesCard(sources = sources, modifier = Modifier.padding(top = 8.dp))
            }
            turn.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (turn.isStreaming) {
                Text(
                    "…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
