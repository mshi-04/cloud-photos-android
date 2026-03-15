package com.appvoyager.cloudphotos.data.media.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_records")
data class UploadRecordEntity(
    @PrimaryKey val mediaId: String,
    val cloudStoragePath: String,
    val isDeleted: Boolean,
    val syncStatus: String,
    val uploadedAt: Long
)
