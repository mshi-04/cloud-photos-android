package com.appvoyager.cloudphotos.data.media.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.appvoyager.cloudphotos.domain.media.repository.DeleteScheduler
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeleteSchedulerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DeleteScheduler {

    override fun scheduleDelete(mediaId: MediaId) {
        val request = OneTimeWorkRequestBuilder<DeleteMediaWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                DeleteMediaWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
    }

}
