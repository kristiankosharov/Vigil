package app.vigil.feature.auth.domain.di

import app.vigil.common.core.mapResult
import app.vigil.feature.auth.data.AuthRepository
import app.vigil.feature.auth.data.SessionRepository
import app.vigil.feature.auth.domain.mapper.toDomain
import app.vigil.feature.auth.domain.usecase.GetOidcProvidersUseCase
import app.vigil.feature.auth.domain.usecase.ObserveSessionExpiredUseCase
import app.vigil.feature.auth.domain.usecase.RequestPhoneCodeUseCase
import app.vigil.feature.auth.domain.usecase.SendPasswordResetUseCase
import app.vigil.feature.auth.domain.usecase.SignInWithEmailUseCase
import app.vigil.feature.auth.domain.usecase.SignInWithOidcUseCase
import app.vigil.feature.auth.domain.usecase.SignUpWithEmailUseCase
import app.vigil.feature.auth.domain.usecase.VerifyPhoneCodeUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Pure Kotlin module (hilt-core only): SingletonComponent, unscoped, so every ViewModel gets its own instance.
@Module
@InstallIn(SingletonComponent::class)
internal object AuthUseCaseModule {
    @Provides
    fun provideSignInWithEmailUseCase(repository: AuthRepository) = SignInWithEmailUseCase(repository::signInWithEmail)

    @Provides
    fun provideSignUpWithEmailUseCase(repository: AuthRepository) = SignUpWithEmailUseCase(repository::signUpWithEmail)

    @Provides
    fun provideSendPasswordResetUseCase(repository: AuthRepository) =
        SendPasswordResetUseCase(repository::sendPasswordReset)

    @Provides
    fun provideRequestPhoneCodeUseCase(repository: AuthRepository) =
        RequestPhoneCodeUseCase { phoneNumber -> repository.requestPhoneCode(phoneNumber).mapResult { it.toDomain() } }

    @Provides
    fun provideVerifyPhoneCodeUseCase(repository: AuthRepository) = VerifyPhoneCodeUseCase(repository::verifyPhoneCode)

    @Provides
    fun provideGetOidcProvidersUseCase(repository: AuthRepository) =
        GetOidcProvidersUseCase { repository.oidcProviders().map { it.toDomain() } }

    @Provides
    fun provideSignInWithOidcUseCase(repository: AuthRepository) = SignInWithOidcUseCase(repository::signInWithOidc)

    @Provides
    fun provideObserveSessionExpiredUseCase(repository: SessionRepository) =
        ObserveSessionExpiredUseCase(repository::observeSessionExpired)
}
