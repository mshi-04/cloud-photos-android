package com.appvoyager.cloudphotos.ui.media.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.appvoyager.cloudphotos.core.ui.R
import com.appvoyager.cloudphotos.domain.media.model.Media
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun ImageDetailContent(
    media: Media,
    onSingleTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val typeLabel = stringResource(R.string.media_content_description_image)
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dateLabel = remember(media.createdAt.value) { dateFormat.format(Date(media.createdAt.value)) }
    val contentDesc = stringResource(R.string.media_content_description_format, typeLabel, dateLabel)

    ZoomableContainer(
        modifier = modifier,
        onSingleTap = onSingleTap,
        onZoomChanged = onZoomChanged
    ) {
        AsyncImage(
            model = media.url.value,
            contentDescription = contentDesc,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}
