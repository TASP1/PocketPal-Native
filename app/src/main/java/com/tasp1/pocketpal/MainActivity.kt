package com.tasp1.pocketpal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tasp1.pocketpal.ui.navigation.PocketPalRoot
import com.tasp1.pocketpal.ui.theme.PocketPalTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = LocalContext.current.applicationContext as PocketPalApp
            val themeMode by app.container.settings.settings
                .map { it.theme }
                .collectAsState(initial = "system")
            val systemDark = isSystemInDarkTheme()
            val dark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }
            PocketPalTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize()) {
                    PocketPalRoot()
                }
            }
        }
    }
}
