package com.appvoyager.cloudphotos.data.media.repository

import com.appvoyager.cloudphotos.data.media.datasource.UploadRecordLocalDataSource
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import javax.inject.Inject

class LocalUploadRecordsRepositoryImpl @Inject constructor(
    private val localDataSource: UploadRecordLocalDataSource
) : LocalUploadRecordsRepository {

    override suspend fun getUploadRecords(mediaIds: List<MediaId>): List<UploadRecord> =
        localDataSource.getUploadRecords(mediaIds)

    override suspend fun getPendingRecordMediaIds(): Set<MediaId> =
        localDataSource.getPendingRecordMediaIds()

    override suspend fun saveUploadRecords(records: List<UploadRecord>) =
        localDataSource.saveUploadRecords(records)

}
