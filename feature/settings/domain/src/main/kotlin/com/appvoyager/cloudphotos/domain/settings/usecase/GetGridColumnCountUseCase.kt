package com.appvoyager.cloudphotos.domain.settings.usecase

import com.appvoyager.cloudphotos.domain.settings.repository.SettingsRepository
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetGridColumnCountUseCase @Inject constructor(private val settingsRepository: SettingsRepository) {

    operator fun invoke(): Flow<GridColumnCount> = settingsRepository.gridColumnCount
}
