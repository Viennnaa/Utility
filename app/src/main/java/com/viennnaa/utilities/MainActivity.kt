package com.viennnaa.utilities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.viennnaa.utilities.core.shortcuts.publishMiniAppShortcuts
import com.viennnaa.utilities.miniapp.MiniAppCatalog
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Refreshed on every launch so the shortcuts follow the catalog rather
        // than whatever it looked like when the app was installed.
        publishMiniAppShortcuts(this, MiniAppCatalog)
        setContent {
            UtilitiesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    UtilitiesApp()
                }
            }
        }
    }
}
