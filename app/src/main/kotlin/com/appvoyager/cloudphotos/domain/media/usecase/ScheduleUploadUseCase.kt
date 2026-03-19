package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.repository.UploadScheduler
import javax.inject.Inject

class ScheduleUploadUseCase @Inject constructor(
    private val uploadScheduler: UploadScheduler
) {

    suspend operator fun invoke() = uploadScheduler.scheduleUpload()

}
