package app.vigil.feature.auth.data.remote

import app.vigil.feature.auth.data.OidcProviderData

/**
 * The OpenID Connect providers on the login screen, signed in through Firebase (`OAuthProvider`).
 * Empty: none is set up yet, so the screen shows no SSO button.
 *
 * TODO(oidc): to add a provider:
 *  1. Upgrade the Firebase project to Identity Platform (Authentication → Settings); plain Firebase
 *     Auth has no OpenID Connect.
 *  2. Authentication → Sign-in method → Add new provider → OpenID Connect: name, client ID, issuer
 *     URL (and client secret for the code flow). Firebase names it `oidc.<name>`.
 *  3. In the identity provider, allow the redirect URI
 *     `https://vigil-app-25b0.firebaseapp.com/__/auth/handler`.
 *  4. Add it below, e.g. `OidcProviderData(id = "oidc.company", displayName = "Company SSO")`.
 *  5. Set the scopes it needs in `AuthRepositoryImpl.signInWithOidc`.
 *  6. Later, if the list must change without a release: read it from Remote Config here.
 */
internal val OidcProviders: List<OidcProviderData> = emptyList()
