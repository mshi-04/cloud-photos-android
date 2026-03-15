package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.DeleteScheduler
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import javax.inject.Inject

class DeleteMediaUseCase @Inject constructor(
    private val localRepository: LocalUploadRecordsRepository,
    private val deleteScheduler: DeleteScheduler
) {

    suspend operator fun invoke(record: UploadRecord) {
        if (record.syncStatus == SyncStatus.PENDING_UPLOAD) {
            localRepository.saveUploadRecords(
                listOf(
                    record.copy(
                        isDeleted = IsDeleted.of(true),
                        syncStatus = SyncStatus.PENDING_DELETE
                    )
                )
            )
            return
        }

        val deleteRecord = record.copy(
            isDeleted = IsDeleted.of(true),
            syncStatus = SyncStatus.PENDING_DELETE
        )
        localRepository.saveUploadRecords(listOf(deleteRecord))
        deleteScheduler.scheduleDelete(record.mediaId)
    }

}
