package com.tasp1.pocketpal.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    title: String,
    modelId: String,
    onMenu: () -> Unit,
    onNewChat: () -> Unit,
    onDeleteChat: () -> Unit = {},
    onRename: () -> Unit = {},
    onShare: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }

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
                        modelId.take(52),
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
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { menuOpen = false; onShare() },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { menuOpen = false; onRename() },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text("Pin") },
                        onClick = { menuOpen = false },
                        leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text("Add to home") },
                        onClick = { menuOpen = false },
                        leadingIcon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFD1433B)) },
                        onClick = { menuOpen = false; onDeleteChat() },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = Color(0xFFD1433B),
                            )
                        },
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}
