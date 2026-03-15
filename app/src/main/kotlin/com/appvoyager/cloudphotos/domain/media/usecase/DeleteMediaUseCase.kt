package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import javax.inject.Inject

class DeleteMediaUseCase @Inject constructor(
    private val localRepository: LocalUploadRecordsRepository
) {

    suspend operator fun invoke(record: UploadRecord) {
        val deleteRecord = record.copy(
            isDeleted = IsDeleted.of(true),
            syncStatus = SyncStatus.PENDING_DELETE
        )
        localRepository.saveUploadRecords(listOf(deleteRecord))
    }

}
