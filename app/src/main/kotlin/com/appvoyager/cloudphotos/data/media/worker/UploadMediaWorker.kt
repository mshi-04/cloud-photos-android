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
            val current = localRepository.getUploadRecords(listOf(record.mediaId)).firstOrNull()
            if (current == null || current.syncStatus != SyncStatus.PENDING_UPLOAD) continue

            val rawContentType = contentTypeResolver.resolve(record.mediaId)
            if (rawContentType == null) {
                localRepository.saveUploadRecords(
                    listOf(record.copy(syncStatus = SyncStatus.ERROR))
                )
                continue
            }

            val contentType = runCatching { ContentType.of(rawContentType) }.getOrNull()
            if (contentType == null) {
                localRepository.saveUploadRecords(
                    listOf(record.copy(syncStatus = SyncStatus.ERROR))
                )
                continue
            }
            val mediaType = MediaType.fromContentType(contentType.value)

            runCatching {
                val created = remoteRepository.createUploadRecord(
                    CreateUploadRecordRequest(
                        mediaId = record.mediaId,
                        cloudStoragePath = record.cloudStoragePath,
                        contentType = contentType,
                        mediaType = mediaType
                    )
                )
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
        val code = Regex("Unexpected response code (\\d+)").find(message)
            ?.groupValues?.get(1)?.toIntOrNull() ?: return false
        if (code == 404 || code == 429) return false
        return code in 400..499
    }

    companion object {
        const val WORK_NAME = "upload_media_worker"
    }

}
