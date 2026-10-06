package com.tasp1.pocketpal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Psychology
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tasp1.pocketpal.domain.Attachment
import com.tasp1.pocketpal.ui.theme.LocalPpExtra

@Composable
private fun CapChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .then(
                if (enabled) Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), shape)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = if (enabled) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

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
    val shape = RoundedCornerShape(28.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CapChip("Web", webEnabled, onToggleWeb)
            CapChip(if (thinkEnabled) "Think · Think" else "Think", thinkEnabled, onToggleThink)
            CapChip("Shell", shellEnabled, onToggleShell)
            CapChip(if (highVision) "Vision HQ" else "Vision", highVision, onToggleHighVision)
            CapChip(if (autoOcr) "OCR" else "OCR off", autoOcr, onToggleOcr)
        }

        if (pending.isNotEmpty() || processingAttach) {
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (processingAttach) {
                    item {
                        Box(
                            Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
                items(pending, key = { it.id }) { a ->
                    Box {
                        if (a.isImage) {
                            AsyncImage(
                                model = a.uri,
                                contentDescription = a.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        } else {
                            Column(
                                Modifier
                                    .width(120.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    a.displayName,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp,
                                )
                                Text(a.sizeLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { onRemovePending(a.id) }
                                .padding(2.dp),
                            tint = Color.White,
                        )
                        if (!a.ocrText.isNullOrBlank()) {
                            Icon(
                                Icons.Outlined.DocumentScanner,
                                contentDescription = "OCR ready",
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .size(16.dp),
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(shape)
                .background(extra.inputBar)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onAttach) {
                Icon(Icons.Default.Add, contentDescription = "Attach image or file", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "More",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (thinkEnabled) Color(0xFF1A1A1A) else Color.Transparent)
                    .then(
                        if (!thinkEnabled) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                        else Modifier,
                    )
                    .clickable(onClick = onToggleThink)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Outlined.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (thinkEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Think",
                    fontSize = 13.sp,
                    color = if (thinkEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (modelLabel.isNotBlank()) {
                Text(
                    modelLabel.substringAfterLast('/').take(22),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onModelClick)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (pending.isEmpty()) "Type your message here"
                        else "Add a caption for ${pending.size} attachment(s)…",
                        color = extra.placeholder,
                        fontSize = 15.sp,
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true,
            )
            if (sending) {
                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank() || pending.isNotEmpty(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (value.isNotBlank() || pending.isNotEmpty()) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}
