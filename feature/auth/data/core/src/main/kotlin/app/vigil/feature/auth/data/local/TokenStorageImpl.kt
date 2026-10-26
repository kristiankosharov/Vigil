package app.vigil.feature.auth.data.local

import androidx.datastore.core.DataStore
import app.vigil.common.core.AppResult
import app.vigil.common.core.ApplicationScope
import app.vigil.common.datastore.safeStoreCall
import app.vigil.common.network.AuthTokens
import app.vigil.common.network.TokenStorage
import app.vigil.common.security.Vault
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * The tokens for Ktor's bearer auth, sealed with the vault key in the auth feature's encrypted
 * DataStore. While the app is locked there are no tokens, so nothing reaches the backend.
 *
 * Sign-in comes before Create PIN, when there is no vault key yet: tokens saved then wait in memory
 * only and are sealed as soon as the vault opens (the PIN is created). If the process dies first,
 * they are gone and the user signs in again. Failures are logged by safeStoreCall.
 */
@Singleton
internal class TokenStorageImpl @Inject constructor(
    private val store: DataStore<AuthSession>,
    private val vault: Vault,
    @ApplicationScope scope: CoroutineScope,
) : TokenStorage {
    private val lock = Mutex()
    private var pending: AuthTokens? = null

    init {
        scope.launch { vault.isOpen.filter { it }.collect { lock.withLock { sealPending() } } }
    }

    override suspend fun read(): AuthTokens? = lock.withLock {
        sealPending()
        pending?.let { return@withLock it }
        if (!vault.isOpen.value) return@withLock null
        when (val result = safeStoreCall { store.data.first().sealedTokens?.let(::unseal) }) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> null // unreadable or locked right now: the request goes out without a token
        }
    }

    override suspend fun save(tokens: AuthTokens) {
        lock.withLock {
            pending = tokens
            sealPending()
        }
    }

    override suspend fun clear() {
        lock.withLock {
            pending = null
            safeStoreCall { store.updateData { AuthSession() } }
        }
    }

    /** Seals the waiting tokens if the vault is open; on a failure they keep waiting. */
    private suspend fun sealPending() {
        val tokens = pending ?: return
        if (!vault.isOpen.value) return
        val result = safeStoreCall { store.updateData { AuthSession(sealedTokens = seal(tokens)) } }
        if (result is AppResult.Success) pending = null
    }

    private fun seal(tokens: AuthTokens): String {
        val json = Json.encodeToString(
            TokensPayload.serializer(),
            TokensPayload(tokens.accessToken, tokens.refreshToken),
        )
        return Base64.getEncoder().encodeToString(vault.seal(json.encodeToByteArray(), TOKENS_AAD))
    }

    private fun unseal(sealed: String): AuthTokens {
        val json = vault.unseal(Base64.getDecoder().decode(sealed), TOKENS_AAD).decodeToString()
        val payload = Json.decodeFromString(TokensPayload.serializer(), json)
        return AuthTokens(payload.accessToken, payload.refreshToken)
    }

    private companion object {
        val TOKENS_AAD = "vigil.auth.tokens".encodeToByteArray()
    }
}
