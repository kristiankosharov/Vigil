package app.vigil.feature.auth.data.di

import app.vigil.feature.auth.data.AuthRepository
import app.vigil.feature.auth.data.SessionRepository
import app.vigil.feature.auth.data.repository.AuthRepositoryImpl
import app.vigil.feature.auth.data.repository.SessionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthRepositoryModule {
    @Binds
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
