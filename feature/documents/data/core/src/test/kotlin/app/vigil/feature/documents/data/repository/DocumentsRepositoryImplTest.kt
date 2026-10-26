package app.vigil.feature.documents.data.repository

import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.datastore.encryptedDataStore
import app.vigil.common.security.AesGcm
import app.vigil.common.security.RsaKeyWrapper
import app.vigil.common.security.Vault
import app.vigil.common.security.VaultLockedException
import app.vigil.feature.documents.data.DocumentData
import app.vigil.feature.documents.data.DocumentStatusData
import app.vigil.feature.documents.data.NewDocumentData
import app.vigil.feature.documents.data.local.DocumentCrypto
import app.vigil.feature.documents.data.local.DocumentFiles
import app.vigil.feature.documents.data.local.DocumentMetadata
import app.vigil.feature.documents.data.local.DocumentsIndex
import app.vigil.feature.documents.data.local.StoredDocument
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import javax.crypto.Cipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class DocumentsRepositoryImplTest : FunSpec({
    AeadConfig.register()
    val aead: Aead = KeysetHandle.newBuilder()
        .addEntry(
            KeysetHandle.generateEntryFromParameters(PredefinedAeadParameters.AES256_GCM).withRandomId().makePrimary(),
        )
        .build()
        .getPrimitive(RegistryConfiguration.get(), Aead::class.java)

    fun rental(addedAt: Long = 1_000L) = NewDocumentData(
        title = "Rental agreement",
        status = DocumentStatusData.ToSign,
        pages = listOf("page one".encodeToByteArray(), "page two".encodeToByteArray()),
        addedAt = addedAt,
        searchText = "landlord",
    )

    test("while locked nothing is emitted; once unlocked the documents come newest first") {
        Wallet(tempdir(), aead).run {
            repository.addDocument(rental(addedAt = 1_000L))
            repository.addDocument(rental(addedAt = 2_000L))
            vault.locked = true

            repository.observeDocuments().test {
                expectNoEvents()
                vault.locked = false
                val documents = awaitItem().shouldBeInstanceOf<AppResult.Success<List<DocumentData>>>().data
                documents.map(DocumentData::addedAt) shouldBe listOf(2_000L, 1_000L)
                documents.first().let {
                    it.title shouldBe "Rental agreement"
                    it.status shouldBe DocumentStatusData.ToSign
                    it.pageCount shouldBe 2
                    it.searchText shouldBe "landlord"
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("pages are stored encrypted and read back decrypted") {
        val dir = tempdir()
        Wallet(dir, aead).run {
            val id = repository.addDocument(rental()).shouldBeInstanceOf<AppResult.Success<String>>().data

            repository.readPage(id, 1).shouldBeInstanceOf<AppResult.Success<ByteArray>>().data.decodeToString() shouldBe
                "page two"
            File(dir, "documents/$id/1.bin").readText() shouldNotContain "page two"
        }
    }

    test("an unknown document or page is NotFound") {
        Wallet(tempdir(), aead).run {
            val id = repository.addDocument(rental()).shouldBeInstanceOf<AppResult.Success<String>>().data

            repository.readPage("nope", 0) shouldBe AppResult.Failure(AppError.Local.NotFound)
            repository.readPage(id, 2) shouldBe AppResult.Failure(AppError.Local.NotFound)
        }
    }

    test("a page moved to another document doesn't decrypt") {
        val dir = tempdir()
        Wallet(dir, aead).run {
            val first = repository.addDocument(rental()).shouldBeInstanceOf<AppResult.Success<String>>().data
            val second = repository.addDocument(rental()).shouldBeInstanceOf<AppResult.Success<String>>().data
            File(dir, "documents/$first/0.bin").copyTo(File(dir, "documents/$second/0.bin"), overwrite = true)

            repository.readPage(second, 0).shouldBeInstanceOf<AppResult.Failure>()
        }
    }

    test("deleting forgets the key and the pages") {
        val dir = tempdir()
        Wallet(dir, aead).run {
            val id = repository.addDocument(rental()).shouldBeInstanceOf<AppResult.Success<String>>().data

            repository.deleteDocument(id) shouldBe AppResult.Success(Unit)

            store.data.first().documents.shouldBeEmpty()
            File(dir, "documents/$id").exists() shouldBe false
            repository.readPage(id, 0) shouldBe AppResult.Failure(AppError.Local.NotFound)
        }
    }

    test("adding while locked fails and leaves no pages behind") {
        val dir = tempdir()
        Wallet(dir, aead).run {
            vault.locked = true

            repository.addDocument(rental()).shouldBeInstanceOf<AppResult.Failure>()

            store.data.first().documents.shouldBeEmpty()
            (File(dir, "documents").listFiles() ?: emptyArray()).shouldBeEmpty()
        }
    }

    test("an unknown status written by a newer version reads as a draft") {
        Wallet(tempdir(), aead).run {
            val metadata = DocumentMetadata("Lease", status = "archived", pageCount = 0, addedAt = 1L, searchText = "")
            val key = crypto.newKey()
            store.updateData {
                DocumentsIndex(
                    listOf(
                        StoredDocument(
                            "x",
                            DocumentCrypto.KEY_ALIAS,
                            crypto.wrapKey("x", key),
                            crypto.sealMetadata("x", metadata),
                        ),
                    ),
                )
            }

            repository.observeDocuments().test {
                awaitItem().shouldBeInstanceOf<AppResult.Success<List<DocumentData>>>().data.single().status shouldBe
                    DocumentStatusData.Draft
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("titles and scanned text never reach the index file in plain text") {
        val dir = tempdir()
        val plainAead = object : Aead { // no outer encryption, so the test sees what the store wrote
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray) = plaintext
            override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray) = ciphertext
        }
        Wallet(dir, plainAead).run { repository.addDocument(rental()) }

        File(dir, "documents_index.json").readText().let {
            it shouldNotContain "Rental"
            it shouldNotContain "landlord"
        }
    }
})

/** The vault from :common:security without Hilt; [locked] closes it. */
private class TestVault : Vault {
    private val key = AesGcm.newKey()
    override val isOpen = MutableStateFlow(true)
    var locked: Boolean
        get() = !isOpen.value
        set(value) {
            isOpen.value = !value
        }

    override fun seal(plaintext: ByteArray, associatedData: ByteArray): ByteArray {
        if (locked) throw VaultLockedException()
        return AesGcm.seal(key, plaintext, associatedData)
    }

    override fun unseal(sealed: ByteArray, associatedData: ByteArray): ByteArray {
        if (locked) throw VaultLockedException()
        return AesGcm.open(key, sealed, associatedData)
    }
}

/** A software RSA key pair per alias in place of the Keystore. */
private class TestRsa : RsaKeyWrapper {
    private val pairs = mutableMapOf<String, KeyPair>()

    private fun pair(alias: String) =
        pairs.getOrPut(alias) { KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair() }

    override fun wrap(alias: String, key: ByteArray): ByteArray =
        Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").run {
            init(Cipher.ENCRYPT_MODE, pair(alias).public)
            doFinal(key)
        }

    override fun unwrap(alias: String, wrapped: ByteArray): ByteArray =
        Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").run {
            init(Cipher.DECRYPT_MODE, pair(alias).private)
            doFinal(wrapped)
        }

    override fun delete(alias: String) {
        pairs.remove(alias)
    }
}

/** One app process over [dir]: the index file and the pages folder. */
private class Wallet(dir: File, aead: Aead) {
    val job = Job()
    val store = encryptedDataStore(
        File(dir, "documents_index.json"),
        DocumentsIndex.serializer(),
        DocumentsIndex(),
        aead,
        CoroutineScope(Dispatchers.IO + job),
    )
    val vault = TestVault()
    val crypto = DocumentCrypto(vault, TestRsa())
    val repository = DocumentsRepositoryImpl(store, DocumentFiles(File(dir, "documents")), crypto, vault)

    suspend fun <T> run(block: suspend Wallet.() -> T): T = try {
        block()
    } finally {
        job.cancelAndJoin()
    }
}
