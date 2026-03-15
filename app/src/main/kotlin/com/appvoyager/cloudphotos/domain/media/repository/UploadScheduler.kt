package com.appvoyager.cloudphotos.domain.media.repository

import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId

interface UploadScheduler {

    fun scheduleUpload(mediaId: MediaId)

}
