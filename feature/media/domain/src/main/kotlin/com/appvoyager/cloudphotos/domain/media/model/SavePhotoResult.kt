package com.appvoyager.cloudphotos.domain.media.model

import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl

sealed class SavePhotoResult {
    data class Success(val url: MediaUrl) : SavePhotoResult()
    data class Error(val type: ErrorType) : SavePhotoResult()

    enum class ErrorType {
        STORAGE_FULL,
        SAVE_FAILED
    }
}
