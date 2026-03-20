package com.appvoyager.cloudphotos.data.settings.repository

import com.appvoyager.cloudphotos.data.settings.datasource.SettingsDataSource
import com.appvoyager.cloudphotos.domain.settings.repository.SettingsRepository
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataSource: SettingsDataSource
) : SettingsRepository {

    override val gridColumnCount: Flow<GridColumnCount> =
        settingsDataSource.gridColumnCount.map { GridColumnCount.of(it) }

    override suspend fun setGridColumnCount(count: GridColumnCount) {
        settingsDataSource.setGridColumnCount(count.value)
    }

}
