package com.appvoyager.cloudphotos.data.media.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.appvoyager.cloudphotos.data.media.db.dao.UploadRecordDao
import com.appvoyager.cloudphotos.data.media.db.entity.UploadRecordEntity

@Database(
    entities = [UploadRecordEntity::class],
    version = 1
)
abstract class CloudPhotosDatabase : RoomDatabase() {

    abstract fun uploadRecordDao(): UploadRecordDao

}
