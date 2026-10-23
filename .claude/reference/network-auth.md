# Network and auth

Rules: `.claude/rules/network-auth.md`.

## Interfaces (`:common:network`)

```kotlin
data class AuthTokens(val accessToken: String, val refreshToken: String)

interface TokenStorage {                       // implemented in :feature:auth:data:core
    suspend fun read(): AuthTokens?
    suspend fun save(tokens: AuthTokens)
    suspend fun clear()
}

interface TokenRefresher {                     // implemented in :feature:auth:data:core
    suspend fun refresh(refreshToken: String): AppResult<AuthTokens>
}

interface SessionEvents {                      // implemented here, @Singleton
    val expired: Flow<Unit>
    fun notifyExpired()
}
```

## Ktor `Auth` plugin

```kotlin
install(Auth) {
    bearer {
        loadTokens {
            tokenStorage.read()?.let { BearerTokens(it.accessToken, it.refreshToken) }
        }
        refreshTokens {
            val refreshToken = oldTokens?.refreshToken
            if (refreshToken == null) {
                sessionEvents.notifyExpired()
                return@refreshTokens null
            }
            when (val result = tokenRefresher.refresh(refreshToken)) {
                is AppResult.Success -> {
                    tokenStorage.save(result.data)
                    BearerTokens(result.data.accessToken, result.data.refreshToken)
                }
                is AppResult.Failure -> {
                    // Log out only if the server rejected the refresh token, never on network errors.
                    val error = result.error
                    if (error is AppError.Network.Http && error.code in 400..401) {
                        tokenStorage.clear()
                        sessionEvents.notifyExpired()
                    }
                    null
                }
            }
        }
    }
}
```
