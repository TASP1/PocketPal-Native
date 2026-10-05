package com.tasp1.pocketpal.ui.screens.models

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasp1.pocketpal.PocketPalApp
import com.tasp1.pocketpal.domain.LocalModel
import com.tasp1.pocketpal.domain.RemoteModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(onOpenDrawer: () -> Unit) {
    val app = LocalContext.current.applicationContext as PocketPalApp
    var tab by remember { mutableStateOf(0) }
    var remote by remember { mutableStateOf<List<RemoteModel>>(emptyList()) }
    var selected by remember { mutableStateOf("google/gemini-2.5-flash") }
    var status by remember { mutableStateOf("Loading models…") }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            status = "Fetching /v1/models…"
            val s = app.container.settings.settings.first()
            app.container.bridge.updateCredentials(s.serverUrl, s.apiKey)
            selected = s.baseModel
            runCatching { app.container.bridge.listModels() }
                .onSuccess {
                    remote = it
                    status = "${it.size} remote models"
                }
                .onFailure { status = "Error: ${it.message}" }
        }
    }

    LaunchedEffect(Unit) { reload() }

    val local = listOf(
        LocalModel("smollm2-1.7b", "SmolLM2 1.7B Instruct", "1.1 GB", "text", LocalModel.Status.Unavailable),
        LocalModel("qwen2.5-1.5b", "Qwen2.5 1.5B Instruct", "1.0 GB", "text", LocalModel.Status.Unavailable),
        LocalModel("gemma-2-2b", "Gemma 2 2B Instruct", "1.6 GB", "text", LocalModel.Status.Unavailable),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Models", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Kaggle Bridge") },
                    leadingIcon = { Icon(Icons.Default.Cloud, null, Modifier.size(18.dp)) })
                FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("On device") },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, null, Modifier.size(18.dp)) })
            }
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            if (tab == 0) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(remote.ifEmpty {
                        listOf(
                            RemoteModel("google/gemini-2.5-flash:web"),
                            RemoteModel("google/gemini-2.5-flash:web:think"),
                            RemoteModel("google/gemini-2.5-pro:web"),
                        )
                    }, key = { it.id }) { m ->
                        val base = m.id.substringBefore(":")
                        val isSel = selected == base || selected == m.id
                        Card(
                            Modifier.fillMaxWidth().clickable {
                                selected = base
                                scope.launch { app.container.settings.setBaseModel(base) }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(m.id, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Remote · ${m.ownedBy}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (isSel) Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text(
                            "On-device GGUF uses llama.cpp (native). Runtime wiring ships next; " +
                                "until then Bridge remains the active backend.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    items(local, key = { it.id }) { m ->
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                val bg = if (m.type == "vision") Color(0xFF9810FA) else Color(0xFF3B82F6)
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
                                    Text(m.type.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text(m.name, fontWeight = FontWeight.Medium)
                                    Text("${m.sizeLabel} · ${m.status}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
