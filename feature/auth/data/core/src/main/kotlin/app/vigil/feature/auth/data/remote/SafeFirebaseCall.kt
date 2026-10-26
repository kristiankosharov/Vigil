package app.vigil.feature.auth.data.remote

import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthWebException
import kotlin.coroutines.cancellation.CancellationException
import timber.log.Timber

private const val TAG = "Auth"

/**
 * Runs a Firebase Auth call: the auth feature's counterpart of `safeApiCall`. Every failure becomes
 * an [AppError.Auth] (or [AppError.Network.NoConnection]); cancellation is rethrown.
 */
@Suppress("TooGenericExceptionCaught") // every failure becomes an AppError; cancellation is rethrown
internal suspend inline fun <T> safeFirebaseCall(crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toAuthError())
    }

/** Maps a Firebase Auth failure and logs it (only here: the wrapper is where errors are logged). */
internal fun Exception.toAuthError(): AppError {
    val error = when (this) {
        is FirebaseNetworkException -> AppError.Network.NoConnection(this)

        is FirebaseTooManyRequestsException -> AppError.Auth.TooManyRequests(this)

        // Before its parent FirebaseAuthInvalidCredentialsException.
        is FirebaseAuthWeakPasswordException -> AppError.Auth.WeakPassword(this)

        is FirebaseAuthInvalidCredentialsException -> when (errorCode) {
            "ERROR_INVALID_PHONE_NUMBER", "ERROR_MISSING_PHONE_NUMBER" -> AppError.Auth.InvalidPhoneNumber(this)
            "ERROR_INVALID_VERIFICATION_CODE", "ERROR_SESSION_EXPIRED" -> AppError.Auth.InvalidCode(this)
            else -> AppError.Auth.InvalidCredentials(this)
        }

        is FirebaseAuthInvalidUserException -> AppError.Auth.InvalidCredentials(this)

        is FirebaseAuthUserCollisionException -> AppError.Auth.AccountExists(this)

        is FirebaseAuthWebException -> when (errorCode) {
            "ERROR_WEB_CONTEXT_CANCELED" -> AppError.Auth.Cancelled(this)
            else -> AppError.Auth.Unknown(this)
        }

        else -> AppError.Auth.Unknown(this)
    }
    if (error is AppError.Auth.Unknown) {
        Timber.tag(TAG).e(this, "Unexpected auth error")
    } else {
        // No throwable: Firebase messages may name the account.
        Timber.tag(TAG).w("Auth call failed: %s", error::class.simpleName)
    }
    return error
}
