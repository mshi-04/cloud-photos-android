package com.appvoyager.cloudphotos.domain.media.repository

import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId

interface RemoteUploadRecordsRepository {

    suspend fun fetchUploadRecords(): List<UploadRecord>

    suspend fun createUploadRecord(request: CreateUploadRecordRequest): UploadRecord

    suspend fun deleteUploadRecord(mediaId: MediaId)

    suspend fun deleteStorageFile(cloudStoragePath: CloudStoragePath)

}
