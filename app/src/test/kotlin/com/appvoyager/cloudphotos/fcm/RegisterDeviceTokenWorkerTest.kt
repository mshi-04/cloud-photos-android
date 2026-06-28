package com.appvoyager.cloudphotos.fcm

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.appvoyager.cloudphotos.data.fcm.DeviceToken
import com.appvoyager.cloudphotos.data.fcm.DeviceTokenDataSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterDeviceTokenWorkerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val deviceTokenDataSource = mockk<DeviceTokenDataSource>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `doWork returns success when token registration succeeds`() = runTest {
        // Arrange
        val tokenSlot = slot<DeviceToken>()
        val worker = createWorker("  fcm-token  ")
        coEvery { deviceTokenDataSource.register(capture(tokenSlot)) } just runs

        // Act
        // Normal: valid token is trimmed and registered successfully
        val result = worker.doWork()

        // Assert
        assertEquals(
            RegistrationResultSnapshot(
                result = ListenableWorker.Result.success(),
                registeredToken = "fcm-token"
            ),
            RegistrationResultSnapshot(result, tokenSlot.captured.value)
        )
    }

    @Test
    fun `doWork returns failure when token input is missing`() = runTest {
        // Arrange
        val worker = createWorker(null)

        // Act
        // Boundary/Error: missing token is non retryable and skips registration
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork returns failure when token input is blank`() = runTest {
        // Arrange
        val worker = createWorker(" \n\t ")

        // Act
        // Boundary/Error: blank token is rejected by DeviceToken validation
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork returns retry when registration throws temporary exception`() = runTest {
        // Arrange
        val worker = createWorker("fcm-token")
        coEvery { deviceTokenDataSource.register(any()) } throws RuntimeException("network timeout")

        // Act
        // Error: unknown provider failure is retryable
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `doWork rethrows CancellationException when registration is cancelled`() = runTest {
        // Arrange
        val worker = createWorker("fcm-token")
        coEvery { deviceTokenDataSource.register(any()) } throws CancellationException()

        // Act & Assert
        // Coroutine: cancellation is propagated instead of converted to retry
        assertThrows<CancellationException> {
            worker.doWork()
        }
    }

    private fun createWorker(token: String?): RegisterDeviceTokenWorker {
        every { workerParams.inputData } returns token.toInputData()
        return RegisterDeviceTokenWorker(context, workerParams, deviceTokenDataSource)
    }

    private fun String?.toInputData(): Data = if (this == null) {
        Data.EMPTY
    } else {
        workDataOf(RegisterDeviceTokenWorker.KEY_TOKEN to this)
    }

    private data class RegistrationResultSnapshot(val result: ListenableWorker.Result, val registeredToken: String)
}
