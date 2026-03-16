package com.appvoyager.cloudphotos.data.media.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.appvoyager.cloudphotos.domain.media.repository.UploadScheduler
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UploadSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UploadScheduler {

    override fun scheduleUpload(mediaId: MediaId) {
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
