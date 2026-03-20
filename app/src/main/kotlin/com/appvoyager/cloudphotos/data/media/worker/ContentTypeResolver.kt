package com.appvoyager.cloudphotos.data.media.worker

import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl

interface ContentTypeResolver {

    fun resolve(mediaId: MediaId): String?
    fun resolveUri(mediaId: MediaId): MediaUrl?

}
