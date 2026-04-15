package com.cleanspace.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferencesKeys {
    val LARGE_FILE_THRESHOLD = longPreferencesKey("large_file_threshold")
    val THEME_MODE = intPreferencesKey("theme_mode")
    val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    val LAST_CLEANUP_DATE = longPreferencesKey("last_cleanup_date")
    val AUTO_CLEANUP_ENABLED = booleanPreferencesKey("auto_cleanup_enabled")
    val SCAN_DEPTH = intPreferencesKey("scan_depth")
    val EXCLUDE_SYSTEM_FILES = booleanPreferencesKey("exclude_system_files")
}

class SettingsDataStore(private val context: Context) {

    val largeFileThreshold: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LARGE_FILE_THRESHOLD] ?: (100L * 1024L * 1024L) // 100MB default
    }

    val themeMode: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.THEME_MODE] ?: 0
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.IS_FIRST_LAUNCH] ?: true
    }

    suspend fun setLargeFileThreshold(value: Long) {
        context.dataStore.edit { it[PreferencesKeys.LARGE_FILE_THRESHOLD] = value }
    }

    suspend fun setThemeMode(value: Int) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = value }
    }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { it[PreferencesKeys.IS_FIRST_LAUNCH] = false }
    }
}

