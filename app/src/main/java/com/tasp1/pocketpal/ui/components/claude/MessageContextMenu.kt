package com.tasp1.pocketpal.ui.components.claude

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun MessageContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onSelect: () -> Unit = {},
    onEdit: () -> Unit = {},
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Copy message") }, onClick = { onCopy(); onDismiss() })
        DropdownMenuItem(text = { Text("Select text") }, onClick = { onSelect(); onDismiss() })
        DropdownMenuItem(text = { Text("Edit") }, onClick = { onEdit(); onDismiss() })
    }
}
