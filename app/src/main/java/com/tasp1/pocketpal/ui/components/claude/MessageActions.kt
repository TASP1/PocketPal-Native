package com.tasp1.pocketpal.ui.components.claude

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageActionBar(
    onCopy: () -> Unit = {},
    onShare: () -> Unit = {},
    onSpeak: () -> Unit = {},
    onGood: () -> Unit = {},
    onBad: () -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    Row(modifier.padding(top = 4.dp)) {
        IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, "Copy", tint = tint) }
        IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, "Share", tint = tint) }
        IconButton(onClick = onSpeak) { Icon(Icons.Outlined.VolumeUp, "Speak", tint = tint) }
        IconButton(onClick = onGood) { Icon(Icons.Outlined.ThumbUp, "Good", tint = tint) }
        IconButton(onClick = onBad) { Icon(Icons.Outlined.ThumbDown, "Bad", tint = tint) }
        IconButton(onClick = onRetry) { Icon(Icons.Outlined.Refresh, "Retry", tint = tint) }
    }
}
