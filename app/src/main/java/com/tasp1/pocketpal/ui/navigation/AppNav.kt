package com.tasp1.pocketpal.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

private fun iconFor(route: String): ImageVector = when (route) {
    Routes.CHAT -> Icons.Default.Chat
    Routes.MODELS -> Icons.Default.ModelTraining
    Routes.PALS -> Icons.Default.People
    Routes.BENCHMARK -> Icons.Default.Speed
    Routes.SETTINGS -> Icons.Default.Settings
    else -> Icons.Default.Info
}

@Composable
fun PocketPalApp() {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
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
                Spacer(Modifier.height(12.dp))
                Text(
                    "PocketPal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                )
                MainDrawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(iconFor(item.route), contentDescription = null) },
                        label = { Text(item.label) },
                        selected = current == item.route,
                        onClick = { go(item.route) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
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
