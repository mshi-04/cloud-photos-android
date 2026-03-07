package com.appvoyager.cloudphotos.domain.settings.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val gridColumnCount: Flow<Int>
    suspend fun setGridColumnCount(count: Int)
}
