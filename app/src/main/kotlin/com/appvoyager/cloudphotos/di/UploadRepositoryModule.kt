package com.appvoyager.cloudphotos.di

import com.appvoyager.cloudphotos.data.upload.repository.UploadRepositoryImpl
import com.appvoyager.cloudphotos.domain.upload.repository.UploadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UploadRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUploadRepository(uploadRepositoryImpl: UploadRepositoryImpl): UploadRepository

}
