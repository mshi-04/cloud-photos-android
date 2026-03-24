package com.appvoyager.cloudphotos.data.media.datasource

import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.UploadSuccessCount

interface UploadRecordRemoteDataSource {

    suspend fun fetchUploadRecords(): List<UploadRecord>

    suspend fun createUploadRecord(request: CreateUploadRecordRequest): UploadRecord

    suspend fun deleteUploadRecord(mediaId: MediaId)

    suspend fun completeUpload(successCount: UploadSuccessCount)

}
