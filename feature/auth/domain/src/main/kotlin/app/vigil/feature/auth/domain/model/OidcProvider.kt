package app.vigil.feature.auth.domain.model

/** An OpenID Connect provider offered on the login screen; [displayName] is its brand name. */
public data class OidcProvider(val id: String, val displayName: String)
