package com.appvoyager.cloudphotos.domain.media.model

enum class MediaType {
    IMAGE,
    VIDEO;

    companion object {
        fun fromContentType(contentType: String): MediaType =
            if (contentType.startsWith("video/")) VIDEO else IMAGE
    }
}
