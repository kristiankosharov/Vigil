package app.vigil.feature.documents.data.local

import app.vigil.common.security.AesGcm
import app.vigil.common.security.RsaKeyWrapper
import app.vigil.common.security.Vault
import java.util.Base64
import javax.inject.Inject
import kotlinx.serialization.json.Json

/**
 * Envelope encryption for documents. Each document has a random AES-256 key that encrypts its pages;
 * that key is wrapped by the Keystore RSA key [KEY_ALIAS] and the result sealed with the vault key, so
 * opening a page needs both the Keystore and an unlocked app. Every sealed value is bound to its
 * document id (and page number), so nothing can be swapped between documents.
 */
internal class DocumentCrypto @Inject constructor(private val vault: Vault, private val rsa: RsaKeyWrapper) {

    fun newKey(): ByteArray = AesGcm.newKey()

    fun wrapKey(id: String, key: ByteArray): String = vault.seal(rsa.wrap(KEY_ALIAS, key), keyAad(id)).encode()

    fun unwrapKey(document: StoredDocument): ByteArray =
        rsa.unwrap(document.keyAlias, vault.unseal(document.wrappedKey.decode(), keyAad(document.id)))

    fun sealPage(key: ByteArray, id: String, index: Int, page: ByteArray): ByteArray =
        AesGcm.seal(key, page, pageAad(id, index))

    fun openPage(key: ByteArray, id: String, index: Int, sealed: ByteArray): ByteArray =
        AesGcm.open(key, sealed, pageAad(id, index))

    fun sealMetadata(id: String, metadata: DocumentMetadata): String {
        val json = Json.encodeToString(DocumentMetadata.serializer(), metadata)
        return vault.seal(json.encodeToByteArray(), metadataAad(id)).encode()
    }

    fun openMetadata(document: StoredDocument): DocumentMetadata {
        val json = vault.unseal(document.sealedMetadata.decode(), metadataAad(document.id)).decodeToString()
        return Json.decodeFromString(DocumentMetadata.serializer(), json)
    }

    fun deleteKeys() = rsa.delete(KEY_ALIAS)

    companion object {
        /** Permanent while documents wrapped by it exist; a new version starts a new alias. */
        const val KEY_ALIAS = "documents_kek_v1"

        private fun keyAad(id: String) = "vigil.documents.key:$id".encodeToByteArray()

        private fun metadataAad(id: String) = "vigil.documents.metadata:$id".encodeToByteArray()

        private fun pageAad(id: String, index: Int) = "vigil.documents.page:$id:$index".encodeToByteArray()
    }
}

private fun ByteArray.encode(): String = Base64.getEncoder().encodeToString(this)

private fun String.decode(): ByteArray = Base64.getDecoder().decode(this)
