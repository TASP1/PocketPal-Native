package com.tasp1.pocketpal.ui.navigation

/** Mirrors src/utils/navigationConstants.ts ROUTES */
object Routes {
    const val CHAT = "chat"
    const val MODELS = "models"
    const val PALS = "pals"
    const val BENCHMARK = "benchmark"
    const val SETTINGS = "settings"
    const val APP_INFO = "app_info"
}

data class DrawerItem(
    val route: String,
    val label: String,
)

val MainDrawerItems = listOf(
    DrawerItem(Routes.CHAT, "Chat"),
    DrawerItem(Routes.MODELS, "Models"),
    DrawerItem(Routes.PALS, "Pals (experimental)"),
    DrawerItem(Routes.BENCHMARK, "Benchmark"),
    DrawerItem(Routes.SETTINGS, "Settings"),
    DrawerItem(Routes.APP_INFO, "App Info"),
)
