package com.appvoyager.cloudphotos.data.media.repository

import com.appvoyager.cloudphotos.data.media.datasource.SettingsDataSource
import com.appvoyager.cloudphotos.domain.media.repository.SettingsRepository
import com.appvoyager.cloudphotos.domain.media.valueobject.GridColumnCount
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl @Inject constructor(private val settingsDataSource: SettingsDataSource) :
    SettingsRepository {

    override val gridColumnCount: Flow<GridColumnCount> =
        settingsDataSource.gridColumnCount.map { GridColumnCount.of(it) }

    override suspend fun setGridColumnCount(count: GridColumnCount) {
        settingsDataSource.setGridColumnCount(count.value)
    }
}
