package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.RemoteUploadRecordsRepository
import javax.inject.Inject

class SyncUploadRecordsUseCase @Inject constructor(
    private val remoteRepository: RemoteUploadRecordsRepository,
    private val localRepository: LocalUploadRecordsRepository
) {
    suspend operator fun invoke() {
        val remoteRecords = remoteRepository.fetchUploadRecords()

        // ローカルで保留中（PENDING_UPLOAD / PENDING_DELETE）のレコードを保護する
        val pendingMediaIds = localRepository.getPendingRecordMediaIds()
        val safeRecords = remoteRecords.filter { it.mediaId !in pendingMediaIds }

        localRepository.saveUploadRecords(safeRecords)
    }
}