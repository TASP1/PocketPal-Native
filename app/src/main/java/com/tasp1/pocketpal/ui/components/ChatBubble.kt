package com.tasp1.pocketpal.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tasp1.pocketpal.data.BridgeContent
import com.tasp1.pocketpal.data.BridgeSource
import com.tasp1.pocketpal.data.BridgeContentParts
import com.tasp1.pocketpal.domain.ChatTurn
import com.tasp1.pocketpal.ui.components.claude.MessageActionBar
import com.tasp1.pocketpal.ui.components.claude.ThinkingBlock
import com.tasp1.pocketpal.ui.components.claude.ToolStepRow
import com.tasp1.pocketpal.ui.theme.LocalPpExtra

@Composable
fun ChatBubble(turn: ChatTurn, modifier: Modifier = Modifier) {
    val isUser = turn.role == ChatTurn.Role.User
    val extra = LocalPpExtra.current
    val body = turn.content
    val sources = remember(turn.sourcesJson, body) {
        if (turn.sourcesJson.isNotBlank()) {
            turn.sourcesJson.lines().mapNotNull { line ->
                val p = line.split("|")
                if (p.size >= 3) BridgeSource(p[0].toIntOrNull() ?: 0, p[1], p[2]) else null
            }
        } else {
            BridgeContent.prepare(body).sources
        }
    }
    val displayBody = remember(body) { BridgeContent.prepare(body).body }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(if (isUser) 18.dp else 4.dp))
                .background(if (isUser) extra.userBubble else extra.assistantBubble)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (turn.imageDataUrls.isNotEmpty()) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            turn.toolSteps.forEach { step ->
                ToolStepRow(step = step, onOpen = {})
            }
            if (displayBody.isNotBlank()) {
                if (isUser) {
                    Text(
                        displayBody,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                } else {
                    SelectionContainer {
                        MarkdownText(text = displayBody)
                    }
                }
            }
            if (turn.error != null) {
                Text(
                    turn.error,
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
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("assistant", displayBody))
                    },
                )
            }
        }
    }
}
