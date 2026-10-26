package app.vigil.feature.auth.data

import app.vigil.common.core.AppResult
import kotlinx.coroutines.flow.Flow

/** Sign-in with the identity provider (Firebase Auth). Every call returns once the user is signed in or failed. */
public interface AuthRepository {
    public suspend fun signInWithEmail(email: String, password: String): AppResult<Unit>

    /** Creates the account and signs in. */
    public suspend fun signUpWithEmail(email: String, password: String): AppResult<Unit>

    public suspend fun sendPasswordReset(email: String): AppResult<Unit>

    /**
     * Sends an SMS code to [phoneNumber] (E.164, e.g. `+359888123456`). Emits [PhoneVerificationData.CodeSent],
     * then [PhoneVerificationData.Verified] if the device reads the SMS itself (or verifies instantly);
     * a failure ends the flow.
     */
    public fun requestPhoneCode(phoneNumber: String): Flow<AppResult<PhoneVerificationData>>

    public suspend fun verifyPhoneCode(verificationId: String, code: String): AppResult<Unit>

    /** The OpenID Connect providers offered on the login screen; empty until one is configured. */
    public fun oidcProviders(): List<OidcProviderData>

    /** Opens the provider's sign-in page; `AppError.Auth.Cancelled` when the user closes it. */
    public suspend fun signInWithOidc(providerId: String): AppResult<Unit>
}

public sealed interface PhoneVerificationData {
    public data class CodeSent(val verificationId: String) : PhoneVerificationData

    public data object Verified : PhoneVerificationData
}

/** [id] is the Firebase provider ID (`oidc.<name>`). */
public data class OidcProviderData(val id: String, val displayName: String)
