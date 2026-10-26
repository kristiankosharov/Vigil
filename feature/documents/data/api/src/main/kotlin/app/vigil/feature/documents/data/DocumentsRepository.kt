package app.vigil.feature.documents.data

import app.vigil.common.core.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * The documents in the wallet, on this device only. Every document has its own AES-256 key (envelope
 * encryption): its pages are encrypted with that key, and the key is wrapped by a Keystore RSA key and
 * then by the vault key, so nothing is readable while the app is locked.
 */
public interface DocumentsRepository {
    /** Newest first. Emits only while the app is unlocked. */
    public fun observeDocuments(): Flow<AppResult<List<DocumentData>>>

    /** Encrypts and stores a new document; returns its id. Needs the app unlocked. */
    public suspend fun addDocument(document: NewDocumentData): AppResult<String>

    /** The decrypted bytes of page [index] (from 0); `AppError.Local.NotFound` for an unknown id or page. */
    public suspend fun readPage(id: String, index: Int): AppResult<ByteArray>

    /** Forgets the document's key first, so its pages can never be decrypted again, then deletes them. */
    public suspend fun deleteDocument(id: String): AppResult<Unit>
}

public data class DocumentData(
    val id: String,
    val title: String,
    val status: DocumentStatusData,
    val pageCount: Int,
    /** Epoch milliseconds. */
    val addedAt: Long,
    /** Parties and text found in the scan. */
    val searchText: String,
)

/** A scanned document to store. [pages] are the encoded page images, in order. */
public class NewDocumentData(
    public val title: String,
    public val status: DocumentStatusData,
    public val pages: List<ByteArray>,
    /** Epoch milliseconds. */
    public val addedAt: Long,
    public val searchText: String,
)

public enum class DocumentStatusData { ToSign, Signed, Draft }
