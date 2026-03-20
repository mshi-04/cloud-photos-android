package com.appvoyager.cloudphotos.domain.media.model

sealed class UploadError(open val message: String?) {

    data class AccessDenied(override val message: String? = null) : UploadError(message)

    data class NotAuthenticated(override val message: String? = null) : UploadError(message)

    data class StorageLimitExceeded(override val message: String? = null) : UploadError(message)

    data class Network(override val message: String? = null) : UploadError(message)

    data class FileNotFound(override val message: String? = null) : UploadError(message)

    data class Unknown(override val message: String? = null) : UploadError(message)

}
