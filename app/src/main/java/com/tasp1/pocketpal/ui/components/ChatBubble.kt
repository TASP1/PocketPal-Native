package com.tasp1.pocketpal.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasp1.pocketpal.data.BridgeContent
import com.tasp1.pocketpal.data.BridgeSource
import com.tasp1.pocketpal.data.MessageCleaner
import com.tasp1.pocketpal.domain.ChatTurn
import com.tasp1.pocketpal.ui.components.claude.MessageActionBar
import com.tasp1.pocketpal.ui.components.claude.ThinkingBlock
import com.tasp1.pocketpal.ui.components.claude.ToolStepRow
import com.tasp1.pocketpal.ui.theme.LocalPpExtra

@Composable
fun ChatBubble(turn: ChatTurn, modifier: Modifier = Modifier) {
    val isUser = turn.role == ChatTurn.Role.User
    val extra = LocalPpExtra.current
    val rawBody = remember(turn.content, turn.isStreaming) {
        runCatching {
            if (turn.isStreaming) MessageCleaner.visibleBody(turn.content) else turn.content
        }.getOrDefault(turn.content)
    }
    val prepared = remember(turn.sourcesJson, rawBody) {
        runCatching {
            val base = BridgeContent.prepare(rawBody)
            if (turn.sourcesJson.isNotBlank()) {
                val srcs = turn.sourcesJson.lines().mapNotNull { line ->
                    val p = line.split("|")
                    if (p.size >= 3) BridgeSource(p[0].toIntOrNull() ?: 0, p[1], p[2]) else null
                }
                if (srcs.isNotEmpty()) base.copy(sources = srcs) else base
            } else base
        }.getOrElse {
            com.tasp1.pocketpal.data.BridgeContentParts(rawBody, emptyList())
        }
    }
    val displayBody = prepared.body
    val sources = prepared.sources

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .then(
                    if (isUser) Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(extra.userBubble)
                    else Modifier,
                )
                .padding(horizontal = if (isUser) 14.dp else 4.dp, vertical = 10.dp),
        ) {
            if (turn.attachmentNames.isNotEmpty()) {
                Text(
                    "📎 " + turn.attachmentNames.joinToString(", "),
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
                    maxLines = 3,
                )
            }
            if (turn.reasoning.isNotBlank() && !isUser) {
                ThinkingBlock(
                    text = turn.reasoning,
                    streaming = turn.isStreaming && turn.content.isBlank(),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            turn.toolSteps.forEach { step ->
                ToolStepRow(step = step, onOpen = {})
            }
            if (displayBody.isNotBlank()) {
                if (isUser) {
                    Text(
                        displayBody,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                } else {
                    MarkdownText(text = displayBody)
                }
            } else if (turn.isStreaming && !isUser) {
                Text("…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            }
            turn.error?.let { err ->
                Text(
                    err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (sources.isNotEmpty() && !isUser) {
                SourcesCard(sources = sources, modifier = Modifier.padding(top = 8.dp))
            }
            if (!isUser && !turn.isStreaming && displayBody.isNotBlank()) {
                val ctx = LocalContext.current
                MessageActionBar(
                    onCopy = {
                        runCatching {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("assistant", displayBody))
                        }
                    },
                )
            }
        }
    }
}
