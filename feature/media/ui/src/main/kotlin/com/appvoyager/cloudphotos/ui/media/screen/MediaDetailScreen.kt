package com.appvoyager.cloudphotos.ui.media.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.appvoyager.cloudphotos.core.ui.R
import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaCreatedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import com.appvoyager.cloudphotos.ui.media.component.ImageDetailContent
import com.appvoyager.cloudphotos.ui.media.component.VideoDetailContent
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme

@Composable
fun MediaDetailScreen(mediaList: List<Media>, initialMediaId: MediaId, onNavigateBack: () -> Unit) {
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val initialIndex = mediaList.indexOfFirst { it.id == initialMediaId }
    val shouldNavigateBack = mediaList.isEmpty() || initialIndex == -1

    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            currentOnNavigateBack()
        }
    }

    if (shouldNavigateBack) return

    MediaDetailContent(
        mediaList = mediaList,
        initialIndex = initialIndex,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaDetailContent(mediaList: List<Media>, initialIndex: Int, onNavigateBack: () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White

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
        val activity = view.context.findActivity() ?: return@LaunchedEffect
        val window = activity.window
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
                val activity = view.context.findActivity() ?: return@onDispose
                val window = activity.window
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
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.3f), shape = CircleShape)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.media_detail_back),
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaDetailContentPreviewImage() {
    CloudPhotosTheme {
        MediaDetailContent(
            mediaList = listOf(
                Media(
                    id = MediaId.of("1"),
                    url = MediaUrl.of("content://media/external/images/1"),
                    type = MediaType.IMAGE,
                    createdAt = MediaCreatedAt.of(1700000000L)
                )
            ),
            initialIndex = 0,
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaDetailContentMultiplePreview() {
    CloudPhotosTheme {
        MediaDetailContent(
            mediaList = listOf(
                Media(
                    id = MediaId.of("3"),
                    url = MediaUrl.of("content://media/external/images/3"),
                    type = MediaType.IMAGE,
                    createdAt = MediaCreatedAt.of(1700000002L)
                ),
                Media(
                    id = MediaId.of("4"),
                    url = MediaUrl.of("content://media/external/video/4"),
                    type = MediaType.VIDEO,
                    createdAt = MediaCreatedAt.of(1700000003L)
                )
            ),
            initialIndex = 0,
            onNavigateBack = {}
        )
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
