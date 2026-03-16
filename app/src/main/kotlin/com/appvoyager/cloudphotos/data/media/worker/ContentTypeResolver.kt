package com.appvoyager.cloudphotos.data.media.worker

import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId

interface ContentTypeResolver {
    fun resolve(mediaId: MediaId): String?
}
