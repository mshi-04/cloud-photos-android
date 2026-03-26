package com.appvoyager.cloudphotos.ui.media.viewmodel

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
        assertEquals(CameraEffect.ShowSnackbar(CameraSnackbarMessage.CameraUnavailable), effect)
    }

    @Test
    fun `retryCamera transitions to Ready`() {
        viewModel.onPermissionGranted()
        viewModel.onCameraError()
        viewModel.retryCamera()
        assertEquals(CameraUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `takePhoto calls writer on success`() = runTest {
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Success(
            MediaUrl.of("content://media/external/images/media/123")
        )

        viewModel.takePhoto(
            captureJpeg = { byteArrayOf(1, 2, 3) },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { mockWriter.write(any()) }
    }

    @Test
    fun `takePhoto transitions to Ready on success`() = runTest {
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Success(
            MediaUrl.of("content://media/external/images/media/123")
        )

        viewModel.takePhoto(
            captureJpeg = { byteArrayOf(1, 2, 3) },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        assertEquals(CameraUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `takePhoto emits OnPhotoCaptured on success`() = runTest {
        // Arrange
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Success(
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
    fun `takePhoto emits ShowSnackbar on SAVE_FAILED`() = runTest {
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)

        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is CameraEffect.ShowSnackbar)
    }

    @Test
    fun `takePhoto emits CaptureFailed message on SAVE_FAILED`() = runTest {
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)

        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertEquals(
            CameraSnackbarMessage.CaptureFailed,
            (effect as CameraEffect.ShowSnackbar).message
        )
    }

    @Test
    fun `takePhoto transitions to Error on STORAGE_FULL`() = runTest {
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Error(SavePhotoResult.ErrorType.STORAGE_FULL)

        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        assertEquals(
            CameraUiState.Error(CameraUiState.ErrorType.STORAGE_FULL),
            viewModel.uiState.value
        )
    }

    @Test
    fun `takePhoto emits ShowStorageFullDialog on STORAGE_FULL`() = runTest {
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Error(SavePhotoResult.ErrorType.STORAGE_FULL)

        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is CameraEffect.ShowStorageFullDialog)
    }

    @Test
    fun `takePhoto does not change state when not in Ready state`() = runTest {
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        assertEquals(CameraUiState.CheckingPermission, viewModel.uiState.value)
    }

    @Test
    fun `takePhoto does not call writer when not in Ready state`() = runTest {
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { mockWriter.write(any()) }
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
