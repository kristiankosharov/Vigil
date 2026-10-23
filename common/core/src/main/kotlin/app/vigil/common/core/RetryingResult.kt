package app.vigil.common.core

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen

private const val DEFAULT_MAX_RETRIES = 3
private val DEFAULT_RETRY_DELAY = 500.milliseconds

/**
 * Wraps an observed local flow (Room or DataStore). A plain `.catch` would complete the flow after
 * the first error and the screen would never update again, so each failure is emitted and the
 * source is re-subscribed with a growing delay.
 *
 * The failure counter **resets after every successful emission**, so only [maxRetries]
 * *consecutive* failures end the flow. (`retryWhen`'s own `attempt` never resets, so rare errors
 * spread over hours would eventually stop the flow for good.) When the flow does end, the last
 * failure stays emitted; the ViewModel subscribes again on its `Refresh` action.
 *
 * The counter lives inside `flow { }`, so every collector gets its own.
 */
fun <T> Flow<T>.asRetryingResult(
    mapError: (Exception) -> AppError,
    maxRetries: Int = DEFAULT_MAX_RETRIES,
    retryDelay: Duration = DEFAULT_RETRY_DELAY,
): Flow<AppResult<T>> = flow {
    var consecutiveFailures = 0
    emitAll(
        this@asRetryingResult
            .onEach { consecutiveFailures = 0 } // success → reset
            .map<T, AppResult<T>> { AppResult.Success(it) }
            .retryWhen { cause, _ ->
                // upstream errors only
                if (cause is CancellationException || cause !is Exception) return@retryWhen false
                emit(AppResult.Failure(mapError(cause)))
                if (consecutiveFailures >= maxRetries) return@retryWhen false
                consecutiveFailures++
                delay(retryDelay * consecutiveFailures)
                true
            }
            .catch { cause ->
                // the failure was already emitted above
                if (cause is CancellationException || cause !is Exception) throw cause
            },
    )
}
