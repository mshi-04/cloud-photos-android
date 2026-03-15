package com.appvoyager.cloudphotos.domain.media.model

import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt

data class UploadRecord(
    val mediaId: MediaId,
    val cloudStoragePath: CloudStoragePath,
    val isDeleted: IsDeleted,
    val syncStatus: SyncStatus,
    val mediaUploadedAt: MediaUploadedAt
)
