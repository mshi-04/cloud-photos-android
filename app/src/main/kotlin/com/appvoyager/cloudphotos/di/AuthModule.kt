package com.appvoyager.cloudphotos.di

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import com.appvoyager.cloudphotos.BuildConfig
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
    fun provideClientId(): String = BuildConfig.COGNITO_CLIENT_ID

}
