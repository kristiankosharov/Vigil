package app.vigil.feature.applock.domain.usecase

import app.vigil.common.core.AppResult
import app.vigil.feature.applock.domain.model.BiometricCipher

public fun interface PrepareBiometricEnrollmentUseCase {
    public operator fun invoke(): AppResult<BiometricCipher>
}
