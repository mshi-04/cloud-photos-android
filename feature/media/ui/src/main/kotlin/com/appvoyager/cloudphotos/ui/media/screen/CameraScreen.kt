package com.appvoyager.cloudphotos.ui.media.screen

import android.Manifest
import android.content.Intent
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.appvoyager.cloudphotos.core.ui.R
import com.appvoyager.cloudphotos.ui.media.camera.CameraPreviewManager
import com.appvoyager.cloudphotos.ui.media.effect.CameraEffect
import com.appvoyager.cloudphotos.ui.media.effect.CameraSnackbarMessage
import com.appvoyager.cloudphotos.ui.media.uistate.CameraUiState
import com.appvoyager.cloudphotos.ui.media.viewmodel.CameraViewModel
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
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
                        lastThumbnailUri = effect.thumbnailUri
                    }
                }
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
        onPauseOrDispose {
            cameraPreviewManager.stopCamera()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            when (val state = uiState) {
                is CameraUiState.PermissionRequired -> {
                    PermissionRequiredContent()
                }

                is CameraUiState.Ready, is CameraUiState.Capturing -> {
                    LaunchedEffect(lensFacing) {
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

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = flashAlpha.value))
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
                        val takePhotoLabel = stringResource(R.string.camera_cd_take_photo)
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(if (isCapturing) Color.LightGray else Color.White)
                                .border(4.dp, Color.LightGray, CircleShape)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = takePhotoLabel
                                }
                                .clickable(enabled = !isCapturing) {
                                    cameraPreviewManager.createCaptureHandle()?.let { handle ->
                                        viewModel.takePhoto(
                                            handle = handle,
                                            onCaptureAnimTrigger = {
                                                coroutineScope.launch {
                                                    flashAlpha.animateTo(0.6f, tween(50))
                                                    flashAlpha.animateTo(0f, tween(150))
                                                }
                                            }
                                        )
                                    }
                                }
                        )

                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
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
                        onRetry = { viewModel.retryCamera() }
                    )
                }
            }
        }
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.retryCamera()
            },
            title = { Text(stringResource(R.string.camera_error_storage_full_title)) },
            text = { Text(stringResource(R.string.camera_error_storage_full_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.retryCamera()
                }) {
                    Text(stringResource(R.string.settings_confirm))
                }
            }
        )
    }
}

@Composable
private fun PermissionRequiredContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
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
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(stringResource(R.string.permission_open_settings))
        }
    }
}

@Composable
private fun ErrorContent(
    errorType: CameraUiState.ErrorType,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
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
