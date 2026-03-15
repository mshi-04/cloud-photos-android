package com.appvoyager.cloudphotos.data.media.datasource

import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId

interface UploadRecordLocalDataSource {

    suspend fun getUploadRecords(mediaIds: List<MediaId>): List<UploadRecord>

    suspend fun getPendingRecordMediaIds(): Set<MediaId>

    suspend fun saveUploadRecords(records: List<UploadRecord>)

}
