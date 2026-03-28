package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.repository.UploadScheduler
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ScheduleUploadUseCaseTest {

    private val uploadScheduler: UploadScheduler = mockk()
    private val useCase = ScheduleUploadUseCase(uploadScheduler)

    @Test
    fun `invoke calls scheduleUpload when invoked`() = runTest {
        // Arrange
        every { uploadScheduler.scheduleUpload() } just runs

        // Act
        useCase()

        // Assert
        verify { uploadScheduler.scheduleUpload() }
    }
}
