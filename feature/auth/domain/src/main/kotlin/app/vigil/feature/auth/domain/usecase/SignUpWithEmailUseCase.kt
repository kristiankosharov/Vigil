package app.vigil.feature.auth.domain.usecase

import app.vigil.common.core.AppResult

/** Creates the account and signs in. */
public fun interface SignUpWithEmailUseCase {
    public suspend operator fun invoke(email: String, password: String): AppResult<Unit>
}
