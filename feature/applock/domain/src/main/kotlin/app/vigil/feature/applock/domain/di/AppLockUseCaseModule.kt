package app.vigil.feature.applock.domain.di

import app.vigil.common.core.mapResult
import app.vigil.feature.applock.data.AppLockRepository
import app.vigil.feature.applock.domain.model.AppLockState
import app.vigil.feature.applock.domain.usecase.CreatePinUseCase
import app.vigil.feature.applock.domain.usecase.IsBiometricAvailableUseCase
import app.vigil.feature.applock.domain.usecase.MarkAppBackgroundedUseCase
import app.vigil.feature.applock.domain.usecase.MarkAppForegroundedUseCase
import app.vigil.feature.applock.domain.usecase.ObserveAppLockStateUseCase
import app.vigil.feature.applock.domain.usecase.UnlockWithBiometricUseCase
import app.vigil.feature.applock.domain.usecase.UnlockWithPinUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Pure Kotlin module (hilt-core only): SingletonComponent, unscoped, so every ViewModel gets its own instance.
@Module
@InstallIn(SingletonComponent::class)
internal object AppLockUseCaseModule {
    @Provides
    fun provideObserveAppLockStateUseCase(repository: AppLockRepository) = ObserveAppLockStateUseCase {
        repository.observeLockState().mapResult { AppLockState(it.isLocked, it.isBiometricEnabled) }
    }

    @Provides
    fun provideCreatePinUseCase(repository: AppLockRepository) = CreatePinUseCase(repository::createPin)

    @Provides
    fun provideUnlockWithPinUseCase(repository: AppLockRepository) = UnlockWithPinUseCase(repository::unlockWithPin)

    @Provides
    fun provideUnlockWithBiometricUseCase(repository: AppLockRepository) =
        UnlockWithBiometricUseCase(repository::unlockWithBiometric)

    @Provides
    fun provideMarkAppBackgroundedUseCase(repository: AppLockRepository) =
        MarkAppBackgroundedUseCase(repository::onAppBackgrounded)

    @Provides
    fun provideMarkAppForegroundedUseCase(repository: AppLockRepository) =
        MarkAppForegroundedUseCase(repository::onAppForegrounded)

    @Provides
    fun provideIsBiometricAvailableUseCase(repository: AppLockRepository) =
        IsBiometricAvailableUseCase(repository::isBiometricAvailable)
}
