package com.appvoyager.cloudphotos.fcm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.appvoyager.cloudphotos.data.common.AmplifyRestException
import com.appvoyager.cloudphotos.data.fcm.DeviceToken
import com.appvoyager.cloudphotos.data.fcm.DeviceTokenDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class RegisterDeviceTokenWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val deviceTokenDataSource: DeviceTokenDataSource
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val rawToken = inputData.getString(KEY_TOKEN) ?: return Result.failure()
        return runCatching {
            val token = DeviceToken.of(rawToken)
            deviceTokenDataSource.register(token)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { e ->
                when (e) {
                    is CancellationException -> throw e
                    is IllegalArgumentException -> Result.failure()
                    is AmplifyRestException if e.isNonServerError -> Result.failure()
                    else -> Result.retry()
                }
            }
        )
    }

    companion object {
        const val WORK_NAME = "register_device_token_worker"
        const val KEY_TOKEN = "token"

        fun enqueue(context: Context, token: DeviceToken) {
            val request = OneTimeWorkRequestBuilder<RegisterDeviceTokenWorker>()
                .setInputData(workDataOf(KEY_TOKEN to token.value))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }
    }
}
