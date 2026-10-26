package app.vigil.feature.auth.data.repository

import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.core.map
import app.vigil.feature.auth.data.AuthRepository
import app.vigil.feature.auth.data.OidcProviderData
import app.vigil.feature.auth.data.PhoneVerificationData
import app.vigil.feature.auth.data.remote.CurrentActivityTracker
import app.vigil.feature.auth.data.remote.OidcProviders
import app.vigil.feature.auth.data.remote.safeFirebaseCall
import app.vigil.feature.auth.data.remote.toAuthError
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
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

// TODO(auth): after every sign-in, exchange the Firebase ID token (`auth.currentUser?.getIdToken(false)`)
//  for the backend's tokens, TokenStorage.save(...) them (sealed with the vault key once the PIN
//  exists), then `auth.signOut()`: Firebase keeps its own refresh token outside the vault, so the
//  PIN can't protect it. Until the backend exists the backend calls go out without a token.
internal class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val activityTracker: CurrentActivityTracker,
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): AppResult<Unit> =
        safeFirebaseCall { auth.signInWithEmailAndPassword(email, password).await() }.map { }

    override suspend fun signUpWithEmail(email: String, password: String): AppResult<Unit> =
        safeFirebaseCall { auth.createUserWithEmailAndPassword(email, password).await() }.map { }

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
        val activity = activityTracker.current
        if (activity == null) {
            send(AppResult.Failure(noActivityError()))
            channel.close()
        } else {
            PhoneAuthProvider.verifyPhoneNumber(
                PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(PHONE_CODE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build(),
            )
        }
        awaitClose()
    }.catch { cause ->
        if (cause is CancellationException || cause !is Exception) throw cause
        emit(AppResult.Failure(cause.toAuthError()))
    }

    override suspend fun verifyPhoneCode(verificationId: String, code: String): AppResult<Unit> =
        signIn(PhoneAuthProvider.getCredential(verificationId, code))

    override fun oidcProviders(): List<OidcProviderData> = OidcProviders

    // TODO(oidc): on start, finish a sign-in the system interrupted (the activity was recreated while
    //  the provider's page was open): `auth.pendingAuthResult?.await()`.
    override suspend fun signInWithOidc(providerId: String): AppResult<Unit> {
        val activity = activityTracker.current ?: return AppResult.Failure(noActivityError())
        val provider = OAuthProvider.newBuilder(providerId)
            .setScopes(listOf("email", "profile")) // TODO(oidc): the scopes the backend needs ("openid" is implied)
            .build()
        return safeFirebaseCall { auth.startActivityForSignInWithProvider(activity, provider).await() }.map { }
    }

    private suspend fun signIn(credential: AuthCredential): AppResult<Unit> =
        safeFirebaseCall { auth.signInWithCredential(credential).await() }.map { }

    private fun noActivityError() = AppError.Auth.Unknown(IllegalStateException("No resumed activity"))
}
