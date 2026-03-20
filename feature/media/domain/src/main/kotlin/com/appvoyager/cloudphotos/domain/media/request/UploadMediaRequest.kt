package com.appvoyager.cloudphotos.domain.media.request

import com.appvoyager.cloudphotos.domain.media.valueobject.ContentType
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl

data class UploadMediaRequest(
    val localUri: MediaUrl,
    val contentType: ContentType
)
