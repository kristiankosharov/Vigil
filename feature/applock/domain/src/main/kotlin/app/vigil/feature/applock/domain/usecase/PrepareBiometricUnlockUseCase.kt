package app.vigil.feature.applock.domain.usecase

import app.vigil.common.core.AppResult
import app.vigil.feature.applock.domain.model.BiometricCipher

/** Null when fingerprint unlock is off or was turned off because the enrolled fingerprints changed. */
public fun interface PrepareBiometricUnlockUseCase {
    public suspend operator fun invoke(): AppResult<BiometricCipher?>
}
