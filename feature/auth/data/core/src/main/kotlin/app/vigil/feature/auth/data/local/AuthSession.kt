package app.vigil.feature.auth.data.local

import kotlinx.serialization.Serializable

/** The auth feature's encrypted store (`auth_session.json`). Every field has a default. */
@Serializable
internal data class AuthSession(val accessToken: String? = null, val refreshToken: String? = null) {
    // Tokens must never reach a log through the generated toString().
    override fun toString(): String = "AuthSession(hasTokens=${accessToken != null && refreshToken != null})"
}
