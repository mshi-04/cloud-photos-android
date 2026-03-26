package com.appvoyager.cloudphotos.ui.media.camera

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.appvoyager.cloudphotos.domain.media.model.PhotoCaptureHandle
import com.appvoyager.cloudphotos.domain.media.model.SavePhotoResult
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
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

    suspend fun startCamera(lensFacing: Int = CameraSelector.LENS_FACING_BACK) {
        try {
            cameraProvider = getCameraProvider()
            val cameraProvider = this.cameraProvider ?: return

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
    }

    fun handlePinchToZoom(zoomDelta: Float) {
        val camera = this.camera ?: return
        val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
        val newZoomRatio = currentZoomRatio * zoomDelta
        camera.cameraControl.setZoomRatio(newZoomRatio)
    }

    fun handleTapToFocus(offset: Offset) {
        val camera = this.camera ?: return
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(offset.x, offset.y)
        val action = FocusMeteringAction.Builder(point).build()
        camera.cameraControl.startFocusAndMetering(action)
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
                                        continuation.resume(
                                            SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)
                                        )
                                    }
                                } catch (_: Exception) {
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
                                if (exception.imageCaptureError == ImageCapture.ERROR_FILE_IO) {
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

    private suspend fun getCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                {
                    try {
                        continuation.resume(cameraProviderFuture.get())
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
