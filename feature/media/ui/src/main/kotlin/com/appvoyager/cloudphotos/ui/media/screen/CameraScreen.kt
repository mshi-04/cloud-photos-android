package com.appvoyager.cloudphotos.ui.media.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.appvoyager.cloudphotos.core.ui.R
import com.appvoyager.cloudphotos.ui.media.camera.CameraPreviewManager
import com.appvoyager.cloudphotos.ui.media.effect.CameraEffect
import com.appvoyager.cloudphotos.ui.media.effect.CameraSnackbarMessage
import com.appvoyager.cloudphotos.ui.media.uistate.CameraUiState
import com.appvoyager.cloudphotos.ui.media.viewmodel.CameraViewModel
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val latestContext = rememberUpdatedState(context)
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val cameraPreviewManager = remember {
        CameraPreviewManager(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            onError = { viewModel.onCameraError() }
        )
    }

    var showStorageDialog by remember { mutableStateOf(false) }
    var lastThumbnailUri by remember { mutableStateOf<Uri?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    val flashAlpha = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    var resumeKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is CameraEffect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(effect.message.toMessage(latestContext.value))
                    }

                    is CameraEffect.ShowStorageFullDialog -> {
                        showStorageDialog = true
                    }

                    is CameraEffect.OnPhotoCaptured -> {
                        lastThumbnailUri = Uri.parse(effect.mediaUrl.value)
                    }
                }
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        resumeKey++
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            viewModel.onPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        onPauseOrDispose {
            cameraPreviewManager.stopCamera()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        CameraContent(
            uiState = uiState,
            isCameraBound = cameraPreviewManager.isCameraBound,
            flashAlpha = flashAlpha.value,
            lastThumbnailUri = lastThumbnailUri,
            showStorageDialog = showStorageDialog,
            onNavigateBack = onNavigateBack,
            onTakePhoto = {
                viewModel.takePhoto(
                    captureJpeg = { cameraPreviewManager.captureToJpeg() },
                    onCaptureAnimTrigger = {
                        coroutineScope.launch {
                            flashAlpha.animateTo(0.6f, tween(50))
                            flashAlpha.animateTo(0f, tween(150))
                        }
                    }
                )
            },
            onSwitchLens = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            },
            onRetry = { viewModel.retryCamera() },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onDismissStorageDialog = {
                showStorageDialog = false
                viewModel.retryCamera()
            },
            modifier = Modifier.padding(innerPadding),
            cameraPreview = {
                LaunchedEffect(lensFacing, resumeKey) {
                    cameraPreviewManager.startCamera(lensFacing)
                }

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                cameraPreviewManager.handlePinchToZoom(zoom)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    cameraPreviewManager.handleTapToFocus(offset)
                                }
                            )
                        }
                )
            }
        )
    }
}

@Composable
private fun CameraContent(
    uiState: CameraUiState,
    isCameraBound: Boolean,
    flashAlpha: Float,
    lastThumbnailUri: Uri?,
    showStorageDialog: Boolean,
    onNavigateBack: () -> Unit,
    onTakePhoto: () -> Unit,
    onSwitchLens: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissStorageDialog: () -> Unit,
    modifier: Modifier = Modifier,
    cameraPreview: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = uiState) {
            is CameraUiState.CheckingPermission -> {
            }

            is CameraUiState.PermissionRequired -> {
                PermissionRequiredContent(onOpenSettings = onOpenSettings)
            }

            is CameraUiState.Ready, is CameraUiState.Capturing -> {
                cameraPreview()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = flashAlpha))
                )

                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.camera_cd_close),
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp, start = 32.dp, end = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (lastThumbnailUri != null) {
                        AsyncImage(
                            model = lastThumbnailUri,
                            contentDescription = stringResource(R.string.camera_cd_last_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    } else {
                        Box(modifier = Modifier.size(48.dp))
                    }

                    val isCapturing = state is CameraUiState.Capturing
                    val isCameraReady = isCameraBound && !isCapturing
                    val takePhotoLabel = stringResource(R.string.camera_cd_take_photo)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isCameraReady) Color.White else Color.LightGray)
                            .border(4.dp, Color.LightGray, CircleShape)
                            .semantics {
                                role = Role.Button
                                contentDescription = takePhotoLabel
                            }
                            .clickable(enabled = isCameraReady, onClick = onTakePhoto)
                    )

                    IconButton(
                        onClick = onSwitchLens,
                        enabled = !isCapturing,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = stringResource(R.string.camera_cd_switch_camera),
                            tint = Color.White
                        )
                    }
                }
            }

            is CameraUiState.Error -> {
                ErrorContent(
                    errorType = state.type,
                    onRetry = onRetry
                )
            }
        }
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = onDismissStorageDialog,
            title = { Text(stringResource(R.string.camera_error_storage_full_title)) },
            text = { Text(stringResource(R.string.camera_error_storage_full_message)) },
            confirmButton = {
                TextButton(onClick = onDismissStorageDialog) {
                    Text(stringResource(R.string.settings_confirm))
                }
            }
        )
    }
}

