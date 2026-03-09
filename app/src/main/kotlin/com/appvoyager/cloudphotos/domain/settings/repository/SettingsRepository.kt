package com.appvoyager.cloudphotos.domain.settings.repository

import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val gridColumnCount: Flow<GridColumnCount>
    suspend fun setGridColumnCount(count: GridColumnCount)

}
