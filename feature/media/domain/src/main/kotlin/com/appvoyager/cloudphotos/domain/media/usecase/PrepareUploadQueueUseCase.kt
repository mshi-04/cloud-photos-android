package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.common.Clock
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.LocalMediaRepository
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.UploadScheduler
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import javax.inject.Inject

class PrepareUploadQueueUseCase @Inject constructor(
    private val localMediaRepository: LocalMediaRepository,
    private val localUploadRecordsRepository: LocalUploadRecordsRepository,
    private val uploadScheduler: UploadScheduler,
    private val clock: Clock
) {

    suspend operator fun invoke() {
        val allMedia = localMediaRepository.getMediaList()
        if (allMedia.isEmpty()) return

        val allMediaIds = allMedia.map { it.id }
        val existingIds = localUploadRecordsRepository.getUploadRecords(allMediaIds)
            .map { it.mediaId }
            .toSet()

        val newRecords = allMedia
            .filter { it.id !in existingIds }
            .map { media ->
                UploadRecord(
                    mediaId = media.id,
                    cloudStoragePath = null,
                    isDeleted = IsDeleted.of(false),
                    mediaUploadedAt = MediaUploadedAt.of(clock.getCurrentTime()),
                    syncStatus = SyncStatus.PENDING_UPLOAD
                )
            }

        if (newRecords.isEmpty()) return

        localUploadRecordsRepository.saveUploadRecords(newRecords)
        uploadScheduler.scheduleUpload()
    }
}
