package app.vigil.common.datastore

import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.core.asRetryingResult
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

private const val TAG = "DataStore"

/** Runs a DataStore write or one-shot read. Every failure becomes an [AppError.Local]; cancellation is rethrown. */
@Suppress("TooGenericExceptionCaught") // every failure becomes an AppError; cancellation is rethrown
suspend inline fun <T> safeStoreCall(crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toStoreError())
    }

/** An observed store as results: failures are emitted and the store re-subscribed (`asRetryingResult`). */
fun <T> Flow<T>.asStoreResult(): Flow<AppResult<T>> = asRetryingResult(mapError = { it.toStoreError() })

/**
 * Maps a failed DataStore call to an [AppError.Local] and logs it (the wrappers are the only place
 * errors are logged).
 */
fun Exception.toStoreError(): AppError.Local {
    val error = when (this) {
        is IOException -> AppError.Local.Storage(this)
        else -> AppError.Local.Unknown(this)
    }
    if (error is AppError.Local.Unknown) {
        Timber.tag(TAG).e(this, "Unexpected DataStore error")
    } else {
        Timber.tag(TAG).w(this, "DataStore call failed")
    }
    return error
}
