package com.appvoyager.cloudphotos.di

import com.appvoyager.cloudphotos.data.media.datasource.UploadDataSource
import com.appvoyager.cloudphotos.data.media.datasource.UploadDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UploadDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindUploadDataSource(uploadDataSourceImpl: UploadDataSourceImpl): UploadDataSource

}
