package com.appvoyager.cloudphotos.domain.media.repository

import com.appvoyager.cloudphotos.domain.media.valueobject.GridColumnCount
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val gridColumnCount: Flow<GridColumnCount>
    suspend fun setGridColumnCount(count: GridColumnCount)
}
