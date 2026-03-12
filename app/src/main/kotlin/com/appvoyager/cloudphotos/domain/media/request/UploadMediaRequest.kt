package com.appvoyager.cloudphotos.domain.media.request

import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import com.appvoyager.cloudphotos.domain.media.valueobject.ContentType

data class UploadMediaRequest(
    val localUri: MediaUrl,
    val contentType: ContentType
)
