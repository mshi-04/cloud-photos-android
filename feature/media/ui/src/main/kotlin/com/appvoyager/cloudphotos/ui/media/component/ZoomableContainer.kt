package com.appvoyager.cloudphotos.ui.media.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 4f
private const val DOUBLE_TAP_SCALE = 2.5f

@Composable
internal fun ZoomableContainer(
    modifier: Modifier = Modifier,
    onZoomChanged: (isZoomed: Boolean) -> Unit = {},
    onSingleTap: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { tapPosition ->
                        if (scale > MIN_SCALE) {
                            scale = MIN_SCALE
                            offset = Offset.Zero
                            onZoomChanged(false)
                        } else {
                            val newScale = DOUBLE_TAP_SCALE
                            val centerX = containerSize.width / 2f
                            val centerY = containerSize.height / 2f
                            val maxX = (containerSize.width * (newScale - 1)) / 2f
                            val maxY = (containerSize.height * (newScale - 1)) / 2f
                            scale = newScale
                            offset = Offset(
                                x = (-(tapPosition.x - centerX) * newScale).coerceIn(-maxX, maxX),
                                y = (-(tapPosition.y - centerY) * newScale).coerceIn(-maxY, maxY)
                            )
                            onZoomChanged(true)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.isConsumed }) break
                        val pointersDown = event.changes.count { it.pressed }
                        when {
                            pointersDown >= 2 -> {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val newScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
                                val maxX = (containerSize.width * (newScale - 1)) / 2f
                                val maxY = (containerSize.height * (newScale - 1)) / 2f
                                offset = Offset(
                                    x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                                    y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
                                )
                                scale = newScale
                                onZoomChanged(scale > MIN_SCALE)
                                event.changes.forEach { it.consume() }
                            }
                            pointersDown == 1 && scale > MIN_SCALE -> {
                                val change = event.changes.first()
                                if (change.positionChanged()) {
                                    val panDelta = change.position - change.previousPosition
                                    val maxX = (containerSize.width * (scale - 1)) / 2f
                                    val maxY = (containerSize.height * (scale - 1)) / 2f
                                    offset = Offset(
                                        x = (offset.x + panDelta.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + panDelta.y).coerceIn(-maxY, maxY)
                                    )
                                    change.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
