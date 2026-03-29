package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.repository.SettingsRepository
import com.appvoyager.cloudphotos.domain.media.valueobject.GridColumnCount
import javax.inject.Inject

class SetGridColumnCountUseCase @Inject constructor(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(count: GridColumnCount) = settingsRepository.setGridColumnCount(count)
}
