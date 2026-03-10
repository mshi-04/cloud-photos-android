package com.appvoyager.cloudphotos.ui.media.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PlayCircleOutline
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaCreatedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount
import com.appvoyager.cloudphotos.ui.media.component.GridColumnSettingsDialog
import com.appvoyager.cloudphotos.ui.media.effect.MediaEffect
import com.appvoyager.cloudphotos.ui.media.uistate.MediaUiState
import com.appvoyager.cloudphotos.ui.media.viewmodel.MediaViewModel
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val URI_SCHEME_PACKAGE = "package"

@Composable
fun MediaScreen(
    viewModel: MediaViewModel = hiltViewModel(),
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val latestResources = rememberUpdatedState(LocalResources.current)
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionCheckKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is MediaEffect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(latestResources.value.getString(effect.messageResId))
                    }
                }
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        permissionCheckKey++
        onPauseOrDispose {}
    }

    RequestMediaPermissions(
        key = permissionCheckKey,
        onGranted = { viewModel.loadMediaList() },
        onDenied = { viewModel.onPermissionDenied() }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MediaContent(
                loadState = uiState.loadState,
                gridColumnCount = uiState.gridColumnCount,
                onGridSettingsClick = { viewModel.onShowSettingsDialog() },
                onSignOut = onSignOut,
                onRetry = { viewModel.loadMediaList() },
                onRetryPermissions = { permissionCheckKey++ }
            )
        }
    }

    if (uiState.isSettingsDialogVisible) {
        GridColumnSettingsDialog(
            currentColumnCount = uiState.gridColumnCount.value,
            onColumnCountChanged = { viewModel.onGridColumnCountChanged(it) },
            onDismiss = { viewModel.onDismissSettingsDialog() }
        )
    }
}

@Composable
private fun MediaContent(
    loadState: MediaUiState.LoadState,
    gridColumnCount: GridColumnCount,
    onGridSettingsClick: () -> Unit,
    onSignOut: () -> Unit,
    onRetry: () -> Unit,
    onRetryPermissions: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val isButtonVisible by rememberScrollButtonVisibility(gridState)

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (loadState) {
            is MediaUiState.LoadState.Loading -> {}

            is MediaUiState.LoadState.PermissionRequired -> {
                PermissionRequiredContent(onRetryPermissions = onRetryPermissions)
            }

            is MediaUiState.LoadState.Error -> {
                ErrorContent(onRetry = onRetry)
            }

            is MediaUiState.LoadState.Success -> {
                if (loadState.mediaList.isNotEmpty()) {
                    MediaGrid(
                        mediaList = loadState.mediaList,
                        gridColumnCount = gridColumnCount,
                        gridState = gridState,
                        topPadding = statusBarPadding,
                        bottomPadding = navigationBarPadding
                    )

                    AnimatedVisibility(
                        visible = isButtonVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(statusBarPadding)
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }

                    AnimatedVisibility(
                        visible = isButtonVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = statusBarPadding + 8.dp, end = 8.dp)
                    ) {
                        AppIconButton(onClick = onGridSettingsClick)
                    }
                } else {
                    EmptyContent()
                }
            }
        }
    }
}

@Composable
private fun rememberScrollButtonVisibility(gridState: LazyGridState): State<Boolean> {
    val isVisible = remember { mutableStateOf(true) }
    var previousFirstVisibleItemIndex by remember { mutableIntStateOf(0) }
    var previousFirstVisibleItemScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }.collect { (currentIndex, currentOffset) ->
            val isScrollingUp = currentIndex < previousFirstVisibleItemIndex ||
                    (currentIndex == previousFirstVisibleItemIndex && currentOffset < previousFirstVisibleItemScrollOffset)
            val isAtTop = currentIndex == 0 && currentOffset == 0

            previousFirstVisibleItemIndex = currentIndex
            previousFirstVisibleItemScrollOffset = currentOffset

            isVisible.value = isScrollingUp || isAtTop
        }
    }

    return isVisible
}

@Composable
private fun AppIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.media_grid_settings),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun MediaGrid(
    mediaList: List<Media>,
    gridColumnCount: GridColumnCount,
    gridState: LazyGridState,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(gridColumnCount.value),
        contentPadding = PaddingValues(
            start = 2.dp,
            end = 2.dp,
            top = topPadding + 2.dp,
            bottom = bottomPadding + 2.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = mediaList,
            key = { it.id.value }
        ) { media ->
            MediaGridItem(media = media)
        }
    }
}

@Composable
private fun MediaGridItem(media: Media) {
    val mediaTypeLabel = if (media.type == MediaType.VIDEO) {
        stringResource(R.string.media_content_description_video)
    } else {
        stringResource(R.string.media_content_description_image)
    }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dateLabel = remember(media.createdAt.value) {
        dateFormat.format(Date(media.createdAt.value))
    }
    val accessibilityLabel = stringResource(
        R.string.media_content_description_format,
        mediaTypeLabel,
        dateLabel
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.extraSmall)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
    ) {
        AsyncImage(
            model = media.url.value,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (media.type == MediaType.VIDEO) {
            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.media_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionRequiredContent(onRetryPermissions: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.error_permission_required),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = { context.startActivity(createAppSettingsIntent(context)) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(stringResource(R.string.permission_open_settings))
        }
        TextButton(
            onClick = onRetryPermissions,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.media_retry))
        }
    }
}

private fun createAppSettingsIntent(context: android.content.Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts(URI_SCHEME_PACKAGE, context.packageName, null)
    }

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.error_media_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        TextButton(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(stringResource(R.string.media_retry))
        }
    }
}

@Composable
private fun RequestMediaPermissions(
    key: Int,
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    val context = LocalContext.current
    val latestOnGranted = rememberUpdatedState(onGranted)
    val latestOnDenied = rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            latestOnGranted.value()
        } else {
            latestOnDenied.value()
        }
    }

    LaunchedEffect(key) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            latestOnGranted.value()
        } else {
            launcher.launch(permissions)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaContentPreview() {
    CloudPhotosTheme {
        MediaContent(
            loadState = MediaUiState.LoadState.Success(
                mediaList = listOf(
                    Media(
                        id = MediaId.of("1"),
                        url = MediaUrl.of("content://media/external/images/1"),
                        type = MediaType.IMAGE,
                        createdAt = MediaCreatedAt.of(1700000000L)
                    ),
                    Media(
                        id = MediaId.of("2"),
                        url = MediaUrl.of("content://media/external/video/2"),
                        type = MediaType.VIDEO,
                        createdAt = MediaCreatedAt.of(1700000001L)
                    )
                )
            ),
            gridColumnCount = GridColumnCount.of(3),
            onGridSettingsClick = {},
            onSignOut = {},
            onRetry = {},
            onRetryPermissions = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaContentErrorPreview() {
    CloudPhotosTheme {
        MediaContent(
            loadState = MediaUiState.LoadState.Error(),
            gridColumnCount = GridColumnCount.of(3),
            onGridSettingsClick = {},
            onSignOut = {},
            onRetry = {},
            onRetryPermissions = {}
        )
    }
}
