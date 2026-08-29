package com.viennnaa.utilities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viennnaa.utilities.core.settings.AppSettings
import com.viennnaa.utilities.core.shortcuts.publishMiniAppShortcuts
import com.viennnaa.utilities.core.storage.rememberAppPreferences
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
            val preferences = rememberAppPreferences()
            // Settings are read here, above the theme, so a change applies at
            // once rather than on the next launch. The first frame may use the
            // defaults: the store is read off the main thread, and blocking
            // startup to avoid a brief default-theme frame is the worse trade.
            val settings by preferences.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )

            UtilitiesTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    UtilitiesApp(settings = settings, preferences = preferences)
                }
            }
        }
    }
}
