package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.repository.DeleteScheduler
import javax.inject.Inject

class ScheduleDeleteUseCase @Inject constructor(
    private val deleteScheduler: DeleteScheduler
) {

    operator fun invoke() = deleteScheduler.scheduleDelete()

}
