package com.appvoyager.cloudphotos.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {

//    @Binds
//    @Singleton
//    abstract fun bindAuthRepository(
//        authRepositoryImpl: AuthRepositoryImpl
//    ): AuthRepository

}
