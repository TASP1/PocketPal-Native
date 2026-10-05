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
import androidx.compose.material3.Button
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
import com.tasp1.pocketpal.BuildConfig
import com.tasp1.pocketpal.PocketPalApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenDrawer: () -> Unit) {
    val app = LocalContext.current.applicationContext as PocketPalApp
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(BuildConfig.BRIDGE_URL) }
    var key by remember { mutableStateOf(BuildConfig.BRIDGE_KEY) }
    var theme by remember { mutableStateOf("system") }
    var haptics by remember { mutableStateOf(true) }
    var healthLine by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val s = app.container.settings.settings.first()
        url = s.serverUrl
        key = s.apiKey
        theme = s.theme
        haptics = s.haptics
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
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text("Kaggle Bridge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Keys are pre-wired from BuildConfig; edit to override.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(url, { url = it }, label = { Text("Server URL") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = true)
            OutlinedTextField(key, { key = it }, label = { Text("API key (BRIDGE_KEY)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        app.container.settings.setServer(url, key)
                        app.container.bridge.updateCredentials(url, key)
                        healthLine = runCatching {
                            val h = app.container.bridge.health()
                            "ok=${h.ok} backend=${h.backendConnected} caps=${h.caps}"
                        }.getOrElse { it.message ?: "error" }
                    }
                }) { Text("Save & ping") }
                Button(onClick = {
                    url = BuildConfig.BRIDGE_URL
                    key = BuildConfig.BRIDGE_KEY
                }) { Text("Reset defaults") }
            }
            if (healthLine.isNotBlank()) {
                Text(healthLine, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("system", "light", "dark").forEach { t ->
                    FilterChip(selected = theme == t, onClick = {
                        theme = t
                        scope.launch { app.container.settings.setTheme(t) }
                    }, label = { Text(t.replaceFirstChar { it.uppercase() }) })
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Haptic feedback")
                    Text("Vibrate on actions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = haptics, onCheckedChange = {
                    haptics = it
                    scope.launch { app.container.settings.setHaptics(it) }
                })
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("Architecture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "• OpenAI-compatible protocol (/v1/chat/completions + SSE)\n" +
                    "• Model flags: :web :think :shell :low|:medium|:high\n" +
                    "• BridgeClient + ChatViewModel streaming pipeline\n" +
                    "• DataStore-backed credentials (defaults = live keys)\n" +
                    "• Caps: stream, native_reasoning",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
