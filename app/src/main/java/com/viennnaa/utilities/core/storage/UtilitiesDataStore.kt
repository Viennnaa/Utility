package com.viennnaa.utilities.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * The one DataStore the app has.
 *
 * There must be exactly one delegate per file for the whole process — declaring
 * a second one over the same name throws at runtime the first time both are
 * touched — so it lives here rather than being redeclared next to each user of
 * it. Mini app settings and app-wide settings share the file and stay apart by
 * key prefix, which also means adding a mini app costs no extra file handle.
 */
internal val Context.utilitiesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mini_app_settings",
)
