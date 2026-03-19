package com.appvoyager.cloudphotos.di

import com.appvoyager.cloudphotos.data.common.SystemClock
import com.appvoyager.cloudphotos.domain.common.Clock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClockModule {

    @Binds
    @Singleton
    abstract fun bindClock(systemClock: SystemClock): Clock

}
