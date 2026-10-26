package app.vigil.feature.auth.data.local

import app.vigil.common.datastore.encryptedDataStore
import app.vigil.common.network.AuthTokens
import app.vigil.common.security.AesGcm
import app.vigil.common.security.Vault
import app.vigil.common.security.VaultLockedException
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class TokenStorageImplTest : FunSpec({
    AeadConfig.register()

    val aead: Aead = KeysetHandle.newBuilder()
        .addEntry(
            KeysetHandle.generateEntryFromParameters(PredefinedAeadParameters.AES256_GCM).withRandomId().makePrimary(),
        )
        .build()
        .getPrimitive(RegistryConfiguration.get(), Aead::class.java)

    val tokens = AuthTokens(accessToken = "access-secret", refreshToken = "refresh-secret")

    fun file() = File(tempdir(), "auth_session.json")

    test("an empty store has no tokens") {
        withStorage(file(), aead, TestVault(AesGcm.newKey())) { storage, _ -> storage.read().shouldBeNull() }
    }

    test("saved tokens are read back while unlocked, and clear removes them") {
        withStorage(file(), aead, TestVault(AesGcm.newKey())) { storage, store ->
            storage.save(tokens)
            storage.read() shouldBe tokens
            store.data.first().sealedTokens.shouldNotBeNull()

            storage.clear()
            storage.read().shouldBeNull()
        }
    }

    test("tokens saved before the vault exists wait in memory and are sealed once it opens") {
        val vault = TestVault(key = null)
        withStorage(file(), aead, vault) { storage, store ->
            storage.save(tokens)
            storage.read() shouldBe tokens // the backend can still be called during onboarding
            store.data.first().sealedTokens.shouldBeNull()

            vault.open(AesGcm.newKey())
            storage.read() shouldBe tokens
            store.data.first().sealedTokens.shouldNotBeNull()
        }
    }

    test("while locked there are no tokens; after unlocking with the same key they are back") {
        val file = file()
        val key = AesGcm.newKey()
        withStorage(file, aead, TestVault(key)) { storage, _ -> storage.save(tokens) }

        val vault = TestVault(key = null)
        withStorage(file, aead, vault) { storage, _ ->
            storage.read().shouldBeNull()
            vault.open(key)
            storage.read() shouldBe tokens
        }
    }

    test("another vault key can't read them") {
        val file = file()
        withStorage(file, aead, TestVault(AesGcm.newKey())) { storage, _ -> storage.save(tokens) }

        withStorage(file, aead, TestVault(AesGcm.newKey())) { storage, _ -> storage.read().shouldBeNull() }
    }

    test("the tokens never reach the file in plain text") {
        val file = file()
        val plainAead = object : Aead { // no outer encryption, so the test sees what the store wrote
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray) = plaintext
            override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray) = ciphertext
        }
        withStorage(file, plainAead, TestVault(AesGcm.newKey())) { storage, _ -> storage.save(tokens) }

        file.readText().let {
            it shouldNotContain "access-secret"
            it shouldNotContain "refresh-secret"
        }
    }
})

/** The vault from :common:security without Hilt; [key] null starts it closed. */
private class TestVault(key: ByteArray?) : Vault {
    private var key: ByteArray? = key
    override val isOpen = MutableStateFlow(key != null)

    fun open(key: ByteArray) {
        this.key = key
        isOpen.value = true
    }

    override fun seal(plaintext: ByteArray, associatedData: ByteArray) =
        AesGcm.seal(key ?: throw VaultLockedException(), plaintext, associatedData)

    override fun unseal(sealed: ByteArray, associatedData: ByteArray) =
        AesGcm.open(key ?: throw VaultLockedException(), sealed, associatedData)
}

/** One store instance per file at a time: the scope is cancelled (and the file released) afterwards. */
private suspend fun withStorage(
    file: File,
    aead: Aead,
    vault: Vault,
    block: suspend (TokenStorageImpl, androidx.datastore.core.DataStore<AuthSession>) -> Unit,
) {
    val job = Job()
    val scope = CoroutineScope(Dispatchers.IO + job)
    val store = encryptedDataStore(file, AuthSession.serializer(), AuthSession(), aead, scope)
    try {
        block(TokenStorageImpl(store, vault, scope), store)
    } finally {
        job.cancelAndJoin()
    }
}
