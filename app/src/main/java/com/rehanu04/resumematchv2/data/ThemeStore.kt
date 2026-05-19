package com.rehanu04.resumematchv2.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object ThemeStore {
    private val KEY_DARK = booleanPreferencesKey("dark_mode")

    fun isDarkFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[KEY_DARK] ?: false } // default LIGHT

    suspend fun setDark(context: Context, value: Boolean) {
        context.dataStore.edit { it[KEY_DARK] = value }
    }
}
