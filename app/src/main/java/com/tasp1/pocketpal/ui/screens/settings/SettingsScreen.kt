package com.tasp1.pocketpal.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tasp1.pocketpal.PocketPalApp
import com.tasp1.pocketpal.data.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenDrawer: () -> Unit) {
    val app = LocalContext.current.applicationContext as PocketPalApp
    val container = app.container
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var theme by remember { mutableStateOf("system") }
    var haptics by remember { mutableStateOf(true) }
    var useLocal by remember { mutableStateOf(false) }
    var secondaryUrl by remember { mutableStateOf("") }
    var secondaryKey by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val s: UserSettings = container.settings.settings.first()
        url = s.serverUrl
        key = s.apiKey
        theme = s.theme
        haptics = s.haptics
        useLocal = s.useLocal
    }

    fun persist() {
        scope.launch {
            container.settings.setServer(url, key)
            container.settings.setTheme(theme)
            container.settings.setHaptics(haptics)
            container.settings.setUseLocal(useLocal)
            container.applyServer(url, key, useLocal)
        }
    }

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
            SectionTitle("Active backend")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                FilterChip(
                    selected = !useLocal,
                    onClick = { useLocal = false; persist() },
                    label = { Text("Remote") },
                )
                FilterChip(
                    selected = useLocal,
                    onClick = { useLocal = true; persist() },
                    label = { Text("On-device GGUF") },
                )
            }
            if (useLocal) {
                Text(
                    "Local engine architecture is ready. Link llama.cpp / ExecuTorch in a later build to run GGUF files offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            SectionTitle("Primary server (Kaggle Bridge / OpenAI-compatible)")
            Text(
                "Native key is pre-filled from BuildConfig. URL without trailing /v1.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; persist() },
                label = { Text("Server URL") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                singleLine = true,
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it; persist() },
                label = { Text("API key") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                singleLine = true,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Secondary OpenAI-compatible (optional)")
            Text(
                "e.g. OpenRouter, Groq, local LM Studio — switch by pasting URL/key into primary fields when needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = secondaryUrl,
                onValueChange = { secondaryUrl = it },
                label = { Text("Alt server URL") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                singleLine = true,
            )
            OutlinedTextField(
                value = secondaryKey,
                onValueChange = { secondaryKey = it },
                label = { Text("Alt API key") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                singleLine = true,
            )
            androidx.compose.material3.TextButton(
                onClick = {
                    if (secondaryUrl.isNotBlank()) {
                        url = secondaryUrl.trimEnd('/')
                        key = secondaryKey
                        useLocal = false
                        persist()
                    }
                },
            ) { Text("Use alt as primary") }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (v, label) ->
                    FilterChip(
                        selected = theme == v,
                        onClick = { theme = v; persist() },
                        label = { Text(label) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Feedback")
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Haptic feedback", style = MaterialTheme.typography.bodyLarge)
                    Text("Vibrate on send", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = haptics, onCheckedChange = { haptics = it; persist() })
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Agent shell")
            Text(
                "Type \$ ls or /shell ls in chat to run on the bridge. " +
                    "Enable Shell chip to prefer the Render agent host. " +
                    "Bridge URL/key above are used for auth.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            SectionTitle("Native capabilities")
            Text(
                "• Vision + OCR (ML Kit) on image attach\n" +
                    "• Large files up to 50 MB (text/PDF/code previews)\n" +
                    "• SSE streaming + stop + Thoughts panel (Claude-style)\n" +
                    "• Multi-server: Bridge / OpenAI-compatible / local shell\n" +
                    "• Model flags: :web :think :shell :effort",
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
        modifier = Modifier.padding(bottom = 4.dp, top = 4.dp),
    )
}
