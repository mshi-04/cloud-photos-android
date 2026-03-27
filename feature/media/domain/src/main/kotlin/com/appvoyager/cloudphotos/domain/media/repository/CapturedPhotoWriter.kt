package com.appvoyager.cloudphotos.domain.media.repository

import com.appvoyager.cloudphotos.domain.media.model.SavePhotoResult

interface CapturedPhotoWriter {
    suspend fun write(jpegData: ByteArray): SavePhotoResult
}
