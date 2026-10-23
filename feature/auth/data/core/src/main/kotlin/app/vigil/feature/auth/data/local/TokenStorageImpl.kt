package app.vigil.feature.auth.data.local

import androidx.datastore.core.DataStore
import app.vigil.common.core.AppResult
import app.vigil.common.datastore.safeStoreCall
import app.vigil.common.network.AuthTokens
import app.vigil.common.network.TokenStorage
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * The tokens for Ktor's bearer auth, in the auth feature's encrypted DataStore. Failures are logged
 * by safeStoreCall.
 */
internal class TokenStorageImpl @Inject constructor(private val store: DataStore<AuthSession>) : TokenStorage {

    override suspend fun read(): AuthTokens? = when (val result = safeStoreCall { store.data.first() }) {
        is AppResult.Success -> result.data.toTokens()
        is AppResult.Failure -> null // unreadable right now: the request goes out without a token
    }

    override suspend fun save(tokens: AuthTokens) {
        safeStoreCall {
            store.updateData { AuthSession(accessToken = tokens.accessToken, refreshToken = tokens.refreshToken) }
        }
    }

    override suspend fun clear() {
        safeStoreCall { store.updateData { AuthSession() } }
    }
}

private fun AuthSession.toTokens(): AuthTokens? =
    if (accessToken != null && refreshToken != null) AuthTokens(accessToken, refreshToken) else null
