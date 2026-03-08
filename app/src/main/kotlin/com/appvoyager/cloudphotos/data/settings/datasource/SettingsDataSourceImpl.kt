package com.appvoyager.cloudphotos.data.settings.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataSourceImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SettingsDataSource {

    override val gridColumnCount: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[KEY_GRID_COLUMN_COUNT] ?: DEFAULT_GRID_COLUMN_COUNT
        }

    override suspend fun setGridColumnCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GRID_COLUMN_COUNT] = count
        }
    }

    companion object {
        private val KEY_GRID_COLUMN_COUNT = intPreferencesKey("grid_column_count")
        private const val DEFAULT_GRID_COLUMN_COUNT = 3
    }

}
