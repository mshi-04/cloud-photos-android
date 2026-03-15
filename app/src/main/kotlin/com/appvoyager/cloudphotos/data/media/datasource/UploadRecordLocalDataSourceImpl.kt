package com.appvoyager.cloudphotos.data.media.datasource

import com.appvoyager.cloudphotos.data.media.db.UploadRecordEntityMapper
import com.appvoyager.cloudphotos.data.media.db.dao.UploadRecordDao
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import javax.inject.Inject

class UploadRecordLocalDataSourceImpl @Inject constructor(
    private val dao: UploadRecordDao
) : UploadRecordLocalDataSource {

    override suspend fun getUploadRecords(mediaIds: List<MediaId>): List<UploadRecord> =
        dao.getByMediaIds(mediaIds.map { it.value })
            .map(UploadRecordEntityMapper::toDomain)

    override suspend fun getPendingRecordMediaIds(): Set<MediaId> =
        dao.getPendingMediaIds()
            .map { MediaId.of(it) }
            .toSet()

    override suspend fun saveUploadRecords(records: List<UploadRecord>) =
        dao.upsertAll(records.map(UploadRecordEntityMapper::toEntity))

}
