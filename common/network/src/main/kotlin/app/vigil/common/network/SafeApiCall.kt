package app.vigil.common.network

import app.vigil.common.core.ApiErrorBody
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.JsonConvertException
import java.io.IOException
import java.net.SocketTimeoutException
import java.nio.channels.UnresolvedAddressException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import timber.log.Timber

private const val TAG = "Network"

/** Runs a network call. Every failure becomes an [AppError.Network]; cancellation is rethrown. */
@Suppress("TooGenericExceptionCaught") // every failure becomes an AppError; cancellation is rethrown
suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toNetworkError())
    }

/** Maps a failed call to an [AppError.Network] and logs it (the wrappers are the only place errors are logged). */
suspend fun Exception.toNetworkError(): AppError.Network {
    val error = when (this) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
            AppError.Network.Timeout(this)

        is UnresolvedAddressException, is IOException -> AppError.Network.NoConnection(this)

        is ResponseException -> AppError.Network.Http(
            code = response.status.value,
            body = response.errorBodyOrNull(),
            cause = this,
        )

        is SerializationException, is JsonConvertException -> AppError.Network.Serialization(this)

        else -> AppError.Network.Unknown(this)
    }
    if (error is AppError.Network.Unknown) {
        Timber.tag(TAG).e(this, "Unexpected network error")
    } else {
        Timber.tag(TAG).w(this, "Network call failed: %s", error::class.simpleName)
    }
    return error
}

/** The server's error payload, or null when the body is missing or has another shape. */
@Suppress("TooGenericExceptionCaught", "SwallowedException") // a missing or odd error body is normal
suspend fun HttpResponse.errorBodyOrNull(): ApiErrorBody? =
    try {
        body<ApiErrorBody>()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
