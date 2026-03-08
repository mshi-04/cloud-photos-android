package com.appvoyager.cloudphotos.ui.media.screen

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaCreatedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount
import com.appvoyager.cloudphotos.ui.auth.component.LoadingOverlay
import com.appvoyager.cloudphotos.ui.media.component.GridColumnSettingsDialog
import com.appvoyager.cloudphotos.ui.media.effect.MediaEffect
import com.appvoyager.cloudphotos.ui.media.viewmodel.MediaViewModel
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme

@Composable
fun MediaScreen(
    viewModel: MediaViewModel = hiltViewModel(),
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val latestResources = rememberUpdatedState(LocalResources.current)

    LaunchedEffect(Unit) {
        viewModel.loadGridColumnCount()
        viewModel.loadMediaList()
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MediaEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(latestResources.value.getString(effect.messageResId))
                }
            }
        }
    }

    BackHandler(enabled = uiState.isLoading) { }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = innerPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = innerPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    bottom = 0.dp
                )
        ) {
            MediaContent(
                mediaList = uiState.mediaList,
                gridColumnCount = uiState.gridColumnCount,
                isLoading = uiState.isLoading,
                isError = uiState.isError,
                onGridSettingsClick = { viewModel.onShowSettingsDialog() },
                onSignOut = onSignOut,
                onRetry = { viewModel.onRetry() }
            )

            if (uiState.isLoading) {
                LoadingOverlay()
            }
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
    mediaList: List<Media>,
    gridColumnCount: GridColumnCount,
    isLoading: Boolean,
    isError: Boolean,
    onGridSettingsClick: () -> Unit,
    onSignOut: () -> Unit,
    onRetry: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val isButtonVisible by rememberScrollButtonVisibility(gridState)

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            isError && mediaList.isEmpty() -> {
                ErrorContent(onRetry = onRetry)
            }

            !isLoading && mediaList.isEmpty() -> {
                EmptyContent()
            }

            else -> {
                MediaGrid(
                    mediaList = mediaList,
                    gridColumnCount = gridColumnCount,
                    gridState = gridState,
                    topPadding = statusBarPadding,
                    bottomPadding = navigationBarPadding
                )
            }
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
    }
}

@Composable
private fun rememberScrollButtonVisibility(gridState: LazyGridState): androidx.compose.runtime.State<Boolean> {
    var previousFirstVisibleItemIndex by remember { mutableIntStateOf(0) }
    var previousFirstVisibleItemScrollOffset by remember { mutableIntStateOf(0) }

    return remember {
        derivedStateOf {
            val currentIndex = gridState.firstVisibleItemIndex
            val currentOffset = gridState.firstVisibleItemScrollOffset

            val isScrollingUp = currentIndex < previousFirstVisibleItemIndex ||
                    (currentIndex == previousFirstVisibleItemIndex && currentOffset < previousFirstVisibleItemScrollOffset)
            val isAtTop = currentIndex == 0 && currentOffset == 0

            previousFirstVisibleItemIndex = currentIndex
            previousFirstVisibleItemScrollOffset = currentOffset

            isScrollingUp || isAtTop
        }
    }
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
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.extraSmall)
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

@Preview(showBackground = true)
@Composable
private fun MediaContentPreview() {
    CloudPhotosTheme {
        MediaContent(
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
            ),
            gridColumnCount = GridColumnCount.of(3),
            isLoading = false,
            isError = false,
            onGridSettingsClick = {},
            onSignOut = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaContentEmptyPreview() {
    CloudPhotosTheme {
        MediaContent(
            mediaList = emptyList(),
            gridColumnCount = GridColumnCount.of(3),
            isLoading = false,
            isError = false,
            onGridSettingsClick = {},
            onSignOut = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaContentErrorPreview() {
    CloudPhotosTheme {
        MediaContent(
            mediaList = emptyList(),
            gridColumnCount = GridColumnCount.of(3),
            isLoading = false,
            isError = true,
            onGridSettingsClick = {},
            onSignOut = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaContentLoadingPreview() {
    CloudPhotosTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MediaContent(
                mediaList = emptyList(),
                gridColumnCount = GridColumnCount.of(3),
                isLoading = true,
                isError = false,
                onGridSettingsClick = {},
                onSignOut = {},
                onRetry = {}
            )
            LoadingOverlay()
        }
    }
}
