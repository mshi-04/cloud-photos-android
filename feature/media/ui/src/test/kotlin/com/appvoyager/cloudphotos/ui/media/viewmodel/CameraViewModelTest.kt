package com.appvoyager.cloudphotos.ui.media.viewmodel

import app.cash.turbine.test
import com.appvoyager.cloudphotos.domain.media.model.SavePhotoResult
import com.appvoyager.cloudphotos.domain.media.repository.CapturedPhotoWriter
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import com.appvoyager.cloudphotos.ui.media.effect.CameraEffect
import com.appvoyager.cloudphotos.ui.media.effect.CameraSnackbarMessage
import com.appvoyager.cloudphotos.ui.media.uistate.CameraUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockWriter = mockk<CapturedPhotoWriter>()
    private lateinit var viewModel: CameraViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CameraViewModel(mockWriter)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState returns CheckingPermission when viewModel is created`() {
        // Arrange
        // State: freshly created viewModel starts by checking permission

        // Act
        val state = viewModel.uiState.value

        // Assert
        assertEquals(CameraUiState.CheckingPermission, state)
    }

    @Test
    fun `onPermissionGranted sets uiState to Ready when in CheckingPermission state`() {
        // Arrange
        // State: permission check is the initial state

        // Act
        // State: permission grant transitions to ready
        viewModel.onPermissionGranted()

        // Assert
        assertEquals(CameraUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `onPermissionGranted sets uiState to Ready when in PermissionRequired state`() {
        // Arrange
        viewModel.onPermissionDenied()

        // Act
        // State: permission grant recovers from permission required
        viewModel.onPermissionGranted()

        // Assert
        assertEquals(CameraUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `onPermissionDenied sets uiState to PermissionRequired when called`() {
        // Arrange
        // State: permission can be denied from the initial checking state

        // Act
        // State: permission denial moves to permission required
        viewModel.onPermissionDenied()

        // Assert
        assertEquals(CameraUiState.PermissionRequired, viewModel.uiState.value)
    }

    @Test
    fun `onCameraError sets uiState to Error when called`() = runTest {
        // Arrange
        // State: camera error is accepted from the current screen state

        // Act
        // State: camera error transitions to camera unavailable error
        viewModel.onCameraError()
        advanceUntilIdle()

        // Assert
        assertEquals(
            CameraUiState.Error(CameraUiState.ErrorType.CAMERA_UNAVAILABLE),
            viewModel.uiState.value
        )
    }

    @Test
    fun `onCameraError emits ShowSnackbar when called`() = runTest {
        // Arrange
        // State: effect stream is observed before triggering camera error

        // Act & Assert
        // Flow: camera error emits snackbar effect
        viewModel.effect.test {
            viewModel.onCameraError()
            advanceUntilIdle()

            assertEquals(CameraEffect.ShowSnackbar(CameraSnackbarMessage.CameraUnavailable), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `retryCamera sets uiState to Ready when called`() {
        // Arrange
        viewModel.onPermissionGranted()
        viewModel.onCameraError()

        // Act
        // State: retry recovers camera error to ready
        viewModel.retryCamera()

        // Assert
        assertEquals(CameraUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `takePhoto calls writer when captureJpeg succeeds`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Success(
            MediaUrl.of("content://media/external/images/media/123")
        )

        // Act
        // Interaction: successful capture delegates bytes to the writer
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf(1, 2, 3) },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { mockWriter.write(any()) }
    }

    @Test
    fun `takePhoto sets uiState to Ready when captureJpeg succeeds`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Success(
            MediaUrl.of("content://media/external/images/media/123")
        )

        // Act
        // State: successful capture returns to ready after saving
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf(1, 2, 3) },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        // Assert
        assertEquals(CameraUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `takePhoto emits OnPhotoCaptured when captureJpeg succeeds`() = runTest {
        // Arrange
        // Flow: successful capture emits photo captured effect
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Success(
            MediaUrl.of("content://media/external/images/media/123")
        )

        // Act & Assert
        viewModel.effect.test {
            viewModel.takePhoto(
                captureJpeg = { byteArrayOf(1, 2, 3) },
                onCaptureAnimTrigger = {}
            )
            advanceUntilIdle()

            assertEquals(
                CameraEffect.OnPhotoCaptured(MediaUrl.of("content://media/external/images/media/123")),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `takePhoto sets uiState to Error when photo save fails`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)

        // Act
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        // Assert
        assertEquals(
            CameraUiState.Error(CameraUiState.ErrorType.CAPTURE_FAILED),
            viewModel.uiState.value
        )
    }

    @Test
    fun `takePhoto emits ShowSnackbar when photo save fails`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)

        // Act & Assert
        // Flow: save failure emits snackbar effect
        viewModel.effect.test {
            viewModel.takePhoto(
                captureJpeg = { byteArrayOf() },
                onCaptureAnimTrigger = {}
            )
            advanceUntilIdle()

            assertTrue(awaitItem() is CameraEffect.ShowSnackbar)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `takePhoto emits ShowSnackbar with CaptureFailed when photo save fails`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)

        // Act & Assert
        // Flow: save failure emits capture failed snackbar
        viewModel.effect.test {
            viewModel.takePhoto(
                captureJpeg = { byteArrayOf() },
                onCaptureAnimTrigger = {}
            )
            advanceUntilIdle()

            assertEquals(CameraEffect.ShowSnackbar(CameraSnackbarMessage.CaptureFailed), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `takePhoto sets uiState to Error when storage is full`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Error(SavePhotoResult.ErrorType.STORAGE_FULL)

        // Act
        // State: storage full maps to storage full error state
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        // Assert
        assertEquals(
            CameraUiState.Error(CameraUiState.ErrorType.STORAGE_FULL),
            viewModel.uiState.value
        )
    }

    @Test
    fun `takePhoto emits ShowStorageFullDialog when storage is full`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Error(SavePhotoResult.ErrorType.STORAGE_FULL)

        // Act & Assert
        // Flow: storage full emits storage dialog effect
        viewModel.effect.test {
            viewModel.takePhoto(
                captureJpeg = { byteArrayOf() },
                onCaptureAnimTrigger = {}
            )
            advanceUntilIdle()

            assertTrue(awaitItem() is CameraEffect.ShowStorageFullDialog)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `takePhoto ignores uiState when not in Ready state`() = runTest {
        // Arrange
        // State: initial checking state is not ready for capture

        // Act
        // State: non-ready capture request leaves state unchanged
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        // Assert
        assertEquals(CameraUiState.CheckingPermission, viewModel.uiState.value)
    }

    @Test
    fun `takePhoto ignores writer when not in Ready state`() = runTest {
        // Arrange
        // State: initial checking state blocks capture dependencies

        // Act
        // Interaction: non-ready capture request never calls writer
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { mockWriter.write(any()) }
    }

    @Test
    fun `takePhoto sets uiState to Error when captureJpeg throws`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()

        // Act
        viewModel.takePhoto(
            captureJpeg = { throw RuntimeException("Camera disconnected") },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        // Assert
        assertEquals(
            CameraUiState.Error(CameraUiState.ErrorType.CAPTURE_FAILED),
            viewModel.uiState.value
        )
    }

    @Test
    fun `takePhoto calls onCaptureAnimTrigger when captureJpeg succeeds`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        var triggered = false
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Success(
            MediaUrl.of("content://media/test")
        )

        // Act
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = { triggered = true }
        )
        advanceUntilIdle()

        // Assert
        assertTrue(triggered)
    }
}
