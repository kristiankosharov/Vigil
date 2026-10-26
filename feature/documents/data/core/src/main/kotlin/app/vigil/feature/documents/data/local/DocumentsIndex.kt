package app.vigil.feature.documents.data.local

import kotlinx.serialization.Serializable

/**
 * The documents feature's encrypted store (`documents_index.json`): one entry per document. Every
 * field has a default. Nothing in it is readable without the vault key: titles and scanned text are
 * sealed with it, and each document's own key is wrapped by RSA and then by it.
 */
@Serializable
internal data class DocumentsIndex(val documents: List<StoredDocument> = emptyList()) {
    override fun toString(): String = "DocumentsIndex(${documents.size} documents)"
}

/** Base64 fields. [keyAlias] names the Keystore RSA key pair that wrapped [wrappedKey]. */
@Serializable
internal data class StoredDocument(
    val id: String,
    val keyAlias: String,
    val wrappedKey: String,
    val sealedMetadata: String,
) {
    override fun toString(): String = "StoredDocument($id)"
}

/** What [StoredDocument.sealedMetadata] holds once unsealed. [status] uses stable names (`to_sign`, …). */
@Serializable
internal data class DocumentMetadata(
    val title: String,
    val status: String,
    val pageCount: Int,
    val addedAt: Long,
    val searchText: String,
) {
    override fun toString(): String = "DocumentMetadata(***)"
}
