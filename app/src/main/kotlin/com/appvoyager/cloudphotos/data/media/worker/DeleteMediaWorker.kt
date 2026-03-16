package com.appvoyager.cloudphotos.data.media.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.RemoteUploadRecordsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class DeleteMediaWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val localRepository: LocalUploadRecordsRepository,
    private val remoteRepository: RemoteUploadRecordsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pendingRecords = localRepository.getPendingDeleteRecords()
        var hasTemporaryFailure = false

        for (record in pendingRecords) {
            runCatching {
                remoteRepository.deleteStorageFile(record.cloudStoragePath)
                remoteRepository.deleteUploadRecord(record.mediaId)
                localRepository.deleteUploadRecord(record.mediaId)
            }.onFailure { e ->
                if (e is CancellationException) throw e
                if (isPermanentFailure(e)) {
                    localRepository.saveUploadRecords(
                        listOf(record.copy(syncStatus = SyncStatus.ERROR))
                    )
                } else {
                    hasTemporaryFailure = true
                }
            }
        }

        return if (hasTemporaryFailure) Result.retry() else Result.success()
    }

    private fun isPermanentFailure(e: Throwable): Boolean {
        val message = e.message ?: return false
        val code = Regex("Unexpected response code (\\d+)").find(message)
            ?.groupValues?.get(1)?.toIntOrNull() ?: return false
        if (code == 404 || code == 429) return false
        return code in 400..499
    }

    companion object {
        const val WORK_NAME = "delete_media_worker"
    }

}
