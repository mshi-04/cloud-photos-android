package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.RemoteUploadRecordsRepository
import javax.inject.Inject

class SyncUploadRecordsUseCase @Inject constructor(
    private val remoteRepository: RemoteUploadRecordsRepository,
    private val localRepository: LocalUploadRecordsRepository
) {
    suspend operator fun invoke() {
        // APIから最新の履歴を取得
        val remoteRecords = remoteRepository.fetchUploadRecords()

        // Roomのデータを一括で上書き（Room側は @Insert(onConflict = REPLACE) にする）
        localRepository.saveUploadRecords(remoteRecords)
    }
}