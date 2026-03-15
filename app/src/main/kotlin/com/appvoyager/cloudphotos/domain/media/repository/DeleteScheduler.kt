package com.appvoyager.cloudphotos.domain.media.repository

import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId

interface DeleteScheduler {

    fun scheduleDelete(mediaId: MediaId)

}
