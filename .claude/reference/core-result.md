# AppResult, AppError and the I/O wrappers

Rules: `.claude/rules/data-layer.md`. Once these exist in `:common:*`, the source files win.

## `AppResult` (`:common:core`)

```kotlin
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T, R> Flow<AppResult<T>>.mapResult(crossinline transform: (T) -> R): Flow<AppResult<R>> =
    map { result -> result.map(transform) }

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> =
    also { if (it is AppResult.Success) action(it.data) }

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> =
    also { if (it is AppResult.Failure) action(it.error) }
```

## `AppError` (`:common:core`)

```kotlin
sealed interface AppError {
    val cause: Throwable?

    sealed interface Network : AppError {
        data class NoConnection(override val cause: Throwable?) : Network
        data class Timeout(override val cause: Throwable?) : Network
        data class Http(
            val code: Int,
            val body: ApiErrorBody?,          // parsed server error payload, if any
            override val cause: Throwable?,
        ) : Network
        data class Serialization(override val cause: Throwable?) : Network
        data class Unknown(override val cause: Throwable?) : Network
    }

    sealed interface Local : AppError {
        data object NotFound : Local { override val cause: Throwable? = null }
        data class Constraint(override val cause: Throwable?) : Local
        data class Database(override val cause: Throwable?) : Local
        data class Storage(override val cause: Throwable?) : Local      // DataStore I/O failure
        data class Unknown(override val cause: Throwable?) : Local
    }

    data class Unknown(override val cause: Throwable?) : AppError
}

@Serializable
data class ApiErrorBody(val code: String? = null, val message: String? = null)
```

## `safeApiCall` (`:common:network`)

```kotlin
@Suppress("TooGenericExceptionCaught") // every failure becomes an AppError; cancellation is rethrown
suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toNetworkError())
    }

suspend fun Exception.toNetworkError(): AppError.Network = when (this) {
    is HttpRequestTimeoutException, is SocketTimeoutException -> AppError.Network.Timeout(this)
    is UnresolvedAddressException, is IOException -> AppError.Network.NoConnection(this)
    is ResponseException -> AppError.Network.Http(
        code = response.status.value,
        body = response.errorBodyOrNull(),
        cause = this,
    )
    is SerializationException, is JsonConvertException -> AppError.Network.Serialization(this)
    else -> AppError.Network.Unknown(this)
}

@Suppress("TooGenericExceptionCaught", "SwallowedException") // a missing or odd error body is normal
suspend fun HttpResponse.errorBodyOrNull(): ApiErrorBody? =
    try {
        body<ApiErrorBody>()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
```

## `asRetryingResult` (`:common:core`)

```kotlin
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
    maxRetries: Int = 3,
    retryDelay: Duration = 500.milliseconds,
): Flow<AppResult<T>> = flow {
    var consecutiveFailures = 0
    emitAll(
        this@asRetryingResult
            .onEach { consecutiveFailures = 0 }                  // success → reset
            .map<T, AppResult<T>> { AppResult.Success(it) }
            .retryWhen { cause, _ ->                            // upstream errors only
                if (cause is CancellationException || cause !is Exception) return@retryWhen false
                emit(AppResult.Failure(mapError(cause)))
                if (consecutiveFailures >= maxRetries) return@retryWhen false
                consecutiveFailures++
                delay(retryDelay * consecutiveFailures)
                true
            }
            .catch { cause ->                                   // failure already emitted above
                if (cause is CancellationException || cause !is Exception) throw cause
            },
    )
}
```

## `safeStoreCall` / `asStoreResult` (`:common:datastore`)

```kotlin
@Suppress("TooGenericExceptionCaught") // every failure becomes an AppError; cancellation is rethrown
suspend inline fun <T> safeStoreCall(crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toStoreError())
    }

fun <T> Flow<T>.asStoreResult(): Flow<AppResult<T>> = asRetryingResult(mapError = { it.toStoreError() })

fun Exception.toStoreError(): AppError.Local = when (this) {
    is IOException -> AppError.Local.Storage(this)
    else -> AppError.Local.Unknown(this)
}
```
