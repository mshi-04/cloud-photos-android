package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.repository.SettingsRepository
import com.appvoyager.cloudphotos.domain.media.valueobject.GridColumnCount
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
    fun `invoke calls setGridColumnCount with given GridColumnCount when count is provided`() = runTest {
        // Arrange
        val countToSet = GridColumnCount.of(4)
        coEvery { settingsRepository.setGridColumnCount(any()) } returns Unit

        // Act
        setGridColumnCountUseCase(countToSet)

        // Assert
        coVerify(exactly = 1) { settingsRepository.setGridColumnCount(countToSet) }
    }
}
