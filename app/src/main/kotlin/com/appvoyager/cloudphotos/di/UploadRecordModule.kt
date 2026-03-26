package com.appvoyager.cloudphotos.di

import android.content.Context
import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.data.media.datasource.MediaStoreCapturedPhotoWriter
import com.appvoyager.cloudphotos.data.media.datasource.UploadRecordLocalDataSource
import com.appvoyager.cloudphotos.data.media.datasource.UploadRecordLocalDataSourceImpl
import com.appvoyager.cloudphotos.data.media.datasource.UploadRecordRemoteDataSource
import com.appvoyager.cloudphotos.data.media.datasource.UploadRecordRemoteDataSourceImpl
import com.appvoyager.cloudphotos.data.media.repository.LocalUploadRecordsRepositoryImpl
import com.appvoyager.cloudphotos.data.media.repository.RemoteUploadRecordsRepositoryImpl
import com.appvoyager.cloudphotos.data.media.worker.ContentTypeResolver
import com.appvoyager.cloudphotos.data.media.worker.ContentTypeResolverImpl
import com.appvoyager.cloudphotos.data.media.worker.DeleteSchedulerImpl
import com.appvoyager.cloudphotos.data.media.worker.UploadNotificationHelper
import com.appvoyager.cloudphotos.data.media.worker.UploadSchedulerImpl
import com.appvoyager.cloudphotos.domain.media.repository.DeleteScheduler
import com.appvoyager.cloudphotos.domain.media.repository.CapturedPhotoWriter
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.RemoteUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.UploadScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UploadRecordModule {

    @Binds
    @Singleton
    abstract fun bindLocalUploadRecordsRepository(
        localUploadRecordsRepositoryImpl: LocalUploadRecordsRepositoryImpl
    ): LocalUploadRecordsRepository

    @Binds
    @Singleton
    abstract fun bindRemoteUploadRecordsRepository(
        remoteUploadRecordsRepositoryImpl: RemoteUploadRecordsRepositoryImpl
    ): RemoteUploadRecordsRepository

    @Binds
    @Singleton
    abstract fun bindUploadRecordLocalDataSource(
        uploadRecordLocalDataSourceImpl: UploadRecordLocalDataSourceImpl
    ): UploadRecordLocalDataSource

    @Binds
    @Singleton
    abstract fun bindUploadRecordRemoteDataSource(
        uploadRecordRemoteDataSourceImpl: UploadRecordRemoteDataSourceImpl
    ): UploadRecordRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindUploadScheduler(uploadSchedulerImpl: UploadSchedulerImpl): UploadScheduler

    @Binds
    @Singleton
    abstract fun bindDeleteScheduler(deleteSchedulerImpl: DeleteSchedulerImpl): DeleteScheduler

    @Binds
    @Singleton
    abstract fun bindContentTypeResolver(
        contentTypeResolverImpl: ContentTypeResolverImpl
    ): ContentTypeResolver

    @Binds
    @Singleton
    abstract fun bindCapturedPhotoWriter(
        mediaStoreCapturedPhotoWriter: MediaStoreCapturedPhotoWriter
    ): CapturedPhotoWriter

    companion object {
        @Provides
        @Singleton
        fun provideUploadNotificationHelper(
            @ApplicationContext context: Context
        ): UploadNotificationHelper = UploadNotificationHelper(
            context = context,
            channelName = context.getString(R.string.notification_channel_upload),
            uploadingMessage = { count ->
                context.getString(
                    R.string.notification_uploading,
                    count
                )
            }
        )
    }

}
