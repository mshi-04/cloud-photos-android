package com.appvoyager.cloudphotos.di

import com.appvoyager.cloudphotos.data.settings.datasource.SettingsDataSource
import com.appvoyager.cloudphotos.data.settings.datasource.SettingsDataSourceImpl
import com.appvoyager.cloudphotos.data.settings.repository.SettingsRepositoryImpl
import com.appvoyager.cloudphotos.domain.settings.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsDataModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsDataSource(
        impl: SettingsDataSourceImpl
    ): SettingsDataSource

}
