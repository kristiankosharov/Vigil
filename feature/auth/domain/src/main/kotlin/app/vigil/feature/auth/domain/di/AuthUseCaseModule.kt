package app.vigil.feature.auth.domain.di

import app.vigil.feature.auth.data.AuthRepository
import app.vigil.feature.auth.data.SessionRepository
import app.vigil.feature.auth.domain.usecase.LoginUseCase
import app.vigil.feature.auth.domain.usecase.ObserveSessionExpiredUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Pure Kotlin module (hilt-core only): SingletonComponent, unscoped, so every ViewModel gets its own instance.
@Module
@InstallIn(SingletonComponent::class)
internal object AuthUseCaseModule {
    @Provides
    fun provideLoginUseCase(repository: AuthRepository) = LoginUseCase(repository::login)

    @Provides
    fun provideObserveSessionExpiredUseCase(repository: SessionRepository) =
        ObserveSessionExpiredUseCase(repository::observeSessionExpired)
}
