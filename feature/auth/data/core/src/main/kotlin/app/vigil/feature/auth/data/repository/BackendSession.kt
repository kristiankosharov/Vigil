package app.vigil.feature.auth.data.repository

import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.core.map
import app.vigil.common.network.TokenStorage
import app.vigil.feature.auth.data.remote.AuthApi
import app.vigil.feature.auth.data.remote.safeFirebaseCall
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * After a Firebase sign-in: trades Firebase's ID token for the backend's own tokens and saves them
 * in [TokenStorage] (sealed with the vault key once the PIN exists). The backend is a fake for now
 * ([AuthApi]).
 */
// TODO(auth): secure Firebase's refresh token. The Firebase SDK keeps it in its own storage, outside
//  the vault, so the PIN doesn't protect it and anyone with the app's files can mint new ID tokens.
//  Either sign out of Firebase once the backend tokens are saved (the backend session replaces it),
//  or keep the Firebase session and protect that storage.
internal class BackendSession @Inject constructor(
    private val auth: FirebaseAuth,
    private val api: AuthApi,
    private val tokens: TokenStorage,
) {
    suspend fun start(): AppResult<Unit> {
        val exchanged = when (val idToken = safeFirebaseCall { auth.currentUser?.getIdToken(false)?.await()?.token }) {
            is AppResult.Failure -> idToken
            is AppResult.Success -> idToken.data?.let { api.exchangeFirebaseToken(it) } ?: noIdToken()
        }
        return exchanged.map { tokens.save(it) }
    }

    private fun noIdToken() =
        AppResult.Failure(AppError.Auth.Unknown(IllegalStateException("Signed in without an ID token")))
}
