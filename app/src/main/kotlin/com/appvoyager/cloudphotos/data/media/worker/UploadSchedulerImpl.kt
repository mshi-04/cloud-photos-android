package com.appvoyager.cloudphotos.data.media.worker

import com.appvoyager.cloudphotos.domain.media.repository.UploadScheduler
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import javax.inject.Inject

class UploadSchedulerImpl @Inject constructor() : UploadScheduler {

    override fun scheduleUpload(mediaId: MediaId) {
        // TODO: Implement with WorkManager
    }

}
