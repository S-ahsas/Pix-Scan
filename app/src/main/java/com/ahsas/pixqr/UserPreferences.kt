package com.ahsas.pixqr

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

val QUICK_TILE_KEY = booleanPreferencesKey("quick_tile_enabled")


class UserPreferences(private val context: Context) {
    companion object {
        val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
        val LABELS_KEY = stringPreferencesKey("labels_string")
        val QUICK_TILE_KEY = booleanPreferencesKey("quick_tile_enabled")
    }

    val isGridView: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[IS_GRID_VIEW] ?: false }

    val labels: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val raw = preferences[LABELS_KEY] ?: ""
            if (raw.isBlank()) emptySet()
            else raw.split("|||").filter { it.isNotBlank() }.toSet()
        }

    val quickTileEnabled: Flow<Boolean> = context.dataStore.data   // ← inside here
        .map { preferences -> preferences[QUICK_TILE_KEY] ?: false }

    suspend fun setGridView(isGrid: Boolean) {
        context.dataStore.edit { it[IS_GRID_VIEW] = isGrid }
    }

    suspend fun addLabel(label: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[LABELS_KEY] ?: ""
            val currentList = if (current.isBlank()) emptyList()
            else current.split("|||").filter { it.isNotBlank() }
            if (!currentList.contains(label)) {
                preferences[LABELS_KEY] = (currentList + label).joinToString("|||")
            }
        }
    }

    suspend fun removeLabel(label: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[LABELS_KEY] ?: ""
            val currentList = if (current.isBlank()) emptyList()
            else current.split("|||").filter { it.isNotBlank() }
            preferences[LABELS_KEY] = currentList.filter { it != label }.joinToString("|||")
        }
    }

    suspend fun renameLabel(oldLabel: String, newLabel: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[LABELS_KEY] ?: ""
            val currentList = if (current.isBlank()) emptyList()
            else current.split("|||").filter { it.isNotBlank() }
            preferences[LABELS_KEY] = currentList.map {
                if (it == oldLabel) newLabel else it
            }.joinToString("|||")
        }
    }
    suspend fun setQuickTileEnabled(enabled: Boolean) {    // ← inside here
        context.dataStore.edit { it[QUICK_TILE_KEY] = enabled }
    }
}