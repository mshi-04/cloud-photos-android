package com.appvoyager.cloudphotos.ui.media.screen

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.cloudphotos.core.ui.R
import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.ui.media.component.ImageDetailContent
import com.appvoyager.cloudphotos.ui.media.component.VideoDetailContent
import com.appvoyager.cloudphotos.ui.media.effect.MediaDetailEffect
import com.appvoyager.cloudphotos.ui.media.uistate.MediaDetailUiState
import com.appvoyager.cloudphotos.ui.media.viewmodel.MediaDetailViewModel

@Composable
fun MediaDetailScreen(
    initialIndex: Int,
    viewModel: MediaDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestOnNavigateBack = rememberUpdatedState(onNavigateBack)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MediaDetailEffect.NavigateBack -> latestOnNavigateBack.value()
            }
        }
    }

    when (val state = uiState.screenState) {
        is MediaDetailUiState.ScreenState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
        is MediaDetailUiState.ScreenState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
        is MediaDetailUiState.ScreenState.Success -> {
            MediaDetailContent(
                mediaList = state.mediaList,
                initialIndex = initialIndex,
                onNavigateBack = { viewModel.onNavigateBack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaDetailContent(
    mediaList: List<Media>,
    initialIndex: Int,
    onNavigateBack: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black

    var isFullscreen by remember { mutableStateOf(false) }
    var isPagerScrollEnabled by remember { mutableStateOf(true) }

    val safeInitialPage = initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeInitialPage) { mediaList.size }

    LaunchedEffect(pagerState.currentPage) {
        isPagerScrollEnabled = true
    }

    val view = LocalView.current
    LaunchedEffect(isFullscreen) {
        if (view.isInEditMode) return@LaunchedEffect
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        if (isFullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!view.isInEditMode) {
                val window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = isPagerScrollEnabled,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val media = mediaList[page]
            when (media.type) {
                MediaType.IMAGE -> ImageDetailContent(
                    media = media,
                    onSingleTap = { isFullscreen = !isFullscreen },
                    onZoomChanged = { isZoomed -> isPagerScrollEnabled = !isZoomed },
                    modifier = Modifier.fillMaxSize()
                )
                MediaType.VIDEO -> VideoDetailContent(
                    media = media,
                    isCurrentPage = page == pagerState.currentPage,
                    onSingleTap = { isFullscreen = !isFullscreen },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = !isFullscreen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.media_detail_back),
                            tint = contentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = contentColor
                )
            )
        }
    }
}
