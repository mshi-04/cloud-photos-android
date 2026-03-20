package com.appvoyager.cloudphotos.domain.settings.usecase

import com.appvoyager.cloudphotos.domain.settings.repository.SettingsRepository
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount
import javax.inject.Inject

class SetGridColumnCountUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {

    suspend operator fun invoke(count: GridColumnCount) =
        settingsRepository.setGridColumnCount(count)

}
