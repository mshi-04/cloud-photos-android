package com.appvoyager.cloudphotos.data.media.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.appvoyager.cloudphotos.domain.media.repository.UploadScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UploadSchedulerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : UploadScheduler {

    override fun scheduleUpload() {
        val request = OneTimeWorkRequestBuilder<UploadMediaWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                UploadMediaWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
    }

}
