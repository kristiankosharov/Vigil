package app.vigil.feature.auth.domain.model

public sealed interface PhoneVerification {
    /** The SMS is on its way; verify the code the user types with [verificationId]. */
    public data class CodeSent(val verificationId: String) : PhoneVerification

    /** Verified without typing the code: the user is signed in. */
    public data object Verified : PhoneVerification
}
