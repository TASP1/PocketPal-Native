package com.tasp1.pocketpal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasp1.pocketpal.ui.theme.LocalPpExtra

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val meta: String? = null,
)

@Composable
fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val extra = LocalPpExtra.current
    val bg = if (message.isUser) extra.userBubble else MaterialTheme.colorScheme.background
    val align = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (message.isUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(0.dp)
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = align) {
        Column(
            modifier = Modifier
                .widthIn(max = if (message.isUser) 320.dp else 600.dp)
                .then(
                    if (message.isUser) Modifier
                        .clip(shape)
                        .background(bg)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                    else Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                ),
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                textAlign = if (message.isUser) TextAlign.End else TextAlign.Start,
            )
            message.meta?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
