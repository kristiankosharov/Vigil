package app.vigil.feature.auth.data.repository

import app.vigil.common.core.AppResult
import app.vigil.common.core.map
import app.vigil.feature.auth.data.AuthRepository
import app.vigil.feature.auth.data.OidcProviderData
import app.vigil.feature.auth.data.PhoneVerificationData
import app.vigil.feature.auth.data.remote.OidcProviders
import app.vigil.feature.auth.data.remote.safeFirebaseCall
import app.vigil.feature.auth.data.remote.toAuthError
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val PHONE_CODE_TIMEOUT_SECONDS = 60L

/** Signs in with Firebase, then starts the backend session ([BackendSession]). */
internal class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val backendSession: BackendSession,
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): AppResult<Unit> =
        safeFirebaseCall { auth.signInWithEmailAndPassword(email, password).await() }.thenStartSession()

    override suspend fun signUpWithEmail(email: String, password: String): AppResult<Unit> =
        safeFirebaseCall { auth.createUserWithEmailAndPassword(email, password).await() }.thenStartSession()

    // TODO(auth): email verification (`currentUser.sendEmailVerification()`) if the backend requires it.
    override suspend fun sendPasswordReset(email: String): AppResult<Unit> =
        safeFirebaseCall { auth.sendPasswordResetEmail(email).await() }.map { }

    // TODO(auth): "Resend code" with the ForceResendingToken from onCodeSent (setForceResendingToken).
    override fun requestPhoneCode(phoneNumber: String): Flow<AppResult<PhoneVerificationData>> = callbackFlow {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                trySend(AppResult.Success(PhoneVerificationData.CodeSent(verificationId)))
            }

            // Instant verification, or the device read the SMS itself: sign in without the user typing it.
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                launch {
                    send(signIn(credential).map { PhoneVerificationData.Verified })
                    channel.close()
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                trySend(AppResult.Failure(e.toAuthError()))
                channel.close()
            }

            // The code can still be typed; nothing more will come from this request.
            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                channel.close()
            }
        }
        // TODO(auth): setActivity(...) for the reCAPTCHA fallback; without it Firebase fails that case
        //  through onVerificationFailed instead of showing the reCAPTCHA.
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(PHONE_CODE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setCallbacks(callbacks)
                .build(),
        )
        awaitClose()
    }.catch { cause ->
        if (cause is CancellationException || cause !is Exception) throw cause
        emit(AppResult.Failure(cause.toAuthError()))
    }

    override suspend fun verifyPhoneCode(verificationId: String, code: String): AppResult<Unit> =
        signIn(PhoneAuthProvider.getCredential(verificationId, code))

    override fun oidcProviders(): List<OidcProviderData> = OidcProviders

    // TODO(oidc): sign in with `auth.startActivityForSignInWithProvider(activity, provider)`, where
    //  `provider = OAuthProvider.newBuilder(providerId).setScopes(...)` with the scopes the backend
    //  needs. It needs the resumed Activity, which data:core doesn't have yet. On start, also finish a
    //  sign-in the system interrupted: `auth.pendingAuthResult?.await()`. Then `.thenStartSession()`.
    override suspend fun signInWithOidc(providerId: String): AppResult<Unit> = AppResult.Success(Unit)

    private suspend fun signIn(credential: AuthCredential): AppResult<Unit> =
        safeFirebaseCall { auth.signInWithCredential(credential).await() }.thenStartSession()

    private suspend fun AppResult<*>.thenStartSession(): AppResult<Unit> = when (this) {
        is AppResult.Failure -> this
        is AppResult.Success -> backendSession.start()
    }
}
