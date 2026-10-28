package app.vigil.feature.auth.data.di

import app.vigil.feature.auth.data.remote.AuthApi
import app.vigil.feature.auth.data.remote.AuthApiImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthApiModule {
    @Binds
    fun bindAuthApi(impl: AuthApiImpl): AuthApi
}
