package app.vigil.feature.auth.domain.usecase

import app.vigil.common.core.AppResult

public fun interface LoginUseCase {
    public suspend operator fun invoke(): AppResult<Unit>
}
