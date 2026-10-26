package app.vigil.feature.auth.domain.usecase

import app.vigil.common.core.AppResult

public fun interface SendPasswordResetUseCase {
    public suspend operator fun invoke(email: String): AppResult<Unit>
}
