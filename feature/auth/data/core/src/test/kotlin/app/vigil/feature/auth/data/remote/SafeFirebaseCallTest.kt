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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

class SafeFirebaseCallTest : FunSpec({
    listOf<Pair<Exception, KClass<out AppError>>>(
        FirebaseNetworkException("offline") to AppError.Network.NoConnection::class,
        FirebaseTooManyRequestsException("slow down") to AppError.Auth.TooManyRequests::class,
        FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "weak", "short") to AppError.Auth.WeakPassword::class,
        FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "wrong") to
            AppError.Auth.InvalidCredentials::class,
        FirebaseAuthInvalidCredentialsException("ERROR_INVALID_PHONE_NUMBER", "phone") to
            AppError.Auth.InvalidPhoneNumber::class,
        FirebaseAuthInvalidCredentialsException("ERROR_INVALID_VERIFICATION_CODE", "code") to
            AppError.Auth.InvalidCode::class,
        FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "disabled") to AppError.Auth.InvalidCredentials::class,
        FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "taken") to AppError.Auth.AccountExists::class,
        FirebaseAuthWebException("ERROR_WEB_CONTEXT_CANCELED", "closed") to AppError.Auth.Cancelled::class,
        IllegalStateException("other") to AppError.Auth.Unknown::class,
    ).forEach { (exception, expected) ->
        test("${exception::class.simpleName} (${exception.message}) maps to ${expected.simpleName}") {
            val result = safeFirebaseCall { throw exception }
            result.shouldBeInstanceOf<AppResult.Failure>().error::class shouldBe expected
        }
    }

    test("cancellation is rethrown") {
        shouldThrow<CancellationException> { safeFirebaseCall { throw CancellationException("cancelled") } }
    }
})
