package app.vigil.feature.auth.data.remote

import app.vigil.common.core.AppResult
import app.vigil.common.network.AuthTokens
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/** The backend's sign-in: it trusts Firebase's ID token and issues its own tokens. */
internal interface AuthApi {
    suspend fun exchangeFirebaseToken(idToken: String): AppResult<AuthTokens>
}

/**
 * A fake backend until the real one exists: any ID token gets a new pair of random tokens, after a
 * short delay like a network call.
 */
// TODO(auth): the real call, with the @Unauthenticated client (there are no tokens yet):
//  safeApiCall { client.post("auth/firebase") { contentType(Json); setBody(ExchangeRequestDto(idToken)) }
//  .body<TokensDto>() }.map { it.toTokens() }
internal class AuthApiImpl @Inject constructor() : AuthApi {
    override suspend fun exchangeFirebaseToken(idToken: String): AppResult<AuthTokens> {
        delay(FAKE_LATENCY)
        return AppResult.Success(
            AuthTokens(
                accessToken = "fake-access-${UUID.randomUUID()}",
                refreshToken = "fake-refresh-${UUID.randomUUID()}",
            ),
        )
    }

    private companion object {
        val FAKE_LATENCY = 300.milliseconds
    }
}
