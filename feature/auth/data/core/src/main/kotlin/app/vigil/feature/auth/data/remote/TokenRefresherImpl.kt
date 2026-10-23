package app.vigil.feature.auth.data.remote

import app.vigil.common.core.AppResult
import app.vigil.common.core.map
import app.vigil.common.network.AuthTokens
import app.vigil.common.network.TokenRefresher
import app.vigil.common.network.Unauthenticated
import app.vigil.common.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import kotlinx.serialization.Serializable

/** Uses the client without the Auth plugin, so a refresh can never trigger another refresh. */
internal class TokenRefresherImpl @Inject constructor(@param:Unauthenticated private val client: HttpClient) :
    TokenRefresher {

    // TODO(auth): the real endpoint and payload once the backend exists.
    override suspend fun refresh(refreshToken: String): AppResult<AuthTokens> = safeApiCall {
        client.post("auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequestDto(refreshToken))
        }.body<TokensDto>()
    }.map { it.toTokens() }
}

@Serializable
internal data class RefreshRequestDto(val refreshToken: String) {
    override fun toString() = "RefreshRequestDto(***)"
}

@Serializable
internal data class TokensDto(val accessToken: String, val refreshToken: String) {
    override fun toString() = "TokensDto(***)"

    fun toTokens() = AuthTokens(accessToken = accessToken, refreshToken = refreshToken)
}
