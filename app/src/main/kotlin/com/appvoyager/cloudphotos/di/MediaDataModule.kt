package com.appvoyager.cloudphotos.di

import com.appvoyager.cloudphotos.data.media.datasource.LocalMediaDataSource
import com.appvoyager.cloudphotos.data.media.datasource.LocalMediaDataSourceImpl
import com.appvoyager.cloudphotos.data.media.datasource.SettingsDataSource
import com.appvoyager.cloudphotos.data.media.datasource.SettingsDataSourceImpl
import com.appvoyager.cloudphotos.data.media.repository.LocalMediaRepositoryImpl
import com.appvoyager.cloudphotos.data.media.repository.SettingsRepositoryImpl
import com.appvoyager.cloudphotos.domain.media.repository.LocalMediaRepository
import com.appvoyager.cloudphotos.domain.media.repository.SettingsRepository
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
    abstract fun bindMediaRepository(impl: LocalMediaRepositoryImpl): LocalMediaRepository

    @Binds
    @Singleton
    abstract fun bindLocalMediaDataSource(impl: LocalMediaDataSourceImpl): LocalMediaDataSource

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsDataSource(impl: SettingsDataSourceImpl): SettingsDataSource
}
