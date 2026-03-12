package com.appvoyager.cloudphotos.domain.media.model

sealed class UploadResult<out T> {

    data class Success<T>(val value: T) : UploadResult<T>()
    data class Error(val error: UploadError) : UploadResult<Nothing>()

}

fun <T> UploadResult<T>.getOrNull(): T? = (this as? UploadResult.Success)?.value

fun <T> UploadResult<T>.errorOrNull(): UploadError? = (this as? UploadResult.Error)?.error

inline fun <T, R> UploadResult<T>.map(transform: (T) -> R): UploadResult<R> =
    when (this) {
        is UploadResult.Success -> UploadResult.Success(transform(value))
        is UploadResult.Error -> this
    }

inline fun <T, R> UploadResult<T>.flatMap(transform: (T) -> UploadResult<R>): UploadResult<R> =
    when (this) {
        is UploadResult.Success -> transform(value)
        is UploadResult.Error -> this
    }
