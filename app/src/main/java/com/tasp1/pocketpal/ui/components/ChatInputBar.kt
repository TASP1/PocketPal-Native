package com.tasp1.pocketpal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasp1.pocketpal.domain.Attachment
import com.tasp1.pocketpal.ui.theme.LocalPpExtra

/**
 * Claude-inspired floating input:
 * rounded card · + attach · model chip · text · mic · send/stop
 * capability chips (Web / Think / Shell) above when expanded.
 */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit = {},
    modelLabel: String = "",
    onModelClick: () -> Unit = {},
    sending: Boolean = false,
    webEnabled: Boolean,
    thinkEnabled: Boolean,
    shellEnabled: Boolean,
    onToggleWeb: () -> Unit,
    onToggleThink: () -> Unit,
    onToggleShell: () -> Unit,
    pending: List<Attachment> = emptyList(),
    onAttach: () -> Unit = {},
    onRemovePending: (String) -> Unit = {},
    processingAttach: Boolean = false,
    highVision: Boolean = true,
    onToggleHighVision: () -> Unit = {},
    autoOcr: Boolean = true,
    onToggleOcr: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val extra = LocalPpExtra.current
    val cardShape = RoundedCornerShape(28.dp)
    val canSend = (value.isNotBlank() || pending.isNotEmpty()) && !sending

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (pending.isNotEmpty() || processingAttach) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (processingAttach) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Processing…", style = MaterialTheme.typography.labelMedium)
                }
                pending.forEach { a ->
                    AttachmentPreviewChip(
                        attachment = a,
                        onRemove = { onRemovePending(a.id) },
                    )
                }
            }
        }

        // Capability chips row (Claude “tools” feel)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CapChip("Web", webEnabled, Icons.Outlined.TravelExplore, onToggleWeb)
            CapChip("Think", thinkEnabled, Icons.Outlined.Psychology, onToggleThink)
            CapChip("Shell", shellEnabled, Icons.Outlined.Terminal, onToggleShell)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
                .padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        if (pending.isEmpty()) "Reply…" else "Add a caption…",
                        color = extra.placeholder,
                        fontSize = 16.sp,
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                maxLines = 6,
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onAttach) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (modelLabel.isNotBlank()) {
                    Text(
                        modelLabel.substringAfterLast('/').substringBefore(':').take(18),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onModelClick)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                SpacerWeight()
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = "Voice",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (sending) {
                    IconButton(onClick = onStop) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(6.dp),
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSend,
                        enabled = canSend,
                    ) {
                        Icon(
                            if (canSend) Icons.AutoMirrored.Filled.Send else Icons.Outlined.GraphicEq,
                            contentDescription = "Send",
                            tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) MaterialTheme.colorScheme.onBackground
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpacerWeight() {
    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
}

@Composable
private fun CapChip(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF1A1A1A) else Color.Transparent)
            .then(
                if (!selected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            label,
            fontSize = 13.sp,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
