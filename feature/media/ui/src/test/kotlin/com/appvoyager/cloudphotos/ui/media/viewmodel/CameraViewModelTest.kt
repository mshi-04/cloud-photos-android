package com.appvoyager.cloudphotos.ui.media.viewmodel

import com.appvoyager.cloudphotos.domain.media.model.SavePhotoResult
import com.appvoyager.cloudphotos.domain.media.repository.CapturedPhotoWriter
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import com.appvoyager.cloudphotos.ui.media.effect.CameraEffect
import com.appvoyager.cloudphotos.ui.media.effect.CameraSnackbarMessage
import com.appvoyager.cloudphotos.ui.media.uistate.CameraUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
    private lateinit var fakeWriter: FakeCapturedPhotoWriter
    private lateinit var viewModel: CameraViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeWriter = FakeCapturedPhotoWriter()
        viewModel = CameraViewModel(fakeWriter)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is CheckingPermission`() {
        assertEquals(CameraUiState.CheckingPermission, viewModel.uiState.value)
    }

    @Test
    fun `onPermissionGranted transitions to Ready from CheckingPermission`() {
        viewModel.onPermissionGranted()
        assertEquals(CameraUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `onPermissionGranted transitions to Ready from PermissionRequired`() {
        viewModel.onPermissionDenied()
        viewModel.onPermissionGranted()
        assertEquals(CameraUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `onPermissionDenied transitions to PermissionRequired`() {
        viewModel.onPermissionDenied()
        assertEquals(CameraUiState.PermissionRequired, viewModel.uiState.value)
    }

    @Test
    fun `onCameraError transitions to Error with CAMERA_UNAVAILABLE`() = runTest {
        viewModel.onCameraError()
        advanceUntilIdle()
        assertEquals(
            CameraUiState.Error(CameraUiState.ErrorType.CAMERA_UNAVAILABLE),
            viewModel.uiState.value
        )
    }

    @Test
    fun `onCameraError emits ShowSnackbar effect`() = runTest {
        viewModel.onCameraError()
        advanceUntilIdle()
        val effect = viewModel.effect.first()
        assertTrue(effect is CameraEffect.ShowSnackbar)
        assertEquals(
            CameraSnackbarMessage.CameraUnavailable,
            (effect as CameraEffect.ShowSnackbar).message
        )
    }

    @Test
    fun `retryCamera transitions to Ready`() {
        viewModel.onPermissionGranted()
        viewModel.onCameraError()
        viewModel.retryCamera()
        assertEquals(CameraUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `takePhoto transitions to Ready on success`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        fakeWriter.result = SavePhotoResult.Success(
            MediaUrl.of("content://media/external/images/media/123")
        )

        // Act
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf(1, 2, 3) },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        // Assert
        assertEquals(1, fakeWriter.callCount)
        assertEquals(CameraUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `takePhoto emits OnPhotoCaptured on success`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        fakeWriter.result = SavePhotoResult.Success(
            MediaUrl.of("content://media/external/images/media/123")
        )

        // Act
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf(1, 2, 3) },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        // Assert
        val effect = viewModel.effect.first()
        assertTrue(effect is CameraEffect.OnPhotoCaptured)
    }

    @Test
    fun `takePhoto transitions to Error on SAVE_FAILED`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        fakeWriter.result = SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)

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
    fun `takePhoto emits ShowSnackbar on SAVE_FAILED`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        fakeWriter.result = SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)

        // Act
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        // Assert
        val effect = viewModel.effect.first()
        assertTrue(effect is CameraEffect.ShowSnackbar)
        assertEquals(
            CameraSnackbarMessage.CaptureFailed,
            (effect as CameraEffect.ShowSnackbar).message
        )
    }

    @Test
    fun `takePhoto emits ShowStorageFullDialog on STORAGE_FULL`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        fakeWriter.result = SavePhotoResult.Error(SavePhotoResult.ErrorType.STORAGE_FULL)

        // Act
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
        val effect = viewModel.effect.first()
        assertTrue(effect is CameraEffect.ShowStorageFullDialog)
    }

    @Test
    fun `takePhoto does nothing when not in Ready state`() = runTest {
        // Act
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        // Assert
        assertEquals(CameraUiState.CheckingPermission, viewModel.uiState.value)
        assertEquals(0, fakeWriter.callCount)
    }

    @Test
    fun `takePhoto handles captureJpeg exception as SAVE_FAILED`() = runTest {
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
    fun `takePhoto calls onCaptureAnimTrigger`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        var triggered = false
        fakeWriter.result = SavePhotoResult.Success(
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

    private class FakeCapturedPhotoWriter : CapturedPhotoWriter {
        var result: SavePhotoResult = SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)
        var callCount = 0

        override suspend fun write(jpegData: ByteArray): SavePhotoResult {
            callCount++
            return result
        }
    }
}
