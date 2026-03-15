package com.appvoyager.cloudphotos.data.media.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.appvoyager.cloudphotos.data.media.db.entity.UploadRecordEntity

@Dao
interface UploadRecordDao {

    @Query("SELECT * FROM upload_records WHERE mediaId IN (:mediaIds)")
    suspend fun getByMediaIds(mediaIds: List<String>): List<UploadRecordEntity>

    @Query("SELECT mediaId FROM upload_records WHERE syncStatus IN ('PENDING_UPLOAD', 'PENDING_DELETE')")
    suspend fun getPendingMediaIds(): List<String>

    @Upsert
    suspend fun upsertAll(records: List<UploadRecordEntity>)

}
