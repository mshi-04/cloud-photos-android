package com.appvoyager.cloudphotos.domain.media.request

import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.ContentType
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId

data class CreateUploadRecordRequest(
    val mediaId: MediaId,
    val cloudStoragePath: CloudStoragePath,
    val contentType: ContentType,
    val mediaType: MediaType
)