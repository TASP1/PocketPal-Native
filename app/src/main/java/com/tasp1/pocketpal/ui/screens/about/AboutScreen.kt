package com.tasp1.pocketpal.ui.screens.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onOpenDrawer: () -> Unit) {
    val ctx = LocalContext.current
    val version = "1.0.0-compose"
    val build = "1"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Info", fontWeight = FontWeight.SemiBold) },
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
            Text("PocketPal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Jetpack Compose port of PocketPal AI · TASP1 Kaggle Bridge",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Text(
                "Version $version ($build)",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .clickable {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("version", "Version $version ($build)"))
                    }
                    .padding(vertical = 4.dp),
            )
            Text(
                "Tap version to copy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("Links", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LinkRow("Upstream PocketPal AI", "https://github.com/a-ghorbani/pocketpal-ai")
            LinkRow("TASP1 RN fork", "https://github.com/TASP1/pocketpal-kaggle")
            LinkRow("This Compose repo", "https://github.com/TASP1/PocketPal-Native")
            LinkRow("Kaggle Bridge", "https://github.com/Adarshstar/kaggle-pocketpal-bridge")

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text(
                "MIT License · UI tokens and routes mirrored from upstream PocketPal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LinkRow(label: String, url: String) {
    val ctx = LocalContext.current
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }
            .padding(vertical = 10.dp),
    )
}
