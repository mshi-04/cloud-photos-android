package com.appvoyager.cloudphotos.data.media.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.RemoteUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.ContentType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class UploadMediaWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val localRepository: LocalUploadRecordsRepository,
    private val remoteRepository: RemoteUploadRecordsRepository,
    private val contentTypeResolver: ContentTypeResolver
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pendingRecords = localRepository.getPendingUploadRecords()
        var hasTemporaryFailure = false

        for (record in pendingRecords) {
            val rawContentType = contentTypeResolver.resolve(record.mediaId)
            if (rawContentType == null) {
                localRepository.saveUploadRecords(
                    listOf(record.copy(syncStatus = SyncStatus.ERROR))
                )
                continue
            }

            val contentType = ContentType.of(rawContentType)
            val mediaType = MediaType.fromContentType(rawContentType)

            runCatching {
                remoteRepository.createUploadRecord(
                    CreateUploadRecordRequest(
                        mediaId = record.mediaId,
                        cloudStoragePath = record.cloudStoragePath,
                        contentType = contentType,
                        mediaType = mediaType
                    )
                )
            }.onSuccess { created ->
                localRepository.saveUploadRecords(listOf(created))
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
        return message.contains("Unexpected response code 4")
    }

    companion object {
        const val WORK_NAME = "upload_media_worker"
    }

}
