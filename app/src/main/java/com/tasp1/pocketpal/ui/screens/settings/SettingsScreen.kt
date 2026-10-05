package com.tasp1.pocketpal.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tasp1.pocketpal.data.AppState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenDrawer: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionTitle("Kaggle Bridge server")
            Text(
                "OpenAI-compatible remote. Do not append /v1.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = AppState.serverUrl,
                onValueChange = { AppState.serverUrl = it },
                label = { Text("Server URL") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                singleLine = true,
            )
            OutlinedTextField(
                value = AppState.apiKey,
                onValueChange = { AppState.apiKey = it },
                label = { Text("API key (BRIDGE_KEY)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                singleLine = true,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Appearance")
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "System", false to "Light", true to "Dark").forEach { (v, label) ->
                    FilterChip(
                        selected = AppState.darkTheme == v,
                        onClick = { AppState.darkTheme = v },
                        label = { Text(label) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Model initialization (local)")
            OutlinedTextField(
                value = AppState.contextSize,
                onValueChange = { AppState.contextSize = it.filter { c -> c.isDigit() } },
                label = { Text("Context size") },
                supportingText = { Text("Needs model reload · max depends on device RAM") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                singleLine = true,
            )
            Text("Flash attention", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("auto", "on", "off").forEach { opt ->
                    FilterChip(
                        selected = AppState.flashAttention == opt,
                        onClick = { AppState.flashAttention = opt },
                        label = { Text(opt.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Feedback")
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Haptic feedback", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Vibrate on send / actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = AppState.haptics, onCheckedChange = { AppState.haptics = it })
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Server capabilities")
            Text(
                "• stream — SSE token streaming\n" +
                    "• native_reasoning — :think / effort levels\n" +
                    "• :web — live search + Sources cards\n" +
                    "• :shell — Render MCP shell\n" +
                    "• OpenAI /v1/chat/completions + /v1/models",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}
