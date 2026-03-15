package com.appvoyager.cloudphotos.data.media.repository

import com.appvoyager.cloudphotos.data.media.datasource.UploadRecordRemoteDataSource
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.RemoteUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import javax.inject.Inject

class RemoteUploadRecordsRepositoryImpl @Inject constructor(
    private val remoteDataSource: UploadRecordRemoteDataSource
) : RemoteUploadRecordsRepository {

    override suspend fun fetchUploadRecords(): List<UploadRecord> =
        remoteDataSource.fetchUploadRecords()

    override suspend fun createUploadRecord(request: CreateUploadRecordRequest): UploadRecord =
        remoteDataSource.createUploadRecord(request)

    override suspend fun deleteUploadRecord(mediaId: MediaId) =
        remoteDataSource.deleteUploadRecord(mediaId)

    override suspend fun deleteStorageFile(cloudStoragePath: CloudStoragePath) =
        remoteDataSource.deleteStorageFile(cloudStoragePath)

}
