package com.appvoyager.cloudphotos.data.media.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.appvoyager.cloudphotos.data.media.db.entity.UploadRecordEntity

@Dao
interface UploadRecordDao {

    @Query("SELECT * FROM upload_records WHERE mediaId IN (:mediaIds)")
    suspend fun getByMediaIds(mediaIds: List<String>): List<UploadRecordEntity>

    /**
     * 同期が未完了の mediaId 一覧を返す。
     *
     * 注意: クエリ内の文字列リテラル 'PENDING_UPLOAD' および 'PENDING_DELETE' は
     * [com.appvoyager.cloudphotos.domain.media.model.SyncStatus] の enum 名をハードコードしている。
     * enum エントリをリネームした場合はこのクエリも合わせて更新すること。
     * ガードテストは UploadRecordDaoSyncStatusTest を参照。
     */
    @Query("SELECT mediaId FROM upload_records WHERE syncStatus IN ('PENDING_UPLOAD', 'PENDING_DELETE')")
    suspend fun getPendingMediaIds(): List<String>

    @Upsert
    suspend fun upsertAll(records: List<UploadRecordEntity>)

}
