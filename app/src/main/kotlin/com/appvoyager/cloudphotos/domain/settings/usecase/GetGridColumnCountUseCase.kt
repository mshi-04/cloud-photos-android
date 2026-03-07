package com.appvoyager.cloudphotos.domain.settings.usecase

import com.appvoyager.cloudphotos.domain.settings.repository.SettingsRepository
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGridColumnCountUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<GridColumnCount> = settingsRepository.gridColumnCount
}
