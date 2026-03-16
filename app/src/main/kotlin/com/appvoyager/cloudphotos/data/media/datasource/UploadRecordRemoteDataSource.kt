package com.appvoyager.cloudphotos.data.media.datasource

import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId

interface UploadRecordRemoteDataSource {

    suspend fun fetchUploadRecords(): List<UploadRecord>

    suspend fun createUploadRecord(request: CreateUploadRecordRequest): UploadRecord

    suspend fun deleteUploadRecord(mediaId: MediaId)

    suspend fun deleteStorageFile(cloudStoragePath: CloudStoragePath)

}
