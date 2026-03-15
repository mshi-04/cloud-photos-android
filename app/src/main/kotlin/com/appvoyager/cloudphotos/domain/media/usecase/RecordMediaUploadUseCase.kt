package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.UploadScheduler
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import javax.inject.Inject

class RecordMediaUploadUseCase @Inject constructor(
    private val localRepository: LocalUploadRecordsRepository,
    private val uploadScheduler: UploadScheduler
) {

    suspend operator fun invoke(
        mediaId: MediaId,
        cloudStoragePath: CloudStoragePath,
        mediaUploadedAt: MediaUploadedAt
    ) {
        val uploadRecord = UploadRecord(
            mediaId = mediaId,
            cloudStoragePath = cloudStoragePath,
            isDeleted = IsDeleted.of(false),
            mediaUploadedAt = mediaUploadedAt,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        localRepository.saveUploadRecords(listOf(uploadRecord))
        uploadScheduler.scheduleUpload(mediaId)
    }

}
