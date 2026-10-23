package app.vigil.common.core

import kotlinx.serialization.Serializable

/** Every failure, structured by source. The UI maps it to a message with `toUiText()` (:common:ui). */
sealed interface AppError {
    val cause: Throwable?

    sealed interface Network : AppError {
        data class NoConnection(override val cause: Throwable?) : Network

        data class Timeout(override val cause: Throwable?) : Network

        data class Http(
            val code: Int,
            val body: ApiErrorBody?, // parsed server error payload, if any
            override val cause: Throwable?,
        ) : Network

        data class Serialization(override val cause: Throwable?) : Network

        data class Unknown(override val cause: Throwable?) : Network
    }

    sealed interface Local : AppError {
        data object NotFound : Local {
            override val cause: Throwable? = null
        }

        data class Constraint(override val cause: Throwable?) : Local

        data class Database(override val cause: Throwable?) : Local

        data class Storage(override val cause: Throwable?) : Local // DataStore I/O failure

        data class Unknown(override val cause: Throwable?) : Local
    }

    data class Unknown(override val cause: Throwable?) : AppError
}

/** The error payload our API returns with non-2xx responses. */
@Serializable
data class ApiErrorBody(val code: String? = null, val message: String? = null)

/** Carries an [AppError] where only an exception fits, such as Paging's `LoadState.Error`. */
class AppErrorException(val error: AppError) : Exception(error.cause)
