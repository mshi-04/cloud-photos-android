package com.appvoyager.cloudphotos.data.media.worker

import com.appvoyager.cloudphotos.domain.media.repository.DeleteScheduler
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import javax.inject.Inject

class DeleteSchedulerImpl @Inject constructor() : DeleteScheduler {

    override fun scheduleDelete(mediaId: MediaId) {
        // TODO: Implement with WorkManager
    }

}
