package com.tasp1.pocketpal.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tasp1.pocketpal.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("pocketpal_settings")

class SettingsRepository(private val context: Context) {
    private val KEY_URL = stringPreferencesKey("bridge_url")
    private val KEY_KEY = stringPreferencesKey("bridge_key")
    private val KEY_MODEL = stringPreferencesKey("base_model")
    private val KEY_THEME = stringPreferencesKey("theme") // system|light|dark
    private val KEY_HAPTICS = booleanPreferencesKey("haptics")

    val settings: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            serverUrl = p[KEY_URL] ?: BuildConfig.BRIDGE_URL,
            apiKey = p[KEY_KEY] ?: BuildConfig.BRIDGE_KEY,
            baseModel = p[KEY_MODEL] ?: "google/gemini-2.5-flash",
            theme = p[KEY_THEME] ?: "system",
            haptics = p[KEY_HAPTICS] ?: true,
        )
    }

    suspend fun setServer(url: String, key: String) {
        context.dataStore.edit {
            it[KEY_URL] = url.trimEnd('/')
            it[KEY_KEY] = key
        }
    }

    suspend fun setBaseModel(id: String) {
        context.dataStore.edit { it[KEY_MODEL] = id }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun setHaptics(v: Boolean) {
        context.dataStore.edit { it[KEY_HAPTICS] = v }
    }
}

data class UserSettings(
    val serverUrl: String,
    val apiKey: String,
    val baseModel: String,
    val theme: String,
    val haptics: Boolean,
)
