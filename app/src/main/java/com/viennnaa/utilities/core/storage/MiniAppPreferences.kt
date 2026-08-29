package com.viennnaa.utilities.core.storage

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * The settings one mini app has asked to keep between launches.
 *
 * This is for what the user built up or chose — a list of names, a preferred die,
 * a usual tip — not for results and history, which are meant to be transient.
 *
 * Reads are one-shot rather than a subscription: a mini app loads its settings
 * when it opens and owns them from then on, so there is no second writer to
 * observe.
 */
class MiniAppPreferences(
    private val context: Context,
    private val miniAppId: String,
) {
    private fun name(key: String) = "$miniAppId.$key"

    suspend fun getInt(key: String, default: Int): Int =
        context.utilitiesDataStore.data.first()[intPreferencesKey(name(key))] ?: default

    suspend fun setInt(key: String, value: Int) {
        context.utilitiesDataStore.edit { it[intPreferencesKey(name(key))] = value }
    }

    suspend fun getBoolean(key: String, default: Boolean): Boolean =
        context.utilitiesDataStore.data.first()[booleanPreferencesKey(name(key))] ?: default

    suspend fun setBoolean(key: String, value: Boolean) {
        context.utilitiesDataStore.edit { it[booleanPreferencesKey(name(key))] = value }
    }

    suspend fun getString(key: String, default: String): String =
        context.utilitiesDataStore.data.first()[stringPreferencesKey(name(key))] ?: default

    suspend fun setString(key: String, value: String) {
        context.utilitiesDataStore.edit { it[stringPreferencesKey(name(key))] = value }
    }

    /** Stored through [encodeList], so entry order and contents survive exactly. */
    suspend fun getStringList(key: String): List<String> = decodeList(getString(key, ""))

    suspend fun setStringList(key: String, value: List<String>) =
        setString(key, encodeList(value))
}

/** Remembers the settings handle for one mini app. */
@Composable
fun rememberMiniAppPreferences(miniAppId: String): MiniAppPreferences {
    val context = LocalContext.current
    return remember(miniAppId) { MiniAppPreferences(context, miniAppId) }
}
