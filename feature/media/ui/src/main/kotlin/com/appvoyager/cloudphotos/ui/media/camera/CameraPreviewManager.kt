package com.appvoyager.cloudphotos.ui.media.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
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
            camera = null
            preview = null
            imageCapture = null
            isCameraBound = false

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
            camera = null
            preview = null
            imageCapture = null
            isCameraBound = false
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
        val zoomState = camera.cameraInfo.zoomState.value ?: return
        val newZoomRatio = (zoomState.zoomRatio * zoomDelta).coerceIn(
            zoomState.minZoomRatio,
            zoomState.maxZoomRatio
        )
        val future = camera.cameraControl.setZoomRatio(newZoomRatio)
        future.addListener(
            {
                try {
                    future.get()
                } catch (_: java.util.concurrent.CancellationException) {
                    // Camera unbound or switched — expected, not an error
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
                } catch (_: java.util.concurrent.CancellationException) {
                    // Camera unbound or switched — expected, not an error
                } catch (e: Exception) {
                    onError(e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    suspend fun captureToJpeg(): ByteArray {
        val imageCapture = this.imageCapture
            ?: throw IllegalStateException("Camera is not bound")
        return suspendCancellableCoroutine { continuation ->
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        image.close()
                        continuation.resume(bytes)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
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
}
