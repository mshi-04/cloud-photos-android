package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.repository.SettingsRepository
import com.appvoyager.cloudphotos.domain.media.valueobject.GridColumnCount
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetGridColumnCountUseCase @Inject constructor(private val settingsRepository: SettingsRepository) {

    operator fun invoke(): Flow<GridColumnCount> = settingsRepository.gridColumnCount
}
