package app.vigil.feature.auth.data.local

import app.vigil.common.datastore.encryptedDataStore
import app.vigil.common.network.AuthTokens
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin

class TokenStorageImplTest : FunSpec({
    AeadConfig.register()

    fun newAead(): Aead = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
        .getPrimitive(RegistryConfiguration.get(), Aead::class.java)

    val tokens = AuthTokens(accessToken = "access", refreshToken = "refresh")

    test("an empty store has no tokens") {
        withStorage(File(tempdir(), "auth_session.json"), newAead()) { it.read() shouldBe null }
    }

    test("saved tokens are read back, and clear removes them") {
        withStorage(File(tempdir(), "auth_session.json"), newAead()) { storage ->
            storage.save(tokens)
            storage.read() shouldBe tokens

            storage.clear()
            storage.read() shouldBe null
        }
    }

    test("tokens survive a new store instance on the same file and key") {
        val file = File(tempdir(), "auth_session.json")
        val aead = newAead()
        withStorage(file, aead) { it.save(tokens) }

        withStorage(file, aead) { it.read() shouldBe tokens }
    }

    test("a file encrypted with another key resets to defaults") {
        val file = File(tempdir(), "auth_session.json")
        withStorage(file, newAead()) { it.save(tokens) }

        withStorage(file, newAead()) { it.read() shouldBe null }
    }

    test("a file encrypted under another file name resets to defaults") {
        val dir = tempdir()
        val aead = newAead()
        val original = File(dir, "auth_session.json")
        withStorage(original, aead) { it.save(tokens) }

        val renamed = File(dir, "auth_renamed.json")
        original.renameTo(renamed)
        withStorage(renamed, aead) { it.read() shouldBe null }
    }
})

/** One store instance per file at a time: the scope is cancelled (and the file released) afterwards. */
private suspend fun withStorage(file: File, aead: Aead, block: suspend (TokenStorageImpl) -> Unit) {
    val job = Job()
    val store =
        encryptedDataStore(file, AuthSession.serializer(), AuthSession(), aead, CoroutineScope(Dispatchers.IO + job))
    try {
        block(TokenStorageImpl(store))
    } finally {
        job.cancelAndJoin()
    }
}
