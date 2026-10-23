package app.vigil.common.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Result of every repository call. Repositories never throw; failures are an [AppError]. */
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
