package app.vigil.feature.auth.domain.usecase

import app.vigil.common.core.AppResult

public fun interface VerifyPhoneCodeUseCase {
    public suspend operator fun invoke(verificationId: String, code: String): AppResult<Unit>
}
