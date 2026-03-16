package com.appvoyager.cloudphotos.data.media.worker

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.RemoteUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.ContentType
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class UploadMediaWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val localRepository: LocalUploadRecordsRepository,
    private val remoteRepository: RemoteUploadRecordsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pendingRecords = localRepository.getPendingUploadRecords()
        var hasTemporaryFailure = false

        for (record in pendingRecords) {
            val rawContentType = resolveContentType(record.mediaId)
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

    private fun resolveContentType(mediaId: MediaId): String? {
        val lastUnderscore = mediaId.value.lastIndexOf('_')
        if (lastUnderscore < 0) return null
        val volumeName = mediaId.value.substring(0, lastUnderscore)
        val id = mediaId.value.substring(lastUnderscore + 1).toLongOrNull() ?: return null
        val uri = ContentUris.withAppendedId(
            MediaStore.Files.getContentUri(volumeName),
            id
        )
        return applicationContext.contentResolver.getType(uri)
    }

    private fun isPermanentFailure(e: Throwable): Boolean {
        val message = e.message ?: return false
        return message.contains("Unexpected response code 4")
    }

    companion object {
        const val WORK_NAME = "upload_media_worker"
    }

}
