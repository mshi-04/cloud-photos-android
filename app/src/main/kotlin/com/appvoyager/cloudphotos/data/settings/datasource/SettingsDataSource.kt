package com.appvoyager.cloudphotos.data.settings.datasource

import kotlinx.coroutines.flow.Flow

interface SettingsDataSource {
    
    val gridColumnCount: Flow<Int>
    suspend fun setGridColumnCount(count: Int)
    
}