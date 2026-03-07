package com.appvoyager.cloudphotos.domain.media.model

import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl

data class CloudMedia(
    val id: MediaId,
    val url: MediaUrl,
    val type: MediaType,
    val thumbnailUrl: MediaUrl? = null,
    val createdAt: Long
)
