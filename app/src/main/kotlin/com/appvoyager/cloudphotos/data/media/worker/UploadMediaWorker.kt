package com.appvoyager.cloudphotos.data.media.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appvoyager.cloudphotos.data.media.datasource.UploadDataSource
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadError
import com.appvoyager.cloudphotos.domain.media.model.UploadResult
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.RemoteUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.request.UploadMediaRequest
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
    private val contentTypeResolver: ContentTypeResolver,
    private val uploadDataSource: UploadDataSource,
    private val notificationHelper: UploadNotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pendingRecords = localRepository.getPendingUploadRecords()
        if (pendingRecords.isEmpty()) return Result.success()

        notificationHelper.show(pendingRecords.size)

        var hasTemporaryFailure = false

        for (record in pendingRecords) {
            val current = localRepository.getUploadRecords(listOf(record.mediaId)).firstOrNull()
            if (current == null || current.syncStatus != SyncStatus.PENDING_UPLOAD) continue

            val rawContentType = contentTypeResolver.resolve(current.mediaId)
            if (rawContentType == null) {
                localRepository.saveUploadRecords(
                    listOf(current.copy(syncStatus = SyncStatus.ERROR))
                )
                continue
            }

            val contentType = runCatching { ContentType.of(rawContentType) }.getOrNull()
            if (contentType == null) {
                localRepository.saveUploadRecords(
                    listOf(current.copy(syncStatus = SyncStatus.ERROR))
                )
                continue
            }

            val uploadedRecord = if (current.cloudStoragePath != null) {
                current
            } else {
                val localUri = contentTypeResolver.resolveUri(current.mediaId)
                if (localUri == null) {
                    localRepository.saveUploadRecords(
                        listOf(current.copy(syncStatus = SyncStatus.ERROR))
                    )
                    continue
                }

                when (val uploadResult = uploadDataSource.uploadMedia(
                    UploadMediaRequest(
                        localUri = localUri,
                        contentType = contentType
                    )
                )) {
                    is UploadResult.Success -> {
                        val uploaded = current.copy(cloudStoragePath = uploadResult.value)
                        localRepository.saveUploadRecords(listOf(uploaded))
                        uploaded
                    }

                    is UploadResult.Error -> {
                        if (isS3PermanentFailure(uploadResult.error)) {
                            localRepository.saveUploadRecords(
                                listOf(current.copy(syncStatus = SyncStatus.ERROR))
                            )
                        } else {
                            hasTemporaryFailure = true
                        }
                        continue
                    }
                }
            }

            val cloudStoragePath = requireNotNull(uploadedRecord.cloudStoragePath) {
                "cloudStoragePath must not be null after upload"
            }
            val mediaType = MediaType.fromContentType(contentType.value)

            runCatching {
                val created = remoteRepository.createUploadRecord(
                    CreateUploadRecordRequest(
                        mediaId = uploadedRecord.mediaId,
                        cloudStoragePath = cloudStoragePath,
                        contentType = contentType,
                        mediaType = mediaType
                    )
                )
                localRepository.saveUploadRecords(listOf(created))
            }.onFailure { e ->
                if (e is CancellationException) throw e
                if (isPermanentFailure(e)) {
                    val cleanupResult =
                        runCatching { uploadDataSource.deleteUploadedObject(cloudStoragePath) }
                            .onFailure { if (it is CancellationException) throw it }
                    if (cleanupResult.isSuccess) {
                        localRepository.saveUploadRecords(
                            listOf(
                                uploadedRecord.copy(
                                    cloudStoragePath = null,
                                    syncStatus = SyncStatus.ERROR
                                )
                            )
                        )
                    } else {
                        localRepository.saveUploadRecords(
                            listOf(uploadedRecord.copy(syncStatus = SyncStatus.PENDING_DELETE))
                        )
                    }
                } else {
                    hasTemporaryFailure = true
                }
            }
        }

        notificationHelper.cancel()
        return if (hasTemporaryFailure) Result.retry() else Result.success()
    }

    private fun isS3PermanentFailure(error: UploadError): Boolean =
        when (error) {
            is UploadError.AccessDenied -> true
            is UploadError.NotAuthenticated -> true
            is UploadError.StorageLimitExceeded -> true
            is UploadError.FileNotFound -> true
            is UploadError.Network -> false
            is UploadError.Unknown -> false
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
