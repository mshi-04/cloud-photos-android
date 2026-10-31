package com.appvoyager.cloudphotos.domain.media.repository

import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId

interface LocalUploadRecordsRepository {

    /**
     * 実装は [mediaIds] を 1 つの SQL `IN` 句へ渡すため、件数は SQLite のバインド変数上限に収まる必要がある。
     * 大量の ID を照会する呼び出し側は 900 件ずつに分割する。
     */
    suspend fun getUploadRecords(mediaIds: List<MediaId>): List<UploadRecord>

    suspend fun getPendingRecordMediaIds(): Set<MediaId>

    suspend fun getPendingUploadRecords(): List<UploadRecord>

    suspend fun getPendingDeleteRecords(): List<UploadRecord>

    suspend fun saveUploadRecords(records: List<UploadRecord>)

    suspend fun deleteUploadRecord(mediaId: MediaId)
}
