package com.appvoyager.cloudphotos.di

import com.appvoyager.cloudphotos.domain.media.datasource.LocalMediaDataSource
import com.appvoyager.cloudphotos.data.media.datasource.LocalMediaDataSourceImpl
import com.appvoyager.cloudphotos.data.media.repository.LocalMediaRepositoryImpl
import com.appvoyager.cloudphotos.domain.media.repository.LocalMediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaDataModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: LocalMediaRepositoryImpl
    ): LocalMediaRepository

    @Binds
    @Singleton
    abstract fun bindLocalMediaDataSource(
        impl: LocalMediaDataSourceImpl
    ): LocalMediaDataSource

}
