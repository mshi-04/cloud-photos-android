package com.appvoyager.cloudphotos.di

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
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
    fun provideCognitoClient(): CognitoIdentityProviderClient =
        CognitoIdentityProviderClient { region = "ap-northeast-1" }

    @Provides
    @Singleton
    @Named("clientId")
    @JvmName("provideClientId")
    fun provideClientId(): ClientId = ClientId(BuildConfig.COGNITO_CLIENT_ID)

}
