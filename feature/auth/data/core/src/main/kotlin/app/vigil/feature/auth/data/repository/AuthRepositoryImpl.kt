package app.vigil.feature.auth.data.repository

import app.vigil.common.core.AppResult
import app.vigil.feature.auth.data.AuthRepository
import javax.inject.Inject

internal class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    // TODO(auth): call the login endpoint with the @Unauthenticated client through safeApiCall, then
    //  TokenStorage.save(tokens). Until the backend exists, login always succeeds without a call.
    override suspend fun login(): AppResult<Unit> = AppResult.Success(Unit)
}
