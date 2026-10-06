import com.tasp1.pocketpal.PocketPalApp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
package com.tasp1.pocketpal.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ModelTraining
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tasp1.pocketpal.ui.screens.about.AboutScreen
import com.tasp1.pocketpal.ui.screens.benchmark.BenchmarkScreen
import com.tasp1.pocketpal.ui.screens.chat.ChatScreen
import com.tasp1.pocketpal.ui.screens.models.ModelsScreen
import com.tasp1.pocketpal.ui.screens.pals.PalsScreen
import com.tasp1.pocketpal.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

private data class NavEntry(val route: String, val label: String, val icon: ImageVector)

private val primary = listOf(
    NavEntry(Routes.CHAT, "Chats", Icons.AutoMirrored.Outlined.Chat),
    NavEntry(Routes.PALS, "Projects", Icons.Outlined.Folder),
    NavEntry(Routes.MODELS, "Code", Icons.Outlined.Code),
    NavEntry(Routes.BENCHMARK, "Artifacts", Icons.Outlined.Widgets),
)

private val secondary = listOf(
    NavEntry(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
    NavEntry(Routes.APP_INFO, "App Info", Icons.Outlined.Info),
)

@Composable
fun PocketPalApp() {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val app = LocalContext.current.applicationContext as PocketPalApp
    val sessions by app.container.sessions.sessions.collectAsState(initial = emptyList())
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Routes.CHAT

    fun openDrawer() = scope.launch { drawerState.open() }
    fun go(route: String) {
        nav.navigate(route) {
            popUpTo(Routes.CHAT) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                ) {
                    Text(
                        "PocketPal",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 28.sp,
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    )
                    primary.forEach { item ->
                        DrawerRow(
                            label = item.label,
                            icon = item.icon,
                            selected = current == item.route,
                            onClick = { go(item.route) },
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 16.dp))
                    Text(
                        "Recents",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                    if (sessions.isEmpty()) {
                        Text(
                            "No chats yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        )
                    } else {
                        sessions.take(12).forEach { s ->
                            Text(
                                s.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        go(Routes.CHAT)
                                        // load via saved state handle later — emit event
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                maxLines = 1,
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 16.dp))
                    secondary.forEach { item ->
                        DrawerRow(
                            label = item.label,
                            icon = item.icon,
                            selected = current == item.route,
                            onClick = { go(item.route) },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = { go(Routes.CHAT) },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = 20.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null)
                            Text("  New chat", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
    ) {
        NavHost(navController = nav, startDestination = Routes.CHAT) {
            composable(Routes.CHAT) { ChatScreen(onOpenDrawer = { openDrawer() }) }
            composable(Routes.MODELS) { ModelsScreen(onOpenDrawer = { openDrawer() }) }
            composable(Routes.PALS) { PalsScreen(onOpenDrawer = { openDrawer() }) }
            composable(Routes.BENCHMARK) { BenchmarkScreen(onOpenDrawer = { openDrawer() }) }
            composable(Routes.SETTINGS) { SettingsScreen(onOpenDrawer = { openDrawer() }) }
            composable(Routes.APP_INFO) { AboutScreen(onOpenDrawer = { openDrawer() }) }
        }
    }
}

@Composable
private fun DrawerRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
