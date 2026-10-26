package app.vigil.feature.documents.data

import app.vigil.common.core.AppResult
import kotlinx.coroutines.flow.Flow

/** The documents in the wallet, stored on this device only (Room is the source of truth). */
public interface DocumentsRepository {
    /** Newest first. */
    public fun observeDocuments(): Flow<AppResult<List<DocumentData>>>

    // TODO(documents): add `suspend fun addDocument(...)` with the scan flow (design 3.1–3.2).
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

public enum class DocumentStatusData { ToSign, Signed, Draft }
