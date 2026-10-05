package com.tasp1.pocketpal.ui.screens.benchmark

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class BenchConfig(val label: String, val pp: Int, val tg: Int, val nr: Int)

private val Configs = listOf(
    BenchConfig("Default", 512, 128, 3),
    BenchConfig("Fast", 128, 32, 3),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(onOpenDrawer: () -> Unit) {
    var selected by remember { mutableStateOf(Configs[0]) }
    var running by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Benchmark", fontWeight = FontWeight.SemiBold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Measure prompt processing (pp) and text generation (tg) on a local model. " +
                    "Full llama.cpp runner lands with on-device inference.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Preset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Configs.forEach { c ->
                    FilterChip(
                        selected = selected == c,
                        onClick = { selected = c },
                        label = { Text(c.label) },
                    )
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("pp (prompt tokens): ${selected.pp}")
                    Text("tg (gen tokens): ${selected.tg}")
                    Text("repetitions: ${selected.nr}")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        running = true
                        // Placeholder until local runner is ported
                        lastResult =
                            "UI ready · local benchmark engine not linked yet.\n" +
                                "Config: ${selected.label} (pp=${selected.pp}, tg=${selected.tg}, nr=${selected.nr})"
                        running = false
                    },
                    enabled = !running,
                ) { Text(if (running) "Running…" else "Run benchmark") }
                OutlinedButton(onClick = { lastResult = null }) { Text("Clear") }
            }
            lastResult?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(it, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
