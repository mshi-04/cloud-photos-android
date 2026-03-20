package com.appvoyager.cloudphotos.di

import android.content.Context
import androidx.room.Room
import com.appvoyager.cloudphotos.data.media.db.CloudPhotosDatabase
import com.appvoyager.cloudphotos.data.media.db.dao.UploadRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CloudPhotosDatabase =
        Room.databaseBuilder(
            context,
            CloudPhotosDatabase::class.java,
            "cloud_photos.db"
        ).build()

    @Provides
    @Singleton
    fun provideUploadRecordDao(database: CloudPhotosDatabase): UploadRecordDao =
        database.uploadRecordDao()

}
