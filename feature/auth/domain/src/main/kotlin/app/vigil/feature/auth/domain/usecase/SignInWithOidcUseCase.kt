package app.vigil.feature.auth.domain.usecase

import app.vigil.common.core.AppResult

/** Signs in with an OpenID Connect provider; `AppError.Auth.Cancelled` when the user backs out. */
public fun interface SignInWithOidcUseCase {
    public suspend operator fun invoke(providerId: String): AppResult<Unit>
}
