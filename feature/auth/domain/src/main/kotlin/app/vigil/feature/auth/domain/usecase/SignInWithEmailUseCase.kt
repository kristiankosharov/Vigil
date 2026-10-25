package app.vigil.feature.auth.domain.usecase

import app.vigil.common.core.AppResult

public fun interface SignInWithEmailUseCase {
    public suspend operator fun invoke(email: String, password: String): AppResult<Unit>
}
