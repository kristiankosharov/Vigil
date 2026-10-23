package app.vigil.common.network

import app.vigil.common.core.AppResult
import kotlinx.coroutines.flow.Flow

data class AuthTokens(val accessToken: String, val refreshToken: String) {
    // Tokens must never reach a log through a data class's generated toString().
    override fun toString(): String = "AuthTokens(***)"
}

/** Where the tokens live. Implemented in :feature:auth:data:core on the auth feature's encrypted DataStore. */
interface TokenStorage {
    suspend fun read(): AuthTokens?

    suspend fun save(tokens: AuthTokens)

    suspend fun clear()
}

/**
 * Exchanges a refresh token for new tokens. Implemented in :feature:auth:data:core with the
 * @Unauthenticated client.
 */
interface TokenRefresher {
    suspend fun refresh(refreshToken: String): AppResult<AuthTokens>
}

/** Session expiry, raised by the Auth plugin and observed by :feature:auth. */
interface SessionEvents {
    val expired: Flow<Unit>

    fun notifyExpired()
}
