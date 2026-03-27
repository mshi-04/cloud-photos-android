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
    fun `initial state is checking permission`() {
        assertEquals(CameraUiState.CheckingPermission, viewModel.uiState.value)
    }

    @Test
    fun `onPermissionGranted transitions to ready from checking permission`() {
        viewModel.onPermissionGranted()
        assertEquals(CameraUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `onPermissionGranted transitions to ready from permission required`() {
        viewModel.onPermissionDenied()
        viewModel.onPermissionGranted()
        assertEquals(CameraUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `onPermissionDenied transitions to permission required`() {
        viewModel.onPermissionDenied()
        assertEquals(CameraUiState.PermissionRequired, viewModel.uiState.value)
    }

    @Test
    fun `onCameraError transitions to error with camera unavailable`() = runTest {
        viewModel.onCameraError()
        advanceUntilIdle()
        assertEquals(
            CameraUiState.Error(CameraUiState.ErrorType.CAMERA_UNAVAILABLE),
            viewModel.uiState.value
        )
    }

    @Test
    fun `onCameraError emits show snackbar effect`() = runTest {
        viewModel.onCameraError()
        advanceUntilIdle()
        val effect = viewModel.effect.first()
        assertEquals(CameraEffect.ShowSnackbar(CameraSnackbarMessage.CameraUnavailable), effect)
    }

    @Test
    fun `retryCamera transitions to ready`() {
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
    fun `takePhoto transitions to ready on success`() = runTest {
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
    fun `takePhoto emits on photo captured on success`() = runTest {
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
        assertEquals(
            CameraEffect.OnPhotoCaptured(MediaUrl.of("content://media/external/images/media/123")),
            effect
        )
    }

    @Test
    fun `takePhoto transitions to error on save failed`() = runTest {
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
    fun `takePhoto emits show snackbar on save failed`() = runTest {
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
    fun `takePhoto emits capture failed message on save failed`() = runTest {
        viewModel.onPermissionGranted()
        coEvery { mockWriter.write(any()) } returns SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)

        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertEquals(CameraEffect.ShowSnackbar(CameraSnackbarMessage.CaptureFailed), effect)
    }

    @Test
    fun `takePhoto transitions to error on storage full`() = runTest {
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
    fun `takePhoto emits show storage full dialog on storage full`() = runTest {
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
    fun `takePhoto does not change state when not in ready state`() = runTest {
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        assertEquals(CameraUiState.CheckingPermission, viewModel.uiState.value)
    }

    @Test
    fun `takePhoto does not call writer when not in ready state`() = runTest {
        viewModel.takePhoto(
            captureJpeg = { byteArrayOf() },
            onCaptureAnimTrigger = {}
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { mockWriter.write(any()) }
    }

    @Test
    fun `takePhoto handles captureJpeg exception as save failed`() = runTest {
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
