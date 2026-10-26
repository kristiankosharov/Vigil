package app.vigil.feature.applock.domain.usecase

import app.vigil.common.core.AppResult
import app.vigil.feature.applock.domain.model.BiometricCipher

/**
 * Callers check [app.vigil.feature.applock.domain.model.PinRules] first. [biometric] is a cipher from
 * [PrepareBiometricEnrollmentUseCase] that BiometricPrompt authenticated, or null for PIN only.
 */
public fun interface CreatePinUseCase {
    public suspend operator fun invoke(pin: CharArray, biometric: BiometricCipher?): AppResult<Unit>
}
