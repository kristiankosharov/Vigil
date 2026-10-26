package app.vigil.feature.applock.domain.usecase

import app.vigil.common.core.AppResult
import app.vigil.feature.applock.domain.model.BiometricCipher

/** [biometric] comes from [PrepareBiometricUnlockUseCase] and was authenticated by BiometricPrompt. */
public fun interface UnlockWithBiometricUseCase {
    public suspend operator fun invoke(biometric: BiometricCipher): AppResult<Unit>
}
