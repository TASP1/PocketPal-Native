package com.tasp1.pocketpal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tasp1.pocketpal.ui.navigation.PocketPalApp
import com.tasp1.pocketpal.ui.theme.PocketPalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketPalTheme {
                Surface(Modifier.fillMaxSize()) {
                    PocketPalApp()
                }
            }
        }
    }
}
