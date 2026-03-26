package com.appvoyager.cloudphotos.ui.media.camera

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.appvoyager.cloudphotos.domain.media.model.PhotoCaptureHandle
import com.appvoyager.cloudphotos.domain.media.model.SavePhotoResult
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraPreviewManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onError: (Exception) -> Unit
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var preview: Preview? = null

    var isCameraBound by mutableStateOf(false)
        private set

    suspend fun startCamera(lensFacing: Int = CameraSelector.LENS_FACING_BACK) {
        try {
            cameraProvider = getCameraProvider()
            val cameraProvider = this.cameraProvider ?: return

            val previewUseCase = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val imageCaptureUseCase = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                imageCaptureUseCase
            )
            preview = previewUseCase
            imageCapture = imageCaptureUseCase
            isCameraBound = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        camera = null
        imageCapture = null
        preview = null
        isCameraBound = false
    }

    fun handlePinchToZoom(zoomDelta: Float) {
        val camera = this.camera ?: return
        val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
        val newZoomRatio = currentZoomRatio * zoomDelta
        val future = camera.cameraControl.setZoomRatio(newZoomRatio)
        future.addListener(
            {
                try {
                    future.get()
                } catch (e: Exception) {
                    onError(e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun handleTapToFocus(offset: Offset) {
        val camera = this.camera ?: return
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(offset.x, offset.y)
        val action = FocusMeteringAction.Builder(point).build()
        val future = camera.cameraControl.startFocusAndMetering(action)
        future.addListener(
            {
                try {
                    future.get()
                } catch (e: Exception) {
                    onError(e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun createCaptureHandle(): PhotoCaptureHandle? {
        val imageCapture = this.imageCapture ?: return null
        return PhotoCaptureHandle {
            suspendCancellableCoroutine { continuation ->
                val outputOptions = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    createContentValues()
                ).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val savedUri = outputFileResults.savedUri
                            if (savedUri != null) {
                                try {
                                    val pending = ContentValues().apply {
                                        put(MediaStore.Images.Media.IS_PENDING, 0)
                                    }
                                    val rowCount = context.contentResolver.update(savedUri, pending, null, null)
                                    if (rowCount > 0) {
                                        continuation.resume(
                                            SavePhotoResult.Success(MediaUrl.of(savedUri.toString()))
                                        )
                                    } else {
                                        runCatching { context.contentResolver.delete(savedUri, null, null) }
                                            .onFailure { if (it is CancellationException) throw it }
                                        continuation.resume(
                                            SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)
                                        )
                                    }
                                } catch (_: Exception) {
                                    runCatching { context.contentResolver.delete(savedUri, null, null) }
                                        .onFailure { if (it is CancellationException) throw it }
                                    continuation.resume(
                                        SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)
                                    )
                                }
                            } else {
                                continuation.resume(
                                    SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)
                                )
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            val errorType =
                                if (exception.imageCaptureError == ImageCapture.ERROR_FILE_IO && isStorageFull(exception)) {
                                    SavePhotoResult.ErrorType.STORAGE_FULL
                                } else {
                                    SavePhotoResult.ErrorType.SAVE_FAILED
                                }
                            continuation.resume(SavePhotoResult.Error(errorType))
                        }
                    }
                )
            }
        }
    }

    private fun isStorageFull(exception: ImageCaptureException): Boolean {
        var cause: Throwable? = exception
        while (cause != null) {
            val message = cause.message ?: ""
            if (message.contains("ENOSPC") || message.contains("No space left on device")) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    private fun createContentValues(): ContentValues {
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun getCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                {
                    try {
                        continuation.resume(cameraProviderFuture.get())
                    } catch (e: java.util.concurrent.CancellationException) {
                        continuation.cancel(e)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val RELATIVE_PATH = "DCIM/Camera"
    }

}
