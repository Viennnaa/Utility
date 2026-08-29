package com.viennnaa.utilities.core.storage

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.viennnaa.utilities.core.settings.AppSettings
import com.viennnaa.utilities.core.settings.themeModeOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * App-wide settings: the theme, and which mini apps are pinned.
 *
 * Unlike [MiniAppPreferences], these are exposed as a [Flow] rather than read
 * once. The theme wraps every screen, so a change has to reach the UI the moment
 * it is made rather than on the next launch.
 *
 * Keys are prefixed so they cannot collide with a mini app's own namespace in
 * the shared store.
 */
class AppPreferences(private val context: Context) {

    val settings: Flow<AppSettings> = context.utilitiesDataStore.data.map { stored ->
        AppSettings(
            themeMode = themeModeOf(stored[THEME_MODE]),
            dynamicColor = stored[DYNAMIC_COLOR] ?: true,
            favouriteIds = decodeList(stored[FAVOURITES] ?: ""),
        )
    }

    suspend fun setThemeMode(name: String) {
        context.utilitiesDataStore.edit { it[THEME_MODE] = name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.utilitiesDataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    /** Stored through [encodeList], so the pinned order survives exactly. */
    suspend fun setFavourites(ids: List<String>) {
        context.utilitiesDataStore.edit { it[FAVOURITES] = encodeList(ids) }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("app.themeMode")
        val DYNAMIC_COLOR = booleanPreferencesKey("app.dynamicColor")
        val FAVOURITES = stringPreferencesKey("app.favourites")
    }
}

@Composable
fun rememberAppPreferences(): AppPreferences {
    val context = LocalContext.current
    return remember(context) { AppPreferences(context) }
}
