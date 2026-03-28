package com.appvoyager.cloudphotos.domain.media.model

sealed class UploadResult<out T> {
    data class Success<T>(val value: T) : UploadResult<T>()
    data class Error(val error: UploadError) : UploadResult<Nothing>()
}
