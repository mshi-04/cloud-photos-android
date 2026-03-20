package com.appvoyager.cloudphotos.domain.settings.usecase

import com.appvoyager.cloudphotos.domain.settings.repository.SettingsRepository
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SetGridColumnCountUseCaseTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var setGridColumnCountUseCase: SetGridColumnCountUseCase

    @BeforeEach
    fun setUp() {
        settingsRepository = mockk()
        setGridColumnCountUseCase = SetGridColumnCountUseCase(settingsRepository)
    }

    @Test
    fun `invoke calls setGridColumnCount on repository with correct value`() = runTest {
        // Arrange
        val countToSet = GridColumnCount.of(4)
        coEvery { settingsRepository.setGridColumnCount(any()) } returns Unit

        // Act
        setGridColumnCountUseCase(countToSet)

        // Assert
        coVerify(exactly = 1) { settingsRepository.setGridColumnCount(countToSet) }
    }
}
