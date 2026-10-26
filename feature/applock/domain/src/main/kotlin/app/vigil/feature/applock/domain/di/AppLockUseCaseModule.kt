package app.vigil.feature.applock.domain.di

import app.vigil.common.core.map
import app.vigil.common.core.mapResult
import app.vigil.feature.applock.data.AppLockRepository
import app.vigil.feature.applock.data.BiometricCipherData
import app.vigil.feature.applock.data.PinCheckData
import app.vigil.feature.applock.domain.model.AppLockState
import app.vigil.feature.applock.domain.model.BiometricCipher
import app.vigil.feature.applock.domain.model.PinCheck
import app.vigil.feature.applock.domain.usecase.CreatePinUseCase
import app.vigil.feature.applock.domain.usecase.IsBiometricAvailableUseCase
import app.vigil.feature.applock.domain.usecase.MarkAppBackgroundedUseCase
import app.vigil.feature.applock.domain.usecase.MarkAppForegroundedUseCase
import app.vigil.feature.applock.domain.usecase.ObserveAppLockStateUseCase
import app.vigil.feature.applock.domain.usecase.PrepareBiometricEnrollmentUseCase
import app.vigil.feature.applock.domain.usecase.PrepareBiometricUnlockUseCase
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
        repository.observeLockState().mapResult { AppLockState(it.isLocked, it.isBiometricEnabled, it.retryAfter) }
    }

    @Provides
    fun provideCreatePinUseCase(repository: AppLockRepository) = CreatePinUseCase { pin, biometric ->
        repository.createPin(pin, biometric?.let { BiometricCipherData(it.cipher) })
    }

    @Provides
    fun provideUnlockWithPinUseCase(repository: AppLockRepository) = UnlockWithPinUseCase { pin ->
        repository.unlockWithPin(pin).map { check ->
            when (check) {
                PinCheckData.Correct -> PinCheck.Correct
                is PinCheckData.Wrong -> PinCheck.Wrong(check.attemptsLeft, check.retryAfter)
                PinCheckData.Erased -> PinCheck.Erased
            }
        }
    }

    @Provides
    fun providePrepareBiometricEnrollmentUseCase(repository: AppLockRepository) = PrepareBiometricEnrollmentUseCase {
        repository.prepareBiometricEnrollment().map { BiometricCipher(it.cipher) }
    }

    @Provides
    fun providePrepareBiometricUnlockUseCase(repository: AppLockRepository) = PrepareBiometricUnlockUseCase {
        repository.prepareBiometricUnlock().map { data -> data?.let { BiometricCipher(it.cipher) } }
    }

    @Provides
    fun provideUnlockWithBiometricUseCase(repository: AppLockRepository) = UnlockWithBiometricUseCase { biometric ->
        repository.unlockWithBiometric(BiometricCipherData(biometric.cipher))
    }

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
