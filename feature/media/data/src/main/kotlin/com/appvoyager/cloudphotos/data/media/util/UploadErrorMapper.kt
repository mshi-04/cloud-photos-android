package com.appvoyager.cloudphotos.data.media.util

import com.amplifyframework.storage.StorageException
import com.appvoyager.cloudphotos.domain.media.model.UploadError
import java.io.FileNotFoundException
import java.io.IOException

internal object UploadErrorMapper {

    fun map(throwable: Throwable): UploadError =
        when (throwable) {
            is FileNotFoundException -> UploadError.FileNotFound(throwable.message)
            is IOException -> UploadError.Network(throwable.message)
            is StorageException -> mapStorageException(throwable)
            else -> UploadError.Unknown(throwable.message)
        }

    private fun mapStorageException(exception: StorageException): UploadError =
        when {
            isAccessDenied(exception) -> UploadError.AccessDenied(exception.message)
            isNotAuthenticated(exception) -> UploadError.NotAuthenticated(exception.message)
            isStorageLimitExceeded(exception) -> UploadError.StorageLimitExceeded(exception.message)
            exception.cause is IOException -> UploadError.Network(exception.message)
            else -> UploadError.Unknown(exception.message)
        }

    private fun isAccessDenied(exception: StorageException): Boolean {
        val message = exception.message.orEmpty()
        return message.contains("access denied", ignoreCase = true)
    }

    private fun isNotAuthenticated(exception: StorageException): Boolean {
        val message = exception.message.orEmpty()
        return message.contains("signed in", ignoreCase = true)
                || message.contains("not authenticated", ignoreCase = true)
                || message.contains("unauthenticated", ignoreCase = true)
    }

    private fun isStorageLimitExceeded(exception: StorageException): Boolean {
        val message = exception.message.orEmpty()
        return message.contains("limit", ignoreCase = true)
                || message.contains("quota", ignoreCase = true)
    }

}
