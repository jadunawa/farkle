package com.orangezest.farkle.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesSettingsStore(
    private val dataStore: DataStore<Preferences>,
) : SettingsStore {

    override val settings: Flow<Map<String, Any>> = dataStore.data.map { prefs ->
        prefs.asMap().mapKeys { it.key.name }.mapValues { it.value as Any }
    }

    override suspend fun update(key: String, value: Any) {
        dataStore.edit { prefs ->
            when (value) {
                is Int -> prefs[intPreferencesKey(key)] = value
                is Boolean -> prefs[booleanPreferencesKey(key)] = value
                is String -> prefs[stringPreferencesKey(key)] = value
                is Float -> prefs[floatPreferencesKey(key)] = value
                is Long -> prefs[longPreferencesKey(key)] = value
            }
        }
    }
}
