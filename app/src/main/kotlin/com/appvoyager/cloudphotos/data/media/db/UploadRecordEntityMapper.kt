package com.appvoyager.cloudphotos.data.media.db

import com.appvoyager.cloudphotos.data.media.db.entity.UploadRecordEntity
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt

internal object UploadRecordEntityMapper {

    fun toDomain(entity: UploadRecordEntity): UploadRecord = UploadRecord(
        mediaId = MediaId.of(entity.mediaId),
        cloudStoragePath = CloudStoragePath.of(entity.cloudStoragePath),
        isDeleted = IsDeleted.of(entity.isDeleted),
        syncStatus = SyncStatus.valueOf(entity.syncStatus),
        mediaUploadedAt = MediaUploadedAt.of(entity.uploadedAt)
    )

    fun toEntity(record: UploadRecord): UploadRecordEntity = UploadRecordEntity(
        mediaId = record.mediaId.value,
        cloudStoragePath = record.cloudStoragePath.value,
        isDeleted = record.isDeleted.value,
        syncStatus = record.syncStatus.name,
        uploadedAt = record.mediaUploadedAt.value
    )

}
