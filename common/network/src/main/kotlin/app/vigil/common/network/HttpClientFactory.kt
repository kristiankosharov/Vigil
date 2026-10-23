package app.vigil.common.network

import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber

private const val REQUEST_TIMEOUT_MS = 30_000L
private const val CONNECT_TIMEOUT_MS = 15_000L

/** The server rejected the refresh token itself (as opposed to a network or server failure). */
private val RejectedRefreshCodes = setOf(HttpStatusCode.BadRequest.value, HttpStatusCode.Unauthorized.value)

internal val NetworkJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * The shared client setup: JSON, timeouts, base URL, and `expectSuccess = true` so non-2xx responses
 * throw `ResponseException` and are mapped by [safeApiCall]. Logs headers through Timber in debug
 * builds only, never bodies, with the Authorization header hidden.
 */
internal fun createHttpClient(
    baseUrl: String,
    isDebugBuild: Boolean,
    engine: HttpClientEngine = OkHttp.create(),
    configure: HttpClientConfig<*>.() -> Unit = {},
): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(NetworkJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
    }
    defaultRequest { url(baseUrl) }
    if (isDebugBuild) {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Timber.tag("Ktor").d(message)
                }
            }
            level = LogLevel.HEADERS
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
    }
    configure()
}

/**
 * Bearer tokens from [TokenStorage]. Ktor's bearer provider already makes concurrent 401s share one
 * refresh: never add another lock or retry loop.
 */
internal fun HttpClientConfig<*>.installBearerAuth(
    tokenStorage: TokenStorage,
    tokenRefresher: TokenRefresher,
    sessionEvents: SessionEvents,
) {
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
                        if (error is AppError.Network.Http && error.code in RejectedRefreshCodes) {
                            tokenStorage.clear()
                            sessionEvents.notifyExpired()
                        }
                        null
                    }
                }
            }
        }
    }
}
