package com.appvoyager.cloudphotos.di

import com.appvoyager.cloudphotos.data.media.datasource.UploadRecordLocalDataSource
import com.appvoyager.cloudphotos.data.media.datasource.UploadRecordLocalDataSourceImpl
import com.appvoyager.cloudphotos.data.media.datasource.UploadRecordRemoteDataSource
import com.appvoyager.cloudphotos.data.media.datasource.UploadRecordRemoteDataSourceImpl
import com.appvoyager.cloudphotos.data.media.repository.LocalUploadRecordsRepositoryImpl
import com.appvoyager.cloudphotos.data.media.repository.RemoteUploadRecordsRepositoryImpl
import com.appvoyager.cloudphotos.data.media.worker.DeleteSchedulerImpl
import com.appvoyager.cloudphotos.data.media.worker.UploadSchedulerImpl
import com.appvoyager.cloudphotos.domain.media.repository.DeleteScheduler
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.RemoteUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.UploadScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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

}
