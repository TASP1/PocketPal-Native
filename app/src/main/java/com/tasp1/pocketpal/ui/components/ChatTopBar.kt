package com.tasp1.pocketpal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/** Minimal Claude-like chat app bar: menu · title · new · more */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    title: String,
    modelId: String,
    onMenu: () -> Unit,
    onNewChat: () -> Unit,
    onMore: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    title.ifBlank { "Chat" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (modelId.isNotBlank()) {
                    Text(
                        modelId.substringAfterLast('/').take(36),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenu) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, contentDescription = "New chat")
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}
