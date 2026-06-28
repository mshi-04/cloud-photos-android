package com.appvoyager.cloudphotos.domain.media.usecase

import app.cash.turbine.test
import com.appvoyager.cloudphotos.domain.media.repository.SettingsRepository
import com.appvoyager.cloudphotos.domain.media.valueobject.GridColumnCount
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetGridColumnCountUseCaseTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var getGridColumnCountUseCase: GetGridColumnCountUseCase

    @BeforeEach
    fun setUp() {
        settingsRepository = mockk()
        getGridColumnCountUseCase = GetGridColumnCountUseCase(settingsRepository)
    }

    @Test
    fun `invoke returns flow of grid column count from repository`() = runTest {
        // Arrange
        val expectedCount = GridColumnCount.of(3)
        every { settingsRepository.gridColumnCount } returns MutableStateFlow(expectedCount)

        // Act & Assert
        // Flow: repository grid column count is emitted without requiring completion
        getGridColumnCountUseCase().test {
            assertEquals(expectedCount, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }
}
