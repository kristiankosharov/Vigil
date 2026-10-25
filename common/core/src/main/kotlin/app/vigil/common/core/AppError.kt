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

    /**
     * Sign-in failures the UI must tell apart (wrong password vs. invalid code vs. throttled). Raised
     * by the auth feature's identity provider (Firebase Auth); everything else maps to [Network].
     */
    sealed interface Auth : AppError {
        /** Wrong email or password, or no such account (Firebase doesn't say which). */
        data class InvalidCredentials(override val cause: Throwable?) : Auth

        data class AccountExists(override val cause: Throwable?) : Auth

        data class WeakPassword(override val cause: Throwable?) : Auth

        data class InvalidPhoneNumber(override val cause: Throwable?) : Auth

        /** A wrong or expired SMS code. */
        data class InvalidCode(override val cause: Throwable?) : Auth

        data class TooManyRequests(override val cause: Throwable?) : Auth

        /** The user closed the provider's sign-in page (OpenID Connect). Not shown as an error. */
        data class Cancelled(override val cause: Throwable?) : Auth

        data class Unknown(override val cause: Throwable?) : Auth
    }

    data class Unknown(override val cause: Throwable?) : AppError
}

/** The error payload our API returns with non-2xx responses. */
@Serializable
data class ApiErrorBody(val code: String? = null, val message: String? = null)

/** Carries an [AppError] where only an exception fits, such as Paging's `LoadState.Error`. */
class AppErrorException(val error: AppError) : Exception(error.cause)
