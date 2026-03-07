package com.appvoyager.cloudphotos.domain.settings.usecase

import com.appvoyager.cloudphotos.domain.settings.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
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
        val expectedCount = 3
        every { settingsRepository.gridColumnCount } returns flowOf(expectedCount)

        // Act
        val result = getGridColumnCountUseCase().toList()

        // Assert
        assertEquals(1, result.size)
        assertEquals(expectedCount, result.first())
    }
}
