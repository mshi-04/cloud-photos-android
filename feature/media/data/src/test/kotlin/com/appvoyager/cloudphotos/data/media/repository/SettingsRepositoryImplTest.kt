package com.appvoyager.cloudphotos.data.media.repository

import app.cash.turbine.test
import com.appvoyager.cloudphotos.data.media.datasource.SettingsDataSource
import com.appvoyager.cloudphotos.domain.media.valueobject.GridColumnCount
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsRepositoryImplTest {

    private val settingsDataSource = mockk<SettingsDataSource>()

    @Test
    fun `gridColumnCount emits GridColumnCount when data source emits valid count`() = runTest {
        // Arrange
        val source = MutableSharedFlow<Int>(replay = 1)
        every { settingsDataSource.gridColumnCount } returns source
        val repository = SettingsRepositoryImpl(settingsDataSource)

        // Act
        // Flow: valid source value is converted to domain value object in emit order
        repository.gridColumnCount.test {
            source.emit(4)

            // Assert
            assertEquals(GridColumnCount.of(4), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `gridColumnCount throws when data source emits count below minimum`() = runTest {
        // Arrange
        val source = MutableSharedFlow<Int>(replay = 1)
        every { settingsDataSource.gridColumnCount } returns source
        val repository = SettingsRepositoryImpl(settingsDataSource)

        // Act & Assert
        // Boundary/Error: invalid persisted lower boundary is rejected by GridColumnCount
        repository.gridColumnCount.test {
            source.emit(GridColumnCount.MIN - 1)

            val error = awaitError()
            assertEquals(
                "GridColumnCount must be in 2..6, but was 1.",
                error.message
            )
        }
    }

    @Test
    fun `setGridColumnCount calls data source with raw count value`() = runTest {
        // Arrange
        every { settingsDataSource.gridColumnCount } returns MutableSharedFlow()
        coEvery { settingsDataSource.setGridColumnCount(any()) } just runs
        val repository = SettingsRepositoryImpl(settingsDataSource)

        // Act
        // Interaction: repository passes the value object raw value to persistence
        repository.setGridColumnCount(GridColumnCount.of(6))

        // Assert
        coVerify(exactly = 1) { settingsDataSource.setGridColumnCount(6) }
    }
}
