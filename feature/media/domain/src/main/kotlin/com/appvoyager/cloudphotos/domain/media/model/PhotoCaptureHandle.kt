package com.appvoyager.cloudphotos.domain.media.model

fun interface PhotoCaptureHandle {
    suspend fun capture(): SavePhotoResult
}
