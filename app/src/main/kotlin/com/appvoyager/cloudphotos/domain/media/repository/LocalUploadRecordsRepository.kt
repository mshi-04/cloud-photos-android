package com.appvoyager.cloudphotos.domain.media.repository

import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId

interface LocalUploadRecordsRepository {

    suspend fun getUploadRecords(mediaIds: List<MediaId>): List<UploadRecord>

    suspend fun saveUploadRecords(records: List<UploadRecord>)

}
