package com.tasp1.pocketpal.ui.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ModelOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    models: List<ModelOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    effortLabel: String = "Medium",
    onEffort: () -> Unit = {},
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            "Select model",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        models.forEach { m ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(m.id); onDismiss() }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        m.badge?.let {
                            Text(
                                "  $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    Text(m.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (m.id == selectedId) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onEffort)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text("Effort", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(effortLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "More models",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
