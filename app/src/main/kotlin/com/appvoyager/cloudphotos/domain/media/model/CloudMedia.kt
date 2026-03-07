package com.appvoyager.cloudphotos.domain.media.model

data class CloudMedia(
    val id: String,
    val url: String,
    val type: MediaType,
    val thumbnailUrl: String? = null,
    val createdAt: Long
)
