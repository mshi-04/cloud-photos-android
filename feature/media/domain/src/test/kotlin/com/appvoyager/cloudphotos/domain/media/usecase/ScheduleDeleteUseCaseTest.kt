package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.repository.DeleteScheduler
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ScheduleDeleteUseCaseTest {

    private val deleteScheduler: DeleteScheduler = mockk()
    private val useCase = ScheduleDeleteUseCase(deleteScheduler)

    @Test
    fun `invoke calls scheduleDelete when invoked`() = runTest {
        // Arrange
        every { deleteScheduler.scheduleDelete() } just runs

        // Act
        useCase()

        // Assert
        verify { deleteScheduler.scheduleDelete() }
    }
}
