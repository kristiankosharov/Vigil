package app.vigil.feature.auth.data

import app.vigil.common.core.AppResult

public interface AuthRepository {
    // TODO(auth): take the credentials (or a sign-in token) once the login form and backend exist.
    public suspend fun login(): AppResult<Unit>
}
