package com.appvoyager.cloudphotos.di

import com.appvoyager.cloudphotos.BuildConfig
import com.appvoyager.cloudphotos.domain.auth.valueobject.ClientId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    @Named("clientId")
    @JvmName("provideClientId")
    fun provideClientId(): ClientId = ClientId(BuildConfig.COGNITO_CLIENT_ID)

}
