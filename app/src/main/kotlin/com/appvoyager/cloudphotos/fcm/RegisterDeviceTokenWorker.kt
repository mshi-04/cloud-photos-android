package com.appvoyager.cloudphotos.fcm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appvoyager.cloudphotos.data.fcm.DeviceTokenDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class RegisterDeviceTokenWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val deviceTokenDataSource: DeviceTokenDataSource
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_TOKEN) ?: return Result.failure()
        return runCatching { deviceTokenDataSource.register(token) }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { e ->
                    if (e is CancellationException) throw e
                    Result.retry()
                }
            )
    }

    companion object {
        const val WORK_NAME = "register_device_token_worker"
        const val KEY_TOKEN = "token"
    }

}
