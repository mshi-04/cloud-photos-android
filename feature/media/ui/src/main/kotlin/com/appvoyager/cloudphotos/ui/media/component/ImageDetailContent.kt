package com.appvoyager.cloudphotos.ui.media.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.appvoyager.cloudphotos.domain.media.model.Media

@Composable
internal fun ImageDetailContent(
    media: Media,
    onSingleTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ZoomableContainer(
        modifier = modifier,
        onSingleTap = onSingleTap,
        onZoomChanged = onZoomChanged
    ) {
        AsyncImage(
            model = media.url.value,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}
