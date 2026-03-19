package com.appvoyager.cloudphotos.data.media.util

import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import org.json.JSONObject

internal object RemoteUploadRecordMapper {

    fun fromFetchResponse(json: String): List<UploadRecord> {
        val root = JSONObject(json)
        val records = root.getJSONArray("records")
        return (0 until records.length()).mapNotNull { i ->
            runCatching {
                val record = records.getJSONObject(i)
                UploadRecord(
                    mediaId = MediaId.of(record.getString("mediaId")),
                    cloudStoragePath = CloudStoragePath.of(record.getString("cloudStoragePath")),
                    isDeleted = IsDeleted.of(false),
                    syncStatus = SyncStatus.SYNCED,
                    mediaUploadedAt = MediaUploadedAt.of(record.getLong("uploadedAt"))
                )
            }.getOrNull()
        }
    }

    fun fromCreateResponse(json: String, request: CreateUploadRecordRequest): UploadRecord {
        val root = JSONObject(json)
        return UploadRecord(
            mediaId = request.mediaId,
            cloudStoragePath = request.cloudStoragePath,
            isDeleted = IsDeleted.of(false),
            syncStatus = SyncStatus.SYNCED,
            mediaUploadedAt = MediaUploadedAt.of(root.getLong("uploadedAt"))
        )
    }

}