@Composable
private fun PermissionRequiredContent(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.camera_permission_required),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(stringResource(R.string.permission_open_settings))
        }
    }
}

@Composable
private fun ErrorContent(errorType: CameraUiState.ErrorType, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val message = when (errorType) {
        CameraUiState.ErrorType.CAMERA_UNAVAILABLE -> stringResource(R.string.camera_error_camera_unavailable)
        CameraUiState.ErrorType.CAPTURE_FAILED -> stringResource(R.string.camera_error_capture_failed)
        CameraUiState.ErrorType.STORAGE_FULL -> stringResource(R.string.camera_error_storage_full_title)
    }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        TextButton(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(stringResource(R.string.media_retry))
        }
    }
}

private fun CameraSnackbarMessage.toMessage(context: android.content.Context): String = when (this) {
    CameraSnackbarMessage.CameraUnavailable -> context.getString(R.string.camera_error_camera_unavailable)
    CameraSnackbarMessage.CaptureFailed -> context.getString(R.string.camera_error_capture_failed)
}

@Preview(
    name = "Japanese Light",
    locale = "ja",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = 360,
    heightDp = 780
)
@Preview(
    name = "English Dark",
    locale = "en",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 360,
    heightDp = 780
)
@Target(AnnotationTarget.FUNCTION)
private annotation class CameraStatePreviews

@CameraStatePreviews
@Composable
private fun CameraCheckingPermissionPreview() {
    CameraContentPreview(uiState = CameraUiState.CheckingPermission, isCameraBound = false)
}

@CameraStatePreviews
@Composable
private fun CameraPermissionRequiredPreview() {
    CameraContentPreview(uiState = CameraUiState.PermissionRequired, isCameraBound = false)
}

@CameraStatePreviews
@Preview(
    name = "Japanese Dark",
    locale = "ja",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 360,
    heightDp = 780
)
@Preview(
    name = "English Light",
    locale = "en",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = 360,
    heightDp = 780
)
@Composable
private fun CameraReadyPreview() {
    CameraContentPreview(uiState = CameraUiState.Ready)
}

@OptIn(ExperimentalCoilApi::class)
@CameraStatePreviews
@Composable
private fun CameraReadyWithThumbnailPreview() {
    val previewHandler = AsyncImagePreviewHandler {
        ColorImage(color = Color.Cyan.toArgb(), width = 48, height = 48)
    }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        CameraContentPreview(
            uiState = CameraUiState.Ready,
            lastThumbnailUri = "preview://last-photo".toUri()
        )
    }
}

@CameraStatePreviews
@Composable
private fun CameraReadyUnboundPreview() {
    CameraContentPreview(uiState = CameraUiState.Ready, isCameraBound = false)
}

@CameraStatePreviews
@Composable
private fun CameraCapturingPreview() {
    CameraContentPreview(uiState = CameraUiState.Capturing, flashAlpha = 0.6f)
}

@CameraStatePreviews
@Composable
private fun CameraUnavailablePreview() {
    CameraContentPreview(uiState = CameraUiState.Error(CameraUiState.ErrorType.CAMERA_UNAVAILABLE))
}

@CameraStatePreviews
@Composable
private fun CameraCaptureFailedPreview() {
    CameraContentPreview(uiState = CameraUiState.Error(CameraUiState.ErrorType.CAPTURE_FAILED))
}

@CameraStatePreviews
@Composable
private fun CameraStorageFullPreview() {
    CameraContentPreview(uiState = CameraUiState.Error(CameraUiState.ErrorType.STORAGE_FULL))
}

@CameraStatePreviews
@Composable
private fun CameraStorageDialogPreview() {
    CameraContentPreview(
        uiState = CameraUiState.Error(CameraUiState.ErrorType.STORAGE_FULL),
        showStorageDialog = true
    )
}

@Composable
private fun CameraContentPreview(
    uiState: CameraUiState,
    isCameraBound: Boolean = true,
    flashAlpha: Float = 0f,
    lastThumbnailUri: Uri? = null,
    showStorageDialog: Boolean = false
) {
    CloudPhotosTheme(dynamicColor = false) {
        Scaffold { innerPadding ->
            CameraContent(
                uiState = uiState,
                isCameraBound = isCameraBound,
                flashAlpha = flashAlpha,
                lastThumbnailUri = lastThumbnailUri,
                showStorageDialog = showStorageDialog,
                onNavigateBack = {},
                onTakePhoto = {},
                onSwitchLens = {},
                onRetry = {},
                onOpenSettings = {},
                onDismissStorageDialog = {},
                modifier = Modifier.padding(innerPadding),
                cameraPreview = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray)
                    )
                }
            )
        }
    }
}
