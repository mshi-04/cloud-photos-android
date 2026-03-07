package com.appvoyager.cloudphotos.domain.settings.usecase

import com.appvoyager.cloudphotos.domain.settings.repository.SettingsRepository
import javax.inject.Inject

class SetGridColumnCountUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(count: Int) {
        settingsRepository.setGridColumnCount(count)
    }
}
