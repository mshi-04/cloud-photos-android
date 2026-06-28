package com.appvoyager.cloudphotos.data.media.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataSourceImpl internal constructor(private val dataStore: DataStore<Preferences>) : SettingsDataSource {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context.dataStore)

    override val gridColumnCount: Flow<Int> =
        dataStore.data.map { preferences ->
            val stored = preferences[KEY_GRID_COLUMN_COUNT]
            if (stored == null || stored <= 0) DEFAULT_GRID_COLUMN_COUNT else stored
        }

    override suspend fun setGridColumnCount(count: Int) {
        if (count <= 0) return
        dataStore.edit { preferences ->
            preferences[KEY_GRID_COLUMN_COUNT] = count
        }
    }

    companion object {
        private val KEY_GRID_COLUMN_COUNT = intPreferencesKey("grid_column_count")
        private const val DEFAULT_GRID_COLUMN_COUNT = 3
    }
}
