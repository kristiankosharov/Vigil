package app.vigil.feature.auth.data.local

import kotlinx.serialization.Serializable

/**
 * The auth feature's encrypted store (`auth_session.json`). Every field has a default. The tokens
 * are sealed with the vault key (`:common:security`), so they can be read only while the app is
 * unlocked: the PIN or fingerprint is part of what decrypts them.
 */
@Serializable
internal data class AuthSession(val sealedTokens: String? = null) {
    // Even sealed, tokens stay out of logs.
    override fun toString(): String = "AuthSession(hasTokens=${sealedTokens != null})"
}

/** What gets sealed: the JSON of this, never stored as is. */
@Serializable
internal data class TokensPayload(val accessToken: String, val refreshToken: String) {
    override fun toString(): String = "TokensPayload(***)"
}
